#!/system/bin/sh

CONFIG="$MODDIR/config.conf"
LOGFILE="$MODDIR/service.log"
RUNTIME_STATE="$MODDIR/runtime.state"
LOCKDIR="$MODDIR/.apply.lock"

log_msg() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') $*" >> "$LOGFILE"
}

rotate_log() {
  [ -f "$LOGFILE" ] || return 0
  _size="$(wc -c < "$LOGFILE" 2>/dev/null)"
  case "$_size" in
    ''|*[!0-9]*) return 0 ;;
  esac
  if [ "$_size" -gt 131072 ]; then
    tail -n 200 "$LOGFILE" > "$LOGFILE.tmp" 2>/dev/null && mv "$LOGFILE.tmp" "$LOGFILE"
  fi
}

load_config() {
  TARGET_MCC=310
  TARGET_MNC=260
  TARGET_ISO=us
  TARGET_ALPHA=T-Mobile
  SLOT_COUNT=2
  WATCH_INTERVAL=15
  TELEPHONY_SETTLE_SECONDS=8
  LEGACY_SLOT_PROPS=0

  if [ -f "$CONFIG" ]; then
    . "$CONFIG"
  fi

  normalize_config
}

normalize_config() {
  case "$TARGET_MCC" in
    [0-9][0-9][0-9]) ;;
    *) TARGET_MCC=310 ;;
  esac

  case "$TARGET_MNC" in
    [0-9][0-9]|[0-9][0-9][0-9]) ;;
    *) TARGET_MNC=260 ;;
  esac

  TARGET_ISO="$(printf '%s' "$TARGET_ISO" | tr '[:upper:]' '[:lower:]')"
  case "$TARGET_ISO" in
    [a-z][a-z]) ;;
    *) TARGET_ISO=us ;;
  esac

  case "$SLOT_COUNT" in
    1|2|3|4) ;;
    *) SLOT_COUNT=2 ;;
  esac

  case "$WATCH_INTERVAL" in
    ''|*[!0-9]*) WATCH_INTERVAL=15 ;;
  esac
  if [ "$WATCH_INTERVAL" -lt 2 ] || [ "$WATCH_INTERVAL" -gt 300 ]; then
    WATCH_INTERVAL=15
  fi

  case "$TELEPHONY_SETTLE_SECONDS" in
    ''|*[!0-9]*) TELEPHONY_SETTLE_SECONDS=8 ;;
  esac
  if [ "$TELEPHONY_SETTLE_SECONDS" -gt 60 ]; then
    TELEPHONY_SETTLE_SECONDS=8
  fi

  case "$LEGACY_SLOT_PROPS" in
    0|1) ;;
    *) LEGACY_SLOT_PROPS=0 ;;
  esac
}

repeat_value() {
  _value="$1"
  _count=1
  _result="$_value"

  while [ "$_count" -lt "$SLOT_COUNT" ]; do
    _result="$_result,$_value"
    _count=$((_count + 1))
  done

  printf '%s\n' "$_result"
}

prepare_desired_values() {
  TARGET_NUMERIC="${TARGET_MCC}${TARGET_MNC}"
  TARGET_NUMERIC_LIST="$(repeat_value "$TARGET_NUMERIC")"
  TARGET_ISO_LIST="$(repeat_value "$TARGET_ISO")"
  TARGET_ALPHA_LIST="$(repeat_value "$TARGET_ALPHA")"
}

detect_root_manager() {
  if [ "${APATCH:-}" = "true" ] || [ -d /data/adb/ap ]; then
    echo "APatch"
  elif [ -d /data/adb/ksu ]; then
    echo "KernelSU"
  elif [ -d /data/adb/magisk ]; then
    echo "Magisk"
  else
    echo "Unknown"
  fi
}

