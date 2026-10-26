package io.github.xinye.webrtcguard;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Chrome-only safe diagnostics. Does not inject command-line flags,
 * modify networking, inject renderer JavaScript or claim ICE protection.
 * V2 attempted to force disable_non_proxied_udp; this experimental hook
 * is disabled after reports of real ISP IP candidates.
 */
public final class ChromeGuardHook implements IXposedHookLoadPackage {
  @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) {
    if ("com.android.chrome".equals(p.packageName)) {
      XposedBridge.log("[XiNyE-WebRTC-Guard V3] Chrome scope loaded: "
        + p.processName + "; V2 command-line hook DISABLED. "
        + "Chrome WebRTC is NOT protected by this module.");
    }
  }
}
