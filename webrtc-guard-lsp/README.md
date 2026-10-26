# XiNyE-WebRTC-Guard-V3 Safe Regression Fix

This is an installable safety rollback, NOT a full Chrome WebRTC disable solution.
The Chrome V2 experimental `force-webrtc-ip-handling-policy` injection is disabled
because the user observed the real ISP public IP in Chrome WebRTC after V2.
The injection may or may not have caused it; causality is not established.

### Frozen
- GuardHook.java, GuardPolicy.java byte-identical to V1.
- No changes to KernelSU V3, singbox4root, DNS, iptables, WebView API blocking.
- Same Android application ID; versionCode 3.

### Recommended LSPosed scopes
- com.android.webview — user-tested V1 WebView configuration restored.
- com.android.chrome — **diagnostic-only**; no Chrome WebRTC prevention.
Select WebView host apps separately if needed; WebView provider scope alone
does not guarantee hooks run in every WebView-using app.
The module's own application ID is not a selectable scope on some LSPosed versions.

### Security
DO NOT infer Chrome safety from the module's enabled state. A website may
still report the ISP IP, including via WebRTC TCP/TURN or VPN bypass.
For WebRTC ISP-IP leak use a proven browser-side protection option and
check the singbox4root/KSU routing configuration.
Chrome V2 command-line experiments are removed rather than widened.

### Upgrade / rollback
If an Android debug key differs, uninstall the old APK first then install V3.
This loses Android app settings for XiNyE app but does not modify KSU.
Re-enable LSPosed module and scopes, reboot, test WebView and Chrome separately.
