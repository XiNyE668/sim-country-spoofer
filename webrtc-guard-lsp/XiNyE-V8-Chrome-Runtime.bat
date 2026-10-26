@echo off
setlocal
chcp 65001 >nul
title XiNyE WebRTC Guard V8 Chrome Runtime Capture
cd /d "%~dp0"
where adb >nul 2>nul
if errorlevel 1 (
 echo ERROR: ADB not found. Put this BAT in the platform-tools directory.
 pause
 exit /b 1
)
adb devices
echo.
echo Confirm USB debugging is authorized.
pause
adb logcat -c
adb shell am force-stop com.android.chrome
echo.
echo Open Chrome on your phone and test https://ipleak.net/
echo When the WebRTC test has completed, return here.
pause
adb logcat -b all -d -v threadtime -s XiNyEWebRTC:I > "XiNyE-WebRTC-V8-Chrome-Runtime.txt"
echo Saved XiNyE-WebRTC-V8-Chrome-Runtime.txt
echo Upload this TXT together with V8 APK audit and Chrome WebRTC test screenshot.
pause
