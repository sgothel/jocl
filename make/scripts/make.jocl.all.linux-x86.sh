#! /bin/sh

SDIR=`dirname $0` 

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86.sh
fi

LOGF=make.jocl.all.linux-x86.log
rm -f $LOGF

#    -Dgluegen-cpptasks.file=`pwd`/../../gluegen/make/lib/gluegen-cpptasks-linux-32bit.xml \
#

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

ant  \
    -Drootrel.build=build-x86 \
    -Dos.arch=x86 \
    $* 2>&1 | tee -a $LOGF
