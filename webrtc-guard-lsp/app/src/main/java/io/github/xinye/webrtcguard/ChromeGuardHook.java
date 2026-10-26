package io.github.xinye.webrtcguard;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.LongSparseArray;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.util.Map;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * V8: runtime Chrome isolated-split classloader and registry-based
 * JavaScript method selection, preserving frozen V1 WebView code.
 *
 * NOTE: Chromium's Java-side navigation callbacks may occur after the first
 * page script. This can NEVER be considered a verified document-start guard.
 * Does not change VPN, native Blink, Chrome flags, global firewall, or DNS.
 */
public final class ChromeGuardHook implements IXposedHookLoadPackage {
  private static final String CHROME="com.android.chrome";
  private static final String OBS="org.chromium.content.browser.webcontents.WebContentsObserverProxy";
  private static final String CONTENTS="org.chromium.content.browser.webcontents.WebContentsImpl";
  private static final String KEY="xinye_webrtc_webcontents_v8";
  private static volatile Class<?> contentsClass;
  private static final String TAG="V8_CHROME ";
  private static final ThreadLocal<Boolean> BUSY=new ThreadLocal<>();
  private static final Set<Class<?>> OBS_DONE=Collections.synchronizedSet(
      Collections.newSetFromMap(new WeakHashMap<Class<?>,Boolean>()));
  private static final Set<Class<?>> CONTENTS_DONE=Collections.synchronizedSet(
      Collections.newSetFromMap(new WeakHashMap<Class<?>,Boolean>()));

  private static final String JS=GuardPolicy.DOCUMENT_START_SCRIPT+
    ";typeof window.RTCPeerConnection==='undefined'?'BLOCKED':'STILL_AVAILABLE'";

  private static void log(String message) { DiagnosticLog.write(TAG+message); }
  private static boolean busy() {return Boolean.TRUE.equals(BUSY.get());}

