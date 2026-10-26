# XiNyE-WebRTC-Guard V2 (LSPosed 2.2)

## Scope recommendations (two entries)
- io.github.xinye.webrtcguard — V1-compatible WebView self-test.
- com.android.chrome — experimental browser-only IP-policy startup hook.

## Frozen WebView V1
GuardHook.java and GuardPolicy.java are intentionally UNCHANGED.
WebView DocumentStartScript protection is exactly V1 behavior, including
limitations with independent Chromium/Chrome browsers and app calling.

## Chrome V2
ChromeGuardHook.java is isolated from the frozen WebView hook.
At startup the hook attempts to append:
--force-webrtc-ip-handling-policy=disable_non_proxied_udp

Chrome 156 exposes a Chromium IP-handling policy switch, but this does not
guarantee complete disablement of RTCPeerConnection or zero ICE host candidates.
Modern Chromium has removed the historic --disable-webrtc switch.
No native libwebrtc patching; no global iptables changes. WebRTC call sites may
fail if the IP policy takes effect.

## Tests
1. Uninstall V1 first ONLY if Android reports a different APK signature.
   The package ID stays io.github.xinye.webrtcguard and versionCode increases to 2.
2. Install APK and enable both suggested LSPosed scopes, self app and Chrome.
3. Force-stop the module app and Chrome, then relaunch (Chrome multi-process may require reboot).
4. In module app run V1 WebView test: PASS must be preserved.
5. Open Chrome > chrome://version, look for force-webrtc-ip-handling-policy.
6. Then compare ipleak.net candidates Chrome and normal internet access.
7. Inspect LSPosed log for [XiNyE-WebRTC-Guard V2/Chrome].
Do NOT claim Chrome is protected merely because the command-line hook installed.
Use your KSU V3 as before; Chrome-specific component must be verified separately.

## Rollback
Disable Chrome scope in LSPosed and force-stop Chrome.
The WebView V1 test remains available. Uninstall V2 / reinstall V1 if necessary.
