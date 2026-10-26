# XiNyE WebRTC Guard V4 Debug
- Intent: collect reproducible, low-sensitive debug telemetry from Chrome's experimental V3 Xposed hook.
- V1 frozen: `GuardHook.java` and `GuardPolicy.java` byte-identical to baseline.
- V3 injection still experimental: may run too late or not at all. Log events are not proof of protection.
- Scope recommendations: `com.android.webview` and `com.android.chrome`.
- Network layer untouched; no modification to singbox4root or KSU.
- Export uses Android document picker; after Chrome ipleak.net test use Export Chrome debug TXT.
- Grant KernelSU root prompt for READ-ONLY filtered logcat access; app does not alter network/firewall. If refused, use ADB fallback.
- Diagnostic captures only module-generated logcat tag `XiNyEWebRTC`. Does not intentionally log browsing URLs, addresses, phone IDs or credentials.
- If no Chrome LOAD_PACKAGE: LSPosed injection not triggered in Chrome or logcat overwritten.
- If CLASS_PROBE observer=ABSENT: Chromium class missing/obfuscated/classloader not ready.
- If CALLBACKS installed but no LIFECYCLE_CALLBACK: hook not invoked.
- If LIFECYCLE and JS_EVALUATE_SUBMIT but no JS_RESULT: evaluation or callback failure / teardown.
- If JS_RESULT state=BLOCKED and ipleak shows IP: likely injected too late/other frame/worker/uncovered path.
- If JS_RESULT state=STILL_AVAILABLE: script did not disable API.
- Android Chrome real IP bypass still possible; do not trust V4 for privacy until tested.

ADB backup:
`adb logcat -d -v threadtime -s XiNyEWebRTC:I > WebRTC-V4-Diag.txt`

If APK signature changed, uninstall previous package first, install V4, enable recommended scopes and reboot. Test frozen WebView first, then Chrome.
