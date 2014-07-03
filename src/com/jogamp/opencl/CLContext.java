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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.common.os.Platform;
import com.jogamp.opencl.CLDevice.Type;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLSampler.AddressingMode;
import com.jogamp.opencl.CLSampler.FilteringMode;
import com.jogamp.opencl.llb.CL;
import com.jogamp.opencl.llb.CLContextBinding;
import com.jogamp.opencl.llb.CLMemObjBinding;
import com.jogamp.opencl.llb.impl.CLImageFormatImpl;

/**
 * CLContext is responsible for managing objects such as command-queues, memory,
 * program and kernel objects and for executing kernels on one or more devices
 * specified in the context.
 * <p>
 *  Must be released if no longer used to free native resources. {@link #release()} will
 *  also free all associated {@link CLResource} like programs, samplers, command queues and memory
 *  objects.
 * </p>
 * <p>
 *  For a code example see {@link CLPlatform}.
 * <p/>
 *
 * concurrency:<br/>
 * CLContext is threadsafe.
 *
 * @author Michael Bien, et al.
 */
public class CLContext extends CLObjectResource {

    protected CLDevice[] devices;

    protected final Set<CLProgram> programs;
    protected final Set<CLSampler> samplers;
    protected final Set<CLMemory<? extends Buffer>> memoryObjects;

    protected final Map<CLDevice, List<CLCommandQueue>> queuesMap;

    protected final CLPlatform platform;

    private final ErrorDispatcher errorHandler;

    protected CLContext(final CLPlatform platform, final long contextID, final ErrorDispatcher dispatcher) {
        super(contextID);
        this.platform = platform;

        this.programs = Collections.synchronizedSet(new HashSet<CLProgram>());
        this.samplers = Collections.synchronizedSet(new HashSet<CLSampler>());
        this.memoryObjects = Collections.synchronizedSet(new HashSet<CLMemory<? extends Buffer>>());

        this.queuesMap = new HashMap<CLDevice, List<CLCommandQueue>>();

        this.errorHandler = dispatcher;

        /*
        addCLErrorHandler(new CLErrorHandler() {
            public void onError(String errinfo, ByteBuffer private_info, long cb) {
                java.util.logging.Logger.getLogger(getClass().getName()).warning(errinfo);
            }
        });
        */

    }

    private synchronized void initDevices(final CLContextBinding cl) {

        if (devices == null) {

            final PointerBuffer deviceCount = PointerBuffer.allocateDirect(1);

            int ret = cl.clGetContextInfo(ID, CLContextBinding.CL_CONTEXT_DEVICES, 0, null, deviceCount);
            CLException.checkForError(ret, "can not enumerate devices");

            final ByteBuffer deviceIDs = Buffers.newDirectByteBuffer((int)deviceCount.get());
            ret = cl.clGetContextInfo(ID, CLContextBinding.CL_CONTEXT_DEVICES, deviceIDs.capacity(), deviceIDs, null);
            CLException.checkForError(ret, "can not enumerate devices");

            devices = new CLDevice[deviceIDs.capacity() / (Platform.is32Bit() ? 4 : 8)];
            for (int i = 0; i < devices.length; i++) {
                devices[i] = new CLDevice(this, Platform.is32Bit() ? deviceIDs.getInt() : deviceIDs.getLong());
            }
        }
    }

    /**
     * Creates a context on all available devices (CL_DEVICE_TYPE_ALL).
     * The platform to be used is implementation dependent.
     */
    public static CLContext create() {
        return create((CLPlatform)null, Type.ALL);
    }

    /**
     * Creates a context on the specified device types.
     * The platform to be used is implementation dependent.
     */
    public static CLContext create(final Type... deviceTypes) {
        return create(null, deviceTypes);
    }

    /**
     * Creates a context on the specified platform on all available devices (CL_DEVICE_TYPE_ALL).
     */
    public static CLContext create(final CLPlatform platform) {
        return create(platform, Type.ALL);
    }

