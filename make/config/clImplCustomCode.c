// Extern declarations of functions. The earliest version of OpenCL defines them, and the later versions use them.

extern JNIEXPORT jlong JNICALL
Java_com_jogamp_opencl_llb_impl_CLImpl11_dispatch_1clGetExtensionFunctionAddressStatic(JNIEnv *env, jclass _unused, jstring fname, jlong procAddress);

extern JNIEXPORT jlong JNICALL
Java_com_jogamp_opencl_llb_impl_CLImpl11_clCreateContextFromType0(JNIEnv *env, jobject _unused, jobject props, jint props_byte_offset, jlong device_type, jobject cb, jobject global, jobject errcode, jint errcode_byte_offset, jlong procAddress);

extern JNIEXPORT jlong JNICALL
Java_com_jogamp_opencl_llb_impl_CLImpl11_clCreateContext0(JNIEnv *env, jobject _unused, jobject props, jint props_byte_offset, jint numDevices, jobject deviceList, jint device_type_offset, jobject cb, jobject global, jobject errcode, jint errcode_byte_offset, jlong procAddress);

extern JNIEXPORT jint JNICALL
Java_com_jogamp_opencl_llb_impl_CLImpl11_clReleaseContextImpl(JNIEnv *env, jobject _unused, jlong context, jlong global, jlong procAddress);

extern JNIEXPORT jint JNICALL
Java_com_jogamp_opencl_llb_impl_CLImpl11_clBuildProgram0(JNIEnv *env, jobject _unused, jlong program, jint deviceCount, jobject deviceList, jint device_type_offset, jstring options, jobject cb, jlong procAddress);

extern JNIEXPORT jobject JNICALL
Java_com_jogamp_opencl_llb_impl_CLImpl11_clEnqueueMapImage0__JJIJLjava_lang_Object_2ILjava_lang_Object_2ILjava_lang_Object_2ILjava_lang_Object_2IILjava_lang_Object_2ILjava_lang_Object_2ILjava_lang_Object_2I(JNIEnv *env, jobject _unused,
        jlong command_queue, jlong image, jint blocking_map, jlong map_flags,
        jobject origin, jint origin_byte_offset, jobject range, jint range_byte_offset,
        jobject image_row_pitch, jint image_row_pitch_byte_offset, jobject image_slice_pitch,
        jint image_slice_pitch_byte_offset, jint num_events_in_wait_list, jobject event_wait_list,
        jint event_wait_list_byte_offset, jobject event, jint event_byte_offset, jobject errcode_ret, jint errcode_ret_byte_offset,
        jlong imageInfoAddress, jlong mapImageAddress);

extern JNIEXPORT jint JNICALL
Java_com_jogamp_opencl_llb_impl_CLImpl11_clSetEventCallback0(JNIEnv *env, jobject _unused, jlong event, jint trigger, jobject listener, jlong procAddress);

extern JNIEXPORT jint JNICALL
Java_com_jogamp_opencl_llb_impl_CLImpl11_clSetMemObjectDestructorCallback0(JNIEnv *env, jobject _unused, jlong mem, jobject listener, jlong procAddress);
