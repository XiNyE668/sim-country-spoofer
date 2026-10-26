package io.github.xinye.webrtcguard;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import androidx.webkit.WebViewFeature;

/**
 * V7 fixes V5/V6's lost audit report on activity recreation.
 * IMPORTANT: runtime WebRTC suppression still needs Chrome real-device tests.
 */
public final class GuardActivity extends Activity {
  private static final int SAVE_REQUEST=1707;
  private static final String PREFS="xinye_chrome_v7_audit";
  private static final String CACHE="chrome_audit_v7.txt";
  private static final String P_ID="report_chrome_identity";
  private WebView web;
  private TextView state;
  private Button scanButton, exportButton;
  private String report="";
  private String reportIdentity="";
  private boolean scanning=false;
  private boolean exportWhenReady=false;

  @Override public void onCreate(Bundle saved) {
    super.onCreate(saved);
    LinearLayout root=new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(20,16,20,12);
    root.setBackgroundColor(Color.WHITE);
    TextView title=new TextView(this);
    title.setText("XiNyE WebRTC Guard V7");
    title.setTextColor(Color.BLACK); title.setTextSize(20);
    root.addView(title);
    state=new TextView(this);
    state.setTextSize(13);
    state.setTextColor(Color.rgb(155,45,25));
    root.addView(state);

    scanButton=new Button(this);
    scanButton.setText("Re-scan Chrome APK code");
    root.addView(scanButton);
    exportButton=new Button(this);
    exportButton.setText("Export V7 Chrome audit TXT");
    root.addView(exportButton);

    Button browser=new Button(this);
    browser.setText("Open Chrome WebRTC test");
    root.addView(browser);
    Button local=new Button(this);
    local.setText("Test frozen V1 WebView");
    root.addView(local);
    Button online=new Button(this);
    online.setText("Test ipleak in System WebView");
    root.addView(online);
    Button notification=new Button(this);
    notification.setText("Allow Chrome update alerts");
    root.addView(notification);

    web=new WebView(this);
    web.getSettings().setJavaScriptEnabled(true);
    web.setWebViewClient(new WebViewClient());
    root.addView(web,new LinearLayout.LayoutParams(-1,0,1f));
    setContentView(root);

    scanButton.setOnClickListener(v->startScan(false));
    exportButton.setOnClickListener(v->exportRequested());
    local.setOnClickListener(v->testWebView());
    online.setOnClickListener(v->web.loadUrl("https://ipleak.net/"));
    browser.setOnClickListener(v->{
      try{
        Intent i=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://ipleak.net/"));
        i.setPackage("com.android.chrome");
        startActivity(i);
      }catch(Exception e){show("Chrome launch error: "+e.getClass().getSimpleName());}
    });
    notification.setOnClickListener(v->{
      if(Build.VERSION.SDK_INT>=33 &&
          checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},107);
      }else show("Chrome update alert permission available.");
    });
    testWebView();

    // V6's defect: cached Chrome version caused scan to be skipped, but report
    // was always initialized to NO_SCAN_YET on each Activity recreation.
    // V7 persists and reloads the *actual report* and validates its identity.
    String identity=ChromeDexScanner.identity(getApplicationContext());
    report=readCachedReport(identity);
    if(hasValidReport(identity)){
      show("Recovered cached Chrome APK audit for "+identity+
        "\nChrome protection: NOT VERIFIED by this report."+
        "\nYou may export now; Chrome WebRTC must be checked separately.");
    }else{
      show("No valid V7 audit for current Chrome. Auto-scanning installed APK code."+
        "\nChrome protection: NOT VERIFIED");
      startScan(false);
    }
  }

  private void show(String s){ if(state!=null)state.setText(s); }

  private boolean hasValidReport(String identity){
    return identity!=null && identity.equals(reportIdentity)
      && report!=null && report.startsWith("XiNyE WebRTC V7 Chrome APK")
      && report.contains("DEX_ENTRIES=")
      && !report.contains("STATUS=NO_DEX_PARSABLE")
      && !report.contains("STATUS=CHROME_NOT_VISIBLE")
      && !report.contains("STATUS=SCAN_FAILED");
  }

  private String readCachedReport(String identity){
    reportIdentity="";
    if(!identity.equals(getSharedPreferences(PREFS,MODE_PRIVATE).getString(P_ID,"")))return "";
    File file=getFileStreamPath(CACHE);
    if(file==null || !file.isFile() || file.length()<40 || file.length()>524288)return "";
    try(FileInputStream in=openFileInput(CACHE)){
      byte[] contents=new byte[(int)file.length()];
      int total=0;
      while(total<contents.length){
        int count=in.read(contents,total,contents.length-total);
        if(count<0)break;
        total+=count;
      }
      if(total!=contents.length)return "";
      String txt=new String(contents,StandardCharsets.UTF_8);
      if(!txt.contains("CHROME_IDENTITY="+identity))return "";
      reportIdentity=identity;
      return txt;
    }catch(Exception ignored){return "";}
  }

  private void persistReport(String content,String identity){
    File temp=getFileStreamPath(CACHE+".tmp");
    File target=getFileStreamPath(CACHE);
    try(FileOutputStream out=openFileOutput(CACHE+".tmp",MODE_PRIVATE)){
      out.write(content.getBytes(StandardCharsets.UTF_8));
      out.flush();
      out.getFD().sync();
    }catch(Exception e){return;}
    if(temp==null || target==null || !temp.renameTo(target))return;
    getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(P_ID,identity).apply();
  }

  private void exportRequested(){
    String identity=ChromeDexScanner.identity(getApplicationContext());
    if(scanning){
      exportWhenReady=true;
      show("Chrome scan is in progress. Export will open automatically after it finishes.");
    }else if(hasValidReport(identity)){
      launchSaveDialog();
    }else{
      exportWhenReady=true;
      startScan(true);
    }
  }

  private void launchSaveDialog(){
    Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("text/plain");
    intent.putExtra(Intent.EXTRA_TITLE,"XiNyE-WebRTC-V7-Chrome-Audit.txt");
    try{startActivityForResult(intent,SAVE_REQUEST);}
    catch(Exception e){show("Could not open save picker: "+e.getClass().getSimpleName());}
  }

  private void startScan(boolean forExport){
    if(forExport) exportWhenReady=true;
    if(scanning){
      show("Scan already running. Chrome protection remains UNVERIFIED.");
      return;
    }
    scanning=true;
    scanButton.setEnabled(false);
    exportButton.setEnabled(false);
    show("Chrome APK audit running... Do not close the app until completed."+
      "\nA fresh TXT can only be exported after this scan returns."+
      "\nChrome WebRTC protection remains UNVERIFIED.");
    final long start=SystemClock.elapsedRealtime();
    final String requestedIdentity=ChromeDexScanner.identity(getApplicationContext());
    new Thread(()->{
      String data;
      try{
        data=ChromeDexScanner.scan(getApplicationContext());
        if(data==null || data.trim().isEmpty())
          throw new IllegalStateException("Scanner returned blank text");
      }catch(Throwable t){
        StringBuilder err=new StringBuilder("XiNyE WebRTC V7 Chrome APK audit error\n");
        err.append("CHROME_IDENTITY=").append(requestedIdentity).append('\n');
        err.append("STATUS=SCAN_FAILED\n");
        err.append("EXCEPTION=").append(t.getClass().getName()).append('\n');
        err.append("MESSAGE=").append(String.valueOf(t.getMessage())).append('\n');
        StackTraceElement[] frames=t.getStackTrace();
        for(int i=0;i<Math.min(8,frames.length);i++){
          err.append("STACK_").append(i).append('=')
            .append(frames[i].getClassName()).append('.')
            .append(frames[i].getMethodName()).append('\n');
        }
        data=err.toString();
      }
      final String result=data;
      final long duration=SystemClock.elapsedRealtime()-start;
      final String finishedIdentity=ChromeDexScanner.identity(getApplicationContext());
      final boolean same=requestedIdentity.equals(finishedIdentity);
      // Version changed during scan? Do not pass stale audit as valid.
      String audit=result+"\nSCAN_DURATION_MS="+duration+
          "\nCHROME_IDENTITY_STILL_SAME="+same+
          "\nCHROME_WEBRTC_PROTECTION=UNVERIFIED\n";
      if(same && result.contains("DEX_ENTRIES=") && !result.contains("STATUS=NO_DEX_PARSABLE"))
        persistReport(audit,requestedIdentity);
      final String done=audit;
      runOnUiThread(()->{
        scanning=false;
        scanButton.setEnabled(true);
        exportButton.setEnabled(true);
        report=done;
        reportIdentity=same?requestedIdentity:"";
        boolean valid=hasValidReport(finishedIdentity);
        show((valid?"Chrome APK scan completed.":"Chrome APK scan incomplete or failed.")+
          "\nDuration: "+duration+" ms"+
          "\nChrome IP-leak protection: UNVERIFIED."+
          "\nYou can export this report."+
          "\nFor runtime Chrome failure use the supplied ADB capture.");
        // On failure still export detailed errors; never export an empty
        // placeholder. Disable user-risky success indicators.
        if(exportWhenReady){
          exportWhenReady=false;
          launchSaveDialog();
        }
      });
    },"XiNyE-Chrome-V7-Audit").start();
  }

  private void testWebView(){
    String html="<!doctype html><meta name='viewport' content='width=device-width'>"+
      "<h3>WebRTC Guard test</h3><p id='r'>Checking...</p>"+
      "<script>const C=window.RTCPeerConnection||window.webkitRTCPeerConnection;"+
      "document.getElementById('r').textContent=typeof C==='function'?"+
      "'NOT PROTECTED':'PASS: no PeerConnection constructor';</script>";
    web.loadDataWithBaseURL("https://guard-test.example/",html,"text/html","UTF-8",null);
  }

  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(request!=SAVE_REQUEST || result!=RESULT_OK || data==null || data.getData()==null)return;
    String toWrite=report;
    if(toWrite==null || toWrite.trim().length()<40){
      toWrite="XiNyE WebRTC V7 Chrome audit error\nSTATUS=REPORT_MISSING\n"+
        "CHROME_IDENTITY="+ChromeDexScanner.identity(this)+"\n"+
        "ACTION=Re-run scan and export. Capture Chrome runtime via ADB.\n";
    }
    try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"w")){
      if(out==null)throw new java.io.IOException("Null save output stream");
      out.write(toWrite.getBytes(StandardCharsets.UTF_8));
      out.flush();
      show("Saved populated audit TXT. Upload with Chrome runtime ADB log."+
        "\nChrome WebRTC protection: UNVERIFIED");
    }catch(Exception e){show("TXT save failed: "+e.getClass().getSimpleName());}
  }

  @Override protected void onDestroy(){
    if(web!=null){web.destroy();web=null;}
    super.onDestroy();
  }
}
