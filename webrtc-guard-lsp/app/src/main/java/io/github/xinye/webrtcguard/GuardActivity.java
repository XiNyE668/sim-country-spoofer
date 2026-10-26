package io.github.xinye.webrtcguard;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.webkit.WebViewFeature;
public final class GuardActivity extends Activity {
 private WebView web;
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
  root.setBackgroundColor(Color.WHITE);root.setPadding(24,24,24,24);
  TextView title=new TextView(this);title.setText("XiNyE WebRTC Guard V3 Safe");title.setTextColor(Color.BLACK);title.setTextSize(20);root.addView(title);
  TextView info=new TextView(this);info.setTextColor(Color.DKGRAY);
  boolean supported=false;try{supported=WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT);}catch(Throwable ignored){}
  info.setText("DOCUMENT_START_SCRIPT available: "+supported+
  "\nV1 WebView code FROZEN. Select com.android.webview and WebView host apps as needed."+
  "\nIMPORTANT: Chrome protection NOT IMPLEMENTED. V2 unsafe command-line modification removed."+
  "\nKeep KernelSU firewall enabled and test public and WebRTC IP separately.");
  root.addView(info);
  Button local=new Button(this);local.setText("Test local WebRTC constructor");root.addView(local);
  Button online=new Button(this);online.setText("Test ipleak.net in this WebView");root.addView(online);
  web=new WebView(this);web.getSettings().setJavaScriptEnabled(true);web.setWebViewClient(new WebViewClient());
  root.addView(web,new LinearLayout.LayoutParams(-1,0,1f));
  setContentView(root);
  local.setOnClickListener(v->test());
  online.setOnClickListener(v->web.loadUrl("https://ipleak.net/"));
  test();
 }
 private void test(){
  String html="<!doctype html><meta name='viewport' content='width=device-width'><h3>WebRTC Guard test</h3><p id='r'>Checking...</p><script>const C=window.RTCPeerConnection||window.webkitRTCPeerConnection;document.getElementById('r').textContent=typeof C==='function'?'NOT PROTECTED':'PASS: no PeerConnection constructor';</script>";
  web.loadDataWithBaseURL("https://guard-test.example/",html,"text/html","UTF-8",null);
 }
 @Override protected void onDestroy(){if(web!=null){web.destroy();web=null;}super.onDestroy();}
}
