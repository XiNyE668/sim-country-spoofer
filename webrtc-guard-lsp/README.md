# XiNyE WebRTC Guard V6 — Runtime ClassLoader and Signature Hook (EXPERIMENTAL)

## Changelog
- V1 WebView `GuardHook.java` and `GuardPolicy.java` remain BYTE-IDENTICAL, verified in CI.
- V5 scan showed WebContentsObserverProxy and WebContentsImpl in the Chrome 155 APK DEX;
  V4 logs showed absent from the *base* PathClassLoader.
- V6 probes Android Chrome's isolated "chrome" split loader and hooks when known
  WebContents classes become available from ClassLoader.loadClass / BaseDexClassLoader.findClass.
- Captures the actual WebContents instance via observer constructor; also uses
  WebContentsImpl.addObserver when available.
- Resolves JavaScript method by unique signature void(String, interface) rather
  than the obfuscated method name; fail-closed on 0 or ambiguous candidates.
- Observer lifecycle callbacks trigger V1-compatible RTCPeerConnection-blocking
  script, and log whether a callback reports BLOCKED/STILL_AVAILABLE.
- No manipulation of Chromium command-line switches. No change to KernelSU V3
  STUN rules, VPN or singbox4root.

## Important limitations
- **UNVERIFIED ON CHROME 155** at delivery time.
- Java Chromium navigation callbacks are not guaranteed to precede the page's
  earliest scripts; subframes, workers and native WebRTC are not fully covered.
- Seeing "JS_CALLBACK state=BLOCKED" proves one JavaScript evaluation, not
  whole-page, all-frame, persistent protection.
- R8 can change method signatures across Chrome versions. V5's version-change
  monitor remains; no unconditional success state.
- If protection fails, Chrome still exposes local 192.168.x.x ICE candidates.
- No security guarantee without real-device ipleak.net verification.
- May impact websites using WebRTC calls, if injection succeeds.

## Recommended LSPosed scopes
`com.android.webview`, `com.android.chrome`.

## Test and diagnostics
1. Update APK, enable recommended LSPosed scopes, reboot. Do not touch KSU V3.
2. Confirm the V1 System WebView test is still PASS.
3. Launch Chrome and load https://ipleak.net/ for WebRTC detection.
4. Export Chrome code audit TXT in V6 App.
5. Capture Chrome runtime logs using supplied `XiNyE-V6-Chrome-Diag.bat` with USB debugging.
6. Upload Chrome runtime TXT and WebRTC result screenshot to identify active stage.

Key lines:
`PROBE origin=EXPLICIT_CHROME_SPLIT observer=FOUND`
`OBSERVER_INSTALL callback_count=...`
`JS_SUBMIT ... mapped_by_signature=YES`
`JS_CALLBACK state=BLOCKED`

Security: do not claim successful protection unless external ICE test says
"No WebRTC candidates reported" consistently.
