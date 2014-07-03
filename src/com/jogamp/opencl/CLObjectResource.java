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
 * Created on Saturday, June 18 2011 02:36
 */
package com.jogamp.opencl;

/**
 * Releasable resource with an CL object ID.
 * @author Michael Bien, et al.
 */
abstract class CLObjectResource extends CLObject implements CLResource, AutoCloseable {

    private boolean released;

    public CLObjectResource(final long ID) {
        super(ID);
    }

    public CLObjectResource(final CLContext context, final long ID) {
        super(context, ID);
    }

    public void release() {
        if(released) {
            throw new RuntimeException(getClass().getSimpleName()+" was already released.");
        }else{
            released = true;
        }
    }

    /**
     * Implementation detail, satisfying {@link AutoCloseable}.
     * Might be remove as soon we have extension methods, but shall validate with Java 1.5 specs.
     * @deprecated This method is not intended to be called from client code.
     * @see java.lang.AutoCloseable
     */
    @Deprecated
    @Override
    public final void close() {
        if(this instanceof CLResource) {
            ((CLResource)this).release();
        }
    }

    public boolean isReleased() {
        return released;
    }


}
