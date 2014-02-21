/*
 * Created on Thursday, May 19 2011 16:43
 */
package com.jogamp.opencl.spi;

/**
 * Internal utility for common OpenCL clGetFooInfo calls.
 * Provides common accessors to CL objects.
 * @author Michael Bien, et al.
 */
public interface CLInfoAccessor {

    int[] getInts(int key, int n);

    /**
     * Returns the <code>uint32_t</code> value for the given key,
     * reinterpreted as a <code>long</code> value.
     */
    long getUInt32Long(int key);

    /**
     * Returns the long value for the given key.
     */
    long getLong(int key);

    /**
     * Returns the String value for the given key.
     */
    String getString(int key);

}
