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

import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.opencl.impl.CLTLInfoAccessor;
import com.jogamp.opencl.llb.CLImageBinding;
import java.nio.Buffer;

import static com.jogamp.opencl.llb.CL.*;

/**
 *
 * @author Michael Bien, et al.
 */
public abstract class CLImage<B extends Buffer> extends CLMemory<B>  {

    protected CLImageFormat format;

    final CLTLInfoAccessor imageInfo;

    public final int width;
    public final int height;

    protected CLImage(final CLContext context, final B directBuffer, final CLImageFormat format, final int width, final int height, final long id, final int flags) {
        this(context, directBuffer, format, createAccessor(context, id), width, height, id, flags);
    }

    protected CLImage(final CLContext context, final B directBuffer, final CLImageFormat format, final CLImageInfoAccessor accessor, final int width, final int height, final long id, final int flags) {
        super(context, directBuffer, getSizeImpl(context, id), id, flags);
        this.imageInfo = accessor;
        this.format = format;
        this.width = width;
        this.height = height;
    }

    private static CLImageInfoAccessor createAccessor(final CLContext context, final long id) {
        return new CLImageInfoAccessor(context.getPlatform().getImageBinding(), id);
    }

    protected static CLImageFormat createUninitializedImageFormat() {
        return new CLImageFormat();
    }

    /**
     * Returns the image format descriptor specified when image was created.
     */
    public CLImageFormat getFormat() {
        return format;
    }

    /**
     * Returns the size of each element of the image memory object given by image.
     * An element is made up of n channels. The value of n is given in {@link CLImageFormat} descriptor.
     */
    @Override
    public int getElementSize() {
        return (int)imageInfo.getLong(CL_IMAGE_ELEMENT_SIZE);
    }

    /**
     * Returns the size in bytes of a row of elements of the image object given by image.
     */
    public int getRowPitch() {
        return (int)imageInfo.getLong(CL_IMAGE_ROW_PITCH);
    }

    /**
     * Returns width of this image in pixels.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of this image in pixels.
     */
    public int getHeight() {
        return height;
    }


    protected final static class CLImageInfoAccessor extends CLTLInfoAccessor {

        private final long id;
        private final CLImageBinding cl;

        public CLImageInfoAccessor(final CLImageBinding cl, final long id) {
            this.cl = cl;
            this.id = id;
        }
        @Override
        public int getInfo(final int name, final long valueSize, final Buffer value, final PointerBuffer valueSizeRet) {
            return cl.clGetImageInfo(id, name, valueSize, value, valueSizeRet);
        }
    }


}
