package io.github.xinye.webrtcguard;
import android.webkit.WebView;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
public final class GuardHook implements IXposedHookLoadPackage {
 private static final String TAG="[XiNyE-WebRTC-Guard V1] ";
 private static final Set<WebView> INSTALLED=Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<WebView,Boolean>()));
 @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) {
  final String pkg=p.packageName;
  if(pkg==null || pkg.equals("android") || pkg.equals("system") || pkg.equals("com.android.webview") || pkg.equals("com.google.android.webview")) return;
  try{
    XposedBridge.hookAllConstructors(WebView.class,new XC_MethodHook(){
      @Override protected void afterHookedMethod(MethodHookParam x){
        WebView view=(WebView)x.thisObject;
        if(view==null || INSTALLED.contains(view)) return;
        try{
          if(!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)){
            XposedBridge.log(TAG+pkg+": document-start not supported");return;
          }
          WebViewCompat.addDocumentStartJavaScript(view,GuardPolicy.DOCUMENT_START_SCRIPT,Collections.singleton("*"));
          INSTALLED.add(view);
          XposedBridge.log(TAG+pkg+": guard installed");
        }catch(Throwable ex){XposedBridge.log(TAG+pkg+": installation failed: "+ex);}
      }
    });
    XposedBridge.log(TAG+pkg+": hook ready");
  }catch(Throwable ex){XposedBridge.log(TAG+pkg+": constructor hook failed: "+ex);}
 }
}
