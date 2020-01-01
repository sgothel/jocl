#! /bin/sh

SDIR=`dirname $0` 

$SDIR/make.jocl.all.macosx.sh && \
$SDIR/make.jocl.all.ios.amd64.sh && \
$SDIR/make.jocl.all.ios.arm64.sh

# $SDIR/make.jocl.all.macosx.sh
# $SDIR/make.jocl.all.ios.amd64.sh
# $SDIR/make.jocl.all.ios.arm64.sh
# $SDIR/make.jocl.all.win32.bat
# $SDIR/make.jocl.all.win64.bat
# $SDIR/make.jocl.all.linux-ppc64le.sh
# $SDIR/make.jocl.all.linux-armv6hf.sh
# $SDIR/make.jocl.all.linux-aarch64.sh
