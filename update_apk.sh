#!/bin/bash
git add -f app/build/intermediates/apk/debug/app-debug.apk
git commit -m "Automatic update or app-debug.apk"
git push origin master
