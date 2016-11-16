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

import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLMemory.Map;
import com.jogamp.opencl.test.util.UITestCase;
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.Bitstream;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.jogamp.common.nio.Buffers.*;
import static com.jogamp.opencl.test.util.MiscUtils.*;
import static com.jogamp.opencl.util.CLPlatformFilters.*;
import static com.jogamp.opencl.CLVersion.*;

/**
 * @author Michael Bien, et.al.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CLBufferTest extends UITestCase {

    @Test
    public void cloneWithLimitedBufferTest() {
        final int elements = NUM_ELEMENTS;
        final int padding = 312; // Arbitrary number
        final CLContext context = CLContext.create();

        final IntBuffer hostBuffer = ByteBuffer.allocateDirect((elements + padding)*SIZEOF_INT).asIntBuffer();
        hostBuffer.limit(elements);

        final CLBuffer<?> deviceBuffer = context.createBuffer(elements*SIZEOF_INT).cloneWith(hostBuffer);
        assertEquals(elements, deviceBuffer.getCLCapacity());
        assertEquals(elements*SIZEOF_INT, deviceBuffer.getNIOSize());
        assertEquals(elements, deviceBuffer.getNIOCapacity());

        context.release();
    }

    @Test
    public void copyLimitedSlicedBuffersTest() {
        final int size = 4200*SIZEOF_INT; // Arbitrary number that is a multiple of SIZEOF_INT;
        final int padding = 307; // Totally arbitrary number > 0
        final CLContext context = CLContext.create();
        final CLCommandQueue queue = context.getDevices()[0].createCommandQueue();

        // Make a buffer that is offset relative to the originally allocated position and has a limit that is
        // not equal to the capacity to test whether all these attributes are correctly handled.
        ByteBuffer hostBuffer = ByteBuffer.allocateDirect(size + padding);
        hostBuffer.position(padding/2); // Offset the original buffer
        hostBuffer = hostBuffer.slice(); // Slice it to have a new buffer that starts at the offset
        hostBuffer.limit(size);
        hostBuffer.order(ByteOrder.nativeOrder()); // Necessary for comparisons to work later on.
        fillBuffer(hostBuffer, 12345);

        final CLBuffer<ByteBuffer> bufferA = context.createBuffer(size).cloneWith(hostBuffer);
        final CLBuffer<ByteBuffer> bufferB = context.createByteBuffer(size);

        queue.putWriteBuffer(bufferA, false)
             .putCopyBuffer(bufferA, bufferB, bufferA.getNIOSize())
             .putReadBuffer(bufferB, true).finish();

        hostBuffer.rewind();
        bufferB.buffer.rewind();
        checkIfEqual(hostBuffer, bufferB.buffer, size/SIZEOF_INT);
        context.release();
    }

    @Test
    public void createBufferTest() {

        out.println(" - - - highLevelTest; create buffer test - - - ");

        final CLContext context = CLContext.create();
        try{
            final int size = 6;

            final CLBuffer<ByteBuffer> bb = context.createByteBuffer(size);
            final CLBuffer<ShortBuffer> sb = context.createShortBuffer(size);
            final CLBuffer<IntBuffer> ib = context.createIntBuffer(size);
            final CLBuffer<LongBuffer> lb = context.createLongBuffer(size);
            final CLBuffer<FloatBuffer> fb = context.createFloatBuffer(size);
            final CLBuffer<DoubleBuffer> db = context.createDoubleBuffer(size);

            final List<CLMemory<? extends Buffer>> buffers = context.getMemoryObjects();
            assertEquals(6, buffers.size());

            assertEquals(1, bb.getElementSize());
            assertEquals(2, sb.getElementSize());
            assertEquals(4, ib.getElementSize());
            assertEquals(8, lb.getElementSize());
            assertEquals(4, fb.getElementSize());
            assertEquals(8, db.getElementSize());

            final ByteBuffer anotherNIO = newDirectByteBuffer(2);

            for (final CLMemory<? extends Buffer> memory : buffers) {

                final CLBuffer<? extends Buffer> buffer = (CLBuffer<? extends Buffer>) memory;
                final Buffer nio = buffer.getBuffer();

                assertEquals(nio.capacity(), buffer.getCLCapacity());
                assertEquals(buffer.getNIOSize(), buffer.getCLSize());
                assertEquals(sizeOfBufferElem(nio), buffer.getElementSize());
                assertEquals(nio.capacity() * sizeOfBufferElem(nio), buffer.getCLSize());

                final CLBuffer<ByteBuffer> clone = buffer.cloneWith(anotherNIO);

                assertEquals(buffer.ID, clone.ID);
                assertTrue(clone.equals(buffer));
                assertTrue(buffer.equals(clone));

                assertEquals(buffer.getCLSize(), clone.getCLCapacity());
                assertEquals(buffer.getCLSize(), clone.getCLSize());
                assertEquals(anotherNIO.capacity(), clone.getNIOCapacity());
            }

        }finally{
            context.release();
        }

    }

    @Test
    public void writeCopyReadBufferTest() {

        out.println(" - - - highLevelTest; copy buffer test - - - ");

        final int elements = NUM_ELEMENTS;

        final CLContext context = CLContext.create();

         // the CL.MEM_* flag is probably completely irrelevant in our case since we do not use a kernel in this test
        final CLBuffer<ByteBuffer> clBufferA = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);
        final CLBuffer<ByteBuffer> clBufferB = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);

        // fill only first read buffer -> we will copy the payload to the second later.
        fillBuffer(clBufferA.buffer, 12345);

        final CLCommandQueue queue = context.getDevices()[0].createCommandQueue();

        // asynchronous write of data to GPU device, blocking read later to get the computed results back.
        queue.putWriteBuffer(clBufferA, false)                                 // write A
             .putCopyBuffer(clBufferA, clBufferB, clBufferA.buffer.capacity()) // copy A -> B
             .putReadBuffer(clBufferB, true)                                   // read B
             .finish();

        context.release();

        out.println("validating computed results...");
        checkIfEqual(clBufferA.buffer, clBufferB.buffer, elements);
        out.println("results are valid");

    }

    @Test
    public void bufferWithHostPointerTest() {

        out.println(" - - - highLevelTest; host pointer test - - - ");

        final int elements = NUM_ELEMENTS;

        final CLContext context = CLContext.create();

        final ByteBuffer buffer = Buffers.newDirectByteBuffer(elements*SIZEOF_INT);
        // fill only first read buffer -> we will copy the payload to the second later.
        fillBuffer(buffer, 12345);

        final CLCommandQueue queue = context.getDevices()[0].createCommandQueue();

        final Mem[] bufferConfig = new Mem[] {Mem.COPY_BUFFER, Mem.USE_BUFFER};

        for(int i = 0; i < bufferConfig.length; i++) {

            out.println("testing with "+bufferConfig[i] + " config");

            final CLBuffer<ByteBuffer> clBufferA = context.createBuffer(buffer, Mem.READ_ONLY, bufferConfig[i]);
            final CLBuffer<ByteBuffer> clBufferB = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);

            // asynchronous write of data to GPU device, blocking read later to get the computed results back.
            queue.putCopyBuffer(clBufferA, clBufferB, clBufferA.buffer.capacity()) // copy A -> B
                 .putReadBuffer(clBufferB, true)                                   // read B
                 .finish();

            assertEquals(2, context.getMemoryObjects().size());
            clBufferA.release();
            assertEquals(1, context.getMemoryObjects().size());
            clBufferB.release();
            assertEquals(0, context.getMemoryObjects().size());

            // uploading worked when a==b.
            out.println("validating computed results...");
            checkIfEqual(clBufferA.buffer, clBufferB.buffer, elements);
            out.println("results are valid");
        }

        context.release();
    }

    @Test
    public void mapBufferTest() {

        out.println(" - - - highLevelTest; map buffer test - - - ");

        final int elements = NUM_ELEMENTS;
        final int sizeInBytes = elements*SIZEOF_INT;

        CLContext context;
        CLBuffer<?> clBufferA;
        CLBuffer<?> clBufferB;

        // We will have to allocate mappable NIO memory on non CPU contexts
        // since we can't map e.g GPU memory.
        if(CLPlatform.getDefault().listCLDevices(CLDevice.Type.CPU).length > 0) {

            context = CLContext.create(CLDevice.Type.CPU);

            clBufferA = context.createBuffer(sizeInBytes, Mem.READ_WRITE);
            clBufferB = context.createBuffer(sizeInBytes, Mem.READ_WRITE);
        }else{

            context = CLContext.create();

            clBufferA = context.createByteBuffer(sizeInBytes, Mem.READ_WRITE, Mem.USE_BUFFER);
            clBufferB = context.createByteBuffer(sizeInBytes, Mem.READ_WRITE, Mem.USE_BUFFER);
        }

        final CLCommandQueue queue = context.getDevices()[0].createCommandQueue();

        // fill only first buffer -> we will copy the payload to the second later.
        final ByteBuffer mappedBufferA = queue.putMapBuffer(clBufferA, Map.WRITE, true);
        assertEquals(sizeInBytes, mappedBufferA.capacity());

        fillBuffer(mappedBufferA, 12345);                // write to A

        queue.putUnmapMemory(clBufferA, mappedBufferA)// unmap A
             .putCopyBuffer(clBufferA, clBufferB);    // copy A -> B

        // map B for read operations
        final ByteBuffer mappedBufferB = queue.putMapBuffer(clBufferB, Map.READ, true);
        assertEquals(sizeInBytes, mappedBufferB.capacity());

        out.println("validating computed results...");
        checkIfEqual(mappedBufferA, mappedBufferB, elements); // A == B ?
        out.println("results are valid");

        queue.putUnmapMemory(clBufferB, mappedBufferB);     // unmap B

        context.release();

    }

    @Test
    public void subBufferTest01ByteBuffer() {

        out.println(" - - - subBufferTest - - - ");

        @SuppressWarnings("unchecked")
        final
        CLPlatform platform = CLPlatform.getDefault(version(CL_1_1));
        if(platform == null) {
            out.println("aborting subBufferTest");
            return;
        }

        final CLContext context = CLContext.create(platform);
        try{
            final int subelements = 5;
            final long lMaxAlignment = context.getMaxMemBaseAddrAlign();
            final int iMaxAlignment = Bitstream.uint32LongToInt(lMaxAlignment);
            System.err.println("XXX: maxAlignment "+lMaxAlignment+", 0x"+Long.toHexString(lMaxAlignment)+", (int)"+iMaxAlignment+", (int)0x"+Integer.toHexString(iMaxAlignment));
            if( -1 == iMaxAlignment ) {
                throw new RuntimeException("Cannot handle MaxMemBaseAddrAlign > MAX_INT, has 0x"+Long.toHexString(lMaxAlignment));
            }
            // device only
            final CLBuffer<?> buffer = context.createBuffer(iMaxAlignment+subelements);

            assertFalse(buffer.isSubBuffer());
            assertNotNull(buffer.getSubBuffers());
            assertTrue(buffer.getSubBuffers().isEmpty());

            final CLSubBuffer<?> subBuffer = buffer.createSubBuffer(iMaxAlignment, subelements);

            assertTrue(subBuffer.isSubBuffer());
            assertEquals(subelements, subBuffer.getCLSize());
            assertEquals(iMaxAlignment, subBuffer.getOffset());
            assertEquals(iMaxAlignment, subBuffer.getCLOffset());
            assertEquals(buffer, subBuffer.getParent());
            assertEquals(1, buffer.getSubBuffers().size());

            subBuffer.release();
            assertEquals(0, buffer.getSubBuffers().size());
        }finally{
            context.release();
        }

    }

    @Test
    public void subBufferTest02FloatBuffer() {

        out.println(" - - - subBufferTest - - - ");

        @SuppressWarnings("unchecked")
        final
        CLPlatform platform = CLPlatform.getDefault(version(CL_1_1));
        if(platform == null) {
            out.println("aborting subBufferTest");
            return;
        }

        final CLContext context = CLContext.create(platform);
        try{
            final int subelements = 5;
            final long lMaxAlignment = context.getMaxMemBaseAddrAlign();
            final int iMaxAlignment = Bitstream.uint32LongToInt(lMaxAlignment);
            System.err.println("XXX: maxAlignment "+lMaxAlignment+", 0x"+Long.toHexString(lMaxAlignment)+", (int)"+iMaxAlignment+", (int)0x"+Integer.toHexString(iMaxAlignment));
            if( -1 == iMaxAlignment ) {
                throw new RuntimeException("Cannot handle MaxMemBaseAddrAlign > MAX_INT, has 0x"+Long.toHexString(lMaxAlignment));
            }
            // FIXME: See Bug 979: Offset/Alignment via offset calculation per element-count is faulty!
            final int floatsPerAlignment = iMaxAlignment / Buffers.SIZEOF_FLOAT;
            // device + direct buffer
            final CLBuffer<FloatBuffer> buffer = context.createFloatBuffer(floatsPerAlignment+subelements);
            assertFalse(buffer.isSubBuffer());
            assertNotNull(buffer.getSubBuffers());
            assertTrue(buffer.getSubBuffers().isEmpty());

            final CLSubBuffer<FloatBuffer> subBuffer = buffer.createSubBuffer(floatsPerAlignment, subelements);

            assertTrue(subBuffer.isSubBuffer());
            assertEquals(subelements, subBuffer.getBuffer().capacity());
            assertEquals(floatsPerAlignment, subBuffer.getOffset());
            assertEquals(iMaxAlignment, subBuffer.getCLOffset());
            assertEquals(buffer, subBuffer.getParent());
            assertEquals(1, buffer.getSubBuffers().size());

            assertEquals(subBuffer.getCLCapacity(), subBuffer.getBuffer().capacity());

            subBuffer.release();
            assertEquals(0, buffer.getSubBuffers().size());

        }finally{
            context.release();
        }

    }

    @Test
    public void destructorCallbackTest() throws InterruptedException {

        out.println(" - - - destructorCallbackTest - - - ");

        @SuppressWarnings("unchecked")
        final
        CLPlatform platform = CLPlatform.getDefault(version(CL_1_1));
        if(platform == null) {
            out.println("aborting destructorCallbackTest");
            return;
        }

        final CLContext context = CLContext.create(platform);

        try{

            final CLBuffer<?> buffer = context.createBuffer(32);
            final CountDownLatch countdown = new CountDownLatch(1);

            buffer.registerDestructorCallback(new CLMemObjectListener() {
                public void memoryDeallocated(final CLMemory<?> mem) {
                    out.println("buffer released");
                    assertEquals(mem, buffer);
                    countdown.countDown();
                }
            });
            buffer.release();

            countdown.await(2, TimeUnit.SECONDS);
            assertEquals(countdown.getCount(), 0);

        }finally{
            context.release();
        }


    }

    public static void main(final String[] args) throws IOException {
        final String tstname = CLBufferTest.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