  @Override public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam p) {
    if(!CHROME.equals(p.packageName))return;
    final String process=p.processName==null?CHROME:p.processName;
    log("LOAD_PACKAGE process="+process+" pid="+android.os.Process.myPid());
    // All hooks below exist only in Chrome's LSPosed-scoped processes.
    try {
      XposedBridge.hookAllMethods(ClassLoader.class,"loadClass",new XC_MethodHook(){
        @Override protected void afterHookedMethod(MethodHookParam x) {
          if(x.hasThrowable() || busy() || x.args==null || x.args.length==0) return;
          Object name=x.args[0];
          if(!(name instanceof String))return;
          String n=(String)name;
          if(!OBS.equals(n) && !CONTENTS.equals(n)) return;
          if(!(x.getResult() instanceof Class<?>))return;
          installClass((Class<?>)x.getResult(),"LOAD_CLASS",process);
        }
      });
      log("CLASSLOADER_LOADCLASS_HOOK=INSTALLED");
    }catch(Throwable e){log("CLASSLOADER_LOADCLASS_HOOK=FAIL kind="+e.getClass().getSimpleName());}
    try {
      Class<?> dex=Class.forName("dalvik.system.BaseDexClassLoader");
      XposedBridge.hookAllMethods(dex,"findClass",new XC_MethodHook(){
        @Override protected void afterHookedMethod(MethodHookParam x){
          if(x.hasThrowable() || busy() || x.args==null || x.args.length==0) return;
          Object n=x.args[0];
          if(!(n instanceof String) || (!OBS.equals(n) && !CONTENTS.equals(n)))return;
          if(x.getResult() instanceof Class<?>)installClass((Class<?>)x.getResult(),"DEX_FINDCLASS",process);
        }
      });
      log("DEX_FINDCLASS_HOOK=INSTALLED");
    }catch(Throwable e){log("DEX_FINDCLASS_HOOK=FAIL kind="+e.getClass().getSimpleName());}

    // Existing browser split contexts may have a different loader from the
    // loadPackageParam loader. Observe creation before Chrome starts browsing.
    try {
      Class<?> contextImpl=Class.forName("android.app.ContextImpl");
      XposedBridge.hookAllMethods(contextImpl,"createContextForSplit",new XC_MethodHook(){
        @Override protected void afterHookedMethod(MethodHookParam x) {
          if(x.hasThrowable() || x.args==null || x.args.length==0 ||
             !(x.args[0] instanceof String) || !(x.getResult() instanceof Context)) return;
          String split=(String)x.args[0];
          if("chrome".equals(split)){
            log("SPLIT_CONTEXT_OBSERVED split=chrome");
            probe(((Context)x.getResult()).getClassLoader(),"SPLIT_CONTEXT",process);
          }
        }
      });
      log("SPLIT_CONTEXT_HOOK=INSTALLED");
    }catch(Throwable e){log("SPLIT_CONTEXT_HOOK=FAIL kind="+e.getClass().getSimpleName());}

    probe(p.classLoader,"INITIAL_LOADER",process);
    try {
      XposedBridge.hookAllMethods(Application.class,"attach",new XC_MethodHook(){
        @Override protected void afterHookedMethod(MethodHookParam x){
          if(x.args.length<1 || !(x.args[0] instanceof Context))return;
          final Context c=(Context)x.args[0];
          log("APPLICATION_ATTACH process="+process);
          probe(c.getClassLoader(),"ATTACH_BASE",process);
          if(!CHROME.equals(process))return; // browser process only
          try {
            // Only Chrome's own "chrome" installed code split is requested.
            boolean splitFound=false;
            String[] splits=c.getApplicationInfo().splitNames;
            if(splits!=null)for(String split:splits)if("chrome".equals(split))splitFound=true;
            if(!splitFound){log("SPLIT_CHROME_NOT_INSTALLED_OR_MERGED");return;}
            new Handler(Looper.getMainLooper()).post(()->{
              try{
                Context split=c.createContextForSplit("chrome");
                ClassLoader cl=split.getClassLoader();
                log("CHROME_SPLIT_CLASSLOADER="+cl.getClass().getName());
                probe(cl,"EXPLICIT_CHROME_SPLIT",process);
              }catch(Throwable e){log("CHROME_SPLIT_CLASSLOADER_ERROR="+e.getClass().getSimpleName());}
            });
          }catch(Throwable e){log("SPLIT_ENUM_ERROR="+e.getClass().getSimpleName());}
        }
      });
    }catch(Throwable e){log("APPLICATION_HOOK_ERROR="+e.getClass().getSimpleName());}
  }

  private static void probe(ClassLoader loader,String origin,String process){
    if(loader==null || busy())return;
    BUSY.set(true);
    try {
      Class<?> observer=XposedHelpers.findClassIfExists(OBS,loader);
      Class<?> wc=XposedHelpers.findClassIfExists(CONTENTS,loader);
      log("PROBE origin="+origin+" observer="+(observer==null?"ABSENT":"FOUND")
          +" webcontents="+(wc==null?"ABSENT":"FOUND"));
      if(wc!=null)installContents(wc,origin,process);
      if(observer!=null)installObserver(observer,origin,process);
    }catch(Throwable e){log("PROBE_ERROR origin="+origin+" "+e.getClass().getSimpleName());}
    finally{BUSY.remove();}
  }

  private static void installClass(Class<?> clazz,String origin,String process){
    if(busy() || clazz==null)return;
    BUSY.set(true);
    try {
      if(OBS.equals(clazz.getName()))installObserver(clazz,origin,process);
      else if(CONTENTS.equals(clazz.getName()))installContents(clazz,origin,process);
    }catch(Throwable e){log("INSTALL_CLASS_ERROR="+e.getClass().getSimpleName());}
    finally{BUSY.remove();}
  }

  private static void installObserver(Class<?> clazz,String origin,String process){
    synchronized(OBS_DONE) { if(!OBS_DONE.add(clazz))return; }
    int hooked=0;
    try{
      XposedBridge.hookAllConstructors(clazz,new XC_MethodHook(){
        @Override protected void afterHookedMethod(MethodHookParam x){
          if(x.args!=null && x.args.length>0 && x.args[0]!=null){
            XposedHelpers.setAdditionalInstanceField(x.thisObject,KEY,x.args[0]);
            log("OBSERVER_CONSTRUCTOR_LINK=YES");
          }else log("OBSERVER_CONSTRUCTOR_LINK=NO");
        }
      });
      log("OBSERVER_CONSTRUCTORS_HOOKED origin="+origin);
    }catch(Throwable e){log("OBSERVER_CONSTRUCTOR_ERROR="+e.getClass().getSimpleName());}
    // These callbacks are Chromium browser process events. They do NOT
    // guarantee JavaScript ran before the page's first inline script.
    String[] events={
      "didStartNavigationInPrimaryMainFrame",
      "primaryMainDocumentElementAvailable",
      "didFinishNavigationInPrimaryMainFrame",
      "didStartLoading",
      "navigationEntryCommitted",
      "didStopLoading"
    };
    for(final String event:events){
      try{
        int count=XposedBridge.hookAllMethods(clazz,event,new XC_MethodHook(){
          @Override protected void afterHookedMethod(MethodHookParam x){
            try{
              Object wc=XposedHelpers.getAdditionalInstanceField(x.thisObject,KEY);
              if(wc==null) {
                try{wc=XposedHelpers.callMethod(x.thisObject,"getWebContents");}
                catch(Throwable ignored){}
              }
              if(wc==null) wc=resolveFromRegistry(x.thisObject);
              if(wc==null){log("CALLBACK event="+event+" no_webcontents registry_lookup_failed");return;}
              log("CALLBACK event="+event+" linked");
              runJavaScript(wc,event);
            }catch(Throwable e){log("CALLBACK_ERROR event="+event+" "+e.getClass().getSimpleName());}
          }
        }).size();
        hooked+=count;
        if(count>0)log("CALLBACK_HOOK="+event+" count="+count);
      }catch(Throwable e){log("CALLBACK_INSTALL_ERROR event="+event+" "+e.getClass().getSimpleName());}
    }
    log("OBSERVER_INSTALL origin="+origin+" callback_count="+hooked);
  }

  private static void installContents(Class<?> clazz,String origin,String process){
    contentsClass=clazz;
    synchronized(CONTENTS_DONE) {if(!CONTENTS_DONE.add(clazz))return;}
    try {
      int count=XposedBridge.hookAllMethods(clazz,"addObserver",new XC_MethodHook(){
        @Override protected void afterHookedMethod(MethodHookParam x){
          try {
            Object proxy=XposedHelpers.getObjectField(x.thisObject,"mObserverProxy");
            if(proxy!=null){
              XposedHelpers.setAdditionalInstanceField(proxy,KEY,x.thisObject);
              log("ADD_OBSERVER_LINK=YES");
            }
          }catch(Throwable ignored){}
        }
      }).size();
      log("CONTENTS_ADD_OBSERVER_HOOK count="+count);
    }catch(Throwable e){log("CONTENTS_ADD_OBSERVER_ERROR="+e.getClass().getSimpleName());}
    int registryFields=0;
    for(Field f:clazz.getDeclaredFields()){
      if(Modifier.isStatic(f.getModifiers()) &&
        (LongSparseArray.class.isAssignableFrom(f.getType()) ||
         Map.class.isAssignableFrom(f.getType())))registryFields++;
    }
    log("CONTENTS_INSTALL origin="+origin+" method_candidates="+countCandidates(clazz)
      +" possible_static_registries="+registryFields);
  }

  /**
   * Chromium WebContentsImpl normally keeps a static LongSparseArray mapping
   * native pointers to its Java wrapper. The observed proxy can predate our
   * hooks, making constructor hooks too late. Match the existing proxy by
   * reference against all currently registered WebContentsImpl wrappers.
   *
   * Never enumerate URLs/tabs or invoke browser navigation methods.
   */
  private static Object resolveFromRegistry(Object observedProxy){
    final Class<?> type=contentsClass;
    if(type==null)return null;
    try{
      for(Field field:type.getDeclaredFields()){
        if(!Modifier.isStatic(field.getModifiers()))continue;
        Class<?> declared=field.getType();
        if(!LongSparseArray.class.isAssignableFrom(declared)
            && !Map.class.isAssignableFrom(declared))continue;
        field.setAccessible(true);
        Object container=field.get(null);
        if(container==null)continue;
        if(container instanceof LongSparseArray){
          LongSparseArray<?> indexed=(LongSparseArray<?>)container;
          for(int i=0;i<indexed.size();i++){
            Object owner=indexed.valueAt(i);
            if(type.isInstance(owner) && ownerHasProxy(owner,observedProxy)){
              XposedHelpers.setAdditionalInstanceField(observedProxy,KEY,owner);
              log("REGISTRY_ASSOCIATION=FOUND source=LongSparseArray");
              return owner;
            }
          }
        }else if(container instanceof Map){
          for(Object owner:((Map<?,?>)container).values()){
            if(type.isInstance(owner) && ownerHasProxy(owner,observedProxy)){
              XposedHelpers.setAdditionalInstanceField(observedProxy,KEY,owner);
              log("REGISTRY_ASSOCIATION=FOUND source=Map");
              return owner;
            }
          }
        }
      }
    }catch(Throwable e){log("REGISTRY_ASSOCIATION_ERROR="+e.getClass().getSimpleName());}
    return null;
  }

  private static boolean ownerHasProxy(Object owner,Object proxy){
    Class<?> cursor=owner.getClass();
    while(cursor!=null && cursor!=Object.class){
      for(Field f:cursor.getDeclaredFields()){
        if(Modifier.isStatic(f.getModifiers()))continue;
        if(!f.getType().isInstance(proxy))continue;
        try{
          f.setAccessible(true);
          if(f.get(owner)==proxy)return true;
        }catch(Throwable ignored){}
      }
      cursor=cursor.getSuperclass();
    }
    return false;
  }

  /**
   * V5 DEX audit showed 1 void(String,reference) method on WebContentsImpl.
   * V6 erroneously rejected it because the second parameter was not an
   * interface. Resolve only when the non-static signature is UNIQUE.
   */
  private static Method chooseEval(Class<?> clazz){
    Method unique=null;
    int matches=0;
    for(Method m:clazz.getDeclaredMethods()){
      Class<?>[] p=m.getParameterTypes();
      if(p.length!=2 || p[0]!=String.class || p[1].isPrimitive() ||
          p[1]==String.class || m.getReturnType()!=Void.TYPE ||
          Modifier.isStatic(m.getModifiers()) || Modifier.isAbstract(m.getModifiers()))continue;
      unique=m;matches++;
    }
    if(matches==1){
      try{unique.setAccessible(true);}catch(Throwable ignored){}
      return unique;
    }
    return null;
  }

  private static int countCandidates(Class<?> clazz){
    int n=0;
    for(Method m:clazz.getDeclaredMethods()){
      Class<?>[] p=m.getParameterTypes();
      if(p.length==2 && p[0]==String.class && !p[1].isPrimitive()
          && p[1]!=String.class && m.getReturnType()==Void.TYPE &&
          !Modifier.isStatic(m.getModifiers()) && !Modifier.isAbstract(m.getModifiers()))n++;
    }
    return n;
  }

  private static void runJavaScript(Object contents,String phase){
    try{
      Method eval=chooseEval(contents.getClass());
      int count=countCandidates(contents.getClass());
      if(eval==null){log("JS_METHOD_NOT_UNIQUE candidates="+count+" event="+phase);return;}
      Class<?> callbackType=eval.getParameterTypes()[1];
      Object callback=null;
      // JavaScriptCallback is an interface in upstream Chromium; R8 may
      // alter its shape. Passing null is legal for optional callback APIs.
      if(callbackType.isInterface()){
        callback=Proxy.newProxyInstance(callbackType.getClassLoader(),
          new Class<?>[]{callbackType},new InvocationHandler(){
            @Override public Object invoke(Object proxy,Method method,Object[] args){
              if(method.getDeclaringClass()==Object.class){
                if("toString".equals(method.getName()))return "XiNyEChromeV8Callback";
                if("hashCode".equals(method.getName()))return System.identityHashCode(proxy);
                if("equals".equals(method.getName()))return args!=null && args.length>0 && proxy==args[0];
              }
              if(args!=null && args.length>0 && args[0] instanceof String){
                String result=(String)args[0];
                log("JS_CALLBACK event="+phase+" state="+
                    (result.contains("STILL_AVAILABLE")?"STILL_AVAILABLE":
                     result.contains("BLOCKED")?"BLOCKED":"UNKNOWN"));
              }else log("JS_CALLBACK event="+phase+" no_string_result");
              return null;
            }
          });
      }else{
        log("JS_CALLBACK_NOT_INTERFACE param2_type="+callbackType.getName()
            +" using_null_callback=true");
      }
      log("JS_SUBMIT event="+phase+" method="+eval.getName()
        +" mapped_by_unique_signature=YES");
      eval.invoke(contents,JS,callback);
      log("JS_METHOD_RETURN event="+phase+" (not a guarantee of execution)");
    }catch(Throwable e){
      log("JS_INVOKE_ERROR event="+phase+" kind="+e.getClass().getSimpleName()
        +(e.getCause()==null?"":" cause="+e.getCause().getClass().getSimpleName()));
    }
  }
}