    /**
     * Creates a context on the specified platform and with the specified
     * device types.
     */
    public static CLContext create(CLPlatform platform, final Type... deviceTypes) {

        if(platform == null) {
            platform = CLPlatform.getDefault();
        }

        final long type = toDeviceBitmap(deviceTypes);

        final PointerBuffer properties = setupContextProperties(platform);
        final ErrorDispatcher dispatcher = new ErrorDispatcher();
        return new CLContext(platform, createContextFromType(platform, dispatcher, properties, type), dispatcher);
    }

    /**
     * Creates a context on the specified devices.
     */
    public static CLContext create(final CLDevice... devices) {

        if(devices == null) {
            throw new IllegalArgumentException("no devices specified");
        }else if(devices[0] == null) {
            throw new IllegalArgumentException("first device was null");
        }

        final CLPlatform platform = devices[0].getPlatform();

        final PointerBuffer properties = setupContextProperties(platform);
        final ErrorDispatcher dispatcher = new ErrorDispatcher();
        final CLContext context = new CLContext(platform, createContext(platform, dispatcher, properties, devices), dispatcher);
        if(devices != null) {
            for (int i = 0; i < devices.length; i++) {
                devices[i].setContext(context);
            }
        }
        return context;
    }

    protected static long createContextFromType(final CLPlatform platform, final CLErrorHandler handler, final PointerBuffer properties, final long deviceType) {
        final IntBuffer status = Buffers.newDirectIntBuffer(1);
        final CLContextBinding cl = platform.getContextBinding();
        final long context = cl.clCreateContextFromType(properties, deviceType, handler, status);

        CLException.checkForError(status.get(), "can not create CL context");

        return context;
    }

    protected static long createContext(final CLPlatform platform, final CLErrorHandler handler, final PointerBuffer properties, final CLDevice... devices) {
        final IntBuffer status = Buffers.newDirectIntBuffer(1);
        PointerBuffer pb = null;
        if(devices != null && devices.length != 0) {
            pb = PointerBuffer.allocateDirect(devices.length);
            for (int i = 0; i < devices.length; i++) {
                final CLDevice device = devices[i];
                if(device == null) {
                    throw new IllegalArgumentException("device at index "+i+" was null.");
                }
                pb.put(i, device.ID);
            }
        }
        final CLContextBinding cl = platform.getContextBinding();
        final long context = cl.clCreateContext(properties, pb, handler, status);

        CLException.checkForError(status.get(), "can not create CL context");

        return context;
    }

    private static PointerBuffer setupContextProperties(final CLPlatform platform) {
        if(platform == null) {
            throw new RuntimeException("no OpenCL installation found");
        }

        return PointerBuffer.allocateDirect(3).put(CLContextBinding.CL_CONTEXT_PLATFORM)
                                              .put(platform.ID).put(0) // 0 terminated array
                                              .rewind();
    }

    /**
     * Creates a program from the given sources, the returned program is not build yet.
     */
    public CLProgram createProgram(final String src) {
        final CLProgram program = CLProgram.create(this, src);
        programs.add(program);
        return program;
    }

    /**
     * Creates a program and reads the source from stream, the returned program is not build yet.
     * The InputStream is automatically closed after the sources have been read.
     * @throws IOException when a IOException occurred while reading or closing the stream.
     */
    public CLProgram createProgram(final InputStream source) throws IOException {

        if(source == null)
            throw new IllegalArgumentException("input stream for program source must not be null");

        final BufferedReader reader = new BufferedReader(new InputStreamReader(source));
        final StringBuilder sb = new StringBuilder(2048);

        String line;
        try {
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");
        } finally {
            reader.close();
        }

        return createProgram(sb.toString());
    }

    /**
     * Creates a program from the given binaries, the program is not build yet.
     * <br/>Creating a program will fail if:<br/>
     * <ul>
     * <li>the submitted binaries are invalid or can not be loaded from the OpenCL driver</li>
     * <li>the binaries do not fit to the CLDevices associated with this context</li>
     * <li>binaries are missing for one or more CLDevices</li>
     * </ul>
     */
    public CLProgram createProgram(final Map<CLDevice, byte[]> binaries) {
        final CLProgram program = CLProgram.create(this, binaries);
        program.setNoSource();
        programs.add(program);
        return program;
    }

