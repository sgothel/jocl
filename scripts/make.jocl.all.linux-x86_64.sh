#! /bin/sh

SDIR=`dirname $0` 

if [ -e $SDIR/../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh ] ; then
    . $SDIR/../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh
fi

#    -Dtarget.sourcelevel=1.6 \
#    -Dtarget.targetlevel=1.6 \
#    -Dtarget.rt.jar=/opt-share/jre1.6.0_30/lib/rt.jar \

ant  \
    -Dtarget.sourcelevel=1.6 \
    -Dtarget.targetlevel=1.6 \
    -Dtarget.rt.jar=/opt-share/jre1.6.0_30/lib/rt.jar \
    -Drootrel.build=build-x86_64 \
    $* 2>&1 | tee make.jocl.all.linux-x86_64.log
