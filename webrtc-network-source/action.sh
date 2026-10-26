#!/system/bin/sh
MODDIR="${0%/*}"
. "$MODDIR/common.sh"
if [ -e "$ENABLED_FILE" ]; then
  rm -f "$ENABLED_FILE"
  remove_rules
  echo 'WebRTC network filtering: OFF'
else
  touch "$ENABLED_FILE"
  ensure_rules
  echo 'WebRTC network filtering: ON'
fi
echo 'Warning: only partial STUN filtering; ICE candidates may still be exposed.'
