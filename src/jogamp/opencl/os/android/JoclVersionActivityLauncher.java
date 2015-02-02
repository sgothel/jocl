package jogamp.opencl.os.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class JoclVersionActivityLauncher extends Activity {
       @Override
       public void onCreate(final Bundle savedInstanceState) {
           super.onCreate(savedInstanceState);

           final Uri uri = Uri.parse("launch://jogamp.org/jogamp.opencl.os.android.JoclVersionActivity?sys=com.jogamp.common&sys=com.jogamp.opengl&sys=com.jogamp.opencl&pkg=com.jogamp.opencl.test&jogamp.debug=all&jocl.debug=all");
           final Intent intent = new Intent("org.jogamp.launcher.action.LAUNCH_ACTIVITY_NORMAL", uri);
           Log.d(getClass().getSimpleName(), "Launching Activity: "+intent);
           startActivity (intent);

           finish(); // done
       }
}
