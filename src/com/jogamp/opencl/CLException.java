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

package com.jogamp.opencl;

import com.jogamp.opencl.llb.gl.CLGL;
import com.jogamp.opencl.llb.CL;

/**
 * Main Exception type for runtime OpenCL errors and failed function calls (e.g. returning not CL_SUCCESS).
 * @author Michael Bien
 */
public class CLException extends RuntimeException {

    // must be positive
    private static final long serialVersionUID = 6573520735486076436L;

    public final int errorcode;
    public final String error;

    //man page no longer exists
//    private final static String ERROR_CODE_DOC =
//            "http://www.khronos.org/opencl/sdk/1.1/docs/man/xhtml/errors.html";

    public CLException(String message) {
        super(message);
        errorcode = 0;
        error = "none";
    }

    private CLException(int errorcode, String errorStr, String message) {
        super(message + " [error: " + errorStr+"]"/* + " (man page: "+ERROR_CODE_DOC+")"*/);
        this.error = errorStr;
        this.errorcode = errorcode;
    }

    /**
     * Throws a CLException when <code>status != CL_SUCCESS</code>.
     */
    public static void checkForError(int status, String message) {
        if(status != CL.CL_SUCCESS) {
            CLException ex = newException(status, message);
            ex.fillInStackTrace();
            throw ex;
        }
    }

    /**
     * Returns a CLException specific to the error code.
     */
    public static CLException newException(int status, String message) {
        CLException specificEx = createSpecificException(status, message);
        if(specificEx != null) {
            specificEx.fillInStackTrace();
            return specificEx;
        }
        return new CLException(status, "unknown", "unknown cause: code " + status);
    }

    /**
     * Returns a human readable String for the OpenCL error code.
     */
    public String getCLErrorString() {
        return error;
    }


     // - - - generated code do not edit - - -