    /**
     * Creates a CLBuffer with the specified flags and element count. No flags creates a MEM.READ_WRITE buffer.
     */
    public final CLBuffer<ShortBuffer> createShortBuffer(final int size, final Mem... flags) {
        return createBuffer(Buffers.newDirectShortBuffer(size), flags);
    }

    /**
     * Creates a CLBuffer with the specified flags and element count. No flags creates a MEM.READ_WRITE buffer.
     */
    public final CLBuffer<IntBuffer> createIntBuffer(final int size, final Mem... flags) {
        return createBuffer(Buffers.newDirectIntBuffer(size), flags);
    }

    /**
     * Creates a CLBuffer with the specified flags and element count. No flags creates a MEM.READ_WRITE buffer.
     */
    public final CLBuffer<LongBuffer> createLongBuffer(final int size, final Mem... flags) {
        return createBuffer(Buffers.newDirectLongBuffer(size), flags);
    }

    /**
     * Creates a CLBuffer with the specified flags and element count. No flags creates a MEM.READ_WRITE buffer.
     */
    public final CLBuffer<FloatBuffer> createFloatBuffer(final int size, final Mem... flags) {
        return createBuffer(Buffers.newDirectFloatBuffer(size), flags);
    }

    /**
     * Creates a CLBuffer with the specified flags and element count. No flags creates a MEM.READ_WRITE buffer.
     */
    public final CLBuffer<DoubleBuffer> createDoubleBuffer(final int size, final Mem... flags) {
        return createBuffer(Buffers.newDirectDoubleBuffer(size), flags);
    }

    /**
     * Creates a CLBuffer with the specified flags and buffer size in bytes. No flags creates a MEM.READ_WRITE buffer.
     */
    public final CLBuffer<ByteBuffer> createByteBuffer(final int size, final Mem... flags) {
        return createByteBuffer(size, Mem.flagsToInt(flags));
    }

    /**
     * Creates a CLBuffer with the specified flags and buffer size in bytes.
     */
    public final CLBuffer<ByteBuffer> createByteBuffer(final int size, final int flags) {
        return createBuffer(Buffers.newDirectByteBuffer(size), flags);
    }

    /**
     * Creates a CLBuffer with the specified flags. No flags creates a MEM.READ_WRITE buffer.
     */
    public final CLBuffer<?> createBuffer(final int size, final Mem... flags) {
        return createBuffer(size, Mem.flagsToInt(flags));
    }

    /**
     * Creates a CLBuffer with the specified flags.
     */
    public final CLBuffer<?> createBuffer(final int size, final int flags) {
        final CLBuffer<?> buffer = CLBuffer.create(this, size, flags);
        memoryObjects.add(buffer);
        return buffer;
    }

    /**
     * Creates a CLBuffer with the specified flags. No flags creates a MEM.READ_WRITE buffer.
     */
    public final <B extends Buffer> CLBuffer<B> createBuffer(final B directBuffer, final Mem... flags) {
        return createBuffer(directBuffer, Mem.flagsToInt(flags));
    }

    /**
     * Creates a CLBuffer with the specified flags.
     */
    public final <B extends Buffer> CLBuffer<B> createBuffer(final B directBuffer, final int flags) {
        final CLBuffer<B> buffer = CLBuffer.create(this, directBuffer, flags);
        memoryObjects.add(buffer);
        return buffer;
    }

    /**
     * Creates a CLImage2d with the specified format, dimension and flags.
     */
    public final CLImage2d<?> createImage2d(final int width, final int height, final CLImageFormat format, final Mem... flags) {
        return createImage2d(null, width, height, 0, format, flags);
    }

