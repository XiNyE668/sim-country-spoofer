# XiNyE WebRTC Guard V8 — Chrome association and callback mapping fix

## Findings from V7 real-device logs
- LSPosed loads into Chrome.
- Isolated "chrome" split ClassLoader FOUND both WebContents classes.
- All six Java WebContentsObserverProxy navigation callbacks are installed and fired.
- `WebContentsImpl.addObserver` method name was absent after production R8.
- Every callback returned `no_webcontents`; WebRTC script was never submitted.
- On-device DEX scan found exactly one void(String,reference) signature
  in WebContentsImpl, but V6/V7 used a stricter void(String,interface) filter.

## V8 code
1. Associates already-instantiated ObserverProxy with its WebContentsImpl by
   iterating the wrapper's existing static LongSparseArray/Map registry, matching
   the proxy by object identity. The object may have been created before the
   LSPosed constructor hooks were registered.
2. Retains all V6 split ClassLoader hooks and observer callback hooks.
3. Uses a unique non-static void(String,reference) JavaScript method signature.
   If none or several exist, does NOT attempt a potentially wrong call.
4. Handles obfuscated callback classes by passing a null callback (allowed for
   nullable WebContents evaluator), or uses a dynamic interface proxy if possible.
5. Logs `REGISTRY_ASSOCIATION`, `JS_SUBMIT`, `JS_METHOD_RETURN` and
   `JS_CALLBACK` so actual operation is observable.
6. Persistent APK audit V7 diagnostic fix retained.

## Hard limitations
- V8 has NOT been validated on the user's Chrome build at delivery.
- The Java Chromium observer lifecycle has no guaranteed document-start timing.
- Worker, nested-frame, and previously running ICE gathering may evade this
  approach even if JavaScript is injected after a callback.
- The only actual pass criterion is repeatable "No WebRTC candidates reported"
  at ipleak.net with no ISP IP or local candidate exposure. Logging JS_SUBMIT
  alone is NOT proof of browser-wide protection.
- If local IP persists, V8 Chrome guard is NOT considered working.

## Frozen
- WebView V1 GuardHook.java / GuardPolicy.java byte-identical.
- KernelSU V3 unchanged, singbox4root unchanged.
- Scope: com.android.webview;com.android.chrome.

## Instructions
Install V8, retain both LSPosed scopes, reboot.
Confirm WebView V1 PASS.
Run Chrome ipleak test.
Export V8 Chrome APK audit TXT from app.
Run XiNyE-V8-Chrome-Runtime.bat via ADB and submit the output TXT.