    /**
     * Returns a human readable String for the OpenCL error code or null if not known.
     */
    public static String resolveErrorCode(int error) {
        switch(error) {
            case CL.CL_DEVICE_NOT_FOUND:                       return "CL_DEVICE_NOT_FOUND";
            case CL.CL_DEVICE_NOT_AVAILABLE:                   return "CL_DEVICE_NOT_AVAILABLE";
            case CL.CL_COMPILER_NOT_AVAILABLE:                 return "CL_COMPILER_NOT_AVAILABLE";
            case CL.CL_MEM_OBJECT_ALLOCATION_FAILURE:          return "CL_MEM_OBJECT_ALLOCATION_FAILURE";
            case CL.CL_OUT_OF_RESOURCES:                       return "CL_OUT_OF_RESOURCES";
            case CL.CL_OUT_OF_HOST_MEMORY:                     return "CL_OUT_OF_HOST_MEMORY";
            case CL.CL_PROFILING_INFO_NOT_AVAILABLE:           return "CL_PROFILING_INFO_NOT_AVAILABLE";
            case CL.CL_MEM_COPY_OVERLAP:                       return "CL_MEM_COPY_OVERLAP";
            case CL.CL_IMAGE_FORMAT_MISMATCH:                  return "CL_IMAGE_FORMAT_MISMATCH";
            case CL.CL_IMAGE_FORMAT_NOT_SUPPORTED:             return "CL_IMAGE_FORMAT_NOT_SUPPORTED";
            case CL.CL_BUILD_PROGRAM_FAILURE:                  return "CL_BUILD_PROGRAM_FAILURE";
            case CL.CL_MAP_FAILURE:                            return "CL_MAP_FAILURE";
            case CL.CL_INVALID_VALUE:                          return "CL_INVALID_VALUE";
            case CL.CL_INVALID_DEVICE_TYPE:                    return "CL_INVALID_DEVICE_TYPE";
            case CL.CL_INVALID_PLATFORM:                       return "CL_INVALID_PLATFORM";
            case CL.CL_INVALID_DEVICE:                         return "CL_INVALID_DEVICE";
            case CL.CL_INVALID_CONTEXT:                        return "CL_INVALID_CONTEXT";
            case CL.CL_INVALID_QUEUE_PROPERTIES:               return "CL_INVALID_QUEUE_PROPERTIES";
            case CL.CL_INVALID_COMMAND_QUEUE:                  return "CL_INVALID_COMMAND_QUEUE";
            case CL.CL_INVALID_HOST_PTR:                       return "CL_INVALID_HOST_PTR";
            case CL.CL_INVALID_MEM_OBJECT:                     return "CL_INVALID_MEM_OBJECT";
            case CL.CL_INVALID_IMAGE_FORMAT_DESCRIPTOR:        return "CL_INVALID_IMAGE_FORMAT_DESCRIPTOR";
            case CL.CL_INVALID_IMAGE_SIZE:                     return "CL_INVALID_IMAGE_SIZE";
            case CL.CL_INVALID_SAMPLER:                        return "CL_INVALID_SAMPLER";
            case CL.CL_INVALID_BINARY:                         return "CL_INVALID_BINARY";
            case CL.CL_INVALID_BUILD_OPTIONS:                  return "CL_INVALID_BUILD_OPTIONS";
            case CL.CL_INVALID_PROGRAM:                        return "CL_INVALID_PROGRAM";
            case CL.CL_INVALID_PROGRAM_EXECUTABLE:             return "CL_INVALID_PROGRAM_EXECUTABLE";
            case CL.CL_INVALID_KERNEL_NAME:                    return "CL_INVALID_KERNEL_NAME";
            case CL.CL_INVALID_KERNEL_DEFINITION:              return "CL_INVALID_KERNEL_DEFINITION";
            case CL.CL_INVALID_KERNEL:                         return "CL_INVALID_KERNEL";
            case CL.CL_INVALID_ARG_INDEX:                      return "CL_INVALID_ARG_INDEX";
            case CL.CL_INVALID_ARG_VALUE:                      return "CL_INVALID_ARG_VALUE";
            case CL.CL_INVALID_ARG_SIZE:                       return "CL_INVALID_ARG_SIZE";
            case CL.CL_INVALID_KERNEL_ARGS:                    return "CL_INVALID_KERNEL_ARGS";
            case CL.CL_INVALID_WORK_DIMENSION:                 return "CL_INVALID_WORK_DIMENSION";
            case CL.CL_INVALID_WORK_GROUP_SIZE:                return "CL_INVALID_WORK_GROUP_SIZE";
            case CL.CL_INVALID_WORK_ITEM_SIZE:                 return "CL_INVALID_WORK_ITEM_SIZE";
            case CL.CL_INVALID_GLOBAL_OFFSET:                  return "CL_INVALID_GLOBAL_OFFSET";
            case CL.CL_INVALID_EVENT_WAIT_LIST:                return "CL_INVALID_EVENT_WAIT_LIST";
            case CL.CL_INVALID_EVENT:                          return "CL_INVALID_EVENT";
            case CL.CL_INVALID_OPERATION:                      return "CL_INVALID_OPERATION";
            case CL.CL_INVALID_BUFFER_SIZE:                    return "CL_INVALID_BUFFER_SIZE";
            case CL.CL_INVALID_MIP_LEVEL:                      return "CL_INVALID_MIP_LEVEL";
            case CL.CL_INVALID_GLOBAL_WORK_SIZE:               return "CL_INVALID_GLOBAL_WORK_SIZE";
            case CL.CL_INVALID_PROPERTY:                       return "CL_INVALID_PROPERTY";
            case CL.CL_PLATFORM_NOT_FOUND_KHR:                 return "CL_PLATFORM_NOT_FOUND_KHR";
            case CL.CL_MISALIGNED_SUB_BUFFER_OFFSET:             return "CL_MISALIGNED_SUB_BUFFER_OFFSET";
            case CL.CL_EXEC_STATUS_ERROR_FOR_EVENTS_IN_WAIT_LIST:  return "CL_EXEC_STATUS_ERROR_FOR_EVENTS_IN_WAIT_LIST";
            case CLGL.CL_INVALID_GL_OBJECT:                    return "CL_INVALID_GL_OBJECT";
            case CLGL.CL_INVALID_GL_SHAREGROUP_REFERENCE_KHR:  return "CL_INVALID_GL_SHAREGROUP_REFERENCE_KHR";
            default: return null;
        }
    }

