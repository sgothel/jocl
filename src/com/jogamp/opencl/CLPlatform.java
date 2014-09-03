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

import com.jogamp.opencl.llb.CLPlatformBinding;
import com.jogamp.opencl.llb.CLProgramBinding;
import com.jogamp.opencl.llb.CLSamplerBinding;
import com.jogamp.opencl.llb.CLKernelBinding;
import com.jogamp.opencl.llb.CLImageBinding;
import com.jogamp.opencl.llb.CL;
import com.jogamp.opencl.impl.CLTLAccessorFactory;
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.JogampRuntimeException;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.opencl.llb.CLBufferBinding;
import com.jogamp.opencl.llb.CLCommandQueueBinding;
import com.jogamp.opencl.llb.CLContextBinding;
import com.jogamp.opencl.llb.CLDeviceBinding;
import com.jogamp.opencl.llb.CLEventBinding;
import com.jogamp.opencl.llb.CLMemObjBinding;
import com.jogamp.opencl.spi.CLPlatformInfoAccessor;
import com.jogamp.opencl.util.CLUtil;
import com.jogamp.opencl.llb.impl.CLAbstractImpl;
import com.jogamp.opencl.llb.impl.CLImpl;
import com.jogamp.opencl.spi.CLAccessorFactory;
import com.jogamp.opencl.util.Filter;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import static com.jogamp.opencl.CLException.*;
import static com.jogamp.opencl.llb.CL.*;

/**
 * CLPlatfrorm representing a OpenCL implementation (e.g. graphics driver).
 *
 * optional eager initialization:
 * <p><pre>
 *     if( !CLPlatform.isAvailable() ) {
 *        return; // abort
 *     }
 *     try{
 *          CLPlatform.initialize();
 *     }catch(JogampRuntimeException ex) {
 *          throw new RuntimeException("could not load Java OpenCL Binding");
 *     }
 * </pre></p>
 *
 * Example initialization:
 * <p><pre>
 *     if( !CLPlatform.isAvailable() ) {
 *        return; // abort
 *     }
 *     CLPlatform platform = CLPlatform.getDefault(type(GPU));
 *
 *     if(platform == null) {
 *          throw new RuntimeException("please update your graphics drivers");
 *     }
 *
 *     CLContext context = CLContext.create(platform.getMaxFlopsDevice());
 *     try {
 *          // use it
 *     }finally{
 *          context.release();
 *     }
 * </pre></p>
 * concurrency:<br/>
 * CLPlatform is threadsafe.
 *
 * @author Michael Bien, et al.
 * @see #isAvailable()
 * @see #initialize()
 * @see #getDefault()
 * @see #listCLPlatforms()
 */
public class CLPlatform {

    /**
     * OpenCL platform id for this platform.
     */
    public final long ID;

    /**
     * Version of this OpenCL platform.
     */
    public final CLVersion version;

    protected static CL cl;
    private static CLAccessorFactory defaultFactory;
    private final CLAccessorFactory factory;

    private Set<String> extensions;

    protected final CLPlatformInfoAccessor info;

    private CLPlatform(final long id) {
        this(id, null);
    }

    protected CLPlatform(final long id, final CLAccessorFactory factory) {
        initialize();
        this.ID = id;
        if(factory == null) {
            this.factory = defaultFactory;
        }else{
            this.factory = factory;
        }
        this.info = this.factory.createPlatformInfoAccessor(cl, id);
        this.version = new CLVersion(getInfoString(CL_PLATFORM_VERSION));
    }

    /**
     * @returns true if OpenCL is available on this machine,
     * i.e. all native libraries could be loaded (CL and CL/JNI).
     */
    public static boolean isAvailable() { return CLAbstractImpl.isAvailable(); }

    /**
     * Eagerly initializes JOCL. Subsequent calls do nothing.
     * @throws JogampRuntimeException if something went wrong in the initialization (e.g. OpenCL lib not found).
     * @see #isAvailable()
     */
    public static void initialize() throws JogampRuntimeException {
        initialize(null);
    }

