/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

package com.jogamp.opencl.llb.impl;

import com.jogamp.common.jvm.JNILibLoaderBase;
import com.jogamp.common.os.DynamicLibraryBundle;
import com.jogamp.common.os.DynamicLibraryBundleInfo;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.RunnableExecutor;
import com.jogamp.common.util.cache.TempJarCache;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

import jogamp.common.os.PlatformPropsImpl;

public final class CLDynamicLibraryBundleInfo implements DynamicLibraryBundleInfo  {
    private static final boolean isAndroid;
    private static final List<String> glueLibNames;

    static {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                Platform.initSingleton();

                if( TempJarCache.isInitialized() ) {
                   // only: jocl.jar -> jocl-natives-<os.and.arch>.jar
                   JNILibLoaderBase.addNativeJarLibs(new Class<?>[] { jogamp.opencl.Debug.class }, null );
                }
                return null;
            }
        });
        isAndroid = Platform.OSType.ANDROID == PlatformPropsImpl.OS_TYPE;

        glueLibNames = new ArrayList<String>();
        glueLibNames.add("jocl");
    }

    protected CLDynamicLibraryBundleInfo() {
    }

    /**
     * <p>
     * Returns <code>true</code>,
     * since we might load the library and allow symbol access to subsequent libs.
     * </p>
     */
    @Override
    public final boolean shallLinkGlobal() { return true; }

    /**
     * {@inheritDoc}
     * Returns <code>true</code> on <code>Android</code>,
     * and <code>false</code> otherwise.
     */
    @Override
    public final boolean shallLookupGlobal() {
        if ( isAndroid ) {
            // Android requires global symbol lookup
            return true;
        }
        // default behavior for other platforms
        return false;
    }

    @Override
    public final List<String> getGlueLibNames() {
        return glueLibNames;
    }

    @Override
    public final List<List<String>> getToolLibNames() {
        final List<List<String>> libNamesList = new ArrayList<List<String>>();

        final List<String> libCL = new ArrayList<String>();
        {
            // this is the default OpenCL lib name, according to the spec
            libCL.add("libOpenCL.so.1"); // unix
            libCL.add("OpenCL"); // windows, OSX

            if( isAndroid ) {
                libCL.add("libPVROCL.so");
                libCL.add("/system/vendor/lib/libPVROCL.so");
            } else {
                // try this one as well, if spec fails
                libCL.add("libGL.so.1");
            }

            // ES2: This is the default lib name, according to the spec
            libCL.add("libGLESv2.so.2");

            // ES2: Try these as well, if spec fails
            libCL.add("libGLESv2.so");
            libCL.add("GLESv2");

        }
        libNamesList.add(libCL);

        return libNamesList;
    }

    @Override
    public final List<String> getToolGetProcAddressFuncNameList() {
        final List<String> res = new ArrayList<String>();
        res.add("clGetExtensionFunctionAddress");
        return res;
    }

    private static String Impl_str = "Impl";
    private static int Impl_len = Impl_str.length();

    @Override
    public final long toolGetProcAddress(final long toolGetProcAddressHandle, String funcName) {
        //FIXME workaround to fix a gluegen issue
        if( funcName.endsWith(Impl_str) ) {
            funcName = funcName.substring(0, funcName.length() - Impl_len);
        }
        if( funcName.endsWith("KHR") || funcName.endsWith("EXT") ) {
            return CLAbstractImpl.clGetExtensionFunctionAddress(toolGetProcAddressHandle, funcName);
        }
        return 0; // on libs ..
    }

    @Override
    public final boolean useToolGetProcAdressFirst(final String funcName) {
        return true;
    }

    @Override
    public final RunnableExecutor getLibLoaderExecutor() {
        return DynamicLibraryBundle.getDefaultRunnableExecutor();
    }
}


