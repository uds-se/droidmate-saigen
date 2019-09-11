FROM debian:stretch
# FROM debian:stretch-slim
# => couldn't use stretch-slim because of: `dpkg: dependency problems prevent configuration of ca-certificates-java`

# Based on a large extent on: 
# # https://github.com/sweisgerber-dev/android-sdk-ndk
# Helpful links:
# # https://hub.docker.com/r/thyrlian/android-sdk/

ENV SDK_TOOLS_LINUX_WEB_VERSION="3859397" \
	ANDROID_SDK_MAX="28" \
	ANDROID_SDK_MIN="23" \
	ANDROID_BUILD_TOOLS_LEGACY="26.0.2" \
	ANDROID_BUILD_TOOLS="28.0.3" \
	ANDROID_SDK_FOLDER="/android-sdk" \
	JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64"
# Define some environment variables, some programs require these
ENV ANDROID_HOME="${ANDROID_SDK_FOLDER}" \
	ANDROID_SDK_HOME="${ANDROID_SDK_FOLDER}" \
	ANDROID_SDK_ROOT="${ANDROID_SDK_FOLDER}" \
	ANDROID_SDK_MANAGER="${ANDROID_SDK_FOLDER}/tools/bin/sdkmanager" \
	ANDROID_AVD_MANAGER="${ANDROID_SDK_FOLDER}/tools/bin/avdmanager" \
	ANDROID_EMULATOR="${ANDROID_SDK_FOLDER}/emulator/emulator-headless"

# Debian Installation
RUN apt-get update --yes && \
	apt-get install --yes \
		apt-utils \
	        wget \
                curl \
	        tar \
                unzip \
	        lib32stdc++6 \
                lib32z1 \
	        openjdk-8-jdk \
                openjdk-8-jre && \
		apt-get install --yes python \
                git && \
	apt-get upgrade --yes && \
	apt-get dist-upgrade --yes && \
	apt-get update && \
	apt-get install -y gnuplot python3 python3-pip && \
# Install fuzzing book
    pip3 install fuzzingbook matplotlib numpy scipy pandas graphviz svglib selenium jupyter_contrib_nbextensions


# Setup Java
RUN update-alternatives --config java

RUN wget --quiet --output-document=android-sdk.zip https://dl.google.com/android/repository/sdk-tools-linux-${SDK_TOOLS_LINUX_WEB_VERSION}.zip && \
	mkdir -p ${HOME}/.android && \
	echo "count=0\n" > ${HOME}/.android/repositories.cfg && \
	mkdir -p ${ANDROID_SDK_FOLDER} && \
	unzip -d ${ANDROID_SDK_FOLDER} -qq android-sdk.zip && \
# Workaround for
# Warning: File /android-sdk/.android/repositories.cfg could not be loaded
	mkdir -p /android-sdk/.android && \
        touch /android-sdk/.android/repositories.cfg

# SDK Installation
RUN ${ANDROID_SDK_MANAGER} --list || true && \
	echo yes | ${ANDROID_SDK_MANAGER} "platform-tools" && \
	echo yes | ${ANDROID_SDK_MANAGER} "tools" && \
	echo yes | ${ANDROID_SDK_MANAGER} "build-tools;${ANDROID_BUILD_TOOLS}" && \
	echo yes | ${ANDROID_SDK_MANAGER} "build-tools;${ANDROID_BUILD_TOOLS_LEGACY}" && \
	echo yes | ${ANDROID_SDK_MANAGER} "platforms;android-${ANDROID_SDK_MIN}" && \
	echo yes | ${ANDROID_SDK_MANAGER} "platforms;android-${ANDROID_SDK_MAX}" && \
# Android 9 API 28
	echo yes | ${ANDROID_SDK_MANAGER} "system-images;android-28;google_apis;x86" && \
	echo yes | ${ANDROID_SDK_MANAGER} "extras;android;m2repository" && \
	echo yes | ${ANDROID_SDK_MANAGER} "extras;google;m2repository" && \
	echo yes | ${ANDROID_SDK_MANAGER} "extras;google;google_play_services" && \
	echo yes | ${ANDROID_SDK_MANAGER} "extras;google;instantapps" && \
	echo yes | ${ANDROID_SDK_MANAGER} "extras;google;market_apk_expansion" && \
	echo yes | ${ANDROID_SDK_MANAGER} "extras;google;market_licensing" && \
	echo yes | ${ANDROID_SDK_MANAGER} "extras;google;webdriver" && \
	echo yes | ${ANDROID_SDK_MANAGER} "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.2" && \
	echo yes | ${ANDROID_SDK_MANAGER} "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.2" && \
	echo yes | ${ANDROID_SDK_MANAGER} --licenses && \
# Setup emulators
# Create the platforms directory, otherwise the error appears when running the emulator: "PANIC: Broken AVD system path"
	echo no | ${ANDROID_AVD_MANAGER} create avd -n emu -k "system-images;android-28;google_apis;x86" -d  pixel -c 2048M

ENV PATH="$PATH:${ANDROID_HOME}:${ANDROID_HOME}/build-tools/${ANDROID_BUILD_TOOLS}/:${ANDROID_HOME}/build-tools/${ANDROID_BUILD_TOOLS_LEGACY}/:${ANDROID_HOME}/platform-tools/"
ENV PATH="$PATH:${ANDROID_HOME}/tools:${ANDROID_HOME}/tools/bin:${JAVA_HOME}"

# Set work dir
WORKDIR /test


CMD [ "bash" ]