    /**
     * Creates a CLImage2d with the specified format, dimension and flags.
     */
    public final CLImage2d<?> createImage2d(final int width, final int height, final int rowPitch, final CLImageFormat format, final Mem... flags) {
        return createImage2d(null, width, height, rowPitch, format, flags);
    }

    /**
     * Creates a CLImage2d with the specified format, dimension and flags.
     */
    public final <B extends Buffer> CLImage2d<B> createImage2d(final B directBuffer, final int width, final int height, final CLImageFormat format, final Mem... flags) {
        return createImage2d(directBuffer, width, height, 0, format, flags);
    }

    /**
     * Creates a CLImage2d with the specified format, dimension and flags.
     */
    public final <B extends Buffer> CLImage2d<B> createImage2d(final B directBuffer, final int width, final int height, final int rowPitch, final CLImageFormat format, final Mem... flags) {
        final CLImage2d<B> image = CLImage2d.createImage(this, directBuffer, width, height, rowPitch, format, Mem.flagsToInt(flags));
        memoryObjects.add(image);
        return image;
    }

    /**
     * Creates a CLImage3d with the specified format, dimension and flags.
     */
    public final CLImage3d<?> createImage3d(final int width, final int height, final int depth, final CLImageFormat format, final Mem... flags) {
        return createImage3d(null, width, height, depth, format, flags);
    }

    /**
     * Creates a CLImage3d with the specified format, dimension and flags.
     */
    public final CLImage3d<?> createImage3d(final int width, final int height, final int depth, final int rowPitch, final int slicePitch, final CLImageFormat format, final Mem... flags) {
        return createImage3d(null, width, height, depth, rowPitch, slicePitch, format, flags);
    }

    /**
     * Creates a CLImage3d with the specified format, dimension and flags.
     */
    public final <B extends Buffer> CLImage3d<B> createImage3d(final B directBuffer, final int width, final int height, final int depth, final CLImageFormat format, final Mem... flags) {
        return createImage3d(directBuffer, width, height, depth, 0, 0, format, flags);
    }

    /**
     * Creates a CLImage3d with the specified format, dimension and flags.
     */
    public final <B extends Buffer> CLImage3d<B> createImage3d(final B directBuffer, final int width, final int height, final int depth, final int rowPitch, final int slicePitch, final CLImageFormat format, final Mem... flags) {
        final CLImage3d<B> image = CLImage3d.createImage(this, directBuffer, width, height, depth, rowPitch, slicePitch, format, Mem.flagsToInt(flags));
        memoryObjects.add(image);
        return image;
    }

    CLCommandQueue createCommandQueue(final CLDevice device, final long properties) {

        final CLCommandQueue queue = CLCommandQueue.create(this, device, properties);

        synchronized(queuesMap) {
            List<CLCommandQueue> list = queuesMap.get(device);
            if(list == null) {
                list = new ArrayList<CLCommandQueue>();
                queuesMap.put(device, list);
            }
            list.add(queue);
        }

        return queue;
    }

    public CLSampler createSampler(final AddressingMode addrMode, final FilteringMode filtMode, final boolean normalizedCoords) {
        final CLSampler sampler = CLSampler.create(this, addrMode, filtMode, normalizedCoords);
        samplers.add(sampler);
        return sampler;
    }

    void onProgramReleased(final CLProgram program) {
        programs.remove(program);
    }

    void onMemoryReleased(final CLMemory<?> buffer) {
        memoryObjects.remove(buffer);
    }

    void onCommandQueueReleased(final CLDevice device, final CLCommandQueue queue) {
        synchronized(queuesMap) {
            final List<CLCommandQueue> list = queuesMap.get(device);
            list.remove(queue);
            // remove empty lists from map
            if(list.isEmpty())
                queuesMap.remove(device);
        }
    }

    void onSamplerReleased(final CLSampler sampler) {
        samplers.remove(sampler);
    }

    public void addCLErrorHandler(final CLErrorHandler handler) {
        errorHandler.addHandler(handler);
    }

    public void removeCLErrorHandler(final CLErrorHandler handler) {
        errorHandler.removeHandler(handler);
    }

