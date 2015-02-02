/*
 * Copyright 2010 JogAmp Community. All rights reserved.
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

/*
 * Created on Saturday, April 24 2010 02:58 AM
 */

package com.jogamp.opencl.gl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLProgram;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLException;

import com.jogamp.opencl.CLDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.test.util.MiscUtils;
import com.jogamp.opencl.test.util.UITestCase;
import com.jogamp.opencl.util.CLDeviceFilters;
import com.jogamp.opencl.util.CLPlatformFilters;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.fixedfunc.GLPointerFunc;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static com.jogamp.opencl.util.CLPlatformFilters.*;
import static org.junit.Assert.*;
import static java.lang.System.*;

/**
 * Test testing the JOGL - JOCL interoperability.
 * @author Michael Bien, et.al
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CLGLTest extends UITestCase {

    private static GLContext glcontext;
    private static GLWindow glWindow;
    private static Window window;

    public static void initGL() {
        window = NewtFactory.createWindow(new GLCapabilities(GLProfile.getDefault()));
        assertNotNull(window);

        window.setSize(640, 480);

        glWindow = GLWindow.create(window);

        assertNotNull(glWindow);
        glWindow.setVisible(true);

        glcontext = glWindow.getContext();
//        glcontext.makeCurrent();
//        out.println(" - - - - glcontext - - - - ");
//        out.println(glcontext);
//        out.println(" - - - - - - - - - - - - - ");
    }

    private void deinitGL() throws GLException {
        glcontext.release();
        glWindow.destroy();
        window.destroy();

        glcontext = null;
        glWindow = null;
        window = null;
    }

    @Test(timeout=15000)
    public void createContextTest() {

        out.println(" - - - glcl; createContextTest - - - ");

        initGL();

        @SuppressWarnings("unchecked")
        final
        CLPlatform platform = CLPlatform.getDefault(CLPlatformFilters.glSharing());
        @SuppressWarnings("unchecked")
        final
        CLDevice device = platform.getMaxFlopsDevice(CLDeviceFilters.glSharing());

        if(device == null) {
            out.println("Aborting test: no GLCL capable devices found.");
            return;
        }else{
            out.println("isGLMemorySharingSupported==true on: \n    "+device);
        }

        out.println(device.getPlatform());

        assertNotNull(glcontext);
        makeGLCurrent();
        assertTrue(glcontext.isCurrent());

        final CLContext context = CLGLContext.create(glcontext, device);
        assertNotNull(context);

        try{
            out.println(context);
            /*
            CLDevice currentDevice = context.getCurrentGLCLDevice();
            assertNotNull(currentDevice);
            out.println(currentDevice);
             */
        }finally{
            // destroy cl context, gl context still current
            context.release();

            deinitGL();
        }

    }

    @Test(timeout=15000)
    public void vboSharing() {

        out.println(" - - - glcl; vboSharing - - - ");

        initGL();
        makeGLCurrent();
        assertTrue(glcontext.isCurrent());

        @SuppressWarnings("unchecked")
        final
        CLPlatform platform = CLPlatform.getDefault(glSharing(glcontext));
        if(platform == null) {
            out.println("test aborted");
            return;
        }

        @SuppressWarnings("unchecked")
        final
        CLDevice theChosenOne = platform.getMaxFlopsDevice(CLDeviceFilters.glSharing());
        out.println(theChosenOne);

        final CLGLContext context = CLGLContext.create(glcontext, theChosenOne);

        try{
            out.println(context);

            final GL2 gl = glcontext.getGL().getGL2();

            final int[] id = new int[1];
            gl.glGenBuffers(id.length, id, 0);

            final IntBuffer glData = Buffers.newDirectIntBuffer(new int[] {0,1,2,3,4,5,6,7,8});
            glData.rewind();

            // create and write GL buffer
            gl.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, id[0]);
                gl.glBufferData(GL.GL_ARRAY_BUFFER, glData.capacity()*4, glData, GL.GL_STATIC_DRAW);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            gl.glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
            gl.glFinish();


            // create CLGL buffer
            final IntBuffer clData = Buffers.newDirectIntBuffer(9);
            final CLGLBuffer<IntBuffer> clBuffer = context.createFromGLBuffer(clData, id[0], glData.capacity()*4, Mem.READ_ONLY);

            assertEquals(glData.capacity(), clBuffer.getCLCapacity());
            assertEquals(glData.capacity()*4, clBuffer.getCLSize());


            final CLCommandQueue queue = theChosenOne.createCommandQueue();

            // read gl buffer into cl nio buffer
            queue.putAcquireGLObject(clBuffer)
                 .putReadBuffer(clBuffer, true)
                 .putReleaseGLObject(clBuffer);

            while(clData.hasRemaining()) {
                assertEquals(glData.get(), clData.get());
            }

            out.println(clBuffer);

            clBuffer.release();

            gl.glDeleteBuffers(1, id, 0);

        }finally{
            context.release();
            deinitGL();
        }

    }

    @Test(timeout=15000)
    public void textureSharing() {
        out.println(" - - - glcl; textureSharing - - - ");

        initGL();
        makeGLCurrent();
        assertTrue(glcontext.isCurrent());

        @SuppressWarnings("unchecked")
        final
        CLPlatform [] clplatforms = CLPlatform.listCLPlatforms(glSharing(glcontext));
        if(clplatforms.length == 0) {
            out.println("no platform that supports OpenGL-OpenCL interoperability");
            return;
        }

        for(final CLPlatform clplatform : clplatforms) {

            @SuppressWarnings("unchecked")
            final
            CLDevice [] cldevices = clplatform.listCLDevices(CLDeviceFilters.glSharing());

            for(final CLDevice cldevice : cldevices) {
                out.println(cldevice);
                textureSharingInner(cldevice);
            }
        }

        deinitGL();
    }

    public void textureSharingInner(final CLDevice cldevice) {

        final CLGLContext clglcontext = CLGLContext.create(glcontext, cldevice);

        try {
            out.println(clglcontext);

            final GL2 gl = glcontext.getGL().getGL2();

            // create and write GL texture
            final int[] id = new int[1];
            gl.glGenTextures(id.length, id, 0);
            gl.glActiveTexture(GL.GL_TEXTURE0);
            gl.glBindTexture  (GL.GL_TEXTURE_2D, id[0]);
            final int texWidth = 2;
            final int texHeight = 2;
            gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, texWidth, texHeight, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null );
            gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
            gl.glFinish();

            // create CLGL buffer
            final ByteBuffer bufferCL = Buffers.newDirectByteBuffer(texWidth*texHeight*4);
            final CLGLTexture2d<ByteBuffer> clTexture = clglcontext.createFromGLTexture2d(bufferCL, GL.GL_TEXTURE_2D, id[0], 0, CLBuffer.Mem.WRITE_ONLY);

            // set texel values to a formula that can be read back and verified
            final String sourceCL = "__kernel void writeTexture (__write_only image2d_t imageTex, unsigned w, unsigned h ) \n" +
                    "{                                                                        \n" +
                    "    for(unsigned y=1; y<=h; ++y) {                                       \n" +
                    "        for(unsigned x=1; x<=w; ++x) {                                   \n" +
                    "            write_imagef(imageTex, (int2)(x-1,y-1), (float4)(((float)x)/((float)(4*w)), ((float)y)/((float)(4*h)), 0.0f, 1.0f)); \n" +
                    "        }                                                                \n" +
                    "    }                                                                    \n" +
                    "}";
            final CLProgram program = clglcontext.createProgram(sourceCL);
            program.build();
            System.out.println(program.getBuildStatus());
            System.out.println(program.getBuildLog());
            assertTrue(program.isExecutable());

            final CLKernel clkernel = program.createCLKernel("writeTexture")
                    .putArg(clTexture)
                    .putArg(texWidth)
                    .putArg(texHeight)
                    .rewind();

            final CLCommandQueue queue = cldevice.createCommandQueue();

            // write gl texture with cl kernel, then read it to host buffer
            queue.putAcquireGLObject(clTexture)
                .put1DRangeKernel(clkernel, 0, 1, 1)
                .putReadImage(clTexture, true)
                .putReleaseGLObject(clTexture)
                .finish();

            for(int y = 1; y <= texHeight; y++) {
                for(int x = 1; x <= texWidth; x++) {
                    final byte bX = bufferCL.get();
                    final byte bY = bufferCL.get();
                    final byte bZero = bufferCL.get();
                    final byte bMinusOne = bufferCL.get();
                    final byte bXCheck = (byte)(((float)x)/((float)(4*texWidth))*256);
                    final byte bYCheck = (byte)(((float)y)/((float)(4*texHeight))*256);
                    assertEquals(bXCheck, bX);
                    assertEquals(bYCheck, bY);
                    assertEquals(0, bZero);
                    assertEquals(-1, bMinusOne);
                }
            }

            out.println(clTexture);

            clTexture.release();
            gl.glDeleteBuffers(1, id, 0);
        }
        finally {
            clglcontext.release();
        }
    }

    private void makeGLCurrent() {
        // we are patient...
        while(true) {
            try{
                glcontext.makeCurrent();
                break;
            }catch(final RuntimeException ex) {
                try {
                    Thread.sleep(200);
                    // I don't give up yet!
                } catch (final InterruptedException ignore) { }
            }
        }
    }

    public static void main(final String[] args) throws IOException {
        final String tstname = CLGLTest.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
