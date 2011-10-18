#! /bin/sh

SDIR=`dirname $0` 

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh
fi

ant  \
    -Drootrel.build=build-solaris-x86_64 \
    $* 2>&1 | tee make.jocl.all.solaris-x86_64.log
