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

import com.jogamp.common.JogampRuntimeException;
import com.jogamp.opencl.llb.CL;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.CLProperty;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Michael Bien
 */
public class CLUtil {

    public static String clString2JavaString(final byte[] chars, int clLength) {

        // certain char queries on windows always claim to have a fixed length
        // e.g. (clDeviceInfo(CL_DEVICE_NAME) is always 64.. but luckily they are 0 terminated)
        while(clLength > 0 && chars[--clLength] == 0);

        return clLength==0 ? "" : new String(chars, 0, clLength+1);
    }

    public static String clString2JavaString(final ByteBuffer chars, final int clLength) {
        if (clLength==0) {
            return "";
        }else{
            final byte[] array = new byte[clLength];
            chars.get(array).rewind();
            return clString2JavaString(array, clLength);
        }
    }

    /**
     * Returns true if clBoolean == CL.CL_TRUE.
     */
    public static boolean clBoolean(final int clBoolean) {
        return clBoolean == CL.CL_TRUE;
    }

    /**
     * Returns b ? CL.CL_TRUE : CL.CL_FALSE
     */
    public static int clBoolean(final boolean b) {
        return b ? CL.CL_TRUE : CL.CL_FALSE;
    }

    /**
     * Reads all platform properties and returns them as key-value map.
     */
    public static Map<String, String> obtainPlatformProperties(final CLPlatform platform) {
        return readCLProperties(platform);
    }

    /**
     * Reads all device properties and returns them as key-value map.
     */
    public static Map<String, String> obtainDeviceProperties(final CLDevice dev) {
        return readCLProperties(dev);
    }

    private static Map<String, String> readCLProperties(final Object obj) {
        try {
            return invoke(listMethods(obj.getClass()), obj);
        } catch (final IllegalArgumentException ex) {
            throw new JogampRuntimeException(ex);
        } catch (final IllegalAccessException ex) {
            throw new JogampRuntimeException(ex);
        }
    }

    static Map<String, String> invoke(final List<Method> methods, final Object obj) throws IllegalArgumentException, IllegalAccessException {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        for (final Method method : methods) {
            Object info = null;
            try {
                info = method.invoke(obj);
            } catch (final InvocationTargetException ex) {
                info = ex.getTargetException();
            }

            if(info.getClass().isArray()) {
                info = asList(info);
            }

            final String value = method.getAnnotation(CLProperty.class).value();
            map.put(value, info.toString());
        }
        return map;
    }

    static List<Method> listMethods(final Class<?> clazz) throws SecurityException {
        final List<Method> list = new ArrayList<Method>();
        for (final Method method : clazz.getDeclaredMethods()) {
            final Annotation[] annotations = method.getDeclaredAnnotations();
            for (final Annotation annotation : annotations) {
                if (annotation instanceof CLProperty) {
                    list.add(method);
                }
            }
        }
        return list;
    }

    private static List<Number> asList(final Object info) {
        final List<Number> list = new ArrayList<Number>();
        if(info instanceof int[]) {
            final int[] array = (int[]) info;
            for (final int i : array) {
                list.add(i);
            }
        }else if(info instanceof long[]) {
            final long[] array = (long[]) info;
            for (final long i : array) {
                list.add(i);
            }
        }else if(info instanceof float[]) {
            final float[] array = (float[]) info;
            for (final float i : array) {
                list.add(i);
            }
        }else if(info instanceof double[]) {
            final double[] array = (double[]) info;
            for (final double i : array) {
                list.add(i);
            }
        }
        return list;
    }

}
