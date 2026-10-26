# XiNyE WebRTC Guard V7 — Audit Export Fix

## Proven V6 issue
User exported a 12-byte TXT containing only `NO_SCAN_YET`.
V6 initialized its in-memory report to that placeholder on each activity
creation, but skipped scanning if Chrome's previously scanned identity was
unchanged. This made export produce a placeholder even when an earlier scan
had succeeded.

## V7 concrete repair
- Atomic-ish durable audit report persistence in app-private files, reloaded
  and identity-validated every time the app opens.
- A missing/stale cache triggers a new scan even when Chrome version is unchanged.
- Export while scan is running queues the document picker until scan finishes.
- Export when no cache exists starts scanning automatically.
- Every completed scan stores full error details if scanning fails: never export
  bare NO_SCAN_YET.
- Report includes elapsed time, Chrome identity comparison and explicit
  CHROME_WEBRTC_PROTECTION=UNVERIFIED.
- Chrome update version changes invalidate the cached report.

## Preserved unchanged
- V1 GuardHook.java & GuardPolicy.java are byte-identical to frozen baseline.
- V6 ChromeGuardHook.java is intentionally unchanged because user provided
  an APK audit placeholder, NOT runtime ADB hook logs. Unknown Chrome runtime
  problem must not be guessed at.
- LSPosed recommended scopes: com.android.webview;com.android.chrome.
- KSU V3 STUN guard and singbox4root unchanged.

## Testing & next evidence
1. Install V7 and enable both LSPosed scopes.
2. Test System WebView: PASS must continue.
3. In V7 App, tap "Re-scan Chrome APK code". Wait until completed.
4. Tap Export V7 Chrome audit TXT. Re-open app and repeat export to
   confirm the same report is correctly restored from persistent storage.
5. On a Windows PC with ADB, run supplied XiNyE-V7-Chrome-Runtime.bat.
   This captures **runtime** class-loader and callback logs after Chrome
   is force-stopped and re-opened; APK audit alone cannot identify Hook failure.
6. Upload both exported TXT and runtime ADB TXT plus ipleak WebRTC result.

Chrome privacy goal remains NOT VERIFIED. WebContents callbacks are not a
reliable document-start injection mechanism and may not cover all frames.
