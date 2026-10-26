# XiNyE WebRTC Guard V5 — Adaptive Compatibility Audit

V5 is an **adaptive compatibility and regression detection build**, not proof of a Chrome browser WebRTC fix.

## Preserved
- WebView `GuardHook.java` and `GuardPolicy.java`: byte-for-byte identical to successful V1 baseline.
- Same Android application ID, versionCode 5.
- LSPosed recommended scopes: `com.android.webview`, `com.android.chrome`.
- KernelSU V3 and singbox4root unchanged.

## Chrome
V4 logs show LSPosed successfully loaded module in Chrome but all exact source class names were absent in Chrome 155. This is compatible with official Chromium release R8 shrinking/obfuscation.

V5 adds:
1. On-device, read-only scan of installed Chrome APK DEX *code*, including installed splits.
2. Structural method fingerprints without relying only on one exact Java class name.
3. Store an audited Chrome version/update fingerprint and invalidate it when Chrome changes.
4. Optional Chrome update notice if Android permits receiving package replacement broadcasts and notification permission.
5. One-click **Export compatibility TXT** without root/su, fixing V4's IOException diagnostic-export path.
6. Explicit **Chrome protection UNVERIFIED** status rather than success based on module loading.

The existing Chrome V3 experimental callback injection stays as in V4. Source-class
fingerprint hints are *not* automatically hooked: blindly hooking obfuscated
classes without a verified semantic match would risk breaking Chrome.
Chrome 155 is NOT known to be protected. This V5 release helps obtain version-
specific mapping for a later safe hook implementation. No automatic replacement
for Blink/V8 JS engine integration exists in the current public Java API.

## Test
1. Install; if signing mismatch, uninstall old APK first.
2. Enable both scopes, reboot, confirm frozen System WebView test remains PASS.
3. Open XiNyE V5: automatic scanner audits Chrome only when version changed.
4. Tap Export compatibility TXT. It contains code-signature metadata only.
5. Open Chrome and test ipleak.net. If local 192.168.x.x remains, Chrome is not yet fixed.
6. Upload the TXT and optional scoped ADB logs for further analysis.

For Chrome logcat without needing su:
`adb logcat -b all -d -v threadtime | findstr /I /C:"XiNyEWebRTC" /C:"XiNyE-WebRTC-Guard" > XiNyE-V5-ADB.txt`

## Privacy
No Chrome cookies, passwords, profiles or browser history are inspected. The
scanner reads APK code and version metadata. No network requests are made.
Do not confuse any reported structural class candidates with proven WebRTC
candidate suppression.
