#!/bin/bash

echo "Pictures"
cat pictures.txt

echo "Logcat"
adb logcat -d

# Sync the emulator and kill it to get rid of lock files
adb shell sync
adb emu kill

# Delete lock files
find ".android/avd" -name '*.lock' -delete
