/*
 * Copyright (c) 2011 JogAmp Community. All rights reserved.
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

/*
 * Created on Wednesday, May 25 2011 00:57
 */

package com.jogamp.opencl.impl;

import java.nio.IntBuffer;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.opencl.llb.CL;
import com.jogamp.opencl.llb.CLDeviceBinding;
import com.jogamp.opencl.spi.CLAccessorFactory;
import com.jogamp.opencl.spi.CLInfoAccessor;
import com.jogamp.opencl.spi.CLPlatformInfoAccessor;
import java.nio.Buffer;

import static com.jogamp.opencl.CLException.*;

/**
 *
 * @author Michael Bien, et al.
 */
public class CLTLAccessorFactory implements CLAccessorFactory {

    @Override
    public CLInfoAccessor createDeviceInfoAccessor(final CLDeviceBinding cl, final long id) {
        return new CLDeviceInfoAccessor(cl, id);
    }

    @Override
    public CLPlatformInfoAccessor createPlatformInfoAccessor(final CL cl, final long id) {
        return new CLTLPlatformInfoAccessor(cl, id);
    }

    private final static class CLDeviceInfoAccessor extends CLTLInfoAccessor {

        private final CLDeviceBinding cl;
        private final long ID;

        private CLDeviceInfoAccessor(final CLDeviceBinding cl, final long id) {
            this.cl = cl;
            this.ID = id;
        }

        @Override
        public int getInfo(final int name, final long valueSize, final Buffer value, final PointerBuffer valueSizeRet) {
            return cl.clGetDeviceInfo(ID, name, valueSize, value, valueSizeRet);
        }

    }

    private final static class CLTLPlatformInfoAccessor extends CLTLInfoAccessor implements CLPlatformInfoAccessor {

        private final long ID;
        private final CL cl;

        private CLTLPlatformInfoAccessor(final CL cl, final long id) {
            this.ID = id;
            this.cl = cl;
        }

        @Override
        public int getInfo(final int name, final long valueSize, final Buffer value, final PointerBuffer valueSizeRet) {
            return cl.clGetPlatformInfo(ID, name, valueSize, value, valueSizeRet);
        }

        @Override
        public long[] getDeviceIDs(final long type) {

            final IntBuffer buffer = getBB(4).asIntBuffer();
            int ret = cl.clGetDeviceIDs(ID, type, 0, null, buffer);
            final int count = buffer.get(0);

            // return an empty buffer rather than throwing an exception
            if(ret == CLDeviceBinding.CL_DEVICE_NOT_FOUND || count == 0) {
                return new long[0];
            }else{
                checkForError(ret, "error while enumerating devices");

                final PointerBuffer deviceIDs = PointerBuffer.wrap(getBB(count*PointerBuffer.ELEMENT_SIZE));
                ret = cl.clGetDeviceIDs(ID, type, count, deviceIDs, null);
                checkForError(ret, "error while enumerating devices");

                final long[] ids = new long[count];
                for (int i = 0; i < ids.length; i++) {
                    ids[i] = deviceIDs.get(i);
                }
                return ids;
            }

        }

    }

}