    private static CLException createSpecificException(int error, String message) {
        switch(error) {
            case CL.CL_DEVICE_NOT_FOUND:                       return new CLDeviceNotFoundException(message);
            case CL.CL_DEVICE_NOT_AVAILABLE:                   return new CLDeviceNotAvailableException(message);
            case CL.CL_COMPILER_NOT_AVAILABLE:                 return new CLCompilerNotAvailableException(message);
            case CL.CL_MEM_OBJECT_ALLOCATION_FAILURE:          return new CLMemObjectAllocationFailureException(message);
            case CL.CL_OUT_OF_RESOURCES:                       return new CLOutOfResourcesException(message);
            case CL.CL_OUT_OF_HOST_MEMORY:                     return new CLOutOfHostMemoryException(message);
            case CL.CL_PROFILING_INFO_NOT_AVAILABLE:           return new CLProfilingInfoNotAvailableException(message);
            case CL.CL_MEM_COPY_OVERLAP:                       return new CLMemCopyOverlapException(message);
            case CL.CL_IMAGE_FORMAT_MISMATCH:                  return new CLImageFormatMismatchException(message);
            case CL.CL_IMAGE_FORMAT_NOT_SUPPORTED:             return new CLImageFormatNotSupportedException(message);
            case CL.CL_BUILD_PROGRAM_FAILURE:                  return new CLBuildProgramFailureException(message);
            case CL.CL_MAP_FAILURE:                            return new CLMapFailureException(message);
            case CL.CL_INVALID_VALUE:                          return new CLInvalidValueException(message);
            case CL.CL_INVALID_DEVICE_TYPE:                    return new CLInvalidDeviceTypeException(message);
            case CL.CL_INVALID_PLATFORM:                       return new CLInvalidPlatformException(message);
            case CL.CL_INVALID_DEVICE:                         return new CLInvalidDeviceException(message);
            case CL.CL_INVALID_CONTEXT:                        return new CLInvalidContextException(message);
            case CL.CL_INVALID_QUEUE_PROPERTIES:               return new CLInvalidQueuePropertiesException(message);
            case CL.CL_INVALID_COMMAND_QUEUE:                  return new CLInvalidCommandQueueException(message);
            case CL.CL_INVALID_HOST_PTR:                       return new CLInvalidHostPtrException(message);
            case CL.CL_INVALID_MEM_OBJECT:                     return new CLInvalidMemObjectException(message);
            case CL.CL_INVALID_IMAGE_FORMAT_DESCRIPTOR:        return new CLInvalidImageFormatDescriptorException(message);
            case CL.CL_INVALID_IMAGE_SIZE:                     return new CLInvalidImageSizeException(message);
            case CL.CL_INVALID_SAMPLER:                        return new CLInvalidSamplerException(message);
            case CL.CL_INVALID_BINARY:                         return new CLInvalidBinaryException(message);
            case CL.CL_INVALID_BUILD_OPTIONS:                  return new CLInvalidBuildOptionsException(message);
            case CL.CL_INVALID_PROGRAM:                        return new CLInvalidProgramException(message);
            case CL.CL_INVALID_PROGRAM_EXECUTABLE:             return new CLInvalidProgramExecutableException(message);
            case CL.CL_INVALID_KERNEL_NAME:                    return new CLInvalidKernelNameException(message);
            case CL.CL_INVALID_KERNEL_DEFINITION:              return new CLInvalidKernelDefinitionException(message);
            case CL.CL_INVALID_KERNEL:                         return new CLInvalidKernelException(message);
            case CL.CL_INVALID_ARG_INDEX:                      return new CLInvalidArgIndexException(message);
            case CL.CL_INVALID_ARG_VALUE:                      return new CLInvalidArgValueException(message);
            case CL.CL_INVALID_ARG_SIZE:                       return new CLInvalidArgSizeException(message);
            case CL.CL_INVALID_KERNEL_ARGS:                    return new CLInvalidKernelArgsException(message);
            case CL.CL_INVALID_WORK_DIMENSION:                 return new CLInvalidWorkDimensionException(message);
            case CL.CL_INVALID_WORK_GROUP_SIZE:                return new CLInvalidWorkGroupSizeException(message);
            case CL.CL_INVALID_WORK_ITEM_SIZE:                 return new CLInvalidWorkItemSizeException(message);
            case CL.CL_INVALID_GLOBAL_OFFSET:                  return new CLInvalidGlobalOffsetException(message);
            case CL.CL_INVALID_EVENT_WAIT_LIST:                return new CLInvalidEventWaitListException(message);
            case CL.CL_INVALID_EVENT:                          return new CLInvalidEventException(message);
            case CL.CL_INVALID_OPERATION:                      return new CLInvalidOperationException(message);
            case CL.CL_INVALID_BUFFER_SIZE:                    return new CLInvalidBufferSizeException(message);
            case CL.CL_INVALID_MIP_LEVEL:                      return new CLInvalidMipLevelException(message);
            case CL.CL_INVALID_GLOBAL_WORK_SIZE:               return new CLInvalidGlobalWorkSizeException(message);
            case CL.CL_INVALID_PROPERTY:                       return new CLInvalidPropertyException(message);
            case CL.CL_PLATFORM_NOT_FOUND_KHR:                 return new CLPlatformNotFoundKhrException(message);
            case CL.CL_MISALIGNED_SUB_BUFFER_OFFSET:             return new CLMisalignedSubBufferOffsetException(message);
            case CL.CL_EXEC_STATUS_ERROR_FOR_EVENTS_IN_WAIT_LIST:  return new CLExecStatusErrorForEventsInWaitListException(message);
            case CLGL.CL_INVALID_GL_OBJECT:                    return new CLInvalidGLObjectException(message);
            case CLGL.CL_INVALID_GL_SHAREGROUP_REFERENCE_KHR:  return new CLInvalidGLSharegroupReferenceKhrException(message);
            default: return null;
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_DEVICE_NOT_FOUND errors.
     */
    public final static class CLDeviceNotFoundException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_DEVICE_NOT_FOUND;
        public CLDeviceNotFoundException(String message) {
            super(CL.CL_DEVICE_NOT_FOUND, "CL_DEVICE_NOT_FOUND", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_DEVICE_NOT_AVAILABLE errors.
     */
    public final static class CLDeviceNotAvailableException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_DEVICE_NOT_AVAILABLE;
        public CLDeviceNotAvailableException(String message) {
            super(CL.CL_DEVICE_NOT_AVAILABLE, "CL_DEVICE_NOT_AVAILABLE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_COMPILER_NOT_AVAILABLE errors.
     */
    public final static class CLCompilerNotAvailableException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_COMPILER_NOT_AVAILABLE;
        public CLCompilerNotAvailableException(String message) {
            super(CL.CL_COMPILER_NOT_AVAILABLE, "CL_COMPILER_NOT_AVAILABLE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_MEM_OBJECT_ALLOCATION_FAILURE errors.
     */
    public final static class CLMemObjectAllocationFailureException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_MEM_OBJECT_ALLOCATION_FAILURE;
        public CLMemObjectAllocationFailureException(String message) {
            super(CL.CL_MEM_OBJECT_ALLOCATION_FAILURE, "CL_MEM_OBJECT_ALLOCATION_FAILURE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_OUT_OF_RESOURCES errors.
     */
    public final static class CLOutOfResourcesException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_OUT_OF_RESOURCES;
        public CLOutOfResourcesException(String message) {
            super(CL.CL_OUT_OF_RESOURCES, "CL_OUT_OF_RESOURCES", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_OUT_OF_HOST_MEMORY errors.
     */
    public final static class CLOutOfHostMemoryException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_OUT_OF_HOST_MEMORY;
        public CLOutOfHostMemoryException(String message) {
            super(CL.CL_OUT_OF_HOST_MEMORY, "CL_OUT_OF_HOST_MEMORY", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_PROFILING_INFO_NOT_AVAILABLE errors.
     */
    public final static class CLProfilingInfoNotAvailableException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_PROFILING_INFO_NOT_AVAILABLE;
        public CLProfilingInfoNotAvailableException(String message) {
            super(CL.CL_PROFILING_INFO_NOT_AVAILABLE, "CL_PROFILING_INFO_NOT_AVAILABLE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_MEM_COPY_OVERLAP errors.
     */
    public final static class CLMemCopyOverlapException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_MEM_COPY_OVERLAP;
        public CLMemCopyOverlapException(String message) {
            super(CL.CL_MEM_COPY_OVERLAP, "CL_MEM_COPY_OVERLAP", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_IMAGE_FORMAT_MISMATCH errors.
     */
    public final static class CLImageFormatMismatchException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_IMAGE_FORMAT_MISMATCH;
        public CLImageFormatMismatchException(String message) {
            super(CL.CL_IMAGE_FORMAT_MISMATCH, "CL_IMAGE_FORMAT_MISMATCH", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_IMAGE_FORMAT_NOT_SUPPORTED errors.
     */
    public final static class CLImageFormatNotSupportedException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_IMAGE_FORMAT_NOT_SUPPORTED;
        public CLImageFormatNotSupportedException(String message) {
            super(CL.CL_IMAGE_FORMAT_NOT_SUPPORTED, "CL_IMAGE_FORMAT_NOT_SUPPORTED", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_BUILD_PROGRAM_FAILURE errors.
     */
    public final static class CLBuildProgramFailureException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_BUILD_PROGRAM_FAILURE;
        public CLBuildProgramFailureException(String message) {
            super(CL.CL_BUILD_PROGRAM_FAILURE, "CL_BUILD_PROGRAM_FAILURE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_MAP_FAILURE errors.
     */
    public final static class CLMapFailureException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_MAP_FAILURE;
        public CLMapFailureException(String message) {
            super(CL.CL_MAP_FAILURE, "CL_MAP_FAILURE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_VALUE errors.
     * 
     */
    public final static class CLInvalidValueException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_VALUE;
        public CLInvalidValueException(String message) {
            super(CL.CL_INVALID_VALUE, "CL_INVALID_VALUE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_DEVICE_TYPE errors.
     * 
     */
    public final static class CLInvalidDeviceTypeException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_DEVICE_TYPE;
        public CLInvalidDeviceTypeException(String message) {
            super(CL.CL_INVALID_DEVICE_TYPE, "CL_INVALID_DEVICE_TYPE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_PLATFORM errors.
     * 
     */
    public final static class CLInvalidPlatformException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_PLATFORM;
        public CLInvalidPlatformException(String message) {
            super(CL.CL_INVALID_PLATFORM, "CL_INVALID_PLATFORM", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_DEVICE errors.
     * 
     */
    public final static class CLInvalidDeviceException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_DEVICE;
        public CLInvalidDeviceException(String message) {
            super(CL.CL_INVALID_DEVICE, "CL_INVALID_DEVICE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_CONTEXT errors.
     * 
     */
    public final static class CLInvalidContextException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_CONTEXT;
        public CLInvalidContextException(String message) {
            super(CL.CL_INVALID_CONTEXT, "CL_INVALID_CONTEXT", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_QUEUE_PROPERTIES errors.
     * 
     */
    public final static class CLInvalidQueuePropertiesException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_QUEUE_PROPERTIES;
        public CLInvalidQueuePropertiesException(String message) {
            super(CL.CL_INVALID_QUEUE_PROPERTIES, "CL_INVALID_QUEUE_PROPERTIES", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_COMMAND_QUEUE errors.
     * 
     */
    public final static class CLInvalidCommandQueueException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_COMMAND_QUEUE;
        public CLInvalidCommandQueueException(String message) {
            super(CL.CL_INVALID_COMMAND_QUEUE, "CL_INVALID_COMMAND_QUEUE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_HOST_PTR errors.
     * 
     */
    public final static class CLInvalidHostPtrException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_HOST_PTR;
        public CLInvalidHostPtrException(String message) {
            super(CL.CL_INVALID_HOST_PTR, "CL_INVALID_HOST_PTR", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_MEM_OBJECT errors.
     * 
     */
    public final static class CLInvalidMemObjectException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_MEM_OBJECT;
        public CLInvalidMemObjectException(String message) {
            super(CL.CL_INVALID_MEM_OBJECT, "CL_INVALID_MEM_OBJECT", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_IMAGE_FORMAT_DESCRIPTOR errors.
     * 
     */
    public final static class CLInvalidImageFormatDescriptorException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_IMAGE_FORMAT_DESCRIPTOR;
        public CLInvalidImageFormatDescriptorException(String message) {
            super(CL.CL_INVALID_IMAGE_FORMAT_DESCRIPTOR, "CL_INVALID_IMAGE_FORMAT_DESCRIPTOR", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_IMAGE_SIZE errors.
     * 
     */
    public final static class CLInvalidImageSizeException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_IMAGE_SIZE;
        public CLInvalidImageSizeException(String message) {
            super(CL.CL_INVALID_IMAGE_SIZE, "CL_INVALID_IMAGE_SIZE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_SAMPLER errors.
     * 
     */
    public final static class CLInvalidSamplerException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_SAMPLER;
        public CLInvalidSamplerException(String message) {
            super(CL.CL_INVALID_SAMPLER, "CL_INVALID_SAMPLER", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_BINARY errors.
     * 
     */
    public final static class CLInvalidBinaryException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_BINARY;
        public CLInvalidBinaryException(String message) {
            super(CL.CL_INVALID_BINARY, "CL_INVALID_BINARY", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_BUILD_OPTIONS errors.
     * 
     */
    public final static class CLInvalidBuildOptionsException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_BUILD_OPTIONS;
        public CLInvalidBuildOptionsException(String message) {
            super(CL.CL_INVALID_BUILD_OPTIONS, "CL_INVALID_BUILD_OPTIONS", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_PROGRAM errors.
     * 
     */
    public final static class CLInvalidProgramException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_PROGRAM;
        public CLInvalidProgramException(String message) {
            super(CL.CL_INVALID_PROGRAM, "CL_INVALID_PROGRAM", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_PROGRAM_EXECUTABLE errors.
     * 
     */
    public final static class CLInvalidProgramExecutableException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_PROGRAM_EXECUTABLE;
        public CLInvalidProgramExecutableException(String message) {
            super(CL.CL_INVALID_PROGRAM_EXECUTABLE, "CL_INVALID_PROGRAM_EXECUTABLE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_KERNEL_NAME errors.
     * 
     */
    public final static class CLInvalidKernelNameException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_KERNEL_NAME;
        public CLInvalidKernelNameException(String message) {
            super(CL.CL_INVALID_KERNEL_NAME, "CL_INVALID_KERNEL_NAME", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_KERNEL_DEFINITION errors.
     * 
     */
    public final static class CLInvalidKernelDefinitionException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_KERNEL_DEFINITION;
        public CLInvalidKernelDefinitionException(String message) {
            super(CL.CL_INVALID_KERNEL_DEFINITION, "CL_INVALID_KERNEL_DEFINITION", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_KERNEL errors.
     * 
     */
    public final static class CLInvalidKernelException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_KERNEL;
        public CLInvalidKernelException(String message) {
            super(CL.CL_INVALID_KERNEL, "CL_INVALID_KERNEL", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_ARG_INDEX errors.
     * 
     */
    public final static class CLInvalidArgIndexException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_ARG_INDEX;
        public CLInvalidArgIndexException(String message) {
            super(CL.CL_INVALID_ARG_INDEX, "CL_INVALID_ARG_INDEX", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_ARG_VALUE errors.
     * 
     */
    public final static class CLInvalidArgValueException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_ARG_VALUE;
        public CLInvalidArgValueException(String message) {
            super(CL.CL_INVALID_ARG_VALUE, "CL_INVALID_ARG_VALUE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_ARG_SIZE errors.
     * 
     */
    public final static class CLInvalidArgSizeException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_ARG_SIZE;
        public CLInvalidArgSizeException(String message) {
            super(CL.CL_INVALID_ARG_SIZE, "CL_INVALID_ARG_SIZE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_KERNEL_ARGS errors.
     * 
     */
    public final static class CLInvalidKernelArgsException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_KERNEL_ARGS;
        public CLInvalidKernelArgsException(String message) {
            super(CL.CL_INVALID_KERNEL_ARGS, "CL_INVALID_KERNEL_ARGS", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_WORK_DIMENSION errors.
     * 
     */
    public final static class CLInvalidWorkDimensionException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_WORK_DIMENSION;
        public CLInvalidWorkDimensionException(String message) {
            super(CL.CL_INVALID_WORK_DIMENSION, "CL_INVALID_WORK_DIMENSION", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_WORK_GROUP_SIZE errors.
     * 
     */
    public final static class CLInvalidWorkGroupSizeException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_WORK_GROUP_SIZE;
        public CLInvalidWorkGroupSizeException(String message) {
            super(CL.CL_INVALID_WORK_GROUP_SIZE, "CL_INVALID_WORK_GROUP_SIZE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_WORK_ITEM_SIZE errors.
     * 
     */
    public final static class CLInvalidWorkItemSizeException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_WORK_ITEM_SIZE;
        public CLInvalidWorkItemSizeException(String message) {
            super(CL.CL_INVALID_WORK_ITEM_SIZE, "CL_INVALID_WORK_ITEM_SIZE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_GLOBAL_OFFSET errors.
     * 
     */
    public final static class CLInvalidGlobalOffsetException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_GLOBAL_OFFSET;
        public CLInvalidGlobalOffsetException(String message) {
            super(CL.CL_INVALID_GLOBAL_OFFSET, "CL_INVALID_GLOBAL_OFFSET", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_EVENT_WAIT_LIST errors.
     * 
     */
    public final static class CLInvalidEventWaitListException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_EVENT_WAIT_LIST;
        public CLInvalidEventWaitListException(String message) {
            super(CL.CL_INVALID_EVENT_WAIT_LIST, "CL_INVALID_EVENT_WAIT_LIST", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_EVENT errors.
     * 
     */
    public final static class CLInvalidEventException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_EVENT;
        public CLInvalidEventException(String message) {
            super(CL.CL_INVALID_EVENT, "CL_INVALID_EVENT", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_OPERATION errors.
     * 
     */
    public final static class CLInvalidOperationException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_OPERATION;
        public CLInvalidOperationException(String message) {
            super(CL.CL_INVALID_OPERATION, "CL_INVALID_OPERATION", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_GL_OBJECT errors.
     * 
     */
    public final static class CLInvalidGLObjectException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CLGL.CL_INVALID_GL_OBJECT;
        public CLInvalidGLObjectException(String message) {
            super(CLGL.CL_INVALID_GL_OBJECT, "CL_INVALID_GL_OBJECT", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_BUFFER_SIZE errors.
     * 
     */
    public final static class CLInvalidBufferSizeException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_BUFFER_SIZE;
        public CLInvalidBufferSizeException(String message) {
            super(CL.CL_INVALID_BUFFER_SIZE, "CL_INVALID_BUFFER_SIZE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_MIP_LEVEL errors.
     * 
     */
    public final static class CLInvalidMipLevelException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_MIP_LEVEL;
        public CLInvalidMipLevelException(String message) {
            super(CL.CL_INVALID_MIP_LEVEL, "CL_INVALID_MIP_LEVEL", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_GLOBAL_WORK_SIZE errors.
     * 
     */
    public final static class CLInvalidGlobalWorkSizeException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_GLOBAL_WORK_SIZE;
        public CLInvalidGlobalWorkSizeException(String message) {
            super(CL.CL_INVALID_GLOBAL_WORK_SIZE, "CL_INVALID_GLOBAL_WORK_SIZE", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_PROPERTY errors.
     * 
     */
    public final static class CLInvalidPropertyException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_INVALID_PROPERTY;
        public CLInvalidPropertyException(String message) {
            super(CL.CL_INVALID_PROPERTY, "CL_INVALID_PROPERTY", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_INVALID_GL_SHAREGROUP_REFERENCE_KHR errors.
     * 
     */
    public final static class CLInvalidGLSharegroupReferenceKhrException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CLGL.CL_INVALID_GL_SHAREGROUP_REFERENCE_KHR;
        public CLInvalidGLSharegroupReferenceKhrException(String message) {
            super(CLGL.CL_INVALID_GL_SHAREGROUP_REFERENCE_KHR, "CL_INVALID_GL_SHAREGROUP_REFERENCE_KHR", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_PLATFORM_NOT_FOUND_KHR errors.
     * 
     */
    public final static class CLPlatformNotFoundKhrException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_PLATFORM_NOT_FOUND_KHR;
        public CLPlatformNotFoundKhrException(String message) {
            super(CL.CL_PLATFORM_NOT_FOUND_KHR, "CL_PLATFORM_NOT_FOUND_KHR", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_MISALIGNED_SUB_BUFFER_OFFSET errors.
     * 
     */
    public final static class CLMisalignedSubBufferOffsetException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_MISALIGNED_SUB_BUFFER_OFFSET;
        public CLMisalignedSubBufferOffsetException(String message) {
            super(CL.CL_MISALIGNED_SUB_BUFFER_OFFSET, "CL_MISALIGNED_SUB_BUFFER_OFFSET", message);
        }
    }

    /**
     * {@link CLException} thrown on CL.CL_EXEC_STATUS_ERROR_FOR_EVENTS_IN_WAIT_LIST errors.
     * 
     */
    public final static class CLExecStatusErrorForEventsInWaitListException extends CLException {
        private static final long serialVersionUID = CLException.serialVersionUID+CL.CL_EXEC_STATUS_ERROR_FOR_EVENTS_IN_WAIT_LIST;
        public CLExecStatusErrorForEventsInWaitListException(String message) {
            super(CL.CL_EXEC_STATUS_ERROR_FOR_EVENTS_IN_WAIT_LIST, "CL_EXEC_STATUS_ERROR_FOR_EVENTS_IN_WAIT_LIST", message);
        }
    }

}