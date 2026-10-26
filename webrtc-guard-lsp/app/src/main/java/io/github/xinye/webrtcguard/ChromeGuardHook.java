package io.github.xinye.webrtcguard;

import android.app.Application;
import android.content.Context;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Android Chrome-only, best-effort injection into Chromium WebContents.
 *
 * Unlike V2 this hook NEVER manipulates Chromium command-line/IP policy.
 * It attempts script evaluation at Chromium navigation and main document
 * lifecycle callbacks in the browser process. JS injection is not equivalent
 * to document-start / Blink native patching and may miss early or subframe JS.
 */
public final class ChromeGuardHook implements IXposedHookLoadPackage {
    private static final String CHROME = "com.android.chrome";
    private static final String PROXY_CLASS =
        "org.chromium.content.browser.webcontents.WebContentsObserverProxy";
    private static final String CALLBACK_CLASS =
        "org.chromium.content_public.browser.JavaScriptCallback";
    private static final String TAG = "[XiNyE-WebRTC-Guard V3/Chrome] ";
    private static final AtomicBoolean installed = new AtomicBoolean(false);
    private static final String SCRIPT = GuardPolicy.DOCUMENT_START_SCRIPT
        + ";typeof window.RTCPeerConnection==='undefined'?'BLOCKED':'STILL_AVAILABLE'";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam p) {
        if (!CHROME.equals(p.packageName)) return;
        final String process = p.processName == null ? CHROME : p.processName;
        try {
            if (install(p.classLoader, process)) return;
            // Some Chrome variants attach a secondary split loader after
            // LSPosed invokes handleLoadPackage.
            XposedBridge.hookAllMethods(Application.class, "attach", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (installed.get()) return;
                    try {
                        Context context = (Context) param.args[0];
                        install(context.getClassLoader(), process);
                    } catch (Throwable e) {
                        XposedBridge.log(TAG + "attach fallback error: " + e);
                    }
                }
            });
            XposedBridge.log(TAG + process +
                ": Chromium observer unavailable at load; attach fallback armed");
        } catch (Throwable e) {
            XposedBridge.log(TAG + process + ": setup failed: " + e);
        }
    }

    private static boolean install(final ClassLoader loader, final String process) {
        if (installed.get()) return true;
        try {
            Class<?> proxyClass = XposedHelpers.findClassIfExists(PROXY_CLASS, loader);
            if (proxyClass == null) {
                XposedBridge.log(TAG + process + ": observer class not found (may be obfuscated)");
                return false;
            }
            synchronized (ChromeGuardHook.class) {
                if (installed.get()) return true;
                int callbacks = 0;
                callbacks += installMethod(proxyClass, "didFinishNavigationInPrimaryMainFrame",
                    "navigation-commit", loader);
                callbacks += installMethod(proxyClass, "primaryMainDocumentElementAvailable",
                    "document-element", loader);
                callbacks += installMethod(proxyClass, "didStartLoading",
                    "start-loading", loader);
                // The late callback is a fallback when the API is not available
                // when navigation commit is first dispatched.
                callbacks += installMethod(proxyClass, "didStopLoading",
                    "stop-loading-fallback", loader);
                if (callbacks == 0) {
                    XposedBridge.log(TAG + process + ": no supported observer callbacks");
                    return false;
                }
                installed.set(true);
                XposedBridge.log(TAG + process + ": observer callbacks installed=" + callbacks);
                return true;
            }
        } catch (Throwable e) {
            XposedBridge.log(TAG + process + ": install error: " + e);
            return false;
        }
    }

    private static int installMethod(Class<?> observer, String method,
                                     final String phase, final ClassLoader loader) {
        try {
            return XposedBridge.hookAllMethods(observer, method, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Object contents = XposedHelpers.callMethod(param.thisObject,
                            "getWebContents");
                        if (contents == null) return;
                        evaluate(contents, loader, phase);
                    } catch (Throwable e) {
                        XposedBridge.log(TAG + phase + ": invocation failed: " + e);
                    }
                }
            }).size();
        } catch (Throwable e) {
            XposedBridge.log(TAG + "observer callback missing " + method + ": " + e);
            return 0;
        }
    }

    private static void evaluate(Object contents, ClassLoader loader, String phase) {
        try {
            Class<?> callbackClass = XposedHelpers.findClassIfExists(CALLBACK_CLASS, loader);
            Object callback = null;
            if (callbackClass != null) {
                final String currentPhase = phase;
                callback = Proxy.newProxyInstance(callbackClass.getClassLoader(),
                    new Class<?>[] { callbackClass },
                    new InvocationHandler() {
                        @Override public Object invoke(Object proxy, Method method, Object[] args) {
                            if ("handleJavaScriptResult".equals(method.getName())) {
                                String result = args != null && args.length > 0 ?
                                    String.valueOf(args[0]) : "(no result)";
                                XposedBridge.log(TAG + currentPhase + ": JS check=" + result);
                            }
                            if ("toString".equals(method.getName())) return "XiNyEChromeCallback";
                            if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                            if ("equals".equals(method.getName()))
                                return args != null && args.length > 0 && proxy == args[0];
                            return null;
                        }
                    });
            }
            XposedHelpers.callMethod(contents, "evaluateJavaScript", SCRIPT, callback);
            // Submitted != effective. The callback result is the actual JS check.
        } catch (Throwable e) {
            XposedBridge.log(TAG + phase + ": JS evaluate failed: " + e);
        }
    }
}
