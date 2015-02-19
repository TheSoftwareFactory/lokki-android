#!/bin/bash
set -e
SETTINGSFILE="/data/data/com.android.providers.settings/databases/settings.db"
LOCKSETTINGSFILE="/data/system/locksettings.db"
function do_adb_magic() {
  echo "Doing ADB magic - this might take a while"
  emulator -avd "$NAME" -no-audio -no-window &
  android-wait-for-emulator
  echo "  Disabling animations"
  adb shell sqlite3 "$SETTINGSFILE" "INSERT INTO global (name, value) VALUES ('animator_duration_scale', '0.0'), ('transition_animation_scale', '0.0'), ('window_animation_scale', '0.0');"
  echo "  Disabling lock screen"
  adb shell sqlite3 "$LOCKSETTINGSFILE" "UPDATE locksettings SET value = '1' WHERE name = 'lockscreen.disabled';"
  # We want to sync so that changes persist after emu kill
  adb shell sync
  echo "Done"
  adb emu kill
}

# Check if there is no AVD in cache with same name
if [ ! -d "$HOME/.android/avd/$NAME.avd" ] || [ ! -f "$HOME/.android/avd/$NAME.ini" ]; then
  echo "no" | android create avd --force -n "$NAME" -t "$TARGET" --abi "$ABI"
  do_adb_magic
else
  echo "Using AVD from cache"
fi
emulator -avd "$NAME" -no-audio -no-window &
android-wait-for-emulator
# Just to see that animations are disabled
adb shell sqlite3 "$SETTINGSFILE" "SELECT * FROM global WHERE name LIKE '%_scale'"
# Lock screen setting view
adb shell sqlite3 "$LOCKSETTINGSFILE" "SELECT * FROM locksettings WHERE name = 'lockscreen.disabled'"
# Enable screenshots in travis (you need to setup server)
./adb-screenshot.sh > pictures.txt &
