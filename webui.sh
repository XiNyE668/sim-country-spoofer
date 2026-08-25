#!/system/bin/sh

MODDIR=${0%/*}
. "$MODDIR/common.sh"

quote_value() {
  printf "'"
  printf "%s" "$1" | sed "s/'/'\\\\''/g"
  printf "'"
}

write_config() {
  _tmp="$CONFIG.tmp"
  {
    echo "# Target carrier identity. Managed by the module WebUI."
    echo "TARGET_MCC=$TARGET_MCC"
    echo "TARGET_MNC=$TARGET_MNC"
    echo "TARGET_ISO=$TARGET_ISO"
    printf "TARGET_ALPHA="
    quote_value "$TARGET_ALPHA"
    echo
    echo
    echo "# Android 16 TelephonyProperties publish per-phone values as comma-separated lists."
    echo "SLOT_COUNT=$SLOT_COUNT"
    echo
    echo "# Used only when APatch resetprop event watching is unavailable."
    echo "WATCH_INTERVAL=$WATCH_INTERVAL"
    echo
    echo "# Delay after the first post-boot telephony snapshot before enforcement."
    echo "TELEPHONY_SETTLE_SECONDS=$TELEPHONY_SETTLE_SECONDS"
    echo
    echo "# 0 = canonical Android 16 properties only; 1 = also publish legacy .0/.1 properties."
    echo "LEGACY_SLOT_PROPS=$LEGACY_SLOT_PROPS"
  } > "$_tmp" && mv "$_tmp" "$CONFIG"
  chmod 0644 "$CONFIG"
}

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

json_string() {
  printf '"%s"' "$(json_escape "$1")"
}

status_json() {
  load_config
  prepare_desired_values

  WATCH_MODE="not-running"
  SERVICE_PID=""
  STATE_ROOT_MANAGER=""
  if [ -f "$RUNTIME_STATE" ]; then
    . "$RUNTIME_STATE" 2>/dev/null
  fi

  if [ -n "$SERVICE_PID" ] && kill -0 "$SERVICE_PID" 2>/dev/null; then
    SERVICE_STATE="running"
  else
    SERVICE_STATE="not-running"
  fi

  _root="$(detect_root_manager)"
  _api="$(getprop ro.build.version.sdk)"
  _rom="$(getprop ro.lineage.version)"
  _rp="$(resetprop_path)"
  [ -n "$_rp" ] || _rp="setprop-fallback"

  _sim_numeric="$(getprop gsm.sim.operator.numeric)"
  _sim_iso="$(getprop gsm.sim.operator.iso-country)"
  _sim_alpha="$(getprop gsm.sim.operator.alpha)"
  _op_numeric="$(getprop gsm.operator.numeric)"
  _op_iso="$(getprop gsm.operator.iso-country)"
  _op_alpha="$(getprop gsm.operator.alpha)"

  printf '{'
  printf '"TARGET_MCC":'; json_string "$TARGET_MCC"; printf ','
  printf '"TARGET_MNC":'; json_string "$TARGET_MNC"; printf ','
  printf '"TARGET_ISO":'; json_string "$TARGET_ISO"; printf ','
  printf '"TARGET_ALPHA":'; json_string "$TARGET_ALPHA"; printf ','
  printf '"SLOT_COUNT":'; json_string "$SLOT_COUNT"; printf ','
  printf '"WATCH_INTERVAL":'; json_string "$WATCH_INTERVAL"; printf ','
  printf '"EXPECTED_NUMERIC":'; json_string "$TARGET_NUMERIC_LIST"; printf ','
  printf '"EXPECTED_ISO":'; json_string "$TARGET_ISO_LIST"; printf ','
  printf '"EXPECTED_ALPHA":'; json_string "$TARGET_ALPHA_LIST"; printf ','
  printf '"ROOT_MANAGER":'; json_string "$_root"; printf ','
  printf '"ANDROID_API":'; json_string "$_api"; printf ','
  printf '"ROM_VERSION":'; json_string "$_rom"; printf ','
  printf '"RESETPROP":'; json_string "$_rp"; printf ','
  printf '"WATCH_MODE":'; json_string "$WATCH_MODE"; printf ','
  printf '"SERVICE_STATE":'; json_string "$SERVICE_STATE"; printf ','
  printf '"PROP_SIM_NUMERIC":'; json_string "$_sim_numeric"; printf ','
  printf '"PROP_SIM_ISO":'; json_string "$_sim_iso"; printf ','
  printf '"PROP_SIM_ALPHA":'; json_string "$_sim_alpha"; printf ','
  printf '"PROP_OPERATOR_NUMERIC":'; json_string "$_op_numeric"; printf ','
  printf '"PROP_OPERATOR_ISO":'; json_string "$_op_iso"; printf ','
  printf '"PROP_OPERATOR_ALPHA":'; json_string "$_op_alpha"
  printf '}\n'
}

cmd="$1"
shift

case "$cmd" in
  read)
    status_json
    ;;
  apply)
    TARGET_MCC="$1"
    TARGET_MNC="$2"
    TARGET_ISO="$3"
    TARGET_ALPHA="$4"
    SLOT_COUNT="$5"
    WATCH_INTERVAL="$6"
    TELEPHONY_SETTLE_SECONDS=8
    LEGACY_SLOT_PROPS=0
    normalize_config
    write_config
    write_system_prop_file
    apply_sim_country_props "webui"
    status_json
    ;;
  *)
    echo '{"error":"Unknown command"}'
    exit 1
    ;;
esac
