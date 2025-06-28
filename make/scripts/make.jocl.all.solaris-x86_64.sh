#! /bin/sh

SDIR=`dirname $0` 

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh
fi

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

ant  \
    -Drootrel.build=build-solaris-x86_64 \
    $* 2>&1 | tee make.jocl.all.solaris-x86_64.log
