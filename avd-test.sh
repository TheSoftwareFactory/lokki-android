#!/bin/bash
set -e
if [ ! -d "$HOME/.android/avd/$NAME.avd" ] || [ ! -f "$HOME/.android/avd/$NAME.ini" ]; then 
  echo "no" | android create avd --force -n "$NAME" -t "$TARGET" --abi armeabi-v7a
  emulator -avd "$NAME" -no-audio -no-window -no-boot-anim &
  android-wait-for-emulator
  adb shell mount 
  exit 1
  adb shell sqlite3 '/data/data/com.android.providers.settings/databases/settings.db' 'INSERT INTO global (name, value) VALUES ('animator_duration_scale', '0.0'), ('window_animation_scale', '0.0'), ('transition_animation_scale', '0.0');'
  adb shell reboot -p
else
  echo "Using AVD cache"
fi
emulator -avd "$NAME" -no-audio -no-window -no-boot-anim &
android-wait-for-emulator
adb shell input keyevent 82 &
adb shell sqlite3 '/data/data/com.android.providers.settings/databases/settings.db' '.dump'
