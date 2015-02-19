#!/bin/bash
FILE="screen.png"
while true; do
  adb shell screencap -p | perl -pe 's/\x0D\x0A/\x0A/g' > "$FILE"
  echo -e $(./travis-upload.sh "$FILE")"\n"
  rm -f "$FILE"
  sleep 60
done