    private void release(final Collection<? extends CLResource> resources) {
        // resources remove themselves when released, see above
        while(!resources.isEmpty()) {
            resources.iterator().next().release();
        }
    }

    /**
     * Releases this context and all resources.
     */
    @Override
    public synchronized void release() {
        super.release();

        try{
            //release all resources
            release(programs);
            release(memoryObjects);
            release(samplers);

            synchronized(queuesMap) {
                final Collection<List<CLCommandQueue>> queuesList =  queuesMap.values();
                while(!queuesList.isEmpty())
                    release(queuesList.iterator().next());
            }

        } finally {
            final int ret = platform.getContextBinding().clReleaseContext(ID);
            CLException.checkForError(ret, "error releasing context");
        }

    }

    protected void overrideContext(final CLDevice device) {
        device.setContext(this);
    }

    private CLImageFormat[] getSupportedImageFormats(final int flags, final int type) {

        final CLContextBinding binding = platform.getContextBinding();

        final int[] entries = new int[1];
        int ret = binding.clGetSupportedImageFormats(ID, flags, type, 0, null, entries, 0);
        if(ret != CL.CL_SUCCESS) {
            throw CLException.newException(ret, "error calling clGetSupportedImageFormats");
        }

        final int count = entries[0];
        if(count == 0) {
            return new CLImageFormat[0];
        }

        final CLImageFormat[] formats = new CLImageFormat[count];
        final CLImageFormatImpl impl = CLImageFormatImpl.create(Buffers.newDirectByteBuffer(count * CLImageFormatImpl.size()));
        ret = binding.clGetSupportedImageFormats(ID, flags, type, count, impl, null);
        if(ret != CL.CL_SUCCESS) {
            throw CLException.newException(ret, "error calling clGetSupportedImageFormats");
        }

        final ByteBuffer buffer = impl.getBuffer();
        for (int i = 0; i < formats.length; i++) {
            formats[i] = new CLImageFormat(CLImageFormatImpl.create(buffer.slice()));
            buffer.position(i*CLImageFormatImpl.size());
        }

        return formats;

    }

    /**
     * Returns all supported 2d image formats with the (optional) memory allocation flags.
     */
    public CLImageFormat[] getSupportedImage2dFormats(final Mem... flags) {
        return getSupportedImageFormats(flags==null?0:Mem.flagsToInt(flags), CLMemObjBinding.CL_MEM_OBJECT_IMAGE2D);
    }

    /**
     * Returns all supported 3d image formats with the (optional) memory allocation flags.
     */
    public CLImageFormat[] getSupportedImage3dFormats(final Mem... flags) {
        return getSupportedImageFormats(flags==null?0:Mem.flagsToInt(flags), CLMemObjBinding.CL_MEM_OBJECT_IMAGE3D);
    }

    /**
     * Returns the CLPlatform this context is running on.
     */
    @Override
    public CLPlatform getPlatform() {
        return platform;
    }

    @Override
    public CLContext getContext() {
        return this;
    }

    /**
     * Returns a read only shapshot of all programs associated with this context.
     */
    public List<CLProgram> getPrograms() {
        synchronized(programs) {
            return Collections.unmodifiableList(new ArrayList<CLProgram>(programs));
        }
    }

    /**
     * Returns a read only shapshot of all allocated memory objects associated with this context.
     */
    public List<CLMemory<? extends Buffer>> getMemoryObjects() {
        synchronized(memoryObjects) {
            return Collections.unmodifiableList(new ArrayList<CLMemory<? extends Buffer>>(memoryObjects));
        }
    }

    /**
     * Returns a read only shapshot of all samplers associated with this context.
     */
    public List<CLSampler> getSamplers() {
        synchronized(samplers) {
            return Collections.unmodifiableList(new ArrayList<CLSampler>(samplers));
        }
    }