    // keep package private until SPI is stablized
    /**
     * Eagerly initializes JOCL. Subsequent calls do nothing.
     * @param factory CLAccessorFactory used for creating the bindings.
     * @throws JogampRuntimeException if something went wrong in the initialization (e.g. OpenCL lib not found).
     * @see #isAvailable()
     */
    synchronized static void initialize(final CLAccessorFactory factory) throws JogampRuntimeException {
        if(cl != null) {
            return;
        }

        if(defaultFactory == null) {
            if(factory == null) {
                defaultFactory = new CLTLAccessorFactory();
            }else{
                defaultFactory = factory;
            }
        }

        if( !CLAbstractImpl.isAvailable() ) {
            throw new JogampRuntimeException("JOCL is not available");
        }
        cl = new CLImpl();
    }

    /**
     * Returns the default OpenCL platform or null when no platform found.
     */
    public static CLPlatform getDefault() {
        initialize();
        return latest(listCLPlatforms());
    }

    /**
     * Returns the default OpenCL platform or null when no platform found.
     */
    public static CLPlatform getDefault(final Filter<CLPlatform>... filter) {
        final CLPlatform[] platforms = listCLPlatforms(filter);
        if(platforms.length > 0) {
            return latest(platforms);
        }else{
            return null;
        }
    }

    private static CLPlatform latest(final CLPlatform[] platforms) {
        CLPlatform best = platforms[0];
        for (final CLPlatform platform : platforms) {
            if (platform.version.compareTo(best.version) > 0) {
                best = platform;
            }
        }
        return best;
    }

    /**
     * Lists all available OpenCL implementations.
     * @throws CLException if something went wrong initializing OpenCL
     */
    public static CLPlatform[] listCLPlatforms() {
        return listCLPlatforms((Filter<CLPlatform>[])null);
    }

    /**
     * Lists all available OpenCL implementations. The platforms returned must pass all filters.
     * @param filter Acceptance filter for the returned platforms.
     * @throws CLException if something went wrong initializing OpenCL
     */
    public static CLPlatform[] listCLPlatforms(final Filter<CLPlatform>... filter) {
        initialize();

        final IntBuffer ib = Buffers.newDirectIntBuffer(1);
        // find all available OpenCL platforms
        int ret = cl.clGetPlatformIDs(0, null, ib);
        checkForError(ret, "can not enumerate platforms");

        // receive platform ids
        final PointerBuffer platformId = PointerBuffer.allocateDirect(ib.get(0));
        ret = cl.clGetPlatformIDs(platformId.capacity(), platformId, null);
        checkForError(ret, "can not enumerate platforms");

        final List<CLPlatform> platforms = new ArrayList<CLPlatform>();

        for (int i = 0; i < platformId.capacity(); i++) {
            final CLPlatform platform = new CLPlatform(platformId.get(i));
            addIfAccepted(platform, platforms, filter);
        }

        return platforms.toArray(new CLPlatform[platforms.size()]);
    }

    /**
     * Returns the low level binding interface to the OpenCL APIs.
     */
    public static CL getLowLevelCLInterface() {
        initialize();
        return cl;
    }

    /**
     * Hint to allow the implementation to release the resources allocated by the OpenCL compiler.
     * Calls to {@link CLProgram#build()} after unloadCompiler will reload the compiler if necessary.
     */
    public static void unloadCompiler() {
        initialize();
        final int ret = cl.clUnloadCompiler();
        checkForError(ret, "error while sending unload compiler hint");
    }

    /**
     * Lists all physical devices available on this platform.
     * @see #listCLDevices(com.jogamp.opencl.CLDevice.Type...)
     */
    public CLDevice[] listCLDevices() {
        return this.listCLDevices(CLDevice.Type.ALL);
    }

