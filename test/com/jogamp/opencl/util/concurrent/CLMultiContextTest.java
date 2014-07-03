/*
 * Created on Tuesday, May 03 2011
 */
package com.jogamp.opencl.util.concurrent;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.test.util.MiscUtils;
import com.jogamp.opencl.test.util.UITestCase;
import com.jogamp.opencl.util.concurrent.CLQueueContext.CLSimpleQueueContext;
import com.jogamp.opencl.util.concurrent.CLQueueContextFactory.CLSimpleContextFactory;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.runners.MethodSorters;

import com.jogamp.opencl.util.CLMultiContext;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.System.*;

/**
 *
 * @author Michael Bien, et.al
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CLMultiContextTest extends UITestCase {

    @Rule
    public Timeout methodTimeout= new Timeout(10000);

    @Test
    public void createMultiContextTest() {

        final CLMultiContext mc = CLMultiContext.create(CLPlatform.listCLPlatforms());

        try{
            final List<CLContext> contexts = mc.getContexts();
            final List<CLDevice> devices = mc.getDevices();

            assertFalse(contexts.isEmpty());
            assertFalse(devices.isEmpty());

            for (final CLContext context : contexts) {
                out.println(context);
            }
            for (final CLDevice device : devices) {
                out.println(device);
            }

        }finally{
            mc.release();
        }

    }

    private final static String programSource =
          "kernel void compute(global int* array, int numElements) { \n"
        + "    int index = get_global_id(0);                         \n"
        + "    if (index >= numElements)  {                          \n"
        + "        return;                                           \n"
        + "    }                                                     \n"
        + "    array[index]++;                                       \n"
        + "}                                                         \n";

    private final class CLTestTask implements CLTask<CLSimpleQueueContext, Buffer> {

        private final Buffer data;

        public CLTestTask(final Buffer buffer) {
            this.data = buffer;
        }

        public Buffer execute(final CLSimpleQueueContext qc) {

            final CLCommandQueue queue = qc.getQueue();
            final CLContext context = qc.getCLContext();
            final CLKernel kernel = qc.getKernel("compute");

            CLBuffer<Buffer> buffer = null;
            try{
                buffer = context.createBuffer(data);
                final int gws = buffer.getCLCapacity();

                kernel.putArg(buffer).putArg(gws).rewind();

                queue.putWriteBuffer(buffer, true);
                queue.put1DRangeKernel(kernel, 0, gws, 0);
                queue.putReadBuffer(buffer, true);
            }finally{
                if(buffer != null) {
                    buffer.release();
                }
            }

            return data;
        }

    }

    @Test
    public void commandQueuePoolTest() throws InterruptedException, ExecutionException {

        final CLMultiContext mc = CLMultiContext.create(CLPlatform.listCLPlatforms());

        try {

            CLSimpleContextFactory factory = CLQueueContextFactory.createSimple(programSource);
            final CLCommandQueuePool<CLSimpleQueueContext> pool = CLCommandQueuePool.create(factory, mc);

            assertTrue(pool.getSize() > 0);

            final int slice = 64;
            final int tasksPerQueue = 10;
            final int taskCount = pool.getSize() * tasksPerQueue;

            final IntBuffer data = Buffers.newDirectIntBuffer(slice*taskCount);

            final List<CLTestTask> tasks = new ArrayList<CLTestTask>(taskCount);

            for (int i = 0; i < taskCount; i++) {
                final IntBuffer subBuffer = Buffers.slice(data, i*slice, slice);
                assertEquals(slice, subBuffer.capacity());
                tasks.add(new CLTestTask(subBuffer));
            }

            out.println("invoking "+tasks.size()+" tasks on "+pool.getSize()+" queues");

            // blocking invoke
            pool.invokeAll(tasks);
            checkBuffer(1, data);

            // submit blocking emediatly
            for (final CLTestTask task : tasks) {
                pool.submit(task).get();
            }
            checkBuffer(2, data);

            // submitAll using futures
            final List<Future<Buffer>> futures = pool.submitAll(tasks);
            for (final Future<Buffer> future : futures) {
                future.get();
            }
            checkBuffer(3, data);

            // switching contexts using different program
            factory = CLQueueContextFactory.createSimple(programSource.replaceAll("\\+\\+", "--"));
            pool.switchContext(factory);
            pool.invokeAll(tasks);
            checkBuffer(2, data);

            pool.release();
        }finally{
            mc.release();
        }
    }

    private void checkBuffer(final int expected, final IntBuffer data) {
        while(data.hasRemaining()) {
            assertEquals(expected, data.get());
        }
        data.rewind();
    }

    public static void main(final String[] args) throws IOException {
        final String tstname = CLMultiContextTest.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
