package io.github.xinye.webrtcguard;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.MultiDexContainer;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * READ-ONLY code-signature inventory for user's actual installed Chrome APKs.
 * Does NOT execute, load, modify or inject code and NEVER reads Chrome profile.
 * Structural candidates are NOT automatically considered safe hook targets.
 */
public final class ChromeDexScanner {
  public static final String PACKAGE="com.android.chrome";
  public static final String OBS="Lorg/chromium/content/browser/webcontents/WebContentsObserverProxy;";
  public static final String CONTENTS="Lorg/chromium/content/browser/webcontents/WebContentsImpl;";
  private ChromeDexScanner() {}

  public static String identity(Context c) {
    try {
      PackageInfo p=c.getPackageManager().getPackageInfo(PACKAGE,0);
      long v=Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;
      return (p.versionName==null?"?":p.versionName)+":"+v+":"+p.lastUpdateTime;
    } catch(Exception e) {return "MISSING:"+e.getClass().getSimpleName();}
  }

  private static final class Candidate implements Comparable<Candidate> {
    final String id; final int score;
    Candidate(String id,int score){this.id=id;this.score=score;}
    @Override public int compareTo(Candidate other){return Integer.compare(other.score,score);}
  }

  public static String scan(Context c) {
    StringBuilder out=new StringBuilder(8192);
    out.append("XiNyE WebRTC V7 Chrome APK structural compatibility audit\n");
    out.append("TIME=").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(new java.util.Date())).append('\n');
    out.append("ANDROID_SDK=").append(Build.VERSION.SDK_INT).append('\n');
    out.append("CHROME_IDENTITY=").append(identity(c)).append('\n');
    out.append("SCANNING=APK CODE ONLY (no web history, cookies, credentials, or user profiles)\n\n");
    PackageManager pm=c.getPackageManager();
    ApplicationInfo info;
    try { info=pm.getApplicationInfo(PACKAGE,0); }
    catch(Exception ex){return out.append("STATUS=CHROME_NOT_VISIBLE ").append(ex.getClass().getSimpleName()).toString();}
    ArrayList<String> archives=new ArrayList<>();
    if(info.sourceDir!=null) archives.add(info.sourceDir);
    if(info.splitSourceDirs!=null) archives.addAll(Arrays.asList(info.splitSourceDirs));
    out.append("APK_PARTS=").append(archives.size()).append('\n');
    int classes=0,methods=0,dexEntries=0,exactObs=0,exactContents=0,methodEval=0,
        structurallySimilar=0,parseFails=0;
    final int MAX_CLASSES=260000;
    ArrayList<Candidate> candidates=new ArrayList<>();
    for(String archive:archives) {
      // Scan only this installed code archive. Never read any /data/data directory.
      File file=new File(archive);
      if(!file.isFile()){parseFails++;out.append("UNREADABLE_CODE_SPLIT=").append(file.getName()).append('\n');continue;}
      try {
        MultiDexContainer<? extends DexFile> container=
            DexFileFactory.loadDexContainer(file, Opcodes.forApi(Build.VERSION.SDK_INT));
        for(String entryName:container.getDexEntryNames()){
          if(!entryName.matches("classes([0-9]+)?\\.dex")) continue;
          dexEntries++;
          MultiDexContainer.DexEntry<? extends DexFile> entry=container.getEntry(entryName);
          if(entry==null)continue;
          for(ClassDef clazz:entry.getDexFile().getClasses()){
            if(classes++ >= MAX_CLASSES)break;
            String name=clazz.getType();
            if(OBS.equals(name))exactObs++;
            if(CONTENTS.equals(name))exactContents++;
            boolean longField=false,hasEval=false,hasNamedNav=false;
            int n=0,signature=0;
            for(Field f:clazz.getFields())if("J".equals(f.getType()))longField=true;
            for(Method m:clazz.getMethods()){
              methods++;n++;
              String mn=m.getName();
              if("evaluateJavaScript".equals(mn)){hasEval=true;methodEval++;}
              if("didStartLoading".equals(mn) || "didFinishNavigationInPrimaryMainFrame".equals(mn)
                  || "primaryMainDocumentElementAvailable".equals(mn))hasNamedNav=true;
              if("V".equals(m.getReturnType()) && m.getParameters().size()==2) {
                MethodParameter first=m.getParameters().get(0);
                MethodParameter second=m.getParameters().get(1);
                if("Ljava/lang/String;".equals(first.getType()) && second.getType().startsWith("L"))
                  signature++;
              }
            }
            int score=(name.contains("WebContents")?6:0)+(hasEval?8:0)
                +(hasNamedNav?7:0)+(longField?1:0)
                +(signature>0?2:0)+(n>=45?1:0);
            if(signature>0 && n>=45 && longField)structurallySimilar++;
            if(score>=7) {
              candidates.add(new Candidate(name+" [methods="+n+",String/interface="+signature
                  +",evalName="+hasEval+",navName="+hasNamedNav+"]",score));
            }
          }
        }
      } catch(Throwable error){
        parseFails++;
        out.append("SCAN_EXCEPTION split=").append(file.getName()).append(" kind=")
          .append(error.getClass().getSimpleName()).append('\n');
      }
    }
    Collections.sort(candidates);
    out.append("\nDEX_ENTRIES=").append(dexEntries)
      .append("\nCLASSES=").append(classes)
      .append("\nMETHODS=").append(methods)
      .append("\nEXACT_OBSERVER_CLASS=").append(exactObs)
      .append("\nEXACT_WEBCONTENTS_CLASS=").append(exactContents)
      .append("\nEVALUATE_JS_METHOD_NAME_HITS=").append(methodEval)
      .append("\nSTRUCTURAL_CANDIDATE_COUNT=").append(structurallySimilar)
      .append("\nSCAN_ERRORS=").append(parseFails).append('\n');
    out.append("\nCANDIDATES_UP_TO_15 (structural clues, NOT verified hook targets)\n");
    for(int i=0;i<Math.min(15,candidates.size());i++){
      Candidate x=candidates.get(i);
      out.append(i+1).append(". SCORE=").append(x.score).append(" ").append(x.id).append('\n');
    }
    if(exactObs>0 && exactContents>0){
      out.append("\nCLASS_SOURCE_CHECK=KNOWN_NAMES_PRESENT\n");
      out.append("RUNTIME_INJECTION=NOT_VERIFIED; V4 classloader and JS check still required.\n");
    } else {
      out.append("\nCLASS_SOURCE_CHECK=V3_V4_NAMES_NOT_FOUND\n");
      out.append("RUNTIME_INJECTION=NOT_VERIFIED; signature-based code audit is not a working hook.\n");
    }
    if(dexEntries==0)out.append("STATUS=NO_DEX_PARSABLE\n");
    else out.append("STATUS=CHROME_PROTECTION_UNVERIFIED\n");
    out.append("SAFETY=Do not assume Chrome WebRTC is blocked; test ipleak.net after every update.\n");
    return out.toString();
  }
}
