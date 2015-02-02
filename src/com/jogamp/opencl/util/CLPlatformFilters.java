/*
 * Copyright 2009 - 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.opencl.util;

import com.jogamp.opencl.CLCommandQueue.Mode;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.CLVersion;
import java.util.Arrays;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLContext;

/**
 * Pre-defined filters.
 * @author Michael Bien
 * @see CLPlatform#getDefault(com.jogamp.opencl.util.Filter[])
 * @see CLPlatform#listCLPlatforms(com.jogamp.opencl.util.Filter[])
 */
public class CLPlatformFilters {

    /**
     * Accepts all platforms supporting at least the given OpenCL spec version.
     */
    public static Filter<CLPlatform> version(final CLVersion version) {
        return new Filter<CLPlatform>() {
            public boolean accept(final CLPlatform item) {
                return item.isAtLeast(version);
            }
        };
    }

    /**
     * Accepts all platforms containing devices of the given type.
     */
    public static Filter<CLPlatform> type(final CLDevice.Type type) {
        return new Filter<CLPlatform>() {
            public boolean accept(final CLPlatform item) {
                return item.listCLDevices(type).length > 0;
            }
        };
    }

    /**
     * Accepts all platforms containing at least one devices of which supports OpenGL-OpenCL interoperability.
     */
    public static Filter<CLPlatform> glSharing() {
        return new Filter<CLPlatform>() {
            private final Filter<CLDevice> glFilter = CLDeviceFilters.glSharing();
            public boolean accept(final CLPlatform item) {
                final CLDevice[] devices = item.listCLDevices();
                for (final CLDevice device : devices) {
                    if(glFilter.accept(device)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Accepts all with the given OpenGL context compatible platforms containing at least one
     * devices of which supports OpenGL-OpenCL interoperability.
     */
    public static Filter<CLPlatform> glSharing(final GLContext context) {
        return new Filter<CLPlatform>() {
            private final Filter<CLPlatform> glFilter = glSharing();
            public boolean accept(final CLPlatform item) {
                final String glVendor = context.getGL().glGetString(GL.GL_VENDOR);
                final String clVendor = item.getVendor();
                return areVendorsCompatible(glVendor,clVendor) && glFilter.accept(item);
            }
        };
    }

    /**
     * We need this test because:
     *  - On at least some AMD cards, the GL vendor is ATI, but the CL vendor is AMD.
     *  - On at least some Macs, the GL vendor is Nvidia, but the CL vendor is Apple.
     * @param glVendor OpenGL vendor string.
     * @param clVendor OpenCL vendor string.
     * @return true if the strings are either the same, or indicate that they're part of the same card.
     */
    private static boolean areVendorsCompatible(final String glVendor, final String clVendor) {
        return(   clVendor.equals(glVendor)
               || (glVendor.contains("ATI Technologies") && clVendor.contains("Advanced Micro Devices"))
               || (glVendor.contains("NVIDIA Corporation") && clVendor.contains("Apple")));
    }

    /**
     * Accepts all platforms supporting the given extensions.
     */
    public static Filter<CLPlatform> extension(final String... extensions) {
        return new Filter<CLPlatform>() {
            public boolean accept(final CLPlatform item) {
                return item.getExtensions().containsAll(Arrays.asList(extensions));
            }
        };
    }

    /**
     * Accepts all platforms containing at least one devices supporting the specified command queue modes.
     */
    public static Filter<CLPlatform> queueMode(final Mode... modes) {
        return new Filter<CLPlatform>() {
            private final Filter<CLDevice> queueModeFilter = CLDeviceFilters.queueMode(modes);
            public boolean accept(final CLPlatform item) {
                for (final CLDevice device : item.listCLDevices()) {
                    if(queueModeFilter.accept(device)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
