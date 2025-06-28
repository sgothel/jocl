#! /bin/sh

SDIR=`dirname $0`

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh
fi

LOGF=make.jocl.all.linux-x86_64.log
rm -f $LOGF

#    -Dbuild.archiveon=true \

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

# BUILD_ARCHIVE=true \
ant  \
    -Drootrel.build=build-x86_64 \
    $* 2>&1 | tee -a $LOGF

