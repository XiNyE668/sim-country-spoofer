# XiNyE WebRTC Guard V3 — experimental Chrome

**WebView V1 frozen:** \`GuardHook.java\` and \`GuardPolicy.java\` are byte-identical to V1.

## LSPosed recommended scopes
- com.android.webview — user's confirmed WebView provider
- com.android.chrome — Chrome experimental injection
Do not select the module application's own package for scope.

## Chrome V3 design
The V2 Chromium command-line IP policy experiment is completely removed.
On compatible un-obfuscated Chromium Java WebContents implementations,
the module hooks lifecycle events and attempts to evaluate exactly the same
JavaScript PeerConnection-disabling expression used by frozen WebView V1.
This is a BEST-EFFORT injection. Java observer callbacks may occur after
early page scripts, so the result MUST NOT be treated as complete Chrome
protection. Other iframes/workers may still be able to use WebRTC. There is
no proven reliable document-start interface in Android Chrome LSPosed here.

Chrome scope diagnostics appear in LSPosed logs:
- observer class not found: no interception, Chrome class may be obfuscated
- observer callbacks installed: hook installed, but not yet proof of guarding
- JS check="BLOCKED": injection executed in an observed document
- JS check="STILL_AVAILABLE": injection did not disable PeerConnection
Always check ipleak.net after a full Chrome force-stop or phone reboot.
Only "No WebRTC candidates reported" plus passing network tests meets target.

No changes to iptables, KernelSU, singbox4root or VPN routing.
This may disable WebRTC calls in Chrome when interception works.

## Install/rollback
Use Android APK install; if signature mismatch uninstall V2 then install V3.
Enable module + recommended scopes in LSPosed, reboot, test System WebView first.
If Chrome IP is exposed, DO NOT treat module as safe; disable Chrome scope
while retaining WebView scope, and rely on a proven browser extension or
browser with built-in protection if available.
