#! /bin/sh

SDIR=$(readlink -f `dirname $0`)

if [ -e ${SDIR}/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh ] ; then
    . ${SDIR}/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh
fi

LOGF=make.jocl.all.android-x86-cross.log
rm -f ${LOGF}

export ANDROID_HOME=/opt-linux-x86_64/android-sdk-linux_x86_64
export ANDROID_API_LEVEL=24
export ANDROID_HOST_TAG=linux-x86_64
export ANDROID_ABI=x86

if [ -e ${SDIR}/../../../gluegen/make/scripts/setenv-android-tools.sh ] ; then
    . ${SDIR}/../../../gluegen/make/scripts/setenv-android-tools.sh >> $LOGF 2>&1
else
    echo "${SDIR}/../../../setenv-android-tools.sh doesn't exist!" 2>&1 | tee -a ${LOGF}
    exit 1
fi

export GLUEGEN_CPPTASKS_FILE=${SDIR}/../../../gluegen/make/lib/gluegen-cpptasks-android-x86.xml
export PATH_VANILLA=$PATH
export PATH=${ANDROID_TOOLCHAIN_ROOT}/${ANDROID_TOOLCHAIN_NAME}/bin:${ANDROID_TOOLCHAIN_ROOT}/bin:${ANDROID_HOME}/platform-tools:${ANDROID_BUILDTOOLS_ROOT}:${PATH}
echo PATH ${PATH} 2>&1 | tee -a ${LOGF}
echo clang `which clang` 2>&1 | tee -a ${LOGF}

export SOURCE_LEVEL=1.8
export TARGET_LEVEL=1.8
export TARGET_RT_JAR=/opt-share/jre1.8.0_212/lib/rt.jar

#export JUNIT_DISABLED="true"
#export JUNIT_RUN_ARG0="-Dnewt.test.Screen.disableScreenMode"

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

# BUILD_ARCHIVE=true \
ant \
    -Drootrel.build=build-android-x86 \
    -Dgcc.compat.compiler=clang \
    $* 2>&1 | tee -a ${LOGF}

