/*
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

import java.util.Random;

import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.opencl.llb.impl.BuildProgramCallback;
import com.jogamp.opencl.llb.CL;
import com.jogamp.opencl.llb.CLContextBinding;
import com.jogamp.opencl.llb.CLDeviceBinding;
import com.jogamp.opencl.llb.CLMemObjBinding;
import com.jogamp.opencl.llb.CLPlatformBinding;
import com.jogamp.opencl.llb.CLProgramBinding;
import com.jogamp.opencl.test.util.MiscUtils;
import com.jogamp.opencl.test.util.UITestCase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static java.lang.System.*;
import static org.junit.Assert.*;
import static com.jogamp.common.nio.Buffers.*;
import static com.jogamp.common.os.Platform.*;
import static com.jogamp.opencl.test.util.MiscUtils.*;
import static com.jogamp.opencl.util.CLUtil.*;

/**
 * Test testing the low level bindings.
 * @author Michael Bien, et al.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LowLevelBindingTest extends UITestCase {

    private final static String programSource =
              " // OpenCL Kernel Function for element by element vector addition                                  \n"
            + "kernel void VectorAdd(global const int* a, global const int* b, global int* c, int iNumElements) { \n"
            + "    // get index in global data array                                                              \n"
            + "    int iGID = get_global_id(0);                                                                   \n"
            + "    // bound check (equivalent to the limit on a 'for' loop for standard/serial C code             \n"
            + "    if (iGID >= iNumElements)  {                                                                   \n"
            + "        return;                                                                                    \n"
            + "    }                                                                                              \n"
            + "    // add the vector elements                                                                     \n"
            + "    c[iGID] = a[iGID] + b[iGID];                                                                   \n"
            + "}                                                                                                  \n"
            + "kernel void Test(global const int* a, global const int* b, global int* c, int iNumElements) {      \n"
            + "    // get index in global data array                                                              \n"
            + "    int iGID = get_global_id(0);                                                                   \n"
            + "    // bound check (equivalent to the limit on a 'for' loop for standard/serial C code             \n"
            + "    if (iGID >= iNumElements)  {                                                                   \n"
            + "        return;                                                                                    \n"
            + "    }                                                                                              \n"
            + "    c[iGID] = iGID;                                                                                \n"
            + "}                                                                                                  \n";

    private int ELEMENT_COUNT = 11444777;


    @BeforeClass
    public static void setUpClass() throws Exception {
        out.println("OS: " + System.getProperty("os.name"));
        out.println("VM: " + System.getProperty("java.vm.name"));
    }

    @Test
    public void contextlessTest() {

        out.println(" - - - lowLevelTest; contextless binding - - - ");

        final CL cl = CLPlatform.getLowLevelCLInterface();

//        System.out.println(((CLImpl)cl).clGetExtensionFunctionAddress("clCreateFromGLBuffer"));
//        System.out.println(((CLImpl)cl).clGetExtensionFunctionAddress("clEnqueueAcquireGLObjects"));

        int ret = CL.CL_SUCCESS;

        final IntBuffer intBuffer = newDirectIntBuffer(1);
        // find all available OpenCL platforms
        ret = cl.clGetPlatformIDs(0, null, intBuffer);
        checkForError(ret);
        out.println("#platforms: "+intBuffer.get(0));

        final PointerBuffer platformId = PointerBuffer.allocateDirect(intBuffer.get(0));
        ret = cl.clGetPlatformIDs(platformId.capacity(), platformId, null);
        checkForError(ret);

        // print platform info
        final PointerBuffer longBuffer = PointerBuffer.allocateDirect(1);
        final ByteBuffer bb = newDirectByteBuffer(128);

        for (int i = 0; i < platformId.capacity(); i++)  {

            final long platform = platformId.get(i);
            out.println("platform id: "+platform);

            ret = cl.clGetPlatformInfo(platform, CLPlatformBinding.CL_PLATFORM_PROFILE, bb.capacity(), bb, longBuffer);
            checkForError(ret);
            out.println("    profile: " + clString2JavaString(bb, (int)longBuffer.get(0)));

            ret = cl.clGetPlatformInfo(platform, CLPlatformBinding.CL_PLATFORM_VERSION, bb.capacity(), bb, longBuffer);
            checkForError(ret);
            out.println("    version: " + clString2JavaString(bb, (int)longBuffer.get(0)));

            ret = cl.clGetPlatformInfo(platform, CLPlatformBinding.CL_PLATFORM_NAME, bb.capacity(), bb, longBuffer);
            checkForError(ret);
            out.println("    name: " + clString2JavaString(bb, (int)longBuffer.get(0)));

            ret = cl.clGetPlatformInfo(platform, CLPlatformBinding.CL_PLATFORM_VENDOR, bb.capacity(), bb, longBuffer);
            checkForError(ret);
            out.println("    vendor: " + clString2JavaString(bb, (int)longBuffer.get(0)));

            //find all devices
            ret = cl.clGetDeviceIDs(platform, CLDeviceBinding.CL_DEVICE_TYPE_ALL, 0, null, intBuffer);
            checkForError(ret);
            out.println("#devices: "+intBuffer.get(0));

            final PointerBuffer devices = PointerBuffer.allocateDirect(intBuffer.get(0));
            ret = cl.clGetDeviceIDs(platform, CLDeviceBinding.CL_DEVICE_TYPE_ALL, devices.capacity(), devices, null);

            //print device info
            for (int j = 0; j < devices.capacity(); j++) {
                final long device = devices.get(j);
                ret = cl.clGetDeviceInfo(device, CLDeviceBinding.CL_DEVICE_NAME, bb.capacity(), bb, longBuffer);
                checkForError(ret);
                out.println("    device: " + clString2JavaString(bb, (int)longBuffer.get(0)));

                ret = cl.clGetDeviceInfo(device, CLDeviceBinding.CL_DEVICE_TYPE, bb.capacity(), bb, longBuffer);
                checkForError(ret);
                out.println("    type: " + CLDevice.Type.valueOf(bb.get()));
                bb.rewind();

            }

        }
    }

    @Test
    public void createContextTest() {

        out.println(" - - - createContextTest - - - ");

        final CL cl = CLPlatform.getLowLevelCLInterface();

        final IntBuffer intBuffer = newDirectIntBuffer(1);
        // find all available OpenCL platforms
        int ret = cl.clGetPlatformIDs(0, null, intBuffer);
        checkForError(ret);
        out.println("#platforms: "+intBuffer.get(0));

        final PointerBuffer pb = PointerBuffer.allocateDirect(intBuffer.get(0));
        ret = cl.clGetPlatformIDs(pb.capacity(), pb, null);
        checkForError(ret);

        final long platform = pb.get(0);

        //find all devices
        ret = cl.clGetDeviceIDs(platform, CLDeviceBinding.CL_DEVICE_TYPE_ALL, 0, null, intBuffer);
        checkForError(ret);
        out.println("#devices: "+intBuffer.get(0));

        final PointerBuffer devices = PointerBuffer.allocateDirect(intBuffer.get(0));
        ret = cl.clGetDeviceIDs(platform, CLDeviceBinding.CL_DEVICE_TYPE_ALL, devices.capacity(), devices, null);

        final long context = cl.clCreateContext(null, devices, null, intBuffer);
        checkError("on clCreateContext", intBuffer.get());

        //get number of devices
        final PointerBuffer longBuffer = PointerBuffer.allocateDirect(1);
        ret = cl.clGetContextInfo(context, CLContextBinding.CL_CONTEXT_DEVICES, 0, null, longBuffer);
        checkError("on clGetContextInfo", ret);

        final long contextDevices = longBuffer.get(0)/(is32Bit()?4:8);
        out.println("context created on " + contextDevices + " devices");

        //check if equal
        assertEquals("context was not created on all devices specified", devices.capacity(), contextDevices);

        ret = cl.clReleaseContext(context);
        checkError("on clReleaseContext", ret);
    }

    @Test
    public void lowLevelVectorAddTest() throws InterruptedException {

        out.println(" - - - lowLevelTest2; VectorAdd kernel - - - ");

        final CL cl = CLPlatform.getLowLevelCLInterface();

        int ret = CL.CL_SUCCESS;
        final IntBuffer intBuffer = newDirectIntBuffer(1);

        // find all available OpenCL platforms
        ret = cl.clGetPlatformIDs(0, null, intBuffer);
        checkForError(ret);
        assertTrue(intBuffer.get(0) > 0);

        final PointerBuffer pb = PointerBuffer.allocateDirect(intBuffer.get(0));
        ret = cl.clGetPlatformIDs(pb.capacity(), pb, null);
        checkForError(ret);

        final long platform = pb.get(0);
        final PointerBuffer properties = PointerBuffer.allocateDirect(3).put(CLContextBinding.CL_CONTEXT_PLATFORM)
                                              .put(platform).put(0) // 0 terminated array
                                              .rewind();
        final long context = cl.clCreateContextFromType(properties, CLDeviceBinding.CL_DEVICE_TYPE_ALL, null, null);
        out.println("context handle: "+context);
        checkError("on clCreateContextFromType", ret);

        final PointerBuffer longBuffer = PointerBuffer.allocateDirect(1);
        ret = cl.clGetContextInfo(context, CLContextBinding.CL_CONTEXT_DEVICES, 0, null, longBuffer);
        checkError("on clGetContextInfo", ret);

        final int deviceCount = (int) (longBuffer.get(0) / (is32Bit() ? 4 : 8));
        out.println("context created with " + deviceCount + " devices");

        // Was originally 4096, but had to make this bigger or it would crash in UITestCase.oneTimeTearDown(){ System.gc() }
        // without even dumping a stack when using AMD drivers. Presumably the drivers would write past the end
        // of the block and mess up GC info somehow.
        final ByteBuffer bb = newDirectByteBuffer(32768);
        ret = cl.clGetContextInfo(context, CLContextBinding.CL_CONTEXT_DEVICES, bb.capacity(), bb, null);
        checkError("on clGetContextInfo", ret);

        for (int i = 0; i < deviceCount; i++) {
            out.println("device id: " + (is32Bit()?bb.getInt():bb.getLong()));
        }

        // use a random device
        int offset = new Random().nextInt(deviceCount);
        out.println("using device# " + offset);
        offset *= (is32Bit() ? 4 : 8);
        final long device = is32Bit()?bb.getInt(offset):bb.getLong(offset);

        bb.rewind();
        ret = cl.clGetDeviceInfo(device, CLDeviceBinding.CL_DEVICE_MAX_WORK_GROUP_SIZE, bb.capacity(), bb, null);
        checkError("on clGetDeviceInfo", ret);
        final int maxWGS = bb.getInt();
        out.println("max WGS: " + maxWGS);

        // Create a command-queue
        final long commandQueue = cl.clCreateCommandQueue(context, device, 0, intBuffer);
        checkError("on clCreateCommandQueue", intBuffer.get(0));

        final int localWorkSize = Math.min(128, maxWGS);      // set and log Global and Local work size dimensions
        int elementCount = ELEMENT_COUNT;
        int globalWorkSize = 0;

        ByteBuffer srcA = null;
        ByteBuffer srcB = null;
        ByteBuffer dest = null;
        boolean allocated = false;
        int divisor = 1;
        while( !allocated ) {
            try {
                // round up to the nearest multiple of the LocalWorkSize
                globalWorkSize = roundUp(localWorkSize, elementCount);
                out.println("allocating three buffers of size: "+globalWorkSize);
                srcA = newDirectByteBuffer(globalWorkSize*SIZEOF_INT);
                srcB = newDirectByteBuffer(globalWorkSize*SIZEOF_INT);
                dest = newDirectByteBuffer(globalWorkSize*SIZEOF_INT);
                allocated = true;
            }
            catch( final OutOfMemoryError oome ) {
                ++divisor;
                elementCount /= divisor;
                out.println("not enough direct buffer memory; retrying with smaller buffers");
            }
        }

        // Allocate the OpenCL buffer memory objects for source and result on the device GMEM
        final long devSrcA = cl.clCreateBuffer(context, CLMemObjBinding.CL_MEM_READ_ONLY, srcA.capacity(), null, intBuffer);
        checkError("on clCreateBuffer", intBuffer.get(0));
        final long devSrcB = cl.clCreateBuffer(context, CLMemObjBinding.CL_MEM_READ_ONLY, srcB.capacity(), null, intBuffer);
        checkError("on clCreateBuffer", intBuffer.get(0));
        final long devDst  = cl.clCreateBuffer(context, CLMemObjBinding.CL_MEM_WRITE_ONLY, dest.capacity(), null, intBuffer);
        checkError("on clCreateBuffer", intBuffer.get(0));


        // Create the program
        final PointerBuffer lengths = PointerBuffer.allocateDirect(1).put(programSource.length()).rewind();
        final long program = cl.clCreateProgramWithSource(context, 1, new String[] {programSource}, lengths, intBuffer);
        out.println("program id: "+program);
        checkError("on clCreateProgramWithSource", intBuffer.get(0));

        // tests if the callback is called
        final CountDownLatch latch = new CountDownLatch(1);
        final BuildProgramCallback callback = new BuildProgramCallback() {
            @Override
            public void buildFinished(final long cl_program) {
                try{
                    assertEquals(program, cl_program);
                }finally{
                    latch.countDown();
                }
            }
        };

        // spec: building programs is not threadsafe (see loadtest)
        synchronized(CLProgram.class) {
            // Build the program
            ret = cl.clBuildProgram(program, 0, null, null, callback);
            checkError("on clBuildProgram", ret);

            out.println("waiting for program to build...");
            latch.await();
        }
        out.println("done");

        // Read program infos
        bb.rewind();
        ret = cl.clGetProgramInfo(program, CLProgramBinding.CL_PROGRAM_NUM_DEVICES, bb.capacity(), bb, null);
        checkError("on clGetProgramInfo1", ret);
        out.println("program associated with "+bb.getInt(0)+" device(s)");

        ret = cl.clGetProgramInfo(program, CLProgramBinding.CL_PROGRAM_SOURCE, 0, null, longBuffer);
        checkError("on clGetProgramInfo CL_PROGRAM_SOURCE", ret);
        out.println("program source length (cl): "+longBuffer.get(0));
        out.println("program source length (java): "+programSource.length());

        bb.rewind();
        ret = cl.clGetProgramInfo(program, CLProgramBinding.CL_PROGRAM_SOURCE, bb.capacity(), bb, null);
        checkError("on clGetProgramInfo CL_PROGRAM_SOURCE", ret);
        out.println("program source:\n" + clString2JavaString(bb, (int)longBuffer.get(0)));

        // Check program status
        bb.rewind();
        ret = cl.clGetProgramBuildInfo(program, device, CLProgramBinding.CL_PROGRAM_BUILD_STATUS, bb.capacity(), bb, null);
        checkError("on clGetProgramBuildInfo1", ret);

        out.println("program build status: " + CLProgram.Status.valueOf(bb.getInt(0)));
        assertEquals("build status", CLProgramBinding.CL_BUILD_SUCCESS, bb.getInt(0));

        // Read build log
        ret = cl.clGetProgramBuildInfo(program, device, CLProgramBinding.CL_PROGRAM_BUILD_LOG, 0, null, longBuffer);
        checkError("on clGetProgramBuildInfo2", ret);
        out.println("program log length: " + longBuffer.get(0));

        bb.rewind();
        ret = cl.clGetProgramBuildInfo(program, device, CLProgramBinding.CL_PROGRAM_BUILD_LOG, bb.capacity(), bb, null);
        checkError("on clGetProgramBuildInfo3", ret);
        out.println("log:\n" + clString2JavaString(bb, (int)longBuffer.get(0)));

        // Create the kernel
        final long kernel = cl.clCreateKernel(program, "VectorAdd", intBuffer);
        out.println("kernel id: "+kernel);
        checkError("on clCreateKernel", intBuffer.get(0));

//        srcA.limit(elementCount*SIZEOF_FLOAT);
//        srcB.limit(elementCount*SIZEOF_FLOAT);

        fillBuffer(srcA, 23456);
        fillBuffer(srcB, 46987);

        // Set the Argument values
        ret = cl.clSetKernelArg(kernel, 0, is32Bit()?SIZEOF_INT:SIZEOF_LONG, wrap(devSrcA));        checkError("on clSetKernelArg0", ret);
        ret = cl.clSetKernelArg(kernel, 1, is32Bit()?SIZEOF_INT:SIZEOF_LONG, wrap(devSrcB));        checkError("on clSetKernelArg1", ret);
        ret = cl.clSetKernelArg(kernel, 2, is32Bit()?SIZEOF_INT:SIZEOF_LONG, wrap(devDst));         checkError("on clSetKernelArg2", ret);
        ret = cl.clSetKernelArg(kernel, 3, SIZEOF_INT,                       wrap(elementCount));   checkError("on clSetKernelArg3", ret);

        out.println("used device memory: "+ (srcA.capacity()+srcB.capacity()+dest.capacity())/1000000 +"MB");

        // Asynchronous write of data to GPU device
        ret = cl.clEnqueueWriteBuffer(commandQueue, devSrcA, CL.CL_FALSE, 0, srcA.capacity(), srcA, 0, null, null);
        checkError("on clEnqueueWriteBuffer", ret);
        ret = cl.clEnqueueWriteBuffer(commandQueue, devSrcB, CL.CL_FALSE, 0, srcB.capacity(), srcB, 0, null, null);
        checkError("on clEnqueueWriteBuffer", ret);

        // Launch kernel
        final PointerBuffer gWS = PointerBuffer.allocateDirect(1).put(globalWorkSize).rewind();
        final PointerBuffer lWS = PointerBuffer.allocateDirect(1).put(localWorkSize).rewind();
        ret = cl.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, gWS, lWS, 0, null, null);
        checkError("on clEnqueueNDRangeKernel", ret);

        // Synchronous/blocking read of results
        ret = cl.clEnqueueReadBuffer(commandQueue, devDst, CL.CL_TRUE, 0, dest.capacity(), dest, 0, null, null);
        checkError("on clEnqueueReadBuffer", ret);

        out.println("a+b=c result snapshot: ");
        for(int i = 0; i < 10; i++)
            out.print(dest.getInt()+", ");
        out.println("...; "+dest.remaining()/SIZEOF_INT + " more");


        // cleanup
        ret = cl.clReleaseCommandQueue(commandQueue);
        checkError("on clReleaseCommandQueue", ret);

        ret = cl.clReleaseMemObject(devSrcA);
        checkError("on clReleaseMemObject", ret);
        ret = cl.clReleaseMemObject(devSrcB);
        checkError("on clReleaseMemObject", ret);
        ret = cl.clReleaseMemObject(devDst);
        checkError("on clReleaseMemObject", ret);

        ret = cl.clReleaseProgram(program);
        checkError("on clReleaseProgram", ret);

        ret = cl.clReleaseKernel(kernel);
        checkError("on clReleaseKernel", ret);

//        ret = cl.clUnloadCompiler();
//        checkError("on clUnloadCompiler", ret);

        ret = cl.clReleaseContext(context);
        checkError("on clReleaseContext", ret);

    }

//    @Test
    public void loadTest() throws InterruptedException {
        //for memory leak detection; e.g watch out for "out of host memory" errors
        out.println(" - - - loadTest - - - ");

        final ExecutorService pool = Executors.newFixedThreadPool(8);
        final List<Callable<Object>> tasks = new ArrayList<Callable<Object>>();

        for(int i = 0; i < 100; i++) {
            final int n = i;
            tasks.add(new Callable<Object>() {
                @Override
                public Object call() {
                    try {
                        out.println("###start iteration " + n);
                        final LowLevelBindingTest test = new LowLevelBindingTest();
                        test.ELEMENT_COUNT = 123456;
                        test.lowLevelVectorAddTest();
                        out.println("###end iteration " + n);
                    } catch (final InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    return null;
                }
            });
        }

        for (final Future<Object> future : pool.invokeAll(tasks)) {
            try {
                future.get();
            } catch (final ExecutionException ex) {
                ex.printStackTrace();
                pool.shutdown();
                fail();
            }
        }
        pool.shutdown();
        pool.awaitTermination(300, TimeUnit.SECONDS);
    }

    private ByteBuffer wrap(final long value) {
        return (ByteBuffer) newDirectByteBuffer(8).putLong(value).rewind();
    }

    private void checkForError(final int ret) {
        this.checkError("", ret);
    }

    private void checkError(final String msg, final int ret) {
        if(ret != CL.CL_SUCCESS)
            throw CLException.newException(ret, msg);
    }

    public static void main(final String[] args) throws IOException {
        final String tstname = LowLevelBindingTest.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
