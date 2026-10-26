package io.github.xinye.webrtcguard;

import android.util.Log;
import java.util.concurrent.atomic.AtomicInteger;
import de.robv.android.xposed.XposedBridge;

/** Only module-defined event codes; never records visited URLs or raw IP addresses. */
public final class DiagnosticLog {
 private static final String TAG = "XiNyEWebRTC";
 private static final AtomicInteger EVENTS = new AtomicInteger();
 private DiagnosticLog() { }
 public static void write(String message) {
  // 400 lines per process protects battery and logcat from hot loop events.
  int n = EVENTS.incrementAndGet();
  if (n > 400) return;
  if (n == 400) message = "LOG_LIMIT_REACHED (400 events per process)";
  String safe = message == null ? "NULL_EVENT" : message.replace('\n',' ').replace('\r',' ');
  if (safe.length() > 280) safe = safe.substring(0,280) + "[truncated]";
  Log.i(TAG,safe);
  try { XposedBridge.log("[XiNyE-WebRTC-Guard V4/Diag] " + safe); }
  catch (Throwable ignored) { }
 }
}
