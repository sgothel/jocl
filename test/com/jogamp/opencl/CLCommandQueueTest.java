/*
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package com.jogamp.opencl;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.runners.MethodSorters;

import java.util.concurrent.CountDownLatch;

import com.jogamp.opencl.test.util.MiscUtils;
import com.jogamp.opencl.test.util.UITestCase;
import com.jogamp.opencl.util.MultiQueueBarrier;
import com.jogamp.opencl.CLCommandQueue.Mode;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.util.CLDeviceFilters;
import com.jogamp.opencl.util.CLPlatformFilters;
import com.jogamp.opencl.llb.CL;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.jogamp.opencl.test.util.MiscUtils.*;
import static com.jogamp.opencl.CLEvent.*;
import static com.jogamp.opencl.CLVersion.*;
import static com.jogamp.common.nio.Buffers.*;
import static com.jogamp.opencl.CLCommandQueue.Mode.*;

/**
 * @author Michael Bien, et.al.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CLCommandQueueTest extends UITestCase {

    @Rule
    public Timeout methodTimeout = new Timeout(20000);

    @Test
    public void enumsTest() {

        //CLCommandQueueEnums
        final EnumSet<Mode> queueMode = Mode.valuesOf(CL.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE | CL.CL_QUEUE_PROFILING_ENABLE);
        assertTrue(queueMode.contains(OUT_OF_ORDER_MODE));
        assertTrue(queueMode.contains(PROFILING_MODE));

        assertNotNull(Mode.valuesOf(0));
        assertEquals(0, Mode.valuesOf(0).size());
        for (final Mode mode : Mode.values()) {
            assertEquals(mode, Mode.valueOf(mode.QUEUE_MODE));
        }

        // CLEvent enums
        for (final ProfilingCommand cmd : ProfilingCommand.values()) {
            assertEquals(cmd, ProfilingCommand.valueOf(cmd.COMMAND));
        }

        for (final CommandType type : CommandType.values()) {
            assertEquals(type, CommandType.valueOf(type.TYPE));
        }

        for (final ExecutionStatus status : ExecutionStatus.values()) {
            assertEquals(status, ExecutionStatus.valueOf(status.STATUS));
        }

    }

    @Test
    public void eventsTest() throws IOException {

        out.println(" - - - event synchronization test - - - ");

        final CLContext context = CLContext.create();

        try{
            final CLDevice device = context.getDevices()[0];
            final int groupSize = device.getMaxWorkItemSizes()[0];

            final int elements = roundUp(groupSize, ONE_MB / SIZEOF_INT * 5); // 5MB per buffer

            final CLBuffer<ByteBuffer> clBufferA = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
            final CLBuffer<ByteBuffer> clBufferB = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
            final CLBuffer<ByteBuffer> clBufferC = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
            final CLBuffer<ByteBuffer> clBufferD = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);

            fillBuffer(clBufferA.buffer, 12345);
            fillBuffer(clBufferB.buffer, 67890);

            final CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl")).build();
            final CLKernel vectorAddKernel = program.createCLKernel("VectorAddGM").setArg(3, elements);
            final CLCommandQueue queue = device.createCommandQueue();

            out.println(queue);

            final CLEventList events = new CLEventList(2);

            out.println(events);

            assertEquals(0, events.size());

            queue.putWriteBuffer(clBufferA, false, events) // write A
                 .putWriteBuffer(clBufferB, false, events);// write B

            out.println(events);

            assertEquals(2, events.size());
            queue.putWaitForEvents(events, true);

            events.release();
            assertEquals(0, events.size());

            vectorAddKernel.setArgs(clBufferA, clBufferB, clBufferC); // C = A+B
            queue.put1DRangeKernel(vectorAddKernel, 0, elements, groupSize, events);

            vectorAddKernel.setArgs(clBufferA, clBufferB, clBufferD); // D = A+B
            queue.put1DRangeKernel(vectorAddKernel, 0, elements, groupSize, events);

            assertEquals(2, events.size());
            queue.putWaitForEvent(events, 0, true)
                 .putWaitForEvent(events, 1, true);

            events.release();

            queue.putReadBuffer(clBufferC, false, events)
                 .putReadBuffer(clBufferD, false, events);

            queue.putWaitForEvents(events, true);

            events.release();

            checkIfEqual(clBufferC.buffer, clBufferD.buffer, elements);
            out.println("results are valid");
        }finally{
            context.release();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void eventConditionsTest() throws IOException {

        out.println(" - - - event conditions test - - - ");

        final CLPlatform platform = CLPlatform.getDefault(CLPlatformFilters.queueMode(OUT_OF_ORDER_MODE));

        CLDevice device = null;
        // we can still test this with in-order queues
        if(platform == null) {
            device = CLPlatform.getDefault().getMaxFlopsDevice();
        }else{
            device = platform.getMaxFlopsDevice(CLDeviceFilters.queueMode(OUT_OF_ORDER_MODE));
        }

        final CLContext context = CLContext.create(device);

        try{

            final CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl")).build();

            final CLBuffer<IntBuffer> buffer = context.createBuffer(newDirectIntBuffer(new int[]{ 1,1,1, 1,1,1, 1,1,1 }));

            final int elements = buffer.getNIOCapacity();

            CLCommandQueue queue;
            if(device.getQueueProperties().contains(OUT_OF_ORDER_MODE)) {
                queue = device.createCommandQueue(OUT_OF_ORDER_MODE);
            }else{
                queue = device.createCommandQueue();
            }

            // simulate in-order queue by accumulating events of prior commands
            final CLEventList events = new CLEventList(3);

            // (1+1)*2 = 4; conditions enforce propper order
            final CLKernel addKernel = program.createCLKernel("add").putArg(buffer).putArg(1).putArg(elements);
            final CLKernel mulKernel = program.createCLKernel("mul").putArg(buffer).putArg(2).putArg(elements);

            queue.putWriteBuffer(buffer, false, events);

            queue.put1DRangeKernel(addKernel, 0, elements, 1, events, events);
            queue.put1DRangeKernel(mulKernel, 0, elements, 1, events, events);

            queue.putReadBuffer(buffer, false, events, null);

            queue.finish();

            events.release();

            for (int i = 0; i < elements; i++) {
                assertEquals(4, buffer.getBuffer().get(i));
            }

        }finally{
            context.release();
        }

    }

    @Test
    public void profilingEventsTest() throws IOException {

        out.println(" - - - event synchronization test - - - ");

        final CLContext context = CLContext.create();

        try {
            final CLDevice device = context.getDevices()[0];
            final int groupSize = device.getMaxWorkItemSizes()[0];

            final int elements = roundUp(groupSize, ONE_MB / SIZEOF_INT * 5); // 5MB per buffer

            final CLBuffer<ByteBuffer> clBufferA = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
            final CLBuffer<ByteBuffer> clBufferB = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
            final CLBuffer<ByteBuffer> clBufferC = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);

            fillBuffer(clBufferA.buffer, 12345);
            fillBuffer(clBufferB.buffer, 67890);

            final CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl")).build();
            final CLKernel vectorAddKernel = program.createCLKernel("VectorAddGM").setArg(3, elements);
            final CLCommandQueue queue = device.createCommandQueue(PROFILING_MODE);

            out.println(queue);

            queue.putWriteBuffer(clBufferA, true) // write A
                 .putWriteBuffer(clBufferB, true);// write B

            final CLEventList events = new CLEventList(1);

            assertEquals(0, events.size());

            vectorAddKernel.setArgs(clBufferA, clBufferB, clBufferC); // C = A+B
            queue.put1DRangeKernel(vectorAddKernel, 0, elements, groupSize, events);

            assertEquals(1, events.size());
            final CLEvent probe = events.getEvent(0);
            out.println(probe);

            queue.putWaitForEvents(events, true);
            assertEquals(CLEvent.ExecutionStatus.COMPLETE, probe.getStatus());

            out.println(probe);
            final long time = probe.getProfilingInfo(CLEvent.ProfilingCommand.END)
                      - probe.getProfilingInfo(CLEvent.ProfilingCommand.START);
            out.println("time: "+time);
            assertTrue(time > 0);

            events.release();
        }finally{
            context.release();
        }

    }

    @Test
    public void customEventsTest() throws IOException, InterruptedException {
        out.println(" - - - user events test - - - ");

        final CLPlatform[] platforms = CLPlatform.listCLPlatforms();
        CLPlatform theChosenOne = platforms[0];
        for (final CLPlatform platform : platforms) {
            if(platform.isAtLeast(CL_1_1)) {
                theChosenOne = platform;
                break;
            }
        }

        if(!theChosenOne.isAtLeast(CL_1_1)) {
            out.println("test disabled, required CLVersion: "+CL_1_1+" available: "+theChosenOne.getVersion());
            return;
        }

        final CLContext context = CLContext.create(theChosenOne);

        try{
            final CLDevice device = context.getDevices()[0];
            final int groupSize = device.getMaxWorkItemSizes()[0];

            final int elements = roundUp(groupSize, ONE_MB / SIZEOF_INT * 5); // 5MB per buffer

            final CLBuffer<ByteBuffer> clBufferA = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
            final CLBuffer<ByteBuffer> clBufferB = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
            final CLBuffer<ByteBuffer> clBufferC = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);

            fillBuffer(clBufferA.buffer, 12345);
            fillBuffer(clBufferB.buffer, 67890);

            final CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl")).build();
            final CLKernel vectorAddKernel = program.createCLKernel("VectorAddGM").setArg(3, elements);
            final CLCommandQueue queue = device.createCommandQueue();

            queue.putWriteBuffer(clBufferA, true) // write A
                 .putWriteBuffer(clBufferB, true);// write B

            vectorAddKernel.setArgs(clBufferA, clBufferB, clBufferC); // C = A+B

            // the interesting part...

            final CLUserEvent condition = CLUserEvent.create(context);
            assertEquals(CommandType.USER, condition.getType());
            assertEquals(ExecutionStatus.SUBMITTED, condition.getStatus());
            out.println(condition);

            final CLEventList conditions = new CLEventList(condition);
            final CLEventList events     = new CLEventList(1);
            assertEquals(1, conditions.size());
            assertEquals(1, conditions.capacity());
            assertEquals(0, events.size());
            assertEquals(1, events.capacity());

            queue.put1DRangeKernel(vectorAddKernel, 0, elements, groupSize, conditions, events);
            assertEquals(1, events.size());

            Thread.sleep(1000);
            final CLEvent status = events.getEvent(0);

            assertEquals(ExecutionStatus.QUEUED, status.getStatus());
            condition.setComplete();
            assertTrue(condition.isComplete());

            queue.finish();
            assertTrue(status.isComplete());

        }finally{
            context.release();
        }

    }

    @Test
    public void eventCallbackTest() throws InterruptedException {

        out.println(" - - - event callback test - - - ");

        final CLPlatform platform = CLPlatform.getDefault();

        if(!platform.isAtLeast(CL_1_1)) {
            out.println("test disabled, required CLVersion: "+CL_1_1+" available: "+platform.getVersion());
            return;
        }

        final CLContext context = CLContext.create();

        try{

            final CLUserEvent customEvent = CLUserEvent.create(context);

            final CountDownLatch countdown = new CountDownLatch(1);
            customEvent.registerCallback(new CLEventListener() {
                @Override
                public void eventStateChanged(final CLEvent event, final int status) {
                    out.println("event received: "+event);
                    assertEquals(event, customEvent);
                    countdown.countDown();
                }

            });

            customEvent.setStatus(ExecutionStatus.COMPLETE);
            countdown.await(2, TimeUnit.SECONDS);
            assertEquals(countdown.getCount(), 0);

            customEvent.release();
        }finally{
            context.release();
        }

    }

    @Test
    public void concurrencyTest() throws IOException, InterruptedException {

        out.println(" - - - QueueBarrier test - - - ");

        final int elements = ONE_MB / SIZEOF_INT * 10; // 20MB per buffer

        final CLContext context = CLContext.create();

        try{

            final CLDevice[] devices = context.getDevices();

            // ignore this test if we can't test in parallel
            if (devices.length < 2) {
                out.println("aborting test... need at least 2 devices");
                return;
            }

            final CLBuffer<ByteBuffer> clBufferC = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
            final CLBuffer<ByteBuffer> clBufferD = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);

            final CLBuffer<ByteBuffer> clBufferA1 = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
            final CLBuffer<ByteBuffer> clBufferB1 = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
            final CLBuffer<ByteBuffer> clBufferA2 = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
            final CLBuffer<ByteBuffer> clBufferB2 = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);

            final CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl")).build();

            //two independent kernel instances
            final CLKernel vectorAddKernel1 = program.createCLKernel("VectorAddGM").setArg(3, elements);
            final CLKernel vectorAddKernel2 = program.createCLKernel("VectorAddGM").setArg(3, elements);

            final CLCommandQueue queue1 = devices[0].createCommandQueue();
            final CLCommandQueue queue2 = devices[1].createCommandQueue();

            out.println(queue1);
            out.println(queue2);

            fillBuffer(clBufferC.buffer, 12345);


            final MultiQueueBarrier barrier = new MultiQueueBarrier(2);

            final Thread thread1 = new Thread("C") {

                @Override
                public void run() {

                    final int groupSize = queue1.getDevice().getMaxWorkItemSizes()[0];

                    fillBuffer(clBufferA1.buffer, 12345);
                    fillBuffer(clBufferB1.buffer, 67890);

    //                System.out.println("C buffer");
                    queue1.putWriteBuffer(clBufferA1, false)  // write A
                          .putWriteBuffer(clBufferB1, false); // write B

    //                System.out.println("C args");
                    vectorAddKernel1.setArgs(clBufferA1, clBufferB1, clBufferC); // C = A+B

    //                System.out.println("C kernels");
                    final CLEventList events1 = new CLEventList(2);
                    queue1.put1DRangeKernel(vectorAddKernel1, 0, elements, groupSize, events1)
                          .putReadBuffer(clBufferC, false, events1);

                    barrier.waitFor(queue1, events1);

                }
            };

            final Thread thread2 = new Thread("D") {

                @Override
                public void run() {

                    final int maxWorkItemSize = queue2.getDevice().getMaxWorkItemSizes()[0];
                    final int kernelWorkGroupSize = (int)vectorAddKernel2.getWorkGroupSize( queue2.getDevice() );
                    final int localWorkSize = Math.min( maxWorkItemSize, kernelWorkGroupSize );

                    fillBuffer(clBufferA2.buffer, 12345);
                    fillBuffer(clBufferB2.buffer, 67890);

    //                System.out.println("D buffer");
                    queue2.putWriteBuffer(clBufferA2, false)  // write A
                          .putWriteBuffer(clBufferB2, false); // write B

    //                System.out.println("D args");
                    vectorAddKernel2.setArgs(clBufferA2, clBufferB2, clBufferD); // D = A+B

    //                System.out.println("D kernels");
                    final CLEventList events2 = new CLEventList(2);
                    queue2.put1DRangeKernel(vectorAddKernel2, 0, elements, localWorkSize, events2);
                    queue2.putReadBuffer(clBufferD, false, events2);

                    barrier.waitFor(queue2, events2);

                }
            };

            out.println("starting threads");
            thread1.start();
            thread2.start();
            assertTrue(barrier.await(5, TimeUnit.SECONDS));
            out.println("done");

            checkIfEqual(clBufferC.buffer, clBufferD.buffer, elements);
            out.println("results are valid");

        } catch (final Throwable t ) {
            t.printStackTrace();
        } finally {
            context.release();
        }

    }
    public static void main(final String[] args) throws IOException {
        final String tstname = CLCommandQueueTest.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
