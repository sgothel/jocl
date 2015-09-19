/** If null, OpenCL is not available on this machine. */
    static final DynamicLibraryBundle dynamicLookupHelper;

    static {
        dynamicLookupHelper = AccessController.doPrivileged(new PrivilegedAction<DynamicLibraryBundle>() {
                                  public DynamicLibraryBundle run() {
                                      final DynamicLibraryBundle bundle = new DynamicLibraryBundle(new CLDynamicLibraryBundleInfo());
                                      if(!bundle.isToolLibLoaded()) {
                                          // couldn't load native CL library
                                          // TODO: log this?
                                          return null;
                                      }
                                      if(!bundle.isLibComplete()) {
                                          System.err.println("Couln't load native CL/JNI glue library");
                                          return null;
                                      }
                                      addressTable.reset(bundle);
                                      /** Not required nor forced
                                      if( !initializeImpl() ) {
                                          System.err.println("Native initialization failure of CL/JNI glue library");
                                          return null;
                                      } */
                                      return bundle;
                                  } } );
    }

    // maps the context id to its error handler's global object pointer
    private final LongLongHashMap contextCallbackMap = new LongLongHashMap();

    // to use in subclass constructors
    protected void init() {
        this.contextCallbackMap.setKeyNotFoundValue(0);
    }

    /**
     * Accessor.
     * @returns true if OpenCL is available on this machine.
     */
    public static boolean isAvailable() { return dynamicLookupHelper != null; }

    static long clGetExtensionFunctionAddress(long clGetExtensionFunctionAddressHandle, java.lang.String procname)
    {
        if (clGetExtensionFunctionAddressHandle == 0) {
            throw new RuntimeException("Passed null pointer for method \"clGetExtensionFunctionAddress\"");
        }
        return dispatch_clGetExtensionFunctionAddressStatic(procname, clGetExtensionFunctionAddressHandle);
    }

    /** Entry point (through function pointer) to C language function: <br> <code> void* clGetExtensionFunctionAddress(const char *  fname); </code>    */
    long clGetExtensionFunctionAddress(String fname)  {

        final long __addr_ = addressTable._addressof_clGetExtensionFunctionAddress;
        if (__addr_ == 0) {
            throw new UnsupportedOperationException("Method \"clGetExtensionFunctionAddress\" not available");
        }
        return dispatch_clGetExtensionFunctionAddressStatic(fname, __addr_);
    }

    /** Entry point (through function pointer) to C language function: <br> <code> void* clGetExtensionFunctionAddress(const char *  fname); </code>    */
    private static native long dispatch_clGetExtensionFunctionAddressStatic(String fname, long procAddress);

    @Override
    public long clCreateContext(final PointerBuffer properties, final PointerBuffer devices, final CLErrorHandler pfn_notify, final IntBuffer errcode_ret) {

        if (properties != null && !properties.isDirect()) {
            throw new RuntimeException("Argument \"properties\" was not a direct buffer");
        }

        if (errcode_ret != null && !errcode_ret.isDirect()) {
            throw new RuntimeException("Argument \"errcode_ret\" was not a direct buffer");
        }

        final long address = addressTable._addressof_clCreateContext;
        if (address == 0) {
            throw new UnsupportedOperationException("Method not available");
        }

        final long[] global = new long[1];
        final long ctx = this.clCreateContext0(
                properties != null ? properties.getBuffer() : null, Buffers.getDirectBufferByteOffset(properties),
                devices != null ? devices.remaining() : 0, devices != null ? devices.getBuffer() : null, Buffers.getDirectBufferByteOffset(devices),
                pfn_notify, global, errcode_ret, Buffers.getDirectBufferByteOffset(errcode_ret), address);

        if (pfn_notify != null && global[0] != 0) {
            synchronized (contextCallbackMap) {
                contextCallbackMap.put(ctx, global[0]);
            }
        }
        return ctx;
    }

    private native long clCreateContext0(Object cl_context_properties, int props_offset, int numDevices, Object devices, int devices_offset, Object pfn_notify, long[] global, Object errcode_ret, int err_offset, long address);

    @Override
    public long clCreateContextFromType(final PointerBuffer properties, final long device_type, final CLErrorHandler pfn_notify, final IntBuffer errcode_ret) {
        if (properties != null && !properties.isDirect()) {
            throw new RuntimeException("Argument \"properties\" was not a direct buffer");
        }

        if (errcode_ret != null && !errcode_ret.isDirect()) {
            throw new RuntimeException("Argument \"errcode_ret\" was not a direct buffer");
        }

        final long address = addressTable._addressof_clCreateContextFromType;
        if (address == 0) {
            throw new UnsupportedOperationException("Method not available");
        }

        final long[] global = new long[1];
        final long ctx = this.clCreateContextFromType0(
                properties != null ? properties.getBuffer() : null, Buffers.getDirectBufferByteOffset(properties),
                device_type, pfn_notify, global, errcode_ret, Buffers.getDirectBufferByteOffset(errcode_ret), address);

        if (pfn_notify != null && global[0] != 0) {
            synchronized (contextCallbackMap) {
                contextCallbackMap.put(ctx, global[0]);
            }
        }
        return ctx;
    }

    private native long clCreateContextFromType0(Object properties, int props_offset, long device_type, Object pfn_notify, long[] global, Object errcode_ret, int err_offset, long address);

    @Override
    public int clReleaseContext(final long context) {
        long global = 0;
        synchronized (contextCallbackMap) {
            global = contextCallbackMap.remove(context);
        }

        final long address = addressTable._addressof_clReleaseContext;
        if (address == 0) {
            throw new UnsupportedOperationException("Method not available");
        }
        return clReleaseContextImpl(context, global, address);
    }

    /** Interface to C language function: <br> <code> int32_t {@native clReleaseContext}(cl_context context); </code>    */
    public native int clReleaseContextImpl(long context, long global, long address);

    /** Interface to C language function: <br> <code> int32_t clBuildProgram(cl_program, uint32_t, cl_device_id * , const char * , void * ); </code>    */
    @Override
    public int clBuildProgram(final long program, final int deviceCount, final PointerBuffer deviceList, final String options, final BuildProgramCallback cb) {
        if (deviceList != null && !deviceList.isDirect()) {
            throw new RuntimeException("Argument \"properties\" was not a direct buffer");
        }

        final long address = addressTable._addressof_clBuildProgram;
        if (address == 0) {
            throw new UnsupportedOperationException("Method not available");
        }
        return clBuildProgram0(program, deviceCount, deviceList != null ? deviceList.getBuffer() : null,
        		Buffers.getDirectBufferByteOffset(deviceList), options, cb, address);
    }

    /** Entry point to C language function: <code> int32_t clBuildProgram(cl_program, uint32_t, cl_device_id * , const char * , void * ); </code>    */
    private native int clBuildProgram0(long program, int deviceCount, Object deviceList, int deviceListOffset, String options, BuildProgramCallback cb, long address);


    @Override
    public int clSetEventCallback(final long event, final int trigger, final CLEventCallback callback) {
        final long address = addressTable._addressof_clSetEventCallback;
        if (address == 0) {
            throw new UnsupportedOperationException("Method not available");
        }
        return clSetEventCallback0(event, trigger, callback, address);
    }

    private native int clSetEventCallback0(long event, int type, CLEventCallback cb, long address);


    @Override
    public int clSetMemObjectDestructorCallback(final long memObjID, final CLMemObjectDestructorCallback cb) {
        final long address = addressTable._addressof_clSetMemObjectDestructorCallback;
        if (address == 0) {
            throw new UnsupportedOperationException("Method not available");
        }
        return clSetMemObjectDestructorCallback0(memObjID, cb, address);
    }

    private native int clSetMemObjectDestructorCallback0(long memObjID, CLMemObjectDestructorCallback cb, long address);


    /** Interface to C language function: <br> <code> void *  {@native clEnqueueMapImage}(cl_command_queue command_queue, cl_mem image, uint32_t blocking_map, uint64_t map_flags, const size_t * , const size_t * , size_t *  image_row_pitch, size_t *  image_slice_pitch, uint32_t num_events_in_wait_list, cl_event *  event_wait_list, cl_event *  event, int32_t *  errcode_ret); </code>
    @param origin a direct {@link com.jogamp.common.nio.PointerBuffer}
    @param range a direct {@link com.jogamp.common.nio.PointerBuffer}
    @param image_row_pitch a direct {@link com.jogamp.common.nio.PointerBuffer}
    @param image_slice_pitch a direct {@link com.jogamp.common.nio.PointerBuffer}
    @param event_wait_list a direct {@link com.jogamp.common.nio.PointerBuffer}
    @param event a direct {@link com.jogamp.common.nio.PointerBuffer}
    @param errcode_ret a direct {@link java.nio.IntBuffer}   */
    @Override
    public ByteBuffer clEnqueueMapImage(final long command_queue, final long image, final int blocking_map, final long map_flags,
            final PointerBuffer origin, final PointerBuffer range,
            final PointerBuffer image_row_pitch, final PointerBuffer image_slice_pitch,
            final int num_events_in_wait_list,
            final PointerBuffer event_wait_list, final PointerBuffer event, final IntBuffer errcode_ret) {

        if (origin != null && !origin.isDirect()) {
            throw new CLException("Argument \"origin\" was not a direct buffer");
        }
        if (range != null && !range.isDirect()) {
            throw new CLException("Argument \"range\" was not a direct buffer");
        }
        if (image_row_pitch != null && !image_row_pitch.isDirect()) {
            throw new CLException("Argument \"image_row_pitch\" was not a direct buffer");
        }
        if (image_slice_pitch != null && !image_slice_pitch.isDirect()) {
            throw new CLException("Argument \"image_slice_pitch\" was not a direct buffer");
        }
        if (event_wait_list != null && !event_wait_list.isDirect()) {
            throw new CLException("Argument \"event_wait_list\" was not a direct buffer");
        }
        if (event != null && !event.isDirect()) {
            throw new CLException("Argument \"event\" was not a direct buffer");
        }
        if (errcode_ret != null && !errcode_ret.isDirect()) {
            throw new CLException("Argument \"errcode_ret\" was not a direct buffer");
        }

        final long getImageInfoAddress = addressTable._addressof_clGetImageInfo;
        if (getImageInfoAddress == 0) {
            throw new UnsupportedOperationException("Method not available");
        }
        final long mapImageAddress = addressTable._addressof_clEnqueueMapImage;
        if (mapImageAddress == 0) {
            throw new UnsupportedOperationException("Method not available");
        }
        ByteBuffer _res;
        _res = clEnqueueMapImage0(command_queue, image, blocking_map, map_flags, origin != null ? origin.getBuffer() : null,
        		Buffers.getDirectBufferByteOffset(origin), range != null ? range.getBuffer() : null,
        		Buffers.getDirectBufferByteOffset(range), image_row_pitch != null ? image_row_pitch.getBuffer() : null,
        		Buffers.getDirectBufferByteOffset(image_row_pitch), image_slice_pitch != null ? image_slice_pitch.getBuffer() : null,
        		Buffers.getDirectBufferByteOffset(image_slice_pitch), num_events_in_wait_list,
                event_wait_list != null ? event_wait_list.getBuffer() : null, Buffers.getDirectBufferByteOffset(event_wait_list),
                event != null ? event.getBuffer() : null, Buffers.getDirectBufferByteOffset(event), errcode_ret,
                Buffers.getDirectBufferByteOffset(errcode_ret), getImageInfoAddress, mapImageAddress);
        if (_res == null) {
            return null;
        }
        Buffers.nativeOrder(_res);
        return _res;
    }

    /** Entry point to C language function: <code> void *  {@native clEnqueueMapImage}(cl_command_queue command_queue, cl_mem image, uint32_t blocking_map, uint64_t map_flags, const size_t * , const size_t * , size_t *  image_row_pitch, size_t *  image_slice_pitch, uint32_t num_events_in_wait_list, cl_event *  event_wait_list, cl_event *  event, int32_t *  errcode_ret); </code>
    @param origin a direct {@link com.jogamp.common.nio.PointerBuffer}
    @param range a direct {@link com.jogamp.common.nio.PointerBuffer}
    @param image_row_pitch a direct {@link com.jogamp.common.nio.PointerBuffer}
    @param image_slice_pitch a direct {@link com.jogamp.common.nio.PointerBuffer}
    @param event_wait_list a direct {@link com.jogamp.common.nio.PointerBuffer}
    @param event a direct {@link com.jogamp.common.nio.PointerBuffer}
    @param errcode_ret a direct {@link java.nio.IntBuffer}   */
    private native ByteBuffer clEnqueueMapImage0(long command_queue, long image, int blocking_map, long map_flags,
            Object origin, int origin_byte_offset, Object range, int range_byte_offset, Object image_row_pitch,
            int image_row_pitch_byte_offset, Object image_slice_pitch, int image_slice_pitch_byte_offset,
            int num_events_in_wait_list, Object event_wait_list, int event_wait_list_byte_offset, Object event,
            int event_byte_offset, Object errcode_ret, int errcode_ret_byte_offset,
            long getImageInfoAddress, long mapImageAddress);