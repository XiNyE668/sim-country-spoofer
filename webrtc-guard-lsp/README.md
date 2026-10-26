XiNyE WebRTC Guard V1 — LSPosed Android WebView module.
Set LSPosed scope to the module app itself and desired WebView host apps. After restarting target app, use in-app local constructor test and ipleak.net.
Only compatible Android WebView is affected. Chrome, Firefox, native libwebrtc, VPN/ISP leaks are not universally controlled. Requires Document Start Script support. Turn off LSPosed scope to restore WebRTC calling.
Build with Gradle 8.11.1 + SDK35 + AGP 8.9.2; GitHub Actions builds debug-signed APK.