#! /bin/sh

SDIR=`dirname $0` 

$SDIR/make.jocl.all.linux-armv6-cross.sh \
&& $SDIR/make.jocl.all.linux-armv6hf-cross.sh \
&& $SDIR/make.jocl.all.linux-x86_64.sh \
&& $SDIR/make.jocl.all.linux-x86.sh \
&& $SDIR/make.jocl.all.android-armv6-cross.sh \
