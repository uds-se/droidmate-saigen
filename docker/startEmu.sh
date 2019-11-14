#!/usr/bin/env bash
${ANDROID_EMULATOR} -avd emu -no-audio -no-window -no-snapshot &

echo "Sleeping 20 seconds for the emulator to start"
sleep 20
