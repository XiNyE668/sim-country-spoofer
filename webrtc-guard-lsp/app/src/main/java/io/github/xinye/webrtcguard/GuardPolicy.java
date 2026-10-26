package io.github.xinye.webrtcguard;
public final class GuardPolicy {
 private GuardPolicy() {}
 public static final String DOCUMENT_START_SCRIPT =
  "(()=>{'use strict';" +
  "for(const k of ['RTCPeerConnection','webkitRTCPeerConnection','mozRTCPeerConnection']){" +
  "try{Object.defineProperty(window,k,{configurable:false,enumerable:false,get(){return undefined},set(v){}})}" +
  "catch(e){try{window[k]=undefined}catch(_){}}}" +
  "})()";
}
