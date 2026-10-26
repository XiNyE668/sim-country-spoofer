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
    private static final String CONTENTS_KEY = "xinye_rtc_webcontents";
    private static final AtomicBoolean installed = new AtomicBoolean(false);
    private static final String SCRIPT = GuardPolicy.DOCUMENT_START_SCRIPT
        + ";typeof window.RTCPeerConnection==='undefined'?'BLOCKED':'STILL_AVAILABLE'";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam p) {
        if (!CHROME.equals(p.packageName)) return;
        final String process = p.processName == null ? CHROME : p.processName;
        DiagnosticLog.write("LOAD_PACKAGE process=" + process + " pid=" + android.os.Process.myPid()
            + " loader=" + (p.classLoader == null ? "null" : p.classLoader.getClass().getName()));
        try {
            if (install(p.classLoader, process)) return;
            // Some Chrome variants attach a secondary split loader after
            // LSPosed invokes handleLoadPackage.
            XposedBridge.hookAllMethods(Application.class, "attach", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (installed.get()) return;
                    try {
                        Context context = (Context) param.args[0];
                        DiagnosticLog.write("APPLICATION_ATTACH process=" + process
                            + " loader=" + context.getClassLoader().getClass().getName());
                        install(context.getClassLoader(), process);
                    } catch (Throwable e) {
                        DiagnosticLog.write(TAG + "attach fallback error: " + e);
                    }
                }
            });
            DiagnosticLog.write(TAG + process +
                ": Chromium observer unavailable at load; attach fallback armed");
        } catch (Throwable e) {
            DiagnosticLog.write(TAG + process + ": setup failed: " + e);
        }
    }

    private static boolean install(final ClassLoader loader, final String process) {
        if (installed.get()) return true;
        try {
            Class<?> proxyClass = XposedHelpers.findClassIfExists(PROXY_CLASS, loader);
            DiagnosticLog.write("CLASS_PROBE observer=" + (proxyClass == null ? "ABSENT" : "FOUND")
                + " process=" + process);
            Class<?> contentProbe = XposedHelpers.findClassIfExists(
                "org.chromium.content.browser.webcontents.WebContentsImpl", loader);
            DiagnosticLog.write("CLASS_PROBE webcontents=" + (contentProbe == null ? "ABSENT" : "FOUND")
                + " process=" + process);
            Class<?> callbackProbe = XposedHelpers.findClassIfExists(CALLBACK_CLASS, loader);
            DiagnosticLog.write("CLASS_PROBE callback=" + (callbackProbe == null ? "ABSENT" : "FOUND")
                + " process=" + process);
            if (proxyClass == null) {
                DiagnosticLog.write(TAG + process + ": observer class not found (may be obfuscated)");
                return false;
            }
            synchronized (ChromeGuardHook.class) {
                if (installed.get()) return true;
                // Modern WebContentsObserverProxy() calls the no-arg
                // WebContentsObserver base constructor, so its inherited
                // getWebContents() returns null. Preserve constructor argument.
                XposedBridge.hookAllConstructors(proxyClass, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        if (p.args != null && p.args.length > 0 && p.args[0] != null) {
                            XposedHelpers.setAdditionalInstanceField(
                                p.thisObject, CONTENTS_KEY, p.args[0]);
                            DiagnosticLog.write("OBSERVER_INSTANCE_ASSOCIATED");
                        } else {
                            DiagnosticLog.write("OBSERVER_CONSTRUCTOR_NO_CONTENTS");
                        }
                    }
                });
                // Secondary association for already-created proxy instances.
                Class<?> contentsClass = XposedHelpers.findClassIfExists(
                    "org.chromium.content.browser.webcontents.WebContentsImpl", loader);
                if (contentsClass != null) {
                    XposedBridge.hookAllMethods(contentsClass, "addObserver", new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam p) {
                            try {
                                Object proxy = XposedHelpers.getObjectField(
                                    p.thisObject, "mObserverProxy");
                                if (proxy != null) {
                                    XposedHelpers.setAdditionalInstanceField(
                                        proxy, CONTENTS_KEY, p.thisObject);
                                }
                            } catch (Throwable ignored) { }
                        }
                    });
                }
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
                    DiagnosticLog.write(TAG + process + ": no supported observer callbacks");
                    return false;
                }
                installed.set(true);
                DiagnosticLog.write(TAG + process + ": observer callbacks installed=" + callbacks);
                return true;
            }
        } catch (Throwable e) {
            DiagnosticLog.write(TAG + process + ": install error: " + e);
            return false;
        }
    }

    private static int installMethod(Class<?> observer, String method,
                                     final String phase, final ClassLoader loader) {
        try {
            return XposedBridge.hookAllMethods(observer, method, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        DiagnosticLog.write("LIFECYCLE_CALLBACK phase=" + phase);
                        Object contents = XposedHelpers.getAdditionalInstanceField(
                            param.thisObject, CONTENTS_KEY);
                        if (contents == null) {
                            contents = XposedHelpers.callMethod(param.thisObject,
                                "getWebContents");
                        }
                        if (contents == null) {
                            DiagnosticLog.write(TAG + phase +
                                ": WebContents unavailable, cannot inject");
                            return;
                        }
                        evaluate(contents, loader, phase);
                    } catch (Throwable e) {
                        DiagnosticLog.write(TAG + phase + ": invocation failed: " + e);
                    }
                }
            }).size();
        } catch (Throwable e) {
            DiagnosticLog.write(TAG + "observer callback missing " + method + ": " + e);
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
                                DiagnosticLog.write("JS_RESULT phase=" + currentPhase
                                    + " state=" + (result.contains("BLOCKED") ? "BLOCKED"
                                        : result.contains("STILL_AVAILABLE") ? "STILL_AVAILABLE" : "UNKNOWN"));
                            }
                            if ("toString".equals(method.getName())) return "XiNyEChromeCallback";
                            if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                            if ("equals".equals(method.getName()))
                                return args != null && args.length > 0 && proxy == args[0];
                            return null;
                        }
                    });
            }
            DiagnosticLog.write("JS_EVALUATE_SUBMIT phase=" + phase
                + " callbackClass=" + (callbackClass == null ? "MISSING" : "PRESENT"));
            XposedHelpers.callMethod(contents, "evaluateJavaScript", SCRIPT, callback);
            // Submitted != effective. The callback result is the actual JS check.
        } catch (Throwable e) {
            DiagnosticLog.write(TAG + phase + ": JS evaluate failed: " + e);
        }
    }
}
