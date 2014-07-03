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

package com.jogamp.opencl.impl;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.Bitstream;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.spi.CLInfoAccessor;
import com.jogamp.opencl.util.CLUtil;

/**
 * Internal utility for common OpenCL clGetFooInfo calls.
 * Threadsafe, threadlocal implementation.
 * @author Michael Bien, et al.
 */
public abstract class CLTLInfoAccessor implements CLInfoAccessor {

    private static final int BB_SIZE = 512;

    protected final static ThreadLocal<ByteBuffer> localBB = new ThreadLocal<ByteBuffer>() {

        @Override
        protected ByteBuffer initialValue() {
            return Buffers.newDirectByteBuffer(BB_SIZE);
        }

    };
    protected final static ThreadLocal<PointerBuffer> localNSB = new ThreadLocal<PointerBuffer>() {

        @Override
        protected PointerBuffer initialValue() {
            return PointerBuffer.allocateDirect(1);
        }

    };

    @Override
    public final long getUInt32Long(final int key) {
        final ByteBuffer buffer = getBB(4).putInt(0, 0);
        final int ret = getInfo(key, 4, buffer, null);
        CLException.checkForError(ret, "error while asking for info value");
        return Bitstream.toUInt32Long(buffer.getInt(0));
    }

    @Override
    public final long getLong(final int key) {

        final ByteBuffer buffer = getBB(8).putLong(0, 0);
        final int ret = getInfo(key, 8, buffer, null);
        CLException.checkForError(ret, "error while asking for info value");

        return buffer.getLong(0);
    }

    @Override
    public final String getString(final int key) {

        final PointerBuffer sizeBuffer = getNSB();
        int ret = getInfo(key, 0, null, sizeBuffer);
        CLException.checkForError(ret, "error while asking for info string");

        final int clSize = (int)sizeBuffer.get(0);
        final ByteBuffer buffer = getBB(clSize);

        ret = getInfo(key, buffer.capacity(), buffer, null);
        CLException.checkForError(ret, "error while asking for info string");

        final byte[] array = new byte[clSize];
        buffer.get(array).rewind();

        return CLUtil.clString2JavaString(array, clSize);

    }

    @Override
    public final int[] getInts(final int key, final int n) {
        // FIXME: Really 8 bytes per int on 64bit platforms ?
        final ByteBuffer buffer = getBB(n * (Platform.is32Bit()?4:8));
        final int ret = getInfo(key, buffer.capacity(), buffer, null);
        CLException.checkForError(ret, "error while asking for info value");

        final int[] array = new int[n];
        for(int i = 0; i < array.length; i++) {
            if(Platform.is32Bit()) {
                array[i] = buffer.getInt();
            }else{
                array[i] = (int)buffer.getLong();
            }
        }
        buffer.rewind();

        return array;
    }

    protected ByteBuffer getBB(final int minCapacity) {
        if(minCapacity > BB_SIZE) {
            return Buffers.newDirectByteBuffer(minCapacity);
        }else{
            return localBB.get();
        }
    }

    protected PointerBuffer getNSB() {
        return localNSB.get();
    }

    protected abstract int getInfo(int name, long valueSize, Buffer value, PointerBuffer valueSizeRet);


}
