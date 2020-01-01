#! /bin/sh

SDIR=`dirname $0` 

$SDIR/make.jocl.all.android-aarch64-cross.sh && \
$SDIR/make.jocl.all.android-armv6-cross.sh && \
$SDIR/make.jocl.all.android-x86-cross.sh && \
$SDIR/make.jocl.all.linux-aarch64-cross.sh && \
$SDIR/make.jocl.all.linux-armv6hf-cross.sh && \
$SDIR/make.jocl.all.linux-x86.sh && \
$SDIR/make.jocl.all.linux-x86_64.sh

# $SDIR/make.jocl.all.macosx.sh
# $SDIR/make.jocl.all.ios.amd64.sh
# $SDIR/make.jocl.all.ios.arm64.sh
# $SDIR/make.jocl.all.win32.bat
# $SDIR/make.jocl.all.win64.bat
# $SDIR/make.jocl.all.linux-armv6hf.sh
# $SDIR/make.jocl.all.linux-aarch64.sh

