/*
 * Copyright (c) 2009 JogAmp Community. All rights reserved.
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

import static com.jogamp.opencl.CLException.checkForError;
import static com.jogamp.opencl.CLException.newException;
import static com.jogamp.opencl.llb.CL.CL_SUCCESS;
import static com.jogamp.opencl.llb.CLCommandQueueBinding.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE;
import static com.jogamp.opencl.llb.CLCommandQueueBinding.CL_QUEUE_PROFILING_ENABLE;
import static com.jogamp.opencl.util.CLUtil.clBoolean;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import com.jogamp.common.nio.CachedBufferFactory;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.opencl.gl.CLGLObject;
import com.jogamp.opencl.llb.CLCommandQueueBinding;
import com.jogamp.opencl.llb.gl.CLGL;

/**
 * The command queue is used to queue a set of operations for a specific {@link CLDevice}.
 * Having multiple command-queues allows applications to queue multiple independent commands without
 * requiring synchronization. Note that this should work as long as these objects are
 * not being shared.
 * <p>
 * concurrency note:<br/>
 * Sharing of objects across multiple queues or using a CLCommandQueue
 * form multiple Threads will require the application to perform appropriate synchronization.
 * </p>
 * @see CLDevice#createCommandQueue(com.jogamp.opencl.CLCommandQueue.Mode...)
 * @author Michael Bien, et al.
 */
public class CLCommandQueue extends CLObjectResource {

    private final CLCommandQueueBinding cl;
    private final CLDevice device;
    private final long properties;

    /*
     * Those direct memory buffers are used to move data between the JVM and OpenCL.
     */
    private final IntBuffer pbA;
    private final PointerBuffer ibA;
    private final PointerBuffer ibB;
    private final PointerBuffer ibC;

    private CLCommandQueue(final CLContext context, final long id, final CLDevice device, final long properties) {
        super(context, id);

        this.device = device;
        this.properties = properties;
        this.cl = context.getPlatform().getCommandQueueBinding();

        final int pbsize = PointerBuffer.ELEMENT_SIZE;
        final CachedBufferFactory factory = CachedBufferFactory.create(9*pbsize + 4, true);

        this.ibA = PointerBuffer.wrap(factory.newDirectByteBuffer(3*pbsize));
        this.ibB = PointerBuffer.wrap(factory.newDirectByteBuffer(3*pbsize));
        this.ibC = PointerBuffer.wrap(factory.newDirectByteBuffer(3*pbsize));

        this.pbA = factory.newDirectIntBuffer(1);

    }

    static CLCommandQueue create(final CLContext context, final CLDevice device, final long properties) {
        final int[] status = new int[1];
        final CLCommandQueueBinding binding = context.getPlatform().getCommandQueueBinding();
        final long id = binding.clCreateCommandQueue(context.ID, device.ID, properties, status, 0);

        if(status[0] != CL_SUCCESS) {
            throw newException(status[0], "can not create command queue on " + device +" with properties: " + Mode.valuesOf(properties));
        }

        return new CLCommandQueue(context, id, device, properties);
    }

    /**
     * Calls {@native clEnqueueWriteBuffer}.
     */
    public CLCommandQueue putWriteBuffer(final CLBuffer<?> writeBuffer, final boolean blockingRead) {
        return putWriteBuffer(writeBuffer, blockingRead, null, null);
    }

    /**
     * Calls {@native clEnqueueWriteBuffer}.
     */
    public CLCommandQueue putWriteBuffer(final CLBuffer<?> writeBuffer, final boolean blockingRead, final CLEventList events) {
        return putWriteBuffer(writeBuffer, blockingRead, null, events);
    }

