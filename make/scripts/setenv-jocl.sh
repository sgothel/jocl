#! /bin/sh

function print_usage() {
    echo "Usage: $0 jogl-build-dir"
}

if [ -z "$1" ] ; then
    echo JOCL BUILD DIR missing
    print_usage
    exit
fi

if [ -e /opt-share/etc/profile.ant ] ; then
    . /opt-share/etc/profile.ant
fi

JOCL_BUILDDIR="$1"
shift

THISDIR=`pwd`

if [ -e "$JOCL_BUILDDIR" ] ; then
    JOCL_DIR="$JOCL_BUILDDIR/.."
    JOCL_BUILDDIR_BASE=`basename "$JOCL_BUILDDIR"`
else
    echo JOCL_BUILDDIR "$JOCL_BUILDDIR" not exist or not given
    print_usage
    exit
fi

gpf=`find ../../gluegen/make -name jogamp-env.xml`
if [ -z "$gpf" ] ; then
    gpf=`find .. -name jogamp-env.xml`
fi
if [ -z "$gpf" ] ; then
    echo GLUEGEN_BUILDDIR not found
    print_usage
    exit
fi

GLUEGEN_DIR=`dirname $gpf`/..
GLUEGEN_BUILDDIR="$GLUEGEN_DIR"/"$JOCL_BUILDDIR_BASE"
if [ ! -e "$GLUEGEN_BUILDDIR" ] ; then
    echo GLUEGEN_BUILDDIR "$GLUEGEN_BUILDDIR" does not exist
    print_usage
    exit
fi
GLUEGEN_JAR="$GLUEGEN_BUILDDIR"/gluegen-rt.jar
GLUEGEN_OS="$GLUEGEN_BUILDDIR"/obj
JUNIT_JAR="$GLUEGEN_DIR"/make/lib/junit.jar

joalpf=`find ../../joal -name joal.iml`
if [ -z "$joalpf" ] ; then
    joalpf=`find .. -name joal.iml`
fi
if [ -z "$joalpf" ] ; then
    echo JOAL_BUILDDIR not found
    print_usage
    exit
fi
JOAL_DIR=`dirname $joalpf`
JOAL_BUILDDIR="$JOAL_DIR"/"$JOCL_BUILDDIR_BASE"
if [ ! -e "$JOAL_BUILDDIR" ] ; then
    echo JOAL_BUILDDIR "$JOAL_BUILDDIR" does not exist
    print_usage
    exit
fi
JOAL_JAR="$JOAL_BUILDDIR"/jar/joal.jar

joglpf=`find ../../jogl -name jogl.iml`
if [ -z "$joglpf" ] ; then
    joglpf=`find .. -name jogl.iml`
fi
if [ -z "$joglpf" ] ; then
    echo JOGL_BUILDDIR not found
    print_usage
    exit
fi
JOGL_DIR=`dirname $joglpf`
JOGL_BUILDDIR="$JOGL_DIR"/"$JOCL_BUILDDIR_BASE"
if [ ! -e "$JOGL_BUILDDIR" ] ; then
    echo JOGL_BUILDDIR "$JOGL_BUILDDIR" does not exist
    print_usage
    exit
fi
JOGL_JAR="$JOGL_BUILDDIR"/jar/jogl-all.jar

if [ -z "$ANT_PATH" ] ; then
    ANT_PATH=$(dirname $(dirname $(which ant)))
    if [ -e $ANT_PATH/lib/ant.jar ] ; then
        export ANT_PATH
        echo autosetting ANT_PATH to $ANT_PATH
    fi
fi
if [ -z "$ANT_PATH" ] ; then
    echo ANT_PATH does not exist, set it
    print_usage
    exit
fi
ANT_JARS=$ANT_PATH/lib/ant.jar:$ANT_PATH/lib/ant-junit.jar

JOCL_JAR="$JOCL_BUILDDIR"/jar/jocl.jar

echo GLUEGEN BUILDDIR: "$GLUEGEN_BUILDDIR"
echo JOAL BUILDDIR: "$JOAL_BUILDDIR"
echo JOGL BUILDDIR: "$JOGL_BUILDDIR"
echo JOCL DIR: "$JOCL_DIR"
echo JOCL BUILDDIR: "$JOCL_BUILDDIR"
echo JOCL BUILDDIR BASE: "$JOCL_BUILDDIR_BASE"

J2RE_HOME=$(dirname $(dirname $(which java)))
JAVA_HOME=$(dirname $(dirname $(which javac)))
CP_SEP=:

JOGAMP_ALL_CLASSPATH=.:"$GLUEGEN_JAR":"$JOAL_JAR":"$JOGL_JAR":"$JOCL_JAR":"$JUNIT_JAR":"$ANT_JARS"
CLASSPATH="$JOGAMP_ALL_CLASSPATH"
export JOGAMP_ALL_CLASSPATH CLASSPATH

# We use TempJarCache per default now!
#export LD_LIBRARY_PATH="$LD_LIBRARY_PATH":"$GLUEGEN_OS":"$JOGL_LIB_DIR"
#export DYLD_LIBRARY_PATH="$DYLD_LIBRARY_PATH":"$GLUEGEN_OS:"$JOGL_LIB_DIR"

echo JOGAMP_ALL_CLASSPATH: "$JOGAMP_ALL_CLASSPATH"
echo CLASSPATH: "$CLASSPATH"
echo
echo MacOSX REMEMBER to add the JVM arguments "-XstartOnFirstThread -Djava.awt.headless=true" for running demos without AWT, e.g. NEWT
echo MacOSX REMEMBER to add the JVM arguments "-XstartOnFirstThread -Djava.awt.headless=true com.jogamp.newt.util.MainThread" for running demos with NEWT

PATH=$J2RE_HOME/bin:$JAVA_HOME/bin:$PATH
export PATH