    /**
     * Returns the device with maximal FLOPS from this context.
     * The device speed is estimated by calculating the product of
     * MAX_COMPUTE_UNITS and MAX_CLOCK_FREQUENCY.
     * @see #getMaxFlopsDevice(com.jogamp.opencl.CLDevice.Type)
     */
    public CLDevice getMaxFlopsDevice() {
        return CLPlatform.findMaxFlopsDevice(getDevices());
    }

    /**
     * Returns the device with maximal FLOPS of the specified device type from this context.
     * The device speed is estimated by calculating the product of
     * MAX_COMPUTE_UNITS and MAX_CLOCK_FREQUENCY.
     */
    public CLDevice getMaxFlopsDevice(final CLDevice.Type type) {
        return CLPlatform.findMaxFlopsDevice(getDevices(), type);
    }

    /**
     * Returns the maximum {@link CLDevice#getMemBaseAddrAlign()} of all devices.
     */
    public long getMaxMemBaseAddrAlign() {
        long maxAlignment = 0;
        for (final CLDevice device : getDevices()) {
            maxAlignment = Math.max(maxAlignment, device.getMemBaseAddrAlign());
        }
        return maxAlignment;
    }

    /**
     * Returns all devices associated with this CLContext.
     */
    public CLDevice[] getDevices() {
        initDevices(platform.getContextBinding());
        return devices;
    }

    /**
     * Return the low level OpenCL interface.
     */
    public CL getCL() {
        return getPlatform().getCLBinding();
    }

    CLDevice getDevice(final long dID) {
        final CLDevice[] deviceArray = getDevices();
        for (int i = 0; i < deviceArray.length; i++) {
            if(dID == deviceArray[i].ID)
                return deviceArray[i];
        }
        return null;
    }

    protected static long toDeviceBitmap(final Type[] deviceTypes) {
        long bitmap = 0;
        if (deviceTypes != null) {
            for (int i = 0; i < deviceTypes.length; i++) {
                final Type type = deviceTypes[i];
                if(type == null) {
                    throw new IllegalArgumentException("Device type at index "+i+" was null.");
                }
                bitmap |= type.TYPE;
            }
        }
        return bitmap;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+" [id: " + ID
                                          + ", platform: " + getPlatform().getName()
                                          + ", profile: " + getPlatform().getProfile()
                                          + ", devices: " + getDevices().length
                                          + "]";
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CLContext other = (CLContext) obj;
        if (this.ID != other.ID) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + (int) (this.ID ^ (this.ID >>> 32));
        return hash;
    }

    protected static ErrorDispatcher createErrorHandler() {
        return new ErrorDispatcher();
    }

    protected static class ErrorDispatcher implements CLErrorHandler {

        private CLErrorHandler[] clientHandlers = new CLErrorHandler[0];

        @Override
        public synchronized void onError(final String errinfo, final ByteBuffer private_info, final long cb) {
            final CLErrorHandler[] handlers = this.clientHandlers;
            for (int i = 0; i < handlers.length; i++) {
                handlers[i].onError(errinfo, private_info, cb);
            }
        }

        private synchronized void addHandler(final CLErrorHandler handler) {

            if(handler == null) {
                throw new IllegalArgumentException("handler was null.");
            }

            final CLErrorHandler[] handlers = new CLErrorHandler[clientHandlers.length+1];
            System.arraycopy(clientHandlers, 0, handlers, 0, clientHandlers.length);
            handlers[handlers.length-1] = handler;
            clientHandlers = handlers;
        }

        private synchronized void removeHandler(final CLErrorHandler handler) {

            if(handler == null) {
                throw new IllegalArgumentException("handler was null.");
            }

            for (int i = 0; i < clientHandlers.length; i++) {
                if(handler.equals(clientHandlers[i])) {
                    final CLErrorHandler[] handlers = new CLErrorHandler[clientHandlers.length-1];
                    System.arraycopy(clientHandlers, 0, handlers, 0, i);
                    System.arraycopy(clientHandlers, i, handlers, 0, handlers.length-i);
                    clientHandlers = handlers;
                    return;
                }
            }
        }


    }


}
