#!/system/bin/sh

MODDIR=${0%/*}
. "$MODDIR/common.sh"

rotate_log
rmdir "$LOCKDIR" 2>/dev/null
load_config

log_msg "Service start root=$(detect_root_manager) api=$(getprop ro.build.version.sdk) lineage='$(getprop ro.lineage.version)'"

until [ "$(getprop sys.boot_completed)" = "1" ]; do
  sleep 2
done

# Let the Android 16 telephony stack publish its first SIM/network snapshot before enforcing.
wait_for_telephony_ready 45 || log_msg "Telephony properties were still empty after boot wait; applying configured values anyway."
load_config
if [ "$TELEPHONY_SETTLE_SECONDS" -gt 0 ]; then
  sleep "$TELEPHONY_SETTLE_SECONDS"
fi
apply_sim_country_props "boot-settle"

if [ "$(detect_root_manager)" = "APatch" ] && supports_resetprop_wait; then
  write_runtime_state "apatch-resetprop-event"
  log_msg "Watcher mode=apatch-resetprop-event"

  watch_property gsm.sim.operator.numeric & P1=$!
  watch_property gsm.sim.operator.iso-country & P2=$!
  watch_property gsm.sim.operator.alpha & P3=$!
  watch_property gsm.operator.numeric & P4=$!
  watch_property gsm.operator.iso-country & P5=$!
  watch_property gsm.operator.alpha & P6=$!
  WATCH_PIDS="$P1 $P2 $P3 $P4 $P5 $P6"

  trap 'kill $WATCH_PIDS 2>/dev/null; rmdir "$LOCKDIR" 2>/dev/null; exit 0' INT TERM
  wait
else
  write_runtime_state "fallback-poll"
  log_msg "Watcher mode=fallback-poll interval=${WATCH_INTERVAL}s"
  while true; do
    load_config
    if ! props_match; then
      apply_sim_country_props "poll-repair"
    fi
    sleep "$WATCH_INTERVAL"
  done
fi
