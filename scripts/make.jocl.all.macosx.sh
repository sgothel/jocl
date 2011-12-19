#! /bin/sh

SDIR=`dirname $0` 

if [ -e /opt-share/etc/profile.ant ] ; then
    . /opt-share/etc/profile.ant
fi

ant  \
    -Drootrel.build=build-macosx \
    $* 2>&1 | tee make.jocl.all.macosx.log