resetprop_path() {
  if command -v resetprop >/dev/null 2>&1; then
    command -v resetprop
  elif [ -x /data/adb/ap/bin/resetprop ]; then
    echo /data/adb/ap/bin/resetprop
  elif [ -x /data/adb/magisk/resetprop ]; then
    echo /data/adb/magisk/resetprop
  else
    echo ""
  fi
}

supports_resetprop_wait() {
  _rp="$(resetprop_path)"
  [ -n "$_rp" ] || return 1
  "$_rp" --help 2>&1 | grep -q -- '--wait'
}

set_prop_raw() {
  _prop="$1"
  _value="$2"
  _rp="$(resetprop_path)"

  if [ -n "$_rp" ]; then
    "$_rp" -n "$_prop" "$_value" >/dev/null 2>&1
  else
    setprop "$_prop" "$_value" >/dev/null 2>&1
  fi
}

set_prop_if_needed() {
  _prop="$1"
  _value="$2"
  _current="$(getprop "$_prop")"
  [ "$_current" = "$_value" ] && return 0

  if set_prop_raw "$_prop" "$_value"; then
    _verify="$(getprop "$_prop")"
    if [ "$_verify" = "$_value" ]; then
      CHANGED_PROPS=$((CHANGED_PROPS + 1))
      return 0
    fi
  fi

  FAILED_PROPS=$((FAILED_PROPS + 1))
  log_msg "WARN failed to set $_prop expected='$_value' actual='$(getprop "$_prop")'"
  return 1
}

apply_legacy_slot_props() {
  [ "$LEGACY_SLOT_PROPS" = "1" ] || return 0
  _slot=0
  while [ "$_slot" -lt "$SLOT_COUNT" ]; do
    set_prop_if_needed "gsm.sim.operator.numeric.$_slot" "$TARGET_NUMERIC"
    set_prop_if_needed "gsm.sim.operator.iso-country.$_slot" "$TARGET_ISO"
    set_prop_if_needed "gsm.sim.operator.alpha.$_slot" "$TARGET_ALPHA"
    set_prop_if_needed "gsm.operator.numeric.$_slot" "$TARGET_NUMERIC"
    set_prop_if_needed "gsm.operator.iso-country.$_slot" "$TARGET_ISO"
    set_prop_if_needed "gsm.operator.alpha.$_slot" "$TARGET_ALPHA"
    _slot=$((_slot + 1))
  done
}

apply_sim_country_props() {
  _reason="${1:-manual}"
  prepare_desired_values
  CHANGED_PROPS=0
  FAILED_PROPS=0

  # Android 16 TelephonyProperties use comma-separated per-phone lists.
  set_prop_if_needed gsm.sim.operator.numeric "$TARGET_NUMERIC_LIST"
  set_prop_if_needed gsm.sim.operator.iso-country "$TARGET_ISO_LIST"
  set_prop_if_needed gsm.sim.operator.alpha "$TARGET_ALPHA_LIST"
  set_prop_if_needed gsm.operator.numeric "$TARGET_NUMERIC_LIST"
  set_prop_if_needed gsm.operator.iso-country "$TARGET_ISO_LIST"
  set_prop_if_needed gsm.operator.alpha "$TARGET_ALPHA_LIST"

  # Non-standard .0/.1 properties are optional legacy compatibility only.
  apply_legacy_slot_props

  if [ "$CHANGED_PROPS" -gt 0 ] || [ "$FAILED_PROPS" -gt 0 ]; then
    log_msg "Apply reason=$_reason changed=$CHANGED_PROPS failed=$FAILED_PROPS numeric=$TARGET_NUMERIC iso=$TARGET_ISO alpha=$TARGET_ALPHA slots=$SLOT_COUNT"
  fi

  [ "$FAILED_PROPS" -eq 0 ]
}

