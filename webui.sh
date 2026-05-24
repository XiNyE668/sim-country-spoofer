#!/system/bin/sh

MODDIR=${0%/*}

. "$MODDIR/common.sh"

quote_value() {
  printf "'"
  printf "%s" "$1" | sed "s/'/'\\\\''/g"
  printf "'"
}

write_config() {
  tmp="$CONFIG.tmp"

  {
    echo "# Target carrier identity."
    echo "# Managed by KsuWebUI. You can still edit this file manually."
    echo "TARGET_MCC=$TARGET_MCC"
    echo "TARGET_MNC=$TARGET_MNC"
    echo "TARGET_ISO=$TARGET_ISO"
    printf "TARGET_ALPHA="
    quote_value "$TARGET_ALPHA"
    echo
    echo
    echo "# Number of SIM slots to publish in comma-separated telephony properties."
    echo "SLOT_COUNT=$SLOT_COUNT"
    echo
    echo "# How long service.sh should keep reapplying values after boot."
    echo "REAPPLY_SECONDS=$REAPPLY_SECONDS"
    echo "REAPPLY_INTERVAL=$REAPPLY_INTERVAL"
  } > "$tmp"

  mv "$tmp" "$CONFIG"
  chmod 0644 "$CONFIG"
}

print_config() {
  echo "TARGET_MCC=$TARGET_MCC"
  echo "TARGET_MNC=$TARGET_MNC"
  echo "TARGET_ISO=$TARGET_ISO"
  echo "TARGET_ALPHA=$TARGET_ALPHA"
  echo "SLOT_COUNT=$SLOT_COUNT"
  echo "REAPPLY_SECONDS=$REAPPLY_SECONDS"
  echo "REAPPLY_INTERVAL=$REAPPLY_INTERVAL"
}

print_status() {
  print_config
  echo "PROP_SIM_NUMERIC=$(getprop gsm.sim.operator.numeric)"
  echo "PROP_SIM_ISO=$(getprop gsm.sim.operator.iso-country)"
  echo "PROP_OPERATOR_NUMERIC=$(getprop gsm.operator.numeric)"
  echo "PROP_OPERATOR_ISO=$(getprop gsm.operator.iso-country)"
}

cmd="$1"
shift

case "$cmd" in
  read)
    load_config
    print_status
    ;;
  apply)
    TARGET_MCC="$1"
    TARGET_MNC="$2"
    TARGET_ISO="$3"
    TARGET_ALPHA="$4"
    SLOT_COUNT="$5"
    REAPPLY_SECONDS="$6"
    REAPPLY_INTERVAL="$7"
    normalize_config
    write_config
    apply_sim_country_props
    echo "OK=1"
    print_status
    ;;
  *)
    echo "ERR=Unknown command"
    exit 1
    ;;
esac