    /**
     * Lists all physical devices available on this platform matching the given {@link CLDevice.Type}.
     */
    public CLDevice[] listCLDevices(final CLDevice.Type... types) {
        initialize();

        final List<CLDevice> list = new ArrayList<CLDevice>();

        for(int t = 0; t < types.length; t++) {
            final CLDevice.Type type = types[t];

            final long[] deviceIDs = info.getDeviceIDs(type.TYPE);

            //add device to list
            for (int n = 0; n < deviceIDs.length; n++) {
                list.add(createDevice(deviceIDs[n]));
            }
        }

        return list.toArray(new CLDevice[list.size()]);

    }

    /**
     * Lists all physical devices available on this platform matching the given {@link Filter}.
     */
    public CLDevice[] listCLDevices(final Filter<CLDevice>... filters) {
        initialize();

        final List<CLDevice> list = new ArrayList<CLDevice>();

        final long[] deviceIDs = info.getDeviceIDs(CL_DEVICE_TYPE_ALL);

        //add device to list
        for (int n = 0; n < deviceIDs.length; n++) {
            final CLDevice device = createDevice(deviceIDs[n]);
            addIfAccepted(device, list, filters);
        }

        return list.toArray(new CLDevice[list.size()]);

    }

    protected CLDevice createDevice(final long id) {
        return new CLDevice(this, id);
    }

    private static <I> void addIfAccepted(final I item, final List<I> list, final Filter<I>[] filters) {
        if(filters == null) {
            list.add(item);
        }else{
            boolean accepted = true;
            for (final Filter<I> filter : filters) {
                if(!filter.accept(item)) {
                    accepted = false;
                    break;
                }
            }
            if(accepted) {
                list.add(item);
            }
        }
    }

    static CLDevice findMaxFlopsDevice(final CLDevice[] devices) {
        return findMaxFlopsDevice(devices, null);
    }

    static CLDevice findMaxFlopsDevice(final CLDevice[] devices, final CLDevice.Type type) {
        initialize();

        CLDevice maxFLOPSDevice = null;

        int maxflops = -1;

        for (int i = 0; i < devices.length; i++) {

            final CLDevice device = devices[i];

            if(type == null || type.equals(device.getType())) {

                final int maxComputeUnits     = device.getMaxComputeUnits();
                final int maxClockFrequency   = device.getMaxClockFrequency();
                final int flops = maxComputeUnits*maxClockFrequency;

                if(flops > maxflops) {
                    maxflops = flops;
                    maxFLOPSDevice = device;
                }
            }

        }

        return maxFLOPSDevice;
    }


    /**
     * Returns the device with maximal FLOPS from this platform.
     * The device speed is estimated by calculating the product of
     * MAX_COMPUTE_UNITS and MAX_CLOCK_FREQUENCY.
     * @see #getMaxFlopsDevice(com.jogamp.opencl.CLDevice.Type...)
     */
    public CLDevice getMaxFlopsDevice() {
        return findMaxFlopsDevice(listCLDevices());
    }

    /**
     * Returns the device with maximal FLOPS and the specified type from this platform.
     * The device speed is estimated by calculating the product of
     * MAX_COMPUTE_UNITS and MAX_CLOCK_FREQUENCY.
     */
    public CLDevice getMaxFlopsDevice(final CLDevice.Type... types) {
        return findMaxFlopsDevice(listCLDevices(types));
    }

    /**
     * Returns the device with maximal FLOPS and the specified type from this platform.
     * The device speed is estimated by calculating the product of
     * MAX_COMPUTE_UNITS and MAX_CLOCK_FREQUENCY.
     */
    public CLDevice getMaxFlopsDevice(final Filter<CLDevice>... filter) {
        return findMaxFlopsDevice(listCLDevices(filter));
    }

    /**
     * Returns the platform name.
     */
    @CLProperty("CL_PLATFORM_NAME")
    public String getName() {
        return getInfoString(CL_PLATFORM_NAME);
    }

    /**
     * Returns the OpenCL version supported by this platform.
     */
    @CLProperty("CL_PLATFORM_VERSION")
    public CLVersion getVersion() {
        return version;
    }