props_match() {
  prepare_desired_values
  [ "$(getprop gsm.sim.operator.numeric)" = "$TARGET_NUMERIC_LIST" ] || return 1
  [ "$(getprop gsm.sim.operator.iso-country)" = "$TARGET_ISO_LIST" ] || return 1
  [ "$(getprop gsm.sim.operator.alpha)" = "$TARGET_ALPHA_LIST" ] || return 1
  [ "$(getprop gsm.operator.numeric)" = "$TARGET_NUMERIC_LIST" ] || return 1
  [ "$(getprop gsm.operator.iso-country)" = "$TARGET_ISO_LIST" ] || return 1
  [ "$(getprop gsm.operator.alpha)" = "$TARGET_ALPHA_LIST" ] || return 1
  return 0
}

desired_for_prop() {
  _prop="$1"
  prepare_desired_values
  case "$_prop" in
    gsm.sim.operator.numeric|gsm.operator.numeric) printf '%s\n' "$TARGET_NUMERIC_LIST" ;;
    gsm.sim.operator.iso-country|gsm.operator.iso-country) printf '%s\n' "$TARGET_ISO_LIST" ;;
    gsm.sim.operator.alpha|gsm.operator.alpha) printf '%s\n' "$TARGET_ALPHA_LIST" ;;
    *) return 1 ;;
  esac
}

write_system_prop_file() {
  prepare_desired_values
  _tmp="$MODDIR/system.prop.tmp"
  {
    echo "# Generated by SIM Country Spoofer. Loaded early by APatch/Magisk."
    echo "gsm.sim.operator.numeric=$TARGET_NUMERIC_LIST"
    echo "gsm.sim.operator.iso-country=$TARGET_ISO_LIST"
    echo "gsm.sim.operator.alpha=$TARGET_ALPHA_LIST"
    echo "gsm.operator.numeric=$TARGET_NUMERIC_LIST"
    echo "gsm.operator.iso-country=$TARGET_ISO_LIST"
    echo "gsm.operator.alpha=$TARGET_ALPHA_LIST"
  } > "$_tmp" && mv "$_tmp" "$MODDIR/system.prop"
  chmod 0644 "$MODDIR/system.prop" 2>/dev/null
}

write_runtime_state() {
  _mode="$1"
  _tmp="$RUNTIME_STATE.tmp"
  {
    echo "WATCH_MODE='$_mode'"
    echo "SERVICE_PID='$$'"
    echo "ROOT_MANAGER='$(detect_root_manager)'"
    echo "ANDROID_API='$(getprop ro.build.version.sdk)'"
    echo "ROM_VERSION='$(getprop ro.lineage.version)'"
    echo "STARTED_AT='$(date '+%Y-%m-%d %H:%M:%S')'"
  } > "$_tmp" && mv "$_tmp" "$RUNTIME_STATE"
  chmod 0644 "$RUNTIME_STATE" 2>/dev/null
}

wait_for_telephony_ready() {
  _timeout="${1:-45}"
  _elapsed=0
  while [ "$_elapsed" -lt "$_timeout" ]; do
    _sim="$(getprop gsm.sim.operator.numeric)"
    _net="$(getprop gsm.operator.numeric)"
    if [ -n "$_sim$_net" ]; then
      return 0
    fi
    sleep 1
    _elapsed=$((_elapsed + 1))
  done
  return 1
}

apply_with_lock() {
  _reason="$1"
  if mkdir "$LOCKDIR" 2>/dev/null; then
    load_config
    apply_sim_country_props "$_reason"
    rmdir "$LOCKDIR" 2>/dev/null
    return 0
  fi
  return 1
}

watch_property() {
  _watch_prop="$1"
  _rp="$(resetprop_path)"
  [ -n "$_rp" ] || return 1

  while true; do
    load_config
    _expected="$(desired_for_prop "$_watch_prop")"
    _current="$(getprop "$_watch_prop")"

    if [ "$_current" != "$_expected" ]; then
      apply_with_lock "property-change:$_watch_prop"
      sleep 1
      continue
    fi

    # APatch resetprop -w waits until the property differs from the supplied old value.
    "$_rp" -w --timeout 120 "$_watch_prop" "$_expected" >/dev/null 2>&1
  done
}
