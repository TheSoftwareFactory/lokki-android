#!/bin/bash

echo -e "\nPictures"
cat "pictures.txt"

echo -e "\nLogcat"
adb logcat -d
