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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

public class JoclBaseActivity extends Activity {
   boolean isDelegatedActivity;
   Activity rootActivity;
   boolean setThemeCalled = false;

   public JoclBaseActivity() {
       super();
       isDelegatedActivity = false;
       rootActivity = this;
   }

   public void setRootActivity(final Activity rootActivity) {
       this.rootActivity = rootActivity;
       this.isDelegatedActivity = this != rootActivity;
   }

   public final boolean isDelegatedActivity() {
       return isDelegatedActivity;
   }

   public final Activity getActivity() {
       return rootActivity;
   }

   /**
    * Convenient method to set the Android window's flags to fullscreen or size-layout depending on the given NEWT window.
    * <p>
    * Must be called before creating the view and adding any content, i.e. setContentView() !
    * </p>
    * @param androidWindow
    * @param newtWindow
    */
   public void setFullscreenFeature(final android.view.Window androidWindow, final boolean fullscreen) {
        if(null == androidWindow) {
            throw new IllegalArgumentException("Android or Window null");
        }

        if( fullscreen ) {
            androidWindow.requestFeature(android.view.Window.FEATURE_NO_TITLE);
            androidWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            androidWindow.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        } else {
            androidWindow.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            androidWindow.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
   }

   /**
    * Convenient method to set this context's theme to transparency.
    * <p>
    * Must be called before creating the view and adding any content, i.e. setContentView() !
    * </p>
    * <p>
    * Is normally issued by {@link #setContentView(android.view.Window, Window)}
    * if the requested NEWT Capabilities ask for transparency.
    * </p>
    * <p>
    * Can be called only once.
    * </p>
    */
   public void setTransparencyTheme() {
       if(!setThemeCalled) {
           setThemeCalled = true;
           final Context ctx = getActivity().getApplicationContext();
           final String frn = ctx.getPackageName()+":style/Theme.Transparent";
           final int resID = ctx.getResources().getIdentifier("Theme.Transparent", "style", ctx.getPackageName());
           if(0 == resID) {
               Log.d(MD.TAG, "SetTransparencyTheme: Resource n/a: "+frn);
           } else {
               Log.d(MD.TAG, "SetTransparencyTheme: Setting style: "+frn+": 0x"+Integer.toHexString(resID));
               ctx.setTheme(resID);
           }
       }
   }

   @Override
   public android.view.Window getWindow() {
       if( isDelegatedActivity() ) {
           return getActivity().getWindow();
       } else {
           return super.getWindow();
       }
   }

   @Override
   public void onCreate(final Bundle savedInstanceState) {
       Log.d(MD.TAG, "onCreate.0");
       if(!isDelegatedActivity()) {
           super.onCreate(savedInstanceState);
       }
       jogamp.common.os.android.StaticContext.init(rootActivity.getApplicationContext());
       Log.d(MD.TAG, "onCreate.X");
   }

   @Override
   public void onStart() {
     Log.d(MD.TAG, "onStart.0");
     if(!isDelegatedActivity()) {
         super.onStart();
     }
     Log.d(MD.TAG, "onStart.X");
   }

   @Override
   public void onRestart() {
     Log.d(MD.TAG, "onRestart.0");
     if(!isDelegatedActivity()) {
         super.onRestart();
     }
     Log.d(MD.TAG, "onRestart.X");
   }

   @Override
   public void onResume() {
     Log.d(MD.TAG, "onResume.0");
     if(!isDelegatedActivity()) {
         super.onResume();
     }
     Log.d(MD.TAG, "onResume.X");
   }

   @Override
   public void onPause() {
     Log.d(MD.TAG, "onPause.0");
     if( !isDelegatedActivity() ) {
         super.onPause();
     }
     Log.d(MD.TAG, "onPause.X");
   }

   @Override
   public void onStop() {
     Log.d(MD.TAG, "onStop.0");
     if( !isDelegatedActivity() ) {
         super.onStop();
     }
     Log.d(MD.TAG, "onStop.X");
   }

   @Override
   public void onDestroy() {
     Log.d(MD.TAG, "onDestroy.0");
     if(!isDelegatedActivity()) {
         super.onDestroy();
     }
     Log.d(MD.TAG, "onDestroy.X");
   }
}
