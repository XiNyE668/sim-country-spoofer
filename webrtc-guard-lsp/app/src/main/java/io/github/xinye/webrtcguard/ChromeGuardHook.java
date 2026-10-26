package io.github.xinye.webrtcguard;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Chrome-only, best-effort Chromium command-line intervention.
 *
 * IMPORTANT: this is NOT an equivalent of V1 document-start WebView blocking.
 * Chromium M156 does not expose a supported full WebRTC kill switch in Android
 * Chrome. The known --force-webrtc-ip-handling-policy switch restricts
 * non-proxied UDP, but cannot promise "no ICE candidates".
 */
public final class ChromeGuardHook implements IXposedHookLoadPackage {
    private static final String TAG = "[XiNyE-WebRTC-Guard V2/Chrome] ";
    static final String CHROME = "com.android.chrome";
    static final String SWITCH = "force-webrtc-ip-handling-policy";
    static final String VALUE = "disable_non_proxied_udp";
    private static final Set<Object> PATCHED =
        Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<Object, Boolean>()));

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam p) {
        if (!CHROME.equals(p.packageName)) return;
        try {
            final Class<?> commandLine = XposedHelpers.findClassIfExists(
                "org.chromium.base.CommandLine", p.classLoader);
            if (commandLine == null) {
                XposedBridge.log(TAG + p.processName + ": Chromium CommandLine class unavailable; no patch");
                return;
            }
            try {
                XposedHelpers.findAndHookMethod(commandLine, "init", String[].class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String[] before = (String[]) param.args[0];
                            String[] after = ChromeArgs.withRestrictedIce(before);
                            if (after != before) {
                                param.args[0] = after;
                                XposedBridge.log(TAG + p.processName
                                    + ": early IP policy injected into command line");
                            } else {
                                XposedBridge.log(TAG + p.processName
                                    + ": matching policy already present");
                            }
                        }
                    });
                XposedBridge.log(TAG + p.processName + ": early CommandLine.init hook installed");
            } catch (Throwable e) {
                XposedBridge.log(TAG + p.processName + ": init hook unsupported: " + e);
            }

            // Additional best-effort path for different Chromium startup orders.
            // Only modifies an already initialized *Java* command line; avoids JNI
            // mutation after switching to native.
            try {
                XposedHelpers.findAndHookMethod(commandLine, "getInstance",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object instance = param.getResult();
                            if (instance == null || PATCHED.contains(instance)) return;
                            try {
                                boolean initialized = (Boolean)
                                    XposedHelpers.callStaticMethod(commandLine, "isInitialized");
                                if (!initialized) return;
                                boolean nativeMode = (Boolean)
                                    XposedHelpers.callStaticMethod(commandLine, "hasSwitchedToNative");
                                if (nativeMode) {
                                    PATCHED.add(instance);
                                    return;
                                }
                                boolean exists = (Boolean) XposedHelpers.callMethod(
                                    instance, "hasSwitch", SWITCH);
                                if (!exists) {
                                    PATCHED.add(instance);
                                    XposedHelpers.callMethod(instance, "appendSwitchWithValue",
                                        SWITCH, VALUE);
                                    XposedBridge.log(TAG + p.processName
                                        + ": pre-native policy appended on getInstance");
                                } else {
                                    PATCHED.add(instance);
                                }
                            } catch (Throwable e) {
                                XposedBridge.log(TAG + p.processName
                                    + ": post-init policy failed: " + e);
                            }
                        }
                    });
            } catch (Throwable e) {
                XposedBridge.log(TAG + p.processName + ": getInstance hook unavailable: " + e);
            }
        } catch (Throwable e) {
            XposedBridge.log(TAG + p.processName + ": Chrome hook setup failed: " + e);
        }
    }
}
