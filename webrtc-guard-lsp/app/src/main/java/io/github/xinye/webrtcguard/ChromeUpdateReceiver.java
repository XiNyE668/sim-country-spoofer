package io.github.xinye.webrtcguard;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Update invalidation only. Does not claim to detect leaks or WebRTC status.
 * Notification depends on Android permission and package broadcast delivery.
 */
public final class ChromeUpdateReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent intent) {
  if(intent==null || intent.getData()==null ||
      !"com.android.chrome".equals(intent.getData().getSchemeSpecificPart()))return;
  c.getSharedPreferences("xinye_chrome_v6_monitor",Context.MODE_PRIVATE).edit()
    .putBoolean("chrome_updated",true)
    .remove("scanned_identity").apply();
  if(Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
      != PackageManager.PERMISSION_GRANTED)return;
  try{
    NotificationManager m=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
    if(m==null)return;
    m.createNotificationChannel(new NotificationChannel("xinye_chrome_compat",
        "Chrome WebRTC compatibility",NotificationManager.IMPORTANCE_DEFAULT));
    Notification.Builder n=new Notification.Builder(c,"xinye_chrome_compat")
      .setSmallIcon(android.R.drawable.stat_notify_error)
      .setContentTitle("Chrome updated — WebRTC guard unverified")
      .setContentText("Open XiNyE V6 and recheck Chrome protection on ipleak.net.")
      .setAutoCancel(true);
    m.notify(5155,n.build());
  } catch(Throwable ignored){}
 }
}
