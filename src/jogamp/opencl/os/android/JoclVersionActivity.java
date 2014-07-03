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
package jogamp.opencl.os.android;

import jogamp.common.os.PlatformPropsImpl;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.opencl.JoclVersion;

import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;

public class JoclVersionActivity extends JoclBaseActivity {

   @Override
   public void onCreate(final Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);

       setFullscreenFeature(getWindow(), true);

       final Window androidWindow = getWindow();
       androidWindow.requestFeature(android.view.Window.FEATURE_NO_TITLE);
       androidWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
       androidWindow.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

       final android.view.ViewGroup viewGroup = new android.widget.FrameLayout(getActivity().getApplicationContext());
       getWindow().setContentView(viewGroup);

       final TextView tv = new TextView(getActivity());
       final ScrollView scroller = new ScrollView(getActivity());
       scroller.addView(tv);
       setContentView(scroller);

       final JoclVersion joclVersion = JoclVersion.getInstance();
       final String info1 = "JOCL Version Info"+PlatformPropsImpl.NEWLINE+
                            VersionUtil.getPlatformInfo()+PlatformPropsImpl.NEWLINE+
                            GlueGenVersion.getInstance()+PlatformPropsImpl.NEWLINE+
                            joclVersion.toString()+PlatformPropsImpl.NEWLINE+
                            joclVersion.getOpenCLTextInfo(null).toString()+PlatformPropsImpl.NEWLINE;
       System.err.println(info1);
       tv.setText(info1);

       Log.d(MD.TAG, "onCreate - X");
   }
}
