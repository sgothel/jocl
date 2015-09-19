JNIEXPORT jlong JNICALL
Java_com_jogamp_opencl_llb_impl_CLImpl20_dispatch_1clGetExtensionFunctionAddressStatic(JNIEnv *env, jclass _unused, jstring fname, jlong procAddress) {
  return Java_com_jogamp_opencl_llb_impl_CLImpl11_dispatch_1clGetExtensionFunctionAddressStatic(env, _unused, fname, procAddress);
}

JNIEXPORT jlong JNICALL
Java_com_jogamp_opencl_llb_impl_CLImpl20_clCreateContextFromType0(JNIEnv *env, jobject _unused, jobject props, jint props_byte_offset, jlong device_type, jobject cb, jobject global, jobject errcode, jint errcode_byte_offset, jlong procAddress) {
    return Java_com_jogamp_opencl_llb_impl_CLImpl11_clCreateContextFromType0(env, _unused, props, props_byte_offset, device_type, cb, global, errcode, errcode_byte_offset, procAddress);
}

JNIEXPORT jlong JNICALL
Java_com_jogamp_opencl_llb_impl_CLImpl20_clCreateContext0(JNIEnv *env, jobject _unused, jobject props, jint props_byte_offset, jint numDevices, jobject deviceList, jint device_type_offset, jobject cb, jobject global, jobject errcode, jint errcode_byte_offset, jlong procAddress) {
    return Java_com_jogamp_opencl_llb_impl_CLImpl11_clCreateContext0(env, _unused, props, props_byte_offset, numDevices, deviceList, device_type_offset, cb, global, errcode, errcode_byte_offset, procAddress);
}

JNIEXPORT jint JNICALL
Java_com_jogamp_opencl_llb_impl_CLImpl20_clReleaseContextImpl(JNIEnv *env, jobject _unused, jlong context, jlong global, jlong procAddress) {
    return Java_com_jogamp_opencl_llb_impl_CLImpl11_clReleaseContextImpl(env, _unused, context, global, procAddress);
}

JNIEXPORT jint JNICALL
Java_com_jogamp_opencl_llb_impl_CLImpl20_clBuildProgram0(JNIEnv *env, jobject _unused, jlong program, jint deviceCount, jobject deviceList, jint device_type_offset, jstring options, jobject cb, jlong procAddress) {
    return Java_com_jogamp_opencl_llb_impl_CLImpl11_clBuildProgram0(env, _unused, program, deviceCount, deviceList, device_type_offset, options, cb, procAddress);
}

JNIEXPORT jobject JNICALL
Java_com_jogamp_opencl_llb_impl_CLImpl20_clEnqueueMapImage0__JJIJLjava_lang_Object_2ILjava_lang_Object_2ILjava_lang_Object_2ILjava_lang_Object_2IILjava_lang_Object_2ILjava_lang_Object_2ILjava_lang_Object_2I(JNIEnv *env, jobject _unused,
        jlong command_queue, jlong image, jint blocking_map, jlong map_flags,
        jobject origin, jint origin_byte_offset, jobject range, jint range_byte_offset,
        jobject image_row_pitch, jint image_row_pitch_byte_offset, jobject image_slice_pitch,
        jint image_slice_pitch_byte_offset, jint num_events_in_wait_list, jobject event_wait_list,
        jint event_wait_list_byte_offset, jobject event, jint event_byte_offset, jobject errcode_ret, jint errcode_ret_byte_offset,
        jlong imageInfoAddress, jlong mapImageAddress) {
    return Java_com_jogamp_opencl_llb_impl_CLImpl11_clEnqueueMapImage0__JJIJLjava_lang_Object_2ILjava_lang_Object_2ILjava_lang_Object_2ILjava_lang_Object_2IILjava_lang_Object_2ILjava_lang_Object_2ILjava_lang_Object_2I(env, _unused,
        command_queue, image, blocking_map, map_flags,
        origin, origin_byte_offset, range, range_byte_offset,
        image_row_pitch, image_row_pitch_byte_offset, image_slice_pitch,
        image_slice_pitch_byte_offset, num_events_in_wait_list, event_wait_list,
        event_wait_list_byte_offset, event, event_byte_offset, errcode_ret, errcode_ret_byte_offset,
        imageInfoAddress, mapImageAddress);
}

JNIEXPORT jint JNICALL
Java_com_jogamp_opencl_llb_impl_CLImpl20_clSetEventCallback0(JNIEnv *env, jobject _unused, jlong event, jint trigger, jobject listener, jlong procAddress) {
    return Java_com_jogamp_opencl_llb_impl_CLImpl11_clSetEventCallback0(env, _unused, event, trigger, listener, procAddress);
}

JNIEXPORT jint JNICALL
Java_com_jogamp_opencl_llb_impl_CLImpl20_clSetMemObjectDestructorCallback0(JNIEnv *env, jobject _unused, jlong mem, jobject listener, jlong procAddress) {
    return Java_com_jogamp_opencl_llb_impl_CLImpl11_clSetMemObjectDestructorCallback0(env, _unused, mem, listener, procAddress);
}
