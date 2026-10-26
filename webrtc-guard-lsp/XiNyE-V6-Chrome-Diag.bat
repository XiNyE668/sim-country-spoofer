@echo off
setlocal
chcp 65001 >nul
title XiNyE WebRTC V6 Chrome Runtime Capture
cd /d "%~dp0"
where adb >nul 2>nul
if errorlevel 1 (
 echo ERROR: adb not found. Put this BAT in your platform-tools directory.
 pause
 exit /b 1
)
adb devices
echo.
echo Make sure your phone is connected and authorized for USB debugging.
echo This capture reads only the XiNyEWebRTC Android log tag.
pause
echo Clearing old logcat entries for an accurate V6 test...
adb logcat -c
adb shell am force-stop com.android.chrome
echo.
echo Now open Chrome on your phone and visit https://ipleak.net/
echo Run WebRTC Detection, then return here.
pause
set "LOG=XiNyE-WebRTC-V6-Chrome-ADB-Diag.txt"
adb logcat -b all -d -v threadtime -s XiNyEWebRTC:I > "%LOG%"
echo Created: %LOG%
echo Upload this TXT with the Chrome WebRTC detection screenshot.
pause
