#!/system/bin/sh
MODDIR="${0%/*}"
CHAIN="XINYE_RTC_OUT"
ENABLED_FILE="$MODDIR/guard.enabled"
remove_one() {
  FW="$1"
  command -v "$FW" >/dev/null 2>&1 || return 0
  while "$FW" -t filter -C OUTPUT -j "$CHAIN" >/dev/null 2>&1; do
    "$FW" -t filter -D OUTPUT -j "$CHAIN" >/dev/null 2>&1 || break
  done
  "$FW" -t filter -F "$CHAIN" >/dev/null 2>&1 || :
  "$FW" -t filter -X "$CHAIN" >/dev/null 2>&1 || :
}
remove_rules() {
  remove_one iptables
  remove_one ip6tables
}
add_one() {
  FW="$1"
  command -v "$FW" >/dev/null 2>&1 || return 0
  if ! "$FW" -t filter -nL "$CHAIN" >/dev/null 2>&1; then
    "$FW" -t filter -N "$CHAIN" >/dev/null 2>&1 || return 0
    "$FW" -t filter -A "$CHAIN" -p udp -m string --algo bm --hex-string '|2112A442|' --from 24 --to 128 -j DROP >/dev/null 2>&1 || :
    for PORT in 3478 5349 19302:19309; do
      "$FW" -t filter -A "$CHAIN" -p udp --dport "$PORT" -j DROP >/dev/null 2>&1 || :
    done
    "$FW" -t filter -A "$CHAIN" -j RETURN >/dev/null 2>&1 || :
  fi
  "$FW" -t filter -C OUTPUT -j "$CHAIN" >/dev/null 2>&1 || "$FW" -t filter -I OUTPUT 1 -j "$CHAIN" >/dev/null 2>&1 || :
}
ensure_rules() {
  if [ -e "$ENABLED_FILE" ] && [ ! -e "$MODDIR/disable" ]; then
    add_one iptables
    add_one ip6tables
  else
    remove_rules
  fi
}
