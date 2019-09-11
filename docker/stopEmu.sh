#!/usr/bin/env bash

adb devices | grep "emulator-" | while read -r emulator device; do
  adb -s $emulator emu kill
done
