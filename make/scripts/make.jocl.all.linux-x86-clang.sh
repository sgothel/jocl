#! /bin/sh

SDIR=`dirname $0` 

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86.sh
fi

LOGF=make.jocl.all.linux-x86-clang.log
rm -f $LOGF

export SOURCE_LEVEL=1.8
export TARGET_LEVEL=1.8
export TARGET_RT_JAR=/opt-share/jre1.8.0_212/lib/rt.jar

export GLUEGEN_PROPERTIES_FILE="../../gluegen/make/lib/gluegen-clang.properties"
# or -Dgcc.compat.compiler=clang

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

ant  \
    -Drootrel.build=build-x86-clang \
    $* 2>&1 | tee -a $LOGF
