#! /bin/sh

SDIR=`dirname $0` 

if [ -e /usr/local/etc/profile.ant ] ; then
    . /usr/local/etc/profile.ant
fi

JAVA_HOME=`/usr/libexec/java_home -version 21`
#JAVA_HOME=`/usr/libexec/java_home -version 1.8`
PATH=$JAVA_HOME/bin:$PATH
export JAVA_HOME PATH

#    -Dc.compiler.debug=true 

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

ant  \
    -Drootrel.build=build-macosx \
    $* 2>&1 | tee make.jocl.all.macosx.log
