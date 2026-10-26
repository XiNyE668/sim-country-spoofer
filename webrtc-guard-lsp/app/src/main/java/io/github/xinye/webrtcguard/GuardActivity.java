package io.github.xinye.webrtcguard;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.webkit.WebViewFeature;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class GuardActivity extends Activity {
 private static final int SAVE_REQUEST=1505;
 private WebView web;
 private TextView state;
 private String report="NO_SCAN_YET\n";
 private final String prefs="xinye_chrome_v6_monitor";

 @Override public void onCreate(Bundle saved) {
  super.onCreate(saved);
  LinearLayout root=new LinearLayout(this);
  root.setOrientation(LinearLayout.VERTICAL);
  root.setPadding(20,16,20,12);
  root.setBackgroundColor(Color.WHITE);

  TextView heading=new TextView(this);
  heading.setText("XiNyE WebRTC Guard V6");
  heading.setTextSize(21);
  heading.setTextColor(Color.BLACK);
  root.addView(heading);
  state=new TextView(this);
  state.setTextColor(Color.rgb(150,45,25));
  state.setTextSize(13);
  root.addView(state);

  Button scan=new Button(this);scan.setText("Scan Chrome code (V6)");root.addView(scan);
  Button export=new Button(this);export.setText("Export V6 compatibility TXT");root.addView(export);
  Button alerts=new Button(this);alerts.setText("Enable Chrome update notification");root.addView(alerts);
  Button browser=new Button(this);browser.setText("Open Chrome WebRTC test");root.addView(browser);
  Button local=new Button(this);local.setText("Test frozen V1 WebView");root.addView(local);
  Button online=new Button(this);online.setText("Test ipleak in System WebView");root.addView(online);

  web=new WebView(this);
  web.getSettings().setJavaScriptEnabled(true);
  web.setWebViewClient(new WebViewClient());
  root.addView(web,new LinearLayout.LayoutParams(-1,0,1f));
  setContentView(root);

  scan.setOnClickListener(v->startScan());
  export.setOnClickListener(v->{
    Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    i.setType("text/plain");
    i.putExtra(Intent.EXTRA_TITLE,"XiNyE-WebRTC-V6-Chrome-Audit.txt");
    startActivityForResult(i,SAVE_REQUEST);
  });
  alerts.setOnClickListener(v->{
    if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED){
      requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},101);
    }else{
      state.setText(state.getText()+"\nChrome update alert permission: ready.");
    }
  });
  browser.setOnClickListener(v->{
    try{
      Intent i=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://ipleak.net/"));
      i.setPackage("com.android.chrome");
      startActivity(i);
    }catch(Exception e){state.setText("Chrome launch failed: "+e.getClass().getSimpleName());}
  });
  local.setOnClickListener(v->test());
  online.setOnClickListener(v->web.loadUrl("https://ipleak.net/"));
  test();

  String current=ChromeDexScanner.identity(this);
  String scanned=getSharedPreferences(prefs,MODE_PRIVATE).getString("scanned_identity","");
  String versionStatus= getSharedPreferences(prefs,MODE_PRIVATE)
      .getBoolean("chrome_updated",false) ? "CHROME UPDATE DETECTED — RETEST REQUIRED" :
      (current.equals(scanned)?"SAME VERSION — BUT WEBRTC PROTECTION UNVERIFIED":
        "CHROME VERSION NOT AUDITED — RETEST REQUIRED");
  state.setText("System WebView V1: code frozen; verify with local test."
    +"\nChrome: "+current
    +"\n"+versionStatus
    +"\nLSPosed: select com.android.webview + com.android.chrome"
    +"\nV6 scans local Chrome APK code. No browser history or cookies."
    +"\nChrome WebRTC protection UNVERIFIED until ipleak.net passes.");
  if(!current.equals(scanned)) startScan();
 }

 private void startScan(){
  state.setText("Auditing installed Chrome code in a background thread…\n"
      +"Chrome WebRTC protection still UNVERIFIED.");
  new Thread(()->{
    String result;
    try{result=ChromeDexScanner.scan(getApplicationContext());}
    catch(Throwable t){result="CHROME_SCAN_ERROR="+t.getClass().getSimpleName()+"\n";}
    final String done=result;
    runOnUiThread(()->{
      report=done;
      boolean complete=done.contains("DEX_ENTRIES=")
          && !done.contains("STATUS=NO_DEX_PARSABLE");
      if(complete){
        getSharedPreferences(prefs,MODE_PRIVATE).edit()
          .putString("scanned_identity",ChromeDexScanner.identity(this))
          .putBoolean("chrome_updated",false).apply();
      }
      int i=done.indexOf("CLASS_SOURCE_CHECK=");
      String codeLine=i<0?"Could not identify Chrome classes.":done.substring(i,Math.min(done.length(),done.indexOf('\n',i)>i?done.indexOf('\n',i):done.length()));
      state.setText("Chrome APK scan "+(complete?"completed":"incomplete")
        +"\n"+codeLine+"\nChrome WebRTC protection: UNVERIFIED"
        +"\nExport TXT and check ipleak.net; a class signature alone proves nothing.");
    });
  },"XiNyE-Chrome-DEX-Audit").start();
 }

 private void test(){
  String html="<!doctype html><meta name='viewport' content='width=device-width'><h3>WebRTC Guard test</h3><p id='r'>Checking...</p><script>const C=window.RTCPeerConnection||window.webkitRTCPeerConnection;document.getElementById('r').textContent=typeof C==='function'?'NOT PROTECTED':'PASS: no PeerConnection constructor';</script>";
  web.loadDataWithBaseURL("https://guard-test.example/",html,"text/html","UTF-8",null);
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(request!=SAVE_REQUEST || result!=RESULT_OK || data==null || data.getData()==null)return;
  try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"w")){
    if(out==null)throw new java.io.IOException("null output stream");
    out.write(report.getBytes(StandardCharsets.UTF_8));out.flush();
    state.setText("Saved Chrome APK audit TXT. WebRTC protection remains UNVERIFIED.");
  }catch(Exception e){state.setText("Export failed: "+e.getClass().getSimpleName());}
 }
 @Override protected void onDestroy(){
  if(web!=null){web.destroy();web=null;}super.onDestroy();
 }
}
