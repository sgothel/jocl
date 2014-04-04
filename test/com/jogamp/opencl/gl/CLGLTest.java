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

import javax.media.opengl.GL2;
import javax.media.opengl.GLException;

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

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLContext;

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
        if(MiscUtils.isOpenCLUnavailable())
            return;

        initGL();

        @SuppressWarnings("unchecked")
        CLPlatform platform = CLPlatform.getDefault(CLPlatformFilters.glSharing());
        @SuppressWarnings("unchecked")
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

        CLContext context = CLGLContext.create(glcontext, device);
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
        if(MiscUtils.isOpenCLUnavailable())
            return;

        initGL();
        makeGLCurrent();
        assertTrue(glcontext.isCurrent());

        @SuppressWarnings("unchecked")
        CLPlatform platform = CLPlatform.getDefault(glSharing(glcontext));
        if(platform == null) {
            out.println("test aborted");
            return;
        }

        @SuppressWarnings("unchecked")
        CLDevice theChosenOne = platform.getMaxFlopsDevice(CLDeviceFilters.glSharing());
        out.println(theChosenOne);

        CLGLContext context = CLGLContext.create(glcontext, theChosenOne);

        try{
            out.println(context);

            GL2 gl = glcontext.getGL().getGL2();

            int[] id = new int[1];
            gl.glGenBuffers(id.length, id, 0);

            IntBuffer glData = Buffers.newDirectIntBuffer(new int[] {0,1,2,3,4,5,6,7,8});
            glData.rewind();

            // create and write GL buffer
            gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
                gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, id[0]);
                gl.glBufferData(GL2.GL_ARRAY_BUFFER, glData.capacity()*4, glData, GL2.GL_STATIC_DRAW);
                gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
            gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
            gl.glFinish();


            // create CLGL buffer
            IntBuffer clData = Buffers.newDirectIntBuffer(9);
            CLGLBuffer<IntBuffer> clBuffer = context.createFromGLBuffer(clData, id[0], glData.capacity()*4, Mem.READ_ONLY);

            assertEquals(glData.capacity(), clBuffer.getCLCapacity());
            assertEquals(glData.capacity()*4, clBuffer.getCLSize());


            CLCommandQueue queue = theChosenOne.createCommandQueue();

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

//    @Test(timeout=15000)
    @Test
    public void textureSharing() {

        out.println(" - - - glcl; textureSharing - - - ");
        if(MiscUtils.isOpenCLUnavailable())
            return;

        initGL();
        makeGLCurrent();
        assertTrue(glcontext.isCurrent());

        @SuppressWarnings("unchecked")
        CLPlatform platform = CLPlatform.getDefault(glSharing(glcontext));
        if(platform == null) {
            out.println("test aborted");
            return;
        }

        @SuppressWarnings("unchecked")
        CLDevice device = platform.getMaxFlopsDevice(CLDeviceFilters.glSharing());
        out.println(device);

        CLGLContext context = CLGLContext.create(glcontext, device);

        try {
            out.println(context);

            GL2 gl = glcontext.getGL().getGL2();

            // create and write GL texture
            int[] id = new int[1];
            gl.glGenTextures(id.length, id, 0);
            gl.glActiveTexture(GL2.GL_TEXTURE0);
            gl.glBindTexture  (GL2.GL_TEXTURE_2D, id[0]);
//            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
//            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
//            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_BASE_LEVEL, 0);
//            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAX_LEVEL, 0);

            ByteBuffer bufferGL = Buffers.newDirectByteBuffer(new byte [] {
                (byte)0,  (byte)5,  (byte)10, (byte)0xff,
                (byte)15, (byte)20, (byte)25, (byte)0xff,
                (byte)30, (byte)35, (byte)40, (byte)0xff,
                (byte)45, (byte)50, (byte)55, (byte)0xff});
            bufferGL.rewind();
            gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, 2, 2, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, bufferGL);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
            gl.glFinish();

            // create CLGL buffer
            ByteBuffer bufferCL = Buffers.newDirectByteBuffer(2*2*4);
            CLGLTexture2d<ByteBuffer> clTexture = context.createFromGLTexture2d(bufferCL, GL2.GL_TEXTURE_2D, id[0], 0, CLBuffer.Mem.READ_ONLY);

//            assertEquals(bufferGL.capacity(), clTexture.getCLCapacity());
//            assertEquals(bufferGL.capacity(), clTexture.getCLSize());

            CLCommandQueue queue = device.createCommandQueue();

            // read gl buffer into cl nio buffer
            queue.putAcquireGLObject(clTexture)
                 .putReadImage(clTexture, true)
                 .putReleaseGLObject(clTexture);

            while(bufferCL.hasRemaining()) {
                byte bGL = bufferGL.get();
                byte bCL = bufferCL.get();
                assertEquals(bGL, bCL);
            }

            out.println(clTexture);

            clTexture.release();
            gl.glDeleteBuffers(1, id, 0);
        }
        finally {
            context.release();
            deinitGL();
        }
    }

    private void makeGLCurrent() {
        // we are patient...
        while(true) {
            try{
                glcontext.makeCurrent();
                break;
            }catch(RuntimeException ex) {
                try {
                    Thread.sleep(200);
                    // I don't give up yet!
                } catch (InterruptedException ignore) { }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String tstname = CLGLTest.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
