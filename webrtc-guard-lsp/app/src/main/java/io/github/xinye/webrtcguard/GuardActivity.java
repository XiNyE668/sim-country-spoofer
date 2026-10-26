package io.github.xinye.webrtcguard;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.webkit.WebViewFeature;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class GuardActivity extends Activity {
 private static final int EXPORT_REQUEST = 1404;
 private WebView web;
 private TextView status;
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  LinearLayout root=new LinearLayout(this);
  root.setOrientation(LinearLayout.VERTICAL);
  root.setBackgroundColor(Color.WHITE);
  root.setPadding(24,24,24,24);
  TextView title=new TextView(this);
  title.setText("XiNyE WebRTC Guard V4 Debug");
  title.setTextColor(Color.BLACK); title.setTextSize(20);
  root.addView(title);
  TextView info=new TextView(this);
  info.setTextColor(Color.DKGRAY);
  boolean supported=false;
  try { supported=WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT); }
  catch(Throwable ignored) {}
  info.setText("Frozen V1 WebView behavior: "+supported+
      "\nLSPosed recommended scope: Android System WebView + Chrome."+
      "\nChrome V3 injection is experimental: this build records stages."+
      "\nAfter testing Chrome on ipleak.net, return here to export diagnostics."+
      "\nExport reads only XiNyEWebRTC-tagged logs via KernelSU su."+
      "\nNo visited URLs, identifiers, or WebRTC addresses are logged.");
  root.addView(info);
  Button local=new Button(this);
  local.setText("Test local WebRTC constructor");
  root.addView(local);
  Button online=new Button(this);
  online.setText("Test ipleak.net in this WebView");
  root.addView(online);
  Button export=new Button(this);
  export.setText("Export Chrome debug TXT");
  root.addView(export);
  status=new TextView(this);
  status.setTextColor(Color.DKGRAY);
  status.setText("Run Chrome test, then export the log.");
  root.addView(status);
  web=new WebView(this);
  web.getSettings().setJavaScriptEnabled(true);
  web.setWebViewClient(new WebViewClient());
  root.addView(web,new LinearLayout.LayoutParams(-1,0,1f));
  setContentView(root);
  local.setOnClickListener(v->test());
  online.setOnClickListener(v->web.loadUrl("https://ipleak.net/"));
  export.setOnClickListener(v-> {
    Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    i.setType("text/plain");
    i.putExtra(Intent.EXTRA_TITLE,"XiNyE-WebRTC-V4-Diag.txt");
    startActivityForResult(i,EXPORT_REQUEST);
  });
  test();
 }
 private void test(){
  String html="<!doctype html><meta name='viewport' content='width=device-width'><h3>WebRTC Guard test</h3><p id='r'>Checking...</p><script>const C=window.RTCPeerConnection||window.webkitRTCPeerConnection;document.getElementById('r').textContent=typeof C==='function'?'NOT PROTECTED':'PASS: no PeerConnection constructor';</script>";
  web.loadDataWithBaseURL("https://guard-test.example/",html,"text/html","UTF-8",null);
 }
 @Override protected void onActivityResult(int request,int result,Intent data) {
  super.onActivityResult(request,result,data);
  if(request!=EXPORT_REQUEST || result!=RESULT_OK || data==null || data.getData()==null) return;
  final android.net.Uri uri=data.getData();
  status.setText("Exporting Chrome diagnostics…");
  new Thread(()->{
    StringBuilder report=new StringBuilder();
    report.append("XiNyE WEBRTC V4 DIAGNOSTIC REPORT\n");
    report.append("Log contains only XiNyEWebRTC entries, not full system logcat.\n");
    report.append("Please review the TXT before sharing.\n\n");
    report.append("Android SDK: ").append(Build.VERSION.SDK_INT).append('\n');
    report.append("Android release: ").append(Build.VERSION.RELEASE).append('\n');
    report.append("Chrome version: ").append(versionOf("com.android.chrome")).append('\n');
    report.append("System WebView version: ").append(versionOf("com.android.webview")).append('\n');
    report.append("Google WebView version: ").append(versionOf("com.google.android.webview")).append('\n');
    report.append("Module version: 4.0\n");
    report.append("NOTE: Chrome WEBRTC MAY REMAIN UNPROTECTED IN THIS DEBUG BUILD.\n");
    report.append("\n=== CHROME HOOK EVENTS (logcat tag XiNyEWebRTC) ===\n");
    Process p=null;
    try {
      ProcessBuilder pb=new ProcessBuilder("su","-c",
          "logcat -d -v threadtime -t 2500 -s XiNyEWebRTC:I");
      pb.redirectErrorStream(true);
      p=pb.start();
      try(BufferedReader r=new BufferedReader(new InputStreamReader(p.getInputStream(),
           StandardCharsets.UTF_8))) {
        String line;
        int lines=0;
        while((line=r.readLine())!=null && lines<2500) {
          if(line.contains("XiNyEWebRTC") || line.contains("Permission denied")
              || line.contains("not found") || line.contains("su:")) {
            report.append(line).append('\n');lines++;
          }
        }
      }
      int exit=p.waitFor();
      report.append("\nlogcat command exit: ").append(exit).append('\n');
      if(report.indexOf("LOAD_PACKAGE")==-1)
        report.append("NO_CHROME_LOAD_EVENT: check LSPosed Chrome scope and force-stop Chrome.\n");
    } catch(Throwable t) {
      report.append("\nLOG_CAPTURE_ERROR: ").append(t.getClass().getSimpleName()).append('\n');
      report.append("ADB fallback: adb logcat -d -v threadtime -s XiNyEWebRTC:I > WebRTC-V4-Diag.txt\n");
    } finally { if(p!=null) p.destroy(); }
    boolean success=false;
    try(OutputStream out=getContentResolver().openOutputStream(uri,"w")) {
      if(out!=null) {
        out.write(report.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
        success=true;
      }
    } catch(Throwable ignored) {}
    final boolean ok=success;
    runOnUiThread(()->status.setText(ok
        ? "TXT saved. Share this file and your Chrome ipleak.net result."
        : "TXT export failed. Use ADB fallback described below."));
  }).start();
 }
 private String versionOf(String pkg) {
  try {
    PackageInfo p=getPackageManager().getPackageInfo(pkg,0);
    return p.versionName==null?"unknown":p.versionName;
  } catch(PackageManager.NameNotFoundException e){return "not visible/installed";}
  catch(Throwable e){return "lookup-error";}
 }
 @Override protected void onDestroy(){
  if(web!=null) {web.destroy();web=null;}
  super.onDestroy();
 }
}