    /**
     * Returns the OpenCL Specification version supported by this platform.
     */
    public String getSpecVersion() {
        return version.getSpecVersion();
    }

    /**
     * @see CLVersion#isAtLeast(com.jogamp.opencl.CLVersion)
     */
    public boolean isAtLeast(final CLVersion other) {
        return version.isAtLeast(other);
    }

    /**
     * @see CLVersion#isAtLeast(int, int)
     */
    public boolean isAtLeast(final int major, final int minor) {
        return version.isAtLeast(major, minor);
    }

    /**
     * Returns the platform profile.
     */
    @CLProperty("CL_PLATFORM_PROFILE")
    public String getProfile() {
        return getInfoString(CL_PLATFORM_PROFILE);
    }

    /**
     * Returns the platform vendor.
     */
    @CLProperty("CL_PLATFORM_VENDOR")
    public String getVendor() {
        return getInfoString(CL_PLATFORM_VENDOR);
    }

    /**
     * @return true if the vendor is AMD.
     */
    public boolean isVendorAMD() {
        return getVendor().contains("Advanced Micro Devices");
    }
    /**
     * Returns the ICD suffix.
     */
    @CLProperty("CL_PLATFORM_ICD_SUFFIX_KHR")
    public String getICDSuffix() {
        return getInfoString(CL_PLATFORM_ICD_SUFFIX_KHR);
    }

    /**
     * Returns true if the extension is supported on this platform.
     */
    public boolean isExtensionAvailable(final String extension) {
        return getExtensions().contains(extension);
    }

    /**
     * Returns all platform extension names as unmodifiable Set.
     */
    @CLProperty("CL_PLATFORM_EXTENSIONS")
    public synchronized Set<String> getExtensions() {

        if(extensions == null) {
            extensions = new HashSet<String>();
            final String ext = getInfoString(CL_PLATFORM_EXTENSIONS);
            final Scanner scanner = new Scanner(ext);

            while(scanner.hasNext())
                extensions.add(scanner.next());

            scanner.close();
            extensions = Collections.unmodifiableSet(extensions);
        }

        return extensions;
    }

    /**
     * Returns a Map of platform properties with the enum names as keys.
     * @see CLUtil#obtainPlatformProperties(com.jogamp.opencl.CLPlatform)
     */
    public Map<String, String> getProperties() {
        return CLUtil.obtainPlatformProperties(this);
    }

    /**
     * Returns a info string in exchange for a key (CL_PLATFORM_*).
     */
    public final String getInfoString(final int key) {
        return info.getString(key);
    }

    final CLAccessorFactory getAccessorFactory(){
        return factory;
    }

    public final CLPlatformInfoAccessor getCLAccessor(){
        return info;
    }

    protected CLBufferBinding getBufferBinding() {
        return cl;
    }

    protected CLCommandQueueBinding getCommandQueueBinding() {
        return cl;
    }

    protected CLContextBinding getContextBinding() {
        return cl;
    }

    protected CLDeviceBinding getDeviceBinding() {
        return cl;
    }

    protected CLEventBinding getEventBinding() {
        return cl;
    }

    protected CLImageBinding getImageBinding() {
        return cl;
    }

    protected CLKernelBinding getKernelBinding() {
        return cl;
    }

    protected CLMemObjBinding getMemObjectBinding() {
        return cl;
    }

    protected CLPlatformBinding getPlatformBinding() {
        return cl;
    }

    protected CLProgramBinding getProgramBinding() {
        return cl;
    }

    protected CLSamplerBinding getSamplerBinding() {
        return cl;
    }

    protected CL getCLBinding() {
        return cl;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+" [name: " + getName()
                         +", vendor: "+getVendor()
                         +", profile: "+getProfile()
                         +", version: "+getVersion()+"]";
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CLPlatform other = (CLPlatform) obj;
        if (this.ID != other.ID) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + (int) (this.ID ^ (this.ID >>> 32));
        return hash;
    }

}
