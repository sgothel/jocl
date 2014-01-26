/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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

import static com.jogamp.common.util.VersionUtil.getPlatformInfo;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.common.util.JogampVersion;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

public class JoclVersion extends JogampVersion {

    protected static volatile JoclVersion jogampCommonVersionInfo;

    protected JoclVersion(String packageName, Manifest mf) {
        super(packageName, mf);
    }

    public static JoclVersion getInstance() {
        if(null == jogampCommonVersionInfo) { // volatile: ok
            synchronized(JoclVersion.class) {
                if( null == jogampCommonVersionInfo ) {
                    final String packageName = "com.jogamp.opencl";
                    final Manifest mf = VersionUtil.getManifest(JoclVersion.class.getClassLoader(), packageName);
                    jogampCommonVersionInfo = new JoclVersion(packageName, mf);
                }
            }
        }
        return jogampCommonVersionInfo;
    }

    public StringBuilder getAllVersions(StringBuilder sb) {
        if(null==sb) {
            sb = new StringBuilder();
        }

        try{
            getPlatformInfo(sb);
            sb.append(Platform.getNewline());
            GlueGenVersion.getInstance().toString(sb);
            sb.append(Platform.getNewline());
            toString(sb);
            sb.append(Platform.getNewline());
        } catch (Exception e) {
            sb.append(e.getMessage());
            e.printStackTrace();
        }

        return sb;
    }

    private volatile int maxKeyStrlen = -1;

    public StringBuilder getOpenCLTextInfo(StringBuilder sb) {
        if(null==sb) {
            sb = new StringBuilder();
        }

        final CLPlatform[] platforms;
        try {
            platforms = CLPlatform.listCLPlatforms();
        } catch (Throwable t) {
            final Throwable cause;
            {
                Throwable pre = null;
                Throwable next = t;
                while( null != next ) {
                    pre = next;
                    next = next.getCause();
                }
                cause = pre;
            }
            System.err.println("CLPlatform.listCLPlatforms() failed, exception: "+cause.getMessage());
            t.printStackTrace();
            sb.append("CLPlatform.listCLPlatforms() failed, exception: "+cause.getMessage());
            StringWriter stackTrace = new StringWriter();
            cause.printStackTrace(new PrintWriter(stackTrace));
            sb.append(stackTrace.toString());
            return sb;
        }

        // platforms
        final List<Map<String, String>> platProps = new ArrayList<Map<String, String>>();
        if( 0 > maxKeyStrlen ) {
            synchronized(this) {
                if( 0 > maxKeyStrlen ) {
                    for (CLPlatform p : platforms) {
                        platProps.add(p.getProperties());
                        final CLDevice[] devices = p.listCLDevices();
                        for (CLDevice d : devices) {
                            final Map<String,String> props = d.getProperties();
                            final Set<Map.Entry<String, String>> entries =  props.entrySet();
                            for(Map.Entry<String, String> e : entries) {
                                maxKeyStrlen = Math.max(maxKeyStrlen, e.getKey().length());
                            }
                        }
                    }
                }
            }
        }
        sb.append(String.format("PP:DD:EE  -  Platform (PP), Device (DD), Entry (EE)%n"));
        int pI = 0;
        for (CLPlatform p : platforms) {
            pI++;
            platProps.add(p.getProperties());
            CLDevice[] devices = p.listCLDevices();
            int dI = 0;
            for (CLDevice d : devices) {
                dI++;
                final Map<String,String> props = d.getProperties();
                final Set<Map.Entry<String, String>> entries =  props.entrySet();
                int eI = 0;
                for(Map.Entry<String, String> e : entries) {
                    eI++;
                    sb.append(String.format("%02d:%02d:%02d %"+maxKeyStrlen+"s: %s%n", pI, dI, eI, e.getKey(), e.getValue()));
                }
            }
        }
        return sb;
    }

    public StringBuilder getOpenCLHtmlInfo(StringBuilder sb) {
        if(null==sb) {
            sb = new StringBuilder();
        }

        final CLPlatform[] platforms;
        try {
            platforms = CLPlatform.listCLPlatforms();
        } catch (Throwable t) {
            final Throwable cause;
            {
                Throwable pre = null;
                Throwable next = t;
                while( null != next ) {
                    pre = next;
                    next = next.getCause();
                }
                cause = pre;
            }
            System.err.println("CLPlatform.listCLPlatforms() failed, exception: "+cause.getMessage());
            t.printStackTrace();
            sb.append("<pre>CLPlatform.listCLPlatforms() failed, exception: "+cause.getMessage());
            StringWriter stackTrace = new StringWriter();
            cause.printStackTrace(new PrintWriter(stackTrace));
            sb.append(stackTrace.toString()).append("</pre>");
            return sb;
        }
        sb.append("<table border=\"1\">");

        // platforms
        List<Map<String, String>> platProps = new ArrayList<Map<String, String>>();
        List<Integer> spans = new ArrayList<Integer>();
        for (CLPlatform p : platforms) {
            platProps.add(p.getProperties());
            spans.add(p.listCLDevices().length);
        }
        fillHtmlTable(platProps, spans, sb);

        // devices
        ArrayList<Map<String, String>> devProps = new ArrayList<Map<String, String>>();
        for (CLPlatform p : platforms) {
            CLDevice[] devices = p.listCLDevices();
            for (CLDevice d : devices) {
                devProps.add(d.getProperties());
            }
        }
        fillHtmlTable(devProps, sb);
        sb.append("</table>");

        return sb;
    }

    private static void fillHtmlTable(List<Map<String, String>> properties, StringBuilder sb) {
        ArrayList<Integer> spans = new ArrayList<Integer>(properties.size());
        for (int i = 0; i < properties.size(); i++) {
            spans.add(1);
        }
        fillHtmlTable(properties, spans, sb);
    }

    private static void fillHtmlTable(List<Map<String, String>> properties, List<Integer> spans, StringBuilder sb) {
        boolean header = true;
        for (String key : properties.get(0).keySet()) {
            sb.append("<tr>");
                htmlCell(sb, key);
                int i = 0;
                for (Map<String, String> map : properties) {
                    htmlCell(sb, spans.get(i), map.get(key), header);
                    i++;
                }
            sb.append("</tr>");
            header = false;
        }
    }

    private static void htmlCell(StringBuilder sb, String value) {
        sb.append("<td>").append(value).append("</td>");
    }

    private static void htmlCell(StringBuilder sb, int span, String value, boolean header) {
        if(header) {
            sb.append("<th colspan=\"").append(span).append("\">").append(value).append("</th>");
        }else{
            sb.append("<td colspan=\"").append(span).append("\">").append(value).append("</td>");
        }
    }

    public static void main(String args[]) {
        System.err.println(VersionUtil.getPlatformInfo());
        System.err.println(GlueGenVersion.getInstance());
        // System.err.println(NativeWindowVersion.getInstance());
        final JoclVersion v = JoclVersion.getInstance();
        System.err.println(v.toString());
        System.err.println(v.getOpenCLTextInfo(null).toString());
        // System.err.println(v.getOpenCLHtmlInfo(null).toString());
    }
}