    /**
     * Calls {@native clEnqueueWriteBuffer}.
     */
    public CLCommandQueue putWriteBuffer(final CLBuffer<?> writeBuffer, final boolean blockingWrite, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        final int ret = cl.clEnqueueWriteBuffer(
                ID, writeBuffer.ID, clBoolean(blockingWrite),
                0, writeBuffer.getNIOSize(), writeBuffer.buffer,
                conditions, conditionIDs, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue write-buffer: " + writeBuffer + " with " + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    /**
     * Calls {@native clEnqueueReadBuffer}.
     */
    public CLCommandQueue putReadBuffer(final CLBuffer<?> readBuffer, final boolean blockingRead) {
        putReadBuffer(readBuffer, blockingRead, null, null);
        return this;
    }

    /**
     * Calls {@native clEnqueueReadBuffer}.
     */
    public CLCommandQueue putReadBuffer(final CLBuffer<?> readBuffer, final boolean blockingRead, final CLEventList events) {
        putReadBuffer(readBuffer, blockingRead, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueReadBuffer}.
     */
    public CLCommandQueue putReadBuffer(final CLBuffer<?> readBuffer, final boolean blockingRead, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        final int ret = cl.clEnqueueReadBuffer(
                ID, readBuffer.ID, clBoolean(blockingRead),
                0, readBuffer.getNIOSize(), readBuffer.buffer,
                conditions, conditionIDs, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue read-buffer: " + readBuffer + " with " + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    /**
     * Calls {@native clEnqueueCopyBuffer}.
     */
    public CLCommandQueue putCopyBuffer(final CLBuffer<?> src, final CLBuffer<?> dest) {
        return putCopyBuffer(src, dest, 0, 0, src.getCLSize(), null, null);
    }

    /**
     * Calls {@native clEnqueueCopyBuffer}.
     */
    public CLCommandQueue putCopyBuffer(final CLBuffer<?> src, final CLBuffer<?> dest, final long bytesToCopy) {
        return putCopyBuffer(src, dest, 0, 0, bytesToCopy, null, null);
    }

    /**
     * Calls {@native clEnqueueCopyBuffer}.
     */
    public CLCommandQueue putCopyBuffer(final CLBuffer<?> src, final CLBuffer<?> dest, final int srcOffset, final int destOffset, final long bytesToCopy, final CLEventList events) {
        return putCopyBuffer(src, dest, srcOffset, destOffset, bytesToCopy, null, events);
    }

    /**
     * Calls {@native clEnqueueCopyBuffer}.
     */
    public CLCommandQueue putCopyBuffer(final CLBuffer<?> src, final CLBuffer<?> dest, final int srcOffset, final int destOffset, final long bytesToCopy, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        final int ret = cl.clEnqueueCopyBuffer(
                        ID, src.ID, dest.ID, srcOffset, destOffset, bytesToCopy,
                        conditions, conditionIDs, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue copy-buffer from " + src + " to " + dest + " with srcOffset: "+ srcOffset
                    + " dstOffset: " + destOffset + " bytesToCopy: " + bytesToCopy + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    //2D
    /**
     * Calls {@native clEnqueueWriteBufferRect}.
     */
    public CLCommandQueue putWriteBufferRect(final CLBuffer<?> writeBuffer,
            final int originX, final int originY, final int hostX, final int hostY, final int rangeX, final int rangeY,
            final boolean blockingWrite, final CLEventList condition, final CLEventList events) {
        putWriteBufferRect(writeBuffer, originX, originY, hostX, hostY, rangeX, rangeY, 0, 0, 0, 0, blockingWrite, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueWriteBufferRect}.
     */
    public CLCommandQueue putWriteBufferRect(final CLBuffer<?>  writeBuffer,
            final int originX, final int originY, final int hostX, final int hostY, final int rangeX, final int rangeY,
            final long rowPitch, final long slicePitch, final long hostRowPitch, final long hostSlicePitch,
            final boolean blockingWrite, final CLEventList condition, final CLEventList events) {
        // spec: if 2d: origin/hostpos=0, range=1
        putWriteBufferRect( writeBuffer, originX, originY, 0,
                                         hostX, hostY, 0,
                                         rangeX, rangeY, 1,
                                         0, 0, 0, 0, blockingWrite, condition, events);
        return this;
    }

    //3D
    /**
     * Calls {@native clEnqueueWriteBufferRect}.
     */
    public CLCommandQueue putWriteBufferRect(final CLBuffer<?> writeBuffer,
            final int originX, final int originY, final int originZ, final int hostX, final int hostY, final int hostZ, final int rangeX, final int rangeY, final int rangeZ,
            final boolean blockingWrite, final CLEventList condition, final CLEventList events) {
        putWriteBufferRect(writeBuffer, originX, originY, originZ,
                                        hostX, hostY, hostZ,
                                        rangeX, rangeY, rangeZ, 0, 0, 0, 0, blockingWrite, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueWriteBufferRect}.
     */
    public CLCommandQueue putWriteBufferRect(final CLBuffer<?> writeBuffer,
            final int originX, final int originY, final int originZ, final int hostX, final int hostY, final int hostZ, final int rangeX, final int rangeY, final int rangeZ,
            final long rowPitch, final long slicePitch, final long hostRowPitch, final long hostSlicePitch,
            final boolean blockingWrite, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        copy2NIO(ibA, originX, originY, originZ);
        copy2NIO(ibB, hostX, hostY, hostZ);
        copy2NIO(ibC, rangeX, rangeY, rangeZ);

        final int ret = cl.clEnqueueWriteBufferRect(
                ID, writeBuffer.ID, clBoolean(blockingWrite), ibA, ibB, ibC,
                rowPitch, slicePitch, hostRowPitch, hostSlicePitch, writeBuffer.getBuffer(),
                conditions, conditionIDs, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, bufferRectToString("write", writeBuffer,
                                        rowPitch, slicePitch, hostRowPitch, hostSlicePitch,
                                        originX, originY, originZ, hostX, hostY, hostZ,
                                        rangeX, rangeY, rangeZ, condition, events)  );
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    //2D
    /**
     * Calls {@native clEnqueueReadBufferRect}.
     */
    public CLCommandQueue putReadBufferRect(final CLBuffer<?> readBuffer,
            final int originX, final int originY, final int hostX, final int hostY, final int rangeX, final int rangeY,
            final boolean blockingRead, final CLEventList condition, final CLEventList events) {
        putReadBufferRect(readBuffer, originX, originY, hostX, hostY, rangeX, rangeY, 0, 0, 0, 0, blockingRead, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueReadBufferRect}.
     */
    public CLCommandQueue putReadBufferRect(final CLBuffer<?> readBuffer,
            final int originX, final int originY, final int hostX, final int hostY, final int rangeX, final int rangeY,
            final long rowPitch, final long slicePitch, final long hostRowPitch, final long hostSlicePitch,
            final boolean blockingRead, final CLEventList condition, final CLEventList events) {
        // spec: if 2d: origin/hostpos=0, range=1
        putReadBufferRect(  readBuffer, originX, originY, 0,
                            hostX, hostY, 0,
                            rangeX, rangeY, 1,
                            0, 0, 0, 0, blockingRead, condition, events);
        return this;
    }

    //3D
    /**
     * Calls {@native clEnqueueReadBufferRect}.
     */
    public CLCommandQueue putReadBufferRect(final CLBuffer<?> readBuffer,
            final int originX, final int originY, final int originZ, final int hostX, final int hostY, final int hostZ, final int rangeX, final int rangeY, final int rangeZ,
            final boolean blockingRead, final CLEventList condition, final CLEventList events) {
        putReadBufferRect(  readBuffer, originX, originY, originZ,
                            hostX, hostY, hostZ,
                            rangeX, rangeY, rangeZ,
                            0, 0, 0, 0, blockingRead, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueReadBufferRect}.
     */
    public CLCommandQueue putReadBufferRect(final CLBuffer<?> readBuffer,
            final int originX, final int originY, final int originZ, final int hostX, final int hostY, final int hostZ, final int rangeX, final int rangeY, final int rangeZ,
            final long rowPitch, final long slicePitch, final long hostRowPitch, final long hostSlicePitch,
            final boolean blockingRead, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        copy2NIO(ibA, originX, originY, originZ);
        copy2NIO(ibB, hostX, hostY, hostZ);
        copy2NIO(ibC, rangeX, rangeY, rangeZ);

        final int ret = cl.clEnqueueReadBufferRect(
                ID, readBuffer.ID, clBoolean(blockingRead), ibA, ibB, ibC,
                rowPitch, slicePitch, hostRowPitch, hostSlicePitch, readBuffer.getBuffer(),
                conditions, conditionIDs, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, bufferRectToString("read", readBuffer,
                                        rowPitch, slicePitch, hostRowPitch, hostSlicePitch,
                                        originX, originY, originZ, hostX, hostY, hostZ,
                                        rangeX, rangeY, rangeZ, condition, events)  );
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    //2D
    /**
     * Calls {@native clEnqueueCopyBufferRect}.
     */
    public CLCommandQueue putCopyBufferRect(final CLBuffer<?> src, final CLBuffer<?> dest,
            final int srcOriginX, final int srcOriginY, final int destOriginX, final int destOriginY, final int rangeX, final int rangeY,
            final CLEventList condition, final CLEventList events) {
        // spec: if 2d: origin/destpos=0, range=1
        putCopyBufferRect(  src, dest, srcOriginX, srcOriginY, 0,
                            destOriginX, destOriginY, 0,
                            rangeX, rangeY, 1,
                            0, 0, 0, 0, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueCopyBufferRect}.
     */
    public CLCommandQueue putCopyBufferRect(final CLBuffer<?> src, final CLBuffer<?> dest,
            final int srcOriginX, final int srcOriginY, final int destOriginX, final int destOriginY, final int rangeX, final int rangeY,
            final long srcRowPitch, final long srcSlicePitch, final long destRowPitch, final long destSlicePitch,
            final CLEventList condition, final CLEventList events) {
        putCopyBufferRect(  src, dest, srcOriginX, srcOriginY, 0,
                            destOriginX, destOriginY, 0,
                            rangeX, rangeY, 1,
                            srcRowPitch, srcSlicePitch, destRowPitch, destSlicePitch, condition, events);
        return this;
    }


    //3D
    /**
     * Calls {@native clEnqueueCopyBufferRect}.
     */
    public CLCommandQueue putCopyBufferRect(final CLBuffer<?> src, final CLBuffer<?> dest,
            final int srcOriginX, final int srcOriginY, final int srcOriginZ, final int destOriginX, final int destOriginY, final int destOriginZ, final int rangeX, final int rangeY, final int rangeZ,
            final CLEventList condition, final CLEventList events) {
        putCopyBufferRect(  src, dest, srcOriginX, srcOriginY, srcOriginZ,
                            destOriginX, destOriginY, destOriginZ,
                            rangeX, rangeY, rangeZ,
                            0, 0, 0, 0, condition, events);
        return this;
    }
    /**
     * Calls {@native clEnqueueCopyBufferRect}.
     */
    public CLCommandQueue putCopyBufferRect(final CLBuffer<?> src, final CLBuffer<?> dest,
            final int srcOriginX, final int srcOriginY, final int srcOriginZ, final int destOriginX, final int destOriginY, final int destOriginZ, final int rangeX, final int rangeY, final int rangeZ,
            final long srcRowPitch, final long srcSlicePitch, final long destRowPitch, final long destSlicePitch,
            final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        copy2NIO(ibA, srcOriginX, srcOriginY, srcOriginZ);
        copy2NIO(ibB, destOriginX, destOriginY, destOriginZ);
        copy2NIO(ibC, rangeX, rangeY, rangeZ);

        final int ret = cl.clEnqueueCopyBufferRect(
                        ID, src.ID, dest.ID, ibA, ibB, ibC,
                        srcRowPitch, srcSlicePitch, destRowPitch, destSlicePitch,
                        conditions, conditionIDs, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue copy-buffer-rect from " + src + " to " + dest + "\n"
                       + " with srcRowPitch: " + srcRowPitch + " srcSlicePitch: " + srcSlicePitch
                       + " destRowPitch: " + destRowPitch + " destSlicePitch: " + destSlicePitch + "\n"
                       + " srcOrigin: " + toStr(srcOriginX, srcOriginY, srcOriginZ)+ " destOrigin: " + toStr(destOriginX, destOriginY, destOriginZ)
                       + " range: " + toStr(rangeX, rangeY, rangeZ) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }


    //2D
    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(final CLImage2d<?> writeImage, final boolean blockingWrite) {
        return putWriteImage(writeImage, 0, 0, 0, writeImage.width, writeImage.height, blockingWrite, null, null);
    }

    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(final CLImage2d<?> writeImage, final boolean blockingWrite, final CLEventList events) {
        return putWriteImage(writeImage, 0, 0, 0, writeImage.width, writeImage.height, blockingWrite, null, events);
    }

    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(final CLImage2d<?> writeImage, final boolean blockingWrite, final CLEventList condition, final CLEventList events) {
        return putWriteImage(writeImage, 0, 0, 0, writeImage.width, writeImage.height, blockingWrite, condition, events);
    }

    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(final CLImage2d<?> writeImage, final int inputRowPitch,
            final int originX, final int originY, final int rangeX, final int rangeY, final boolean blockingWrite) {
        return putWriteImage(writeImage, inputRowPitch, originX, originY, rangeX, rangeY, blockingWrite, null, null);
    }

    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(final CLImage2d<?> writeImage, final int inputRowPitch,
            final int originX, final int originY, final int rangeX, final int rangeY, final boolean blockingWrite, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        // spec: CL_INVALID_VALUE if image is a 2D image object and origin[2] is not equal to 0
        // or region[2] is not equal to 1 or slice_pitch is not equal to 0.
        copy2NIO(ibA, originX, originY, 0);
        copy2NIO(ibB, rangeX, rangeY, 1);

        final int ret = cl.clEnqueueWriteImage(ID, writeImage.ID, clBoolean(blockingWrite),
                                         ibA, ibB, inputRowPitch, 0, writeImage.buffer,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue write-image " + writeImage + " with inputRowPitch: " + inputRowPitch
                       + " origin: " + toStr(originX, originY)+ " range: " + toStr(rangeX, rangeY) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //3D
    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(final CLImage3d<?> writeImage, final boolean blockingWrite) {
        return putWriteImage(writeImage, 0, 0, 0, 0, 0, writeImage.width, writeImage.height, writeImage.depth, blockingWrite, null, null);
    }

    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(final CLImage3d<?> writeImage, final boolean blockingWrite, final CLEventList events) {
        return putWriteImage(writeImage, 0, 0, 0, 0, 0, writeImage.width, writeImage.height, writeImage.depth, blockingWrite, null, events);
    }

    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(final CLImage3d<?> writeImage, final boolean blockingWrite, final CLEventList condition, final CLEventList events) {
        return putWriteImage(writeImage, 0, 0, 0, 0, 0, writeImage.width, writeImage.height, writeImage.depth, blockingWrite, condition, events);
    }

    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(final CLImage3d<?> writeImage, final int inputRowPitch, final int inputSlicePitch,
            final int originX, final int originY, final int originZ, final int rangeX, final int rangeY, final int rangeZ, final boolean blockingWrite) {
        return putWriteImage(writeImage, inputRowPitch, inputSlicePitch, originX, originY, originZ, rangeX, rangeY, rangeZ, blockingWrite, null, null);
    }

    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(final CLImage3d<?> writeImage, final int inputRowPitch, final int inputSlicePitch,
            final int originX, final int originY, final int originZ, final int rangeX, final int rangeY, final int rangeZ, final boolean blockingWrite, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        copy2NIO(ibA, originX, originY, originZ);
        copy2NIO(ibB, rangeX, rangeY, rangeZ);

        final int ret = cl.clEnqueueWriteImage(ID, writeImage.ID, clBoolean(blockingWrite),
                                         ibA, ibB, inputRowPitch, inputSlicePitch, writeImage.buffer,
                                         conditions, conditionIDs, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue write-image " + writeImage + " with inputRowPitch: " + inputRowPitch + " inputSlicePitch: " + inputSlicePitch
                       + " origin: " + toStr(originX, originY, originZ)+ " range: " + toStr(rangeX, rangeY, rangeZ) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //2D
    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(final CLImage2d<?> readImage, final boolean blockingRead) {
        return putReadImage(readImage, 0, 0, 0, readImage.width, readImage.height, blockingRead, null, null);
    }

    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(final CLImage2d<?> readImage, final boolean blockingRead, final CLEventList events) {
        return putReadImage(readImage, 0, 0, 0, readImage.width, readImage.height, blockingRead, null, events);
    }

    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(final CLImage2d<?> readImage, final boolean blockingRead, final CLEventList condition, final CLEventList events) {
        return putReadImage(readImage, 0, 0, 0, readImage.width, readImage.height, blockingRead, condition, events);
    }

    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(final CLImage2d<?> readImage, final int inputRowPitch,
            final int originX, final int originY, final int rangeX, final int rangeY, final boolean blockingRead) {
        return putReadImage(readImage, inputRowPitch, originX, originY, rangeX, rangeY, blockingRead, null, null);
    }

    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(final CLImage2d<?> readImage, final int inputRowPitch,
            final int originX, final int originY, final int rangeX, final int rangeY, final boolean blockingRead, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        // spec: CL_INVALID_VALUE if image is a 2D image object and origin[2] is not equal to 0
        // or region[2] is not equal to 1 or slice_pitch is not equal to 0.
        copy2NIO(ibA, originX, originY, 0);
        copy2NIO(ibB, rangeX, rangeY, 1);

        final int ret = cl.clEnqueueReadImage(ID, readImage.ID, clBoolean(blockingRead),
                                         ibA, ibB, inputRowPitch, 0, readImage.buffer,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue read-image " + readImage + " with inputRowPitch: " + inputRowPitch
                       + " origin: " + toStr(originX, originY)+ " range: " + toStr(rangeX, rangeY) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //3D
    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(final CLImage3d<?> readImage, final boolean blockingRead) {
        return putReadImage(readImage, 0, 0, 0, 0, 0, readImage.width, readImage.height, readImage.depth, blockingRead, null, null);
    }

    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(final CLImage3d<?> readImage, final boolean blockingRead, final CLEventList events) {
        return putReadImage(readImage, 0, 0, 0, 0, 0, readImage.width, readImage.height, readImage.depth, blockingRead, null, events);
    }

    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(final CLImage3d<?> readImage, final boolean blockingRead, final CLEventList condition, final CLEventList events) {
        return putReadImage(readImage, 0, 0, 0, 0, 0, readImage.width, readImage.height, readImage.depth, blockingRead, condition, events);
    }

    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(final CLImage3d<?> readImage, final int inputRowPitch, final int inputSlicePitch,
            final int originX, final int originY, final int originZ, final int rangeX, final int rangeY, final int rangeZ, final boolean blockingRead) {
        return putReadImage(readImage, inputRowPitch, inputSlicePitch, originX, originY, originZ, rangeX, rangeY, rangeZ, blockingRead, null, null);
    }

    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(final CLImage3d<?> readImage, final int inputRowPitch, final int inputSlicePitch,
            final int originX, final int originY, final int originZ, final int rangeX, final int rangeY, final int rangeZ, final boolean blockingRead, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        copy2NIO(ibA, originX, originY, originZ);
        copy2NIO(ibB, rangeX, rangeY, rangeZ);

        final int ret = cl.clEnqueueReadImage(ID, readImage.ID, clBoolean(blockingRead),
                                        ibA, ibB, inputRowPitch, inputSlicePitch, readImage.buffer,
                                        conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue read-image " + readImage + " with inputRowPitch: " + inputRowPitch + " inputSlicePitch: " + inputSlicePitch
                       + " origin: " + toStr(originX, originY, originZ)+ " range: " + toStr(rangeX, rangeY, rangeZ) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //2D
    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(final CLImage2d<?> srcImage, final CLImage2d<?> dstImage) {
        return putCopyImage(srcImage, dstImage, null);
    }

    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(final CLImage2d<?> srcImage, final CLImage2d<?> dstImage, final CLEventList events) {
        return putCopyImage(srcImage, dstImage, 0, 0, 0, 0, srcImage.width, srcImage.height, null, events);
    }

    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(final CLImage2d<?> srcImage, final CLImage2d<?> dstImage, final CLEventList condition, final CLEventList events) {
        return putCopyImage(srcImage, dstImage, 0, 0, 0, 0, srcImage.width, srcImage.height, condition, events);
    }

    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(final CLImage2d<?> srcImage, final CLImage2d<?> dstImage,
                                        final int srcOriginX, final int srcOriginY,
                                        final int dstOriginX, final int dstOriginY,
                                        final int rangeX, final int rangeY) {
        return putCopyImage(srcImage, dstImage, srcOriginX, srcOriginY, dstOriginX, dstOriginY, rangeX, rangeY, null, null);
    }

    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(final CLImage2d<?> srcImage, final CLImage2d<?> dstImage,
                                        final int srcOriginX, final int srcOriginY,
                                        final int dstOriginX, final int dstOriginY,
                                        final int rangeX, final int rangeY, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        //spec: CL_INVALID_VALUE if src_image is a 2D image object and origin[2] or dst_origin[2] is not equal to 0
        // or region[2] is not equal to 1.
        copy2NIO(ibA, srcOriginX, srcOriginY, 0);
        copy2NIO(ibB, dstOriginX, dstOriginY, 0);
        copy2NIO(ibC, rangeX, rangeY, 1);

        final int ret = cl.clEnqueueCopyImage(ID, srcImage.ID, dstImage.ID, ibA, ibB, ibC,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue copy-image " + srcImage +" to "+ dstImage
                    + " with srcOrigin: " + toStr(srcOriginX, srcOriginY) + " dstOrigin: " + toStr(dstOriginX, dstOriginY)
                    + " range:  " + toStr(rangeX, rangeY) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //3D
    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(final CLImage3d<?> srcImage, final CLImage3d<?> dstImage) {
        return putCopyImage(srcImage, dstImage, null);
    }

    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(final CLImage3d<?> srcImage, final CLImage3d<?> dstImage, final CLEventList events) {
        return putCopyImage(srcImage, dstImage, 0, 0, 0, 0, 0, 0, srcImage.width, srcImage.height, srcImage.depth, null, events);
    }

    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(final CLImage3d<?> srcImage, final CLImage3d<?> dstImage, final CLEventList condition, final CLEventList events) {
        return putCopyImage(srcImage, dstImage, 0, 0, 0, 0, 0, 0, srcImage.width, srcImage.height, srcImage.depth, condition, events);
    }

    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(final CLImage3d<?> srcImage, final CLImage3d<?> dstImage,
                                        final int srcOriginX, final int srcOriginY, final int srcOriginZ,
                                        final int dstOriginX, final int dstOriginY, final int dstOriginZ,
                                        final int rangeX, final int rangeY, final int rangeZ) {
        return putCopyImage(srcImage, dstImage, srcOriginX, srcOriginY, srcOriginZ,
                                                dstOriginX, dstOriginY, dstOriginZ,
                                                rangeX, rangeY, rangeZ, null, null);
    }

    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(final CLImage<?> srcImage, final CLImage<?> dstImage,
                                        final int srcOriginX, final int srcOriginY, final int srcOriginZ,
                                        final int dstOriginX, final int dstOriginY, final int dstOriginZ,
                                        final int rangeX, final int rangeY, final int rangeZ, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        copy2NIO(ibA, srcOriginX, srcOriginY, srcOriginZ);
        copy2NIO(ibB, dstOriginX, dstOriginY, dstOriginZ);
        copy2NIO(ibC, rangeX, rangeY, rangeZ);

        final int ret = cl.clEnqueueCopyImage(ID, srcImage.ID, dstImage.ID, ibA, ibB, ibC,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue copy-image " + srcImage +" to "+ dstImage
                    + " with srcOrigin: " + toStr(srcOriginX, srcOriginY, srcOriginZ) + " dstOrigin: " + toStr(dstOriginX, dstOriginY, dstOriginZ)
                    + " range:  " + toStr(rangeX, rangeY, rangeZ) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //2D
    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(final CLBuffer<?> srcBuffer, final CLImage2d<?> dstImage) {
        return putCopyBufferToImage(srcBuffer, dstImage, null);
    }

    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(final CLBuffer<?> srcBuffer, final CLImage2d<?> dstImage, final CLEventList events) {
        return putCopyBufferToImage(srcBuffer, dstImage, 0, 0, 0, dstImage.width, dstImage.height, null, events);
    }

    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(final CLBuffer<?> srcBuffer, final CLImage2d<?> dstImage, final CLEventList condition, final CLEventList events) {
        return putCopyBufferToImage(srcBuffer, dstImage, 0, 0, 0, dstImage.width, dstImage.height, condition, events);
    }

    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(final CLBuffer<?> srcBuffer, final CLImage2d<?> dstImage,
                                        final long srcOffset, final int dstOriginX, final int dstOriginY,
                                        final int rangeX, final int rangeY) {
        return putCopyBufferToImage(srcBuffer, dstImage,
                srcOffset, dstOriginX, dstOriginY, rangeX, rangeY, null, null);
    }

    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(final CLBuffer<?> srcBuffer, final CLImage2d<?> dstImage,
                                        final long srcOffset, final int dstOriginX, final int dstOriginY,
                                        final int rangeX, final int rangeY, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        // spec: CL_INVALID_VALUE if dst_image is a 2D image object and dst_origin[2] is not equal to 0
        // or region[2] is not equal to 1.
        copy2NIO(ibA, dstOriginX, dstOriginY, 0);
        copy2NIO(ibB, rangeX, rangeY, 1);

        final int ret = cl.clEnqueueCopyBufferToImage(ID, srcBuffer.ID, dstImage.ID,
                                         srcOffset, ibA, ibB,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue a copy from " + srcBuffer +" to "+ dstImage
                    + " with srcOffset: " + srcOffset + " dstOrigin: " + toStr(dstOriginX, dstOriginY)
                    + " range:  " + toStr(rangeX, rangeY) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //3D
    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(final CLBuffer<?> srcBuffer, final CLImage3d<?> dstImage) {
        return putCopyBufferToImage(srcBuffer, dstImage, null);
    }

    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(final CLBuffer<?> srcBuffer, final CLImage3d<?> dstImage, final CLEventList events) {
        return putCopyBufferToImage(srcBuffer, dstImage, 0, 0, 0, 0, dstImage.width, dstImage.height, dstImage.depth, null, events);
    }

    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(final CLBuffer<?> srcBuffer, final CLImage3d<?> dstImage, final CLEventList condition, final CLEventList events) {
        return putCopyBufferToImage(srcBuffer, dstImage, 0, 0, 0, 0, dstImage.width, dstImage.height, dstImage.depth, condition, events);
    }

    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(final CLBuffer<?> srcBuffer, final CLImage3d<?> dstImage,
                                        final long srcOffset, final int dstOriginX, final int dstOriginY, final int dstOriginZ,
                                        final int rangeX, final int rangeY, final int rangeZ) {
        return putCopyBufferToImage(srcBuffer, dstImage,
                srcOffset, dstOriginX, dstOriginY, dstOriginZ, rangeX, rangeY, rangeZ, null, null);

    }

    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(final CLBuffer<?> srcBuffer, final CLImage3d<?> dstImage,
                                        final long srcOffset, final int dstOriginX, final int dstOriginY, final int dstOriginZ,
                                        final int rangeX, final int rangeY, final int rangeZ, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        copy2NIO(ibA, dstOriginX, dstOriginY, dstOriginZ);
        copy2NIO(ibB, rangeX, rangeY, rangeZ);

        final int ret = cl.clEnqueueCopyBufferToImage(ID, srcBuffer.ID, dstImage.ID,
                                         srcOffset, ibA, ibB,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue a copy from " + srcBuffer +" to "+ dstImage
                    + " with srcOffset: " + srcOffset + " dstOrigin: " + toStr(dstOriginX, dstOriginY, dstOriginZ)
                    + " range:  " + toStr(rangeX, rangeY, dstOriginZ) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //2D
    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(final CLImage2d<?> srcImage, final CLBuffer<?> dstBuffer) {
        return putCopyImageToBuffer(srcImage, dstBuffer, null);
    }

    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(final CLImage2d<?> srcImage, final CLBuffer<?> dstBuffer, final CLEventList events) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 0, 0, srcImage.width, srcImage.height, 0, null, events);
    }

    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(final CLImage2d<?> srcImage, final CLBuffer<?> dstBuffer, final CLEventList condition, final CLEventList events) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 0, 0, srcImage.width, srcImage.height, 0, condition, events);
    }

    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(final CLImage2d<?> srcImage, final CLBuffer<?> dstBuffer,
                                        final int srcOriginX, final int srcOriginY,
                                        final int rangeX, final int rangeY, final long dstOffset) {
        return putCopyImageToBuffer(srcImage, dstBuffer,
                srcOriginX, srcOriginY, rangeX, rangeY, dstOffset, null, null);
    }

    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(final CLImage2d<?> srcImage, final CLBuffer<?> dstBuffer,
                                        final int srcOriginX, final int srcOriginY,
                                        final int rangeX, final int rangeY, final long dstOffset, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        // spec: CL_INVALID_VALUE if src_image is a 2D image object and src_origin[2] is not equal to 0
        // or region[2] is not equal to 1.
        copy2NIO(ibA, srcOriginX, srcOriginY, 0);
        copy2NIO(ibB, rangeX, rangeY, 1);

        final int ret = cl.clEnqueueCopyImageToBuffer(ID, srcImage.ID, dstBuffer.ID,
                                         ibA, ibB, dstOffset,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue a copy from " + srcImage +" to "+ dstBuffer
                    + " with srcOrigin: " + toStr(srcOriginX, srcOriginY) + " range: " + toStr(rangeX, rangeY)
                    + " dstOffset: " + dstOffset + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //3D
    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(final CLImage3d<?> srcImage, final CLBuffer<?> dstBuffer) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 0, 0, 0, srcImage.width, srcImage.height, srcImage.depth, 0, null, null);
    }

    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(final CLImage3d<?> srcImage, final CLBuffer<?> dstBuffer, final CLEventList events) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 0, 0, 0, srcImage.width, srcImage.height, srcImage.depth, 0, null, events);
    }

    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(final CLImage3d<?> srcImage, final CLBuffer<?> dstBuffer, final CLEventList condition, final CLEventList events) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 0, 0, 0, srcImage.width, srcImage.height, srcImage.depth, 0, condition, events);
    }

    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(final CLImage3d<?> srcImage, final CLBuffer<?> dstBuffer,
                                        final int srcOriginX, final int srcOriginY, final int srcOriginZ,
                                        final int rangeX, final int rangeY, final int rangeZ, final long dstOffset) {
        return putCopyImageToBuffer(srcImage, dstBuffer,
                srcOriginX, srcOriginY, srcOriginZ, rangeX, rangeY, rangeZ, dstOffset, null, null);

    }

    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(final CLImage3d<?> srcImage, final CLBuffer<?> dstBuffer,
                                        final int srcOriginX, final int srcOriginY, final int srcOriginZ,
                                        final int rangeX, final int rangeY, final int rangeZ, final long dstOffset, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        copy2NIO(ibA, srcOriginX, srcOriginY, srcOriginZ);
        copy2NIO(ibB, rangeX, rangeY, rangeZ);

        final int ret = cl.clEnqueueCopyImageToBuffer(ID, srcImage.ID, dstBuffer.ID,
                                         ibA, ibB, dstOffset,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue a copy from " + srcImage +" to "+ dstBuffer
                    + " with srcOrigin: " + toStr(srcOriginX, srcOriginY, srcOriginZ) + " range: " + toStr(rangeX, rangeY, rangeZ)
                    + " dstOffset: " + dstOffset + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    /**
     * Calls {@native clEnqueueMapBuffer}.
     */
    public ByteBuffer putMapBuffer(final CLBuffer<?> buffer, final CLMemory.Map flag, final boolean blockingMap) {
        return putMapBuffer(buffer, flag, blockingMap, null);
    }

    /**
     * Calls {@native clEnqueueMapBuffer}.
     */
    public ByteBuffer putMapBuffer(final CLBuffer<?> buffer, final CLMemory.Map flag, final boolean blockingMap, final CLEventList events) {
        return putMapBuffer(buffer, flag, 0, buffer.getCLSize(), blockingMap, null, events);
    }

    /**
     * Calls {@native clEnqueueMapBuffer}.
     */
    public ByteBuffer putMapBuffer(final CLBuffer<?> buffer, final CLMemory.Map flag, final boolean blockingMap, final CLEventList condition, final CLEventList events) {
        return putMapBuffer(buffer, flag, 0, buffer.getCLSize(), blockingMap, condition, events);
    }

    /**
     * Calls {@native clEnqueueMapBuffer}.
     */
    public ByteBuffer putMapBuffer(final CLBuffer<?> buffer, final CLMemory.Map flag, final long offset, final long length, final boolean blockingMap) {
        return putMapBuffer(buffer, flag, offset, length, blockingMap, null, null);
    }

    /**
     * Calls {@native clEnqueueMapBuffer}.
     */
    public ByteBuffer putMapBuffer(final CLBuffer<?> buffer, final CLMemory.Map flag, final long offset, final long length, final boolean blockingMap, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        final IntBuffer error = pbA;
        final ByteBuffer mappedBuffer = cl.clEnqueueMapBuffer(ID, buffer.ID, clBoolean(blockingMap),
                                         flag.FLAGS, offset, length,
                                         conditions, conditionIDs, events==null ? null : events.IDs, error);
        if(error.get(0) != CL_SUCCESS) {
            throw newException(error.get(0), "can not map " + buffer + " with: " + flag
                    + " offset: " + offset + " lenght: " + length + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return mappedBuffer;
    }

    // 2D
    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(final CLImage2d<?> image, final CLMemory.Map flag, final boolean blockingMap) {
        return putMapImage(image, flag, blockingMap, null);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(final CLImage2d<?> image, final CLMemory.Map flag, final boolean blockingMap, final CLEventList events) {
        return putMapImage(image, flag, 0, 0, image.width, image.height, blockingMap, null, events);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(final CLImage2d<?> image, final CLMemory.Map flag, final boolean blockingMap, final CLEventList condition, final CLEventList events) {
        return putMapImage(image, flag, 0, 0, image.width, image.height, blockingMap, condition, events);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(final CLImage2d<?> buffer, final CLMemory.Map flag, final int offsetX, final int offsetY,
                                    final int rangeX, final int rangeY, final boolean blockingMap) {
        return putMapImage(buffer, flag, offsetX, offsetY, rangeX, rangeY, blockingMap, null, null);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(final CLImage2d<?> image, final CLMemory.Map flag,
                                    final int offsetX, final int offsetY,
                                    final int rangeX, final int rangeY, final boolean blockingMap, final CLEventList condition, final CLEventList events) {
        return putMapImage(image, flag, offsetX, offsetY, rangeX, rangeY, blockingMap, condition, events, null, null);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(final CLImage2d<?> image, final CLMemory.Map flag,
                                    final int offsetX, final int offsetY,
                                    final int rangeX, final int rangeY, final boolean blockingMap, final CLEventList condition, final CLEventList events,
                                    final long[] imageRowPitch, final long[] imageSlicePitch ) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        final IntBuffer error = pbA;

        // spec: CL_INVALID_VALUE if image is a 2D image object and origin[2] is not equal to 0 or region[2] is not equal to 1
        copy2NIO(ibB, offsetX, offsetY, 0);
        copy2NIO(ibC, rangeX, rangeY, 1);

        final PointerBuffer _imageRowPitch = PointerBuffer.allocateDirect(1); // size_t*
        final PointerBuffer _imageSlicePitch = PointerBuffer.allocateDirect(1); // size_t*

        final ByteBuffer mappedImage = cl.clEnqueueMapImage(ID, image.ID, clBoolean(blockingMap),
                                         flag.FLAGS, ibB, ibC, _imageRowPitch, _imageSlicePitch,
                                         conditions, conditionIDs, events==null ? null : events.IDs, error);
        if(error.get(0) != CL_SUCCESS) {
            throw newException(error.get(0), "can not map " + image + " with: " + flag
                    + " offset: " + toStr(offsetX, offsetY) + " range: " + toStr(rangeX, rangeY) + toStr(condition, events));
        }

        if( null != imageRowPitch ) {
            imageRowPitch[0] = _imageRowPitch.get(0);
        }
        if( null != imageSlicePitch ) {
            imageSlicePitch[0] = _imageSlicePitch.get(0);
        }

        if(events != null) {
            events.createEvent(context);
        }

        return mappedImage;
    }

    // 3D
    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(final CLImage3d<?> image, final CLMemory.Map flag, final boolean blockingMap) {
        return putMapImage(image, flag, blockingMap, null);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(final CLImage3d<?> image, final CLMemory.Map flag, final boolean blockingMap, final CLEventList events) {
        return putMapImage(image, flag, 0, 0, 0, image.width, image.height, image.depth, blockingMap, null, events);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(final CLImage3d<?> image, final CLMemory.Map flag, final boolean blockingMap, final CLEventList condition, final CLEventList events) {
        return putMapImage(image, flag, 0, 0, 0, image.width, image.height, image.depth, blockingMap, condition, events);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(final CLImage3d<?> image, final CLMemory.Map flag,
                                    final int offsetX, final int offsetY, final int offsetZ,
                                    final int rangeX, final int rangeY, final int rangeZ, final boolean blockingMap) {
        return putMapImage(image, flag, offsetX, offsetY, offsetZ, rangeX, rangeY, rangeZ, blockingMap, null, null);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(final CLImage3d<?> image, final CLMemory.Map flag,
                                    final int offsetX, final int offsetY, final int offsetZ,
                                    final int rangeX, final int rangeY, final int rangeZ, final boolean blockingMap, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        final IntBuffer error = pbA;
        copy2NIO(ibB, offsetX, offsetY, offsetZ);
        copy2NIO(ibC, rangeX, rangeY, rangeZ);
        final ByteBuffer mappedImage = cl.clEnqueueMapImage(ID, image.ID, clBoolean(blockingMap),
                                         flag.FLAGS, ibB, ibC, null, null,
                                         conditions, conditionIDs, events==null ? null : events.IDs, error);
        if(error.get(0) != CL_SUCCESS) {
            throw newException(error.get(0), "can not map " + image + " with: " + flag
                    + " offset: " + toStr(offsetX, offsetY, offsetZ) + " range: " + toStr(rangeX, rangeY, rangeZ) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return mappedImage;
    }

    /**
     * Calls {@native clEnqueueUnmapMemObject}.
     */
    public CLCommandQueue putUnmapMemory(final CLMemory<?> memory, final Buffer mapped) {
        return putUnmapMemory(memory, mapped, null, null);
    }

    /**
     * Calls {@native clEnqueueUnmapMemObject}.
     */
    public CLCommandQueue putUnmapMemory(final CLMemory<?> memory, final Buffer mapped, final CLEventList events) {
        return putUnmapMemory(memory, mapped, null, events);
    }

    /**
     * Calls {@native clEnqueueUnmapMemObject}.
     */
    public CLCommandQueue putUnmapMemory(final CLMemory<?> memory, final Buffer mapped, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        final int ret = cl.clEnqueueUnmapMemObject(ID, memory.ID, mapped,
                                        conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not unmap " + memory + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    /**
     * Calls {@native clEnqueueMarker}.
     */
    public CLCommandQueue putMarker(final CLEventList events) {
        final int ret = cl.clEnqueueMarker(ID, events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue marker " + events);
        }
        events.createEvent(context);
        return this;
    }

    /**
     * Calls {@native clWaitForEvents} if blockingWait equals true otherwise {@native clEnqueueWaitForEvents}.
     */
    public CLCommandQueue putWaitForEvent(final CLEventList list, final int index, final boolean blockingWait) {

        if(blockingWait) {
            list.waitForEvent(index);
        } else {
            final PointerBuffer ids = list.getEventBuffer(index);
            final int ret = cl.clEnqueueWaitForEvents(ID, 1, ids);
            if(ret != CL_SUCCESS) {
                throw newException(ret, "can not "+ (blockingWait?"blocking": "") +" wait for event #" + index+ " in "+list);
            }
        }

        return this;
    }

    /**
     * Calls {@native clWaitForEvents} if blockingWait equals true otherwise {@native clEnqueueWaitForEvents}.
     */
    public CLCommandQueue putWaitForEvents(final CLEventList list, final boolean blockingWait) {
        if(blockingWait) {
            list.waitForEvents();
        }else{
            final int ret = cl.clEnqueueWaitForEvents(ID, list.size, list.IDsView);
            if(ret != CL_SUCCESS) {
                throw newException(ret, "can not "+ (blockingWait?"blocking": "") +" wait for events " + list);
            }
        }
        return this;
    }

    /**
     * Calls {@native clEnqueueBarrier}.
     */
    public CLCommandQueue putBarrier() {
        final int ret = cl.clEnqueueBarrier(ID);
        checkForError(ret, "can not enqueue Barrier");
        return this;
    }

    /**
     * Equivalent to calling
     * {@link #put1DRangeKernel(CLKernel kernel, long globalWorkOffset, long globalWorkSize, long localWorkSize)}
     * with globalWorkOffset = null, globalWorkSize set to 1, and localWorkSize set to 1.
     * <p>Calls {@native clEnqueueTask}.</p>
     */
    public CLCommandQueue putTask(final CLKernel kernel) {
        putTask(kernel, null, null);
        return this;
    }

    /**
     * <p>Calls {@native clEnqueueTask}.</p>
     * @see #putTask(com.jogamp.opencl.CLKernel)
     */
    public CLCommandQueue putTask(final CLKernel kernel, final CLEventList events) {
        putTask(kernel, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueTask}.
     * @see #putTask(com.jogamp.opencl.CLKernel)
     */
    public CLCommandQueue putTask(final CLKernel kernel, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        final int ret = cl.clEnqueueTask(ID, kernel.ID, conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            checkForError(ret, "can not enqueue Task: " + kernel + toStr(condition, events));
        }
        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put1DRangeKernel(final CLKernel kernel, final long globalWorkOffset, final long globalWorkSize, final long localWorkSize) {
        this.put1DRangeKernel(kernel, globalWorkOffset, globalWorkSize, localWorkSize, null, null);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put1DRangeKernel(final CLKernel kernel, final long globalWorkOffset, final long globalWorkSize, final long localWorkSize, final CLEventList events) {
        this.put1DRangeKernel(kernel, globalWorkOffset, globalWorkSize, localWorkSize, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put1DRangeKernel(final CLKernel kernel, final long globalWorkOffset, final long globalWorkSize, final long localWorkSize, final CLEventList condition, final CLEventList events) {
        PointerBuffer globWO = null;
        PointerBuffer globWS = null;
        PointerBuffer locWS = null;

        if(globalWorkOffset != 0) {
            globWO = copy2NIO(ibA, globalWorkOffset);
        }
        if(globalWorkSize != 0) {
            globWS = copy2NIO(ibB, globalWorkSize);
        }
        if(localWorkSize != 0) {
            locWS = copy2NIO(ibC, localWorkSize);
        }

        this.putNDRangeKernel(kernel, 1, globWO, globWS, locWS, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put2DRangeKernel(final CLKernel kernel, final long globalWorkOffsetX, final long globalWorkOffsetY,
                                                            final long globalWorkSizeX, final long globalWorkSizeY,
                                                            final long localWorkSizeX, final long localWorkSizeY) {
        this.put2DRangeKernel(kernel,
                globalWorkOffsetX, globalWorkOffsetY,
                globalWorkSizeX, globalWorkSizeY,
                localWorkSizeX, localWorkSizeY, null, null);

        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put2DRangeKernel(final CLKernel kernel, final long globalWorkOffsetX, final long globalWorkOffsetY,
                                                            final long globalWorkSizeX, final long globalWorkSizeY,
                                                            final long localWorkSizeX, final long localWorkSizeY, final CLEventList events) {
        this.put2DRangeKernel(kernel,
                globalWorkOffsetX, globalWorkOffsetY,
                globalWorkSizeX, globalWorkSizeY,
                localWorkSizeX, localWorkSizeY, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put2DRangeKernel(final CLKernel kernel, final long globalWorkOffsetX, final long globalWorkOffsetY,
                                                            final long globalWorkSizeX, final long globalWorkSizeY,
                                                            final long localWorkSizeX, final long localWorkSizeY, final CLEventList condition, final CLEventList events) {
        PointerBuffer globalWorkOffset = null;
        PointerBuffer globalWorkSize = null;
        PointerBuffer localWorkSize = null;

        if(globalWorkOffsetX != 0 || globalWorkOffsetY != 0) {
            globalWorkOffset = copy2NIO(ibA, globalWorkOffsetX, globalWorkOffsetY);
        }
        if(globalWorkSizeX != 0 || globalWorkSizeY != 0) {
            globalWorkSize = copy2NIO(ibB, globalWorkSizeX, globalWorkSizeY);
        }
        if(localWorkSizeX != 0 || localWorkSizeY != 0) {
            localWorkSize = copy2NIO(ibC, localWorkSizeX, localWorkSizeY);
        }
        this.putNDRangeKernel(kernel, 2, globalWorkOffset, globalWorkSize, localWorkSize, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put3DRangeKernel(final CLKernel kernel, final long globalWorkOffsetX, final long globalWorkOffsetY, final long globalWorkOffsetZ,
                                                            final long globalWorkSizeX, final long globalWorkSizeY, final long globalWorkSizeZ,
                                                            final long localWorkSizeX, final long localWorkSizeY, final long localWorkSizeZ) {
        this.put3DRangeKernel(kernel,
                globalWorkOffsetX, globalWorkOffsetY, globalWorkOffsetZ,
                globalWorkSizeX, globalWorkSizeY, globalWorkSizeZ,
                localWorkSizeX, localWorkSizeY, localWorkSizeZ, null, null);

        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put3DRangeKernel(final CLKernel kernel, final long globalWorkOffsetX, final long globalWorkOffsetY, final long globalWorkOffsetZ,
                                                            final long globalWorkSizeX, final long globalWorkSizeY, final long globalWorkSizeZ,
                                                            final long localWorkSizeX, final long localWorkSizeY, final long localWorkSizeZ, final CLEventList events) {
        this.put3DRangeKernel(kernel,
                globalWorkOffsetX, globalWorkOffsetY, globalWorkOffsetZ,
                globalWorkSizeX, globalWorkSizeY, globalWorkSizeZ,
                localWorkSizeX, localWorkSizeY, localWorkSizeZ, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put3DRangeKernel(final CLKernel kernel, final long globalWorkOffsetX, final long globalWorkOffsetY, final long globalWorkOffsetZ,
                                                            final long globalWorkSizeX, final long globalWorkSizeY, final long globalWorkSizeZ,
                                                            final long localWorkSizeX, final long localWorkSizeY, final long localWorkSizeZ, final CLEventList condition, final CLEventList events) {
        PointerBuffer globalWorkOffset = null;
        PointerBuffer globalWorkSize = null;
        PointerBuffer localWorkSize = null;

        if(globalWorkOffsetX != 0 || globalWorkOffsetY != 0 || globalWorkOffsetZ != 0) {
            globalWorkOffset = copy2NIO(ibA, globalWorkOffsetX, globalWorkOffsetY, globalWorkOffsetZ);
        }
        if(globalWorkSizeX != 0 || globalWorkSizeY != 0 || globalWorkSizeZ != 0) {
            globalWorkSize = copy2NIO(ibB, globalWorkSizeX, globalWorkSizeY, globalWorkSizeZ);
        }
        if(localWorkSizeX != 0 || localWorkSizeY != 0 || localWorkSizeZ != 0) {
            localWorkSize = copy2NIO(ibC, localWorkSizeX, localWorkSizeY, localWorkSizeZ);
        }
        this.putNDRangeKernel(kernel, 3, globalWorkOffset, globalWorkSize, localWorkSize, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue putNDRangeKernel(final CLKernel kernel, final int workDimension, final PointerBuffer globalWorkOffset, final PointerBuffer globalWorkSize, final PointerBuffer localWorkSize) {
        this.putNDRangeKernel(kernel, workDimension, globalWorkOffset, globalWorkSize, localWorkSize, null, null);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue putNDRangeKernel(final CLKernel kernel, final int workDimension, final PointerBuffer globalWorkOffset, final PointerBuffer globalWorkSize, final PointerBuffer localWorkSize, final CLEventList events) {
        this.putNDRangeKernel(kernel, workDimension, globalWorkOffset, globalWorkSize, localWorkSize, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue putNDRangeKernel(final CLKernel kernel, final int workDimension, final PointerBuffer globalWorkOffset,
            final PointerBuffer globalWorkSize, final PointerBuffer localWorkSize, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        final int ret = cl.clEnqueueNDRangeKernel(
                ID, kernel.ID, workDimension,
                globalWorkOffset,
                globalWorkSize,
                localWorkSize,
                conditions, conditionIDs,
                events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue "+workDimension+"DRange " + kernel+ "\n"
                    + " with gwo: " + toStr(globalWorkOffset)
                    + " gws: " + toStr(globalWorkSize)
                    + " lws: " + toStr(localWorkSize)
                    + " " + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putAcquireGLObject(final CLGLObject glObject) {
        this.putAcquireGLObject(glObject, null, null);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putAcquireGLObject(final CLGLObject glObject, final CLEventList events) {
        this.putAcquireGLObject(glObject, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putAcquireGLObject(final CLGLObject glObject, final CLEventList condition, final CLEventList events) {
        this.putAcquireGLObjects(copy2NIO(ibA, glObject.getID()), condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putAcquireGLObjects(final CLGLObject glObject1, final CLGLObject glObject2, final CLEventList condition, final CLEventList events) {
        this.putAcquireGLObjects(copy2NIO(ibA, glObject1.getID(), glObject2.getID()), condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putAcquireGLObjects(final CLGLObject glObject1, final CLGLObject glObject2, final CLGLObject glObject3, final CLEventList condition, final CLEventList events) {
        this.putAcquireGLObjects(copy2NIO(ibA, glObject1.getID(), glObject2.getID(), glObject3.getID()), condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putAcquireGLObjects(final PointerBuffer glObjectIDs, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        final CLGL xl = (CLGL) cl;

        final int ret = xl.clEnqueueAcquireGLObjects(ID, glObjectIDs.remaining(), glObjectIDs,
                    conditions, conditionIDs,
                    events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not aquire " + glObjectIDs + " with " + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    /**
     * Calls {@native clEnqueueReleaseGLObjects}.
     */
    public CLCommandQueue putReleaseGLObject(final CLGLObject glObject) {
        this.putReleaseGLObject(glObject, null);
        return this;
    }

    /**
     * Calls {@native clEnqueueReleaseGLObjects}.
     */
    public CLCommandQueue putReleaseGLObject(final CLGLObject glObject, final CLEventList events) {
        this.putReleaseGLObject(glObject, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueReleaseGLObjects}.
     */
    public CLCommandQueue putReleaseGLObject(final CLGLObject glObject, final CLEventList condition, final CLEventList events) {
        this.putReleaseGLObjects(copy2NIO(ibA, glObject.getID()), condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putReleaseGLObjects(final CLGLObject glObject1, final CLGLObject glObject2, final CLEventList condition, final CLEventList events) {
        this.putReleaseGLObjects(copy2NIO(ibA, glObject1.getID(), glObject2.getID()), condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putReleaseGLObjects(final CLGLObject glObject1, final CLGLObject glObject2, final CLGLObject glObject3, final CLEventList condition, final CLEventList events) {
        this.putReleaseGLObjects(copy2NIO(ibA, glObject1.getID(), glObject2.getID(), glObject3.getID()), condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueReleaseGLObjects}.
     */
    public CLCommandQueue putReleaseGLObjects(final PointerBuffer glObjectIDs, final CLEventList condition, final CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        final CLGL xl = (CLGL) cl;

        final int ret = xl.clEnqueueReleaseGLObjects(ID, glObjectIDs.remaining(), glObjectIDs,
                conditions, conditionIDs,
                events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not release " + glObjectIDs + "with " + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    /**
     * Calls {@native clFinish}.
     */
    public CLCommandQueue finish() {
        final int ret = cl.clFinish(ID);
        checkForError(ret, "can not finish command queue");
        return this;
    }

    /**
     * Calls {@native clFlush}.
     */
    public CLCommandQueue flush() {
        final int ret = cl.clFlush(ID);
        checkForError(ret, "can not flush command queue");
        return this;
    }

    /**
     * Returns true only when {@link Mode#PROFILING_MODE} has been enabled.
     */
    public boolean isProfilingEnabled() {
        return (Mode.PROFILING_MODE.QUEUE_MODE & properties) != 0;
    }

    /**
     * Returns true only when {@link Mode#OUT_OF_ORDER_MODE} mode has been enabled.
     */
    public boolean isOutOfOrderModeEnabled() {
        return (Mode.OUT_OF_ORDER_MODE.QUEUE_MODE & properties) != 0;
    }

    @Override
    public void release() {
        super.release();
        final int ret = cl.clReleaseCommandQueue(ID);
        context.onCommandQueueReleased(device, this);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not release "+this);
        }
    }

    private static PointerBuffer copy2NIO(final PointerBuffer buffer, final long a) {
        return buffer.put(2, a).position(2);
    }

    private static PointerBuffer copy2NIO(final PointerBuffer buffer, final long a, final long b) {
        return buffer.position(1).put(a).put(b).position(1);
    }

    private static PointerBuffer copy2NIO(final PointerBuffer buffer, final long a, final long b, final long c) {
        return buffer.rewind().put(a).put(b).put(c).rewind();
    }

    private static String toStr(final PointerBuffer buffer) {
        if(buffer == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (int i = buffer.position(); i < buffer.capacity(); i++) {
            sb.append(buffer.get(i));
            if(i != buffer.capacity()-1) {
                sb.append(", ");
            }
        }
        return sb.append('}').toString();
    }

    private static String toStr(final CLEventList condition, final CLEventList events) {
        return "\ncond.: " + condition +" events: "+events;
    }

    private String toStr(final Integer... values) {
        return Arrays.asList(values).toString();
    }

    private String bufferRectToString(final String action, final CLBuffer<?> buffer,
            final long rowPitch, final long slicePitch, final long hostRowPitch, final long hostSlicePitch,
            final int originX, final int originY, final int originZ, final int hostX, final int hostY, final int hostZ,
            final int rangeX, final int rangeY, final int rangeZ, final CLEventList condition, final CLEventList events) {
        return "can not enqueue "+action+"-buffer-rect: " + buffer + "\n"
                + " with rowPitch: " + rowPitch + " slicePitch: " + slicePitch
                + " hostRowPitch: " + hostRowPitch + " hostSlicePitch: " + hostSlicePitch + "\n"
                + " origin: " + toStr(originX, originY, originZ) + " hostPos: " + toStr(hostX, hostY, hostZ)
                + " range: " + toStr(rangeX, rangeY, rangeZ) + toStr(condition, events);
    }

    /**
     * Returns the device of this command queue.
     */
    public CLDevice getDevice() {
        return device;
    }

    /**
     * Returns the command queue properties as EnumSet.
     */
    public EnumSet<Mode> getProperties() {
        return Mode.valuesOf(properties);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +" "+getProperties()+" on "+ getDevice();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CLCommandQueue other = (CLCommandQueue) obj;
        if (this.ID != other.ID) {
            return false;
        }
        if (this.context != other.context && (this.context == null || !this.context.equals(other.context))) {
            return false;
        }
        if (this.device != other.device && (this.device == null || !this.device.equals(other.device))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + (int) (this.ID ^ (this.ID >>> 32));
        hash = 89 * hash + (this.context != null ? this.context.hashCode() : 0);
        hash = 89 * hash + (this.device != null ? this.device.hashCode() : 0);
        return hash;
    }

    /**
     * Enumeration for the command-queue settings.
     */
    public enum Mode {
        /**
         * If set, the commands in the command-queue are
         * executed out-of-order. Otherwise, commands are executed in-order.
         */
        OUT_OF_ORDER_MODE(CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE),

        /**
         * Enables profiling of commands in the command-queue.
         * If set, the profiling of commands is enabled. Otherwise profiling of
         * commands is disabled. See {@link com.jogamp.opencl.CLEvent} for more information.
         */
        PROFILING_MODE(CL_QUEUE_PROFILING_ENABLE);

        /**
         * Value of wrapped OpenCL device type.
         */
        public final int QUEUE_MODE;

        private Mode(final int value) {
            this.QUEUE_MODE = value;
        }

        public static Mode valueOf(final int queueMode) {
            switch(queueMode) {
                case(CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE):
                    return OUT_OF_ORDER_MODE;
                case(CL_QUEUE_PROFILING_ENABLE):
                    return PROFILING_MODE;
            }
            return null;
        }

        public static EnumSet<Mode> valuesOf(final long bitfield) {
            final List<Mode> matching = new ArrayList<Mode>();
            final Mode[] values = Mode.values();
            for (final Mode value : values) {
                if((value.QUEUE_MODE & bitfield) != 0)
                    matching.add(value);
            }
            if(matching.isEmpty())
                return EnumSet.noneOf(Mode.class);
            else
                return EnumSet.copyOf(matching);
        }

    }
}
