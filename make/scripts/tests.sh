#! /bin/bash

if [ -z "$1" -o -z "$2" -o -z "$3" ] ; then
    echo Usage $0 java-exe java-xargs build-dir
    exit 0
fi

javaexe="$1"
shift
javaxargs=$1
shift
bdir="$1"
shift

if [ ! -x "$javaexe" ] ; then
    echo java-exe "$javaexe" is not an executable
    exit 1
fi
if [ ! -d "$bdir" ] ; then
    echo build-dir "$bdir" is not a directory
    exit 1
fi

rm -f java-run.log

spath=`dirname $0`

. $spath/setenv-jocl.sh "$bdir"
unset CLASSPATH

MOSX=0
MOSX_MT=0
uname -a | grep -i Darwin && MOSX=1
if [ $MOSX -eq 1 ] ; then
    echo setup OSX environment vars
    #export NSZombieEnabled=YES
    export NSTraceEvents=YES
    #export OBJC_PRINT_EXCEPTIONS=YES
    echo NSZombieEnabled $NSZombieEnabled 2>&1 | tee -a java-run.log
    echo NSTraceEvents $NSTraceEvents  2>&1 | tee -a java-run.log
    echo OBJC_PRINT_EXCEPTIONS $OBJC_PRINT_EXCEPTIONS  2>&1 | tee -a java-run.log
    MOSX_MT=1
fi

#export LD_LIBRARY_PATH=/opt-linux-x86_64/opencl-lala/lib64:$LD_LIBRARY_PATH

which "$javaexe" 2>&1 | tee -a java-run.log
"$javaexe" -version 2>&1 | tee -a java-run.log
echo LD_LIBRARY_PATH $LD_LIBRARY_PATH 2>&1 | tee -a java-run.log
echo "$javaexe" $javaxargs $X_ARGS $D_ARGS $* 2>&1 | tee -a java-run.log
echo MacOsX $MOSX

function jrun() {
    awton=$1
    shift

    MODULE_ARGS="--enable-native-access=ALL-UNNAMED --add-opens java.desktop/sun.awt=ALL-UNNAMED --add-opens java.desktop/sun.awt.windows=ALL-UNNAMED --add-opens java.desktop/sun.java2d=ALL-UNNAMED"

    #D_ARGS="-Djocl.debug.DebugCL -Djocl.debug.TraceCL"

    #D_ARGS="-Djogamp.debug=all"
    #D_ARGS="-Dnativewindow.debug=all"
    #D_ARGS="-Djogl.debug=all"
    #D_ARGS="-Dnewt.debug=all"
    #D_ARGS="-Djocl.debug=all"
    #D_ARGS="-Djogamp.debug=all -Djocl.debug=all"
    #D_ARGS="-Djogamp.debug.ProcAddressHelper -Djogamp.debug.NativeLibrary -Djogamp.debug.NativeLibrary.Lookup -Djogamp.debug.JNILibLoader -Djogamp.debug.TempJarCache -Djogamp.debug.JarUtil -Djocl.debug=all"
    #D_ARGS="-Djogamp.debug.ProcAddressHelper -Djogamp.debug.NativeLibrary -Djogamp.debug.NativeLibrary.Lookup -Djogamp.debug.JNILibLoader"

    #X_ARGS="-Dsun.java2d.noddraw=true -Dsun.awt.noerasebackground=true"
    #X_ARGS="-verbose:jni"
    #X_ARGS="-Xrs"

    if [ $awton -eq 1 ] ; then
        export CLASSPATH=$JOGAMP_ALL_CLASSPATH
        echo CLASSPATH $CLASSPATH
        X_ARGS="-Djava.awt.headless=false $X_ARGS"
    else
        export CLASSPATH=$JOGAMP_ALL_CLASSPATH
        X_ARGS="-Djava.awt.headless=true $X_ARGS"
    fi
    if [ ! -z "$CUSTOM_CLASSPATH" ] ; then
        export CLASSPATH=$CUSTOM_CLASSPATH:$CLASSPATH
    fi
    echo CLASSPATH $CLASSPATH
    if [ $MOSX_MT -eq 1 ] ; then
        if [ $awton -eq 0 ] ; then
            # No AWT, No SWT -> Preserve Main-Thread
            X_ARGS="-XstartOnFirstThread $X_ARGS"
            C_ARG="com.jogamp.newt.util.MainThread"
        fi
    fi
    echo
    echo "Test Start: $*"
    echo
    echo "$javaexe" $javaxargs $MODULE_ARGS $X_ARGS $D_ARGS $C_ARG $*
    #gdb --args "$javaexe" $javaxargs $MODULE_ARGS $X_ARGS $D_ARGS $C_ARG $*
    "$javaexe" $javaxargs $MODULE_ARGS $X_ARGS $D_ARGS $C_ARG $*
    echo
    echo "Test End: $*"
    echo
}

function testnoawt() {
    jrun 0 $* 2>&1 | tee -a java-run.log
}

function testawt() {
    MOSX_MT=0
    jrun 1 $* 2>&1 | tee -a java-run.log
}

#
# Version
#
#testnoawt com.jogamp.opencl.JoclVersion $*

#
#
#testnoawt com.jogamp.opencl.CLBufferTest $*
#testnoawt com.jogamp.opencl.CLCommandQueueTest $*
#testnoawt com.jogamp.opencl.CLExceptionTest $*
#testnoawt com.jogamp.opencl.CLImageTest $*
#testnoawt com.jogamp.opencl.CLProgramTest $*
testnoawt com.jogamp.opencl.HighLevelBindingTest $*
#testnoawt com.jogamp.opencl.LowLevelBindingTest $*
#testnoawt com.jogamp.opencl.TestJoclVersion $*
#testnoawt com.jogamp.opencl.TestUtils $*
#testnoawt com.jogamp.opencl.gl.CLGLTest $*
#testnoawt com.jogamp.opencl.util.concurrent.CLMultiContextTest $*


