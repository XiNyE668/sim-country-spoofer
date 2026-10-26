@echo off
setlocal
chcp 65001 >nul
title XiNyE WebRTC Guard V7 Chrome Runtime
cd /d "%~dp0"
where adb >nul 2>nul
if errorlevel 1 (
 echo ERROR: ADB not found. Put this BAT with adb.exe in platform-tools.
 pause
 exit /b 1
)
adb devices
echo.
echo Confirm phone unlocked and USB debugging is authorized.
pause
echo Clearing old logcat and restarting only Chrome...
adb logcat -c
adb shell am force-stop com.android.chrome
echo.
echo On phone, open Chrome. Visit ipleak.net and wait for WebRTC Detection.
pause
adb logcat -b all -d -v threadtime -s XiNyEWebRTC:I > "XiNyE-WebRTC-V7-Chrome-Runtime.txt"
echo.
echo Saved XiNyE-WebRTC-V7-Chrome-Runtime.txt
echo Upload this TXT with XiNyE-WebRTC-V7-Chrome-Audit.txt.
pause
