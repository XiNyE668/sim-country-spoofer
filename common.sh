#!/system/bin/sh

CONFIG="$MODDIR/config.conf"
LOGFILE="$MODDIR/service.log"

log_msg() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') $*" >> "$LOGFILE"
}

load_config() {
  TARGET_MCC=310
  TARGET_MNC=260
  TARGET_ISO=us
  TARGET_ALPHA=T-Mobile
  SLOT_COUNT=2
  REAPPLY_SECONDS=120
  REAPPLY_INTERVAL=5

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

  TARGET_ISO="$(echo "$TARGET_ISO" | tr '[:upper:]' '[:lower:]')"
  case "$TARGET_ISO" in
    [a-z][a-z]) ;;
    *) TARGET_ISO=us ;;
  esac

  case "$SLOT_COUNT" in
    1|2|3|4) ;;
    *) SLOT_COUNT=2 ;;
  esac

  case "$REAPPLY_SECONDS" in
    ''|*[!0-9]*) REAPPLY_SECONDS=120 ;;
  esac

  case "$REAPPLY_INTERVAL" in
    ''|*[!0-9]*) REAPPLY_INTERVAL=5 ;;
  esac

  if [ "$REAPPLY_INTERVAL" -lt 1 ]; then
    REAPPLY_INTERVAL=1
  fi
}

repeat_value() {
  value="$1"
  count=1
  result="$value"

  while [ "$count" -lt "$SLOT_COUNT" ]; do
    result="$result,$value"
    count=$((count + 1))
  done

  echo "$result"
}

set_prop() {
  prop="$1"
  value="$2"

  if command -v resetprop >/dev/null 2>&1; then
    resetprop -n "$prop" "$value" >/dev/null 2>&1
  elif [ -x /data/adb/magisk/resetprop ]; then
    /data/adb/magisk/resetprop -n "$prop" "$value" >/dev/null 2>&1
  else
    setprop "$prop" "$value" >/dev/null 2>&1
  fi
}

apply_sim_country_props() {
  numeric="${TARGET_MCC}${TARGET_MNC}"
  numeric_list="$(repeat_value "$numeric")"
  iso_list="$(repeat_value "$TARGET_ISO")"
  alpha_list="$(repeat_value "$TARGET_ALPHA")"

  set_prop gsm.sim.operator.numeric "$numeric_list"
  set_prop gsm.sim.operator.iso-country "$iso_list"
  set_prop gsm.sim.operator.alpha "$alpha_list"

  set_prop gsm.operator.numeric "$numeric_list"
  set_prop gsm.operator.iso-country "$iso_list"
  set_prop gsm.operator.alpha "$alpha_list"

  slot=0
  while [ "$slot" -lt "$SLOT_COUNT" ]; do
    set_prop "gsm.sim.operator.numeric.$slot" "$numeric"
    set_prop "gsm.sim.operator.iso-country.$slot" "$TARGET_ISO"
    set_prop "gsm.sim.operator.alpha.$slot" "$TARGET_ALPHA"

    set_prop "gsm.operator.numeric.$slot" "$numeric"
    set_prop "gsm.operator.iso-country.$slot" "$TARGET_ISO"
    set_prop "gsm.operator.alpha.$slot" "$TARGET_ALPHA"

    slot=$((slot + 1))
  done

  log_msg "Applied SIM/operator props: numeric=$numeric iso=$TARGET_ISO alpha=$TARGET_ALPHA slots=$SLOT_COUNT"
}
