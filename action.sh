#!/system/bin/sh

MODDIR=${0%/*}
. "$MODDIR/common.sh"

load_config
prepare_desired_values

printf '%s\n' "SIM Country Spoofer A16"
printf '%s\n' "Root manager : $(detect_root_manager)"
printf '%s\n' "Android API  : $(getprop ro.build.version.sdk)"
printf '%s\n' "LineageOS    : $(getprop ro.lineage.version)"
printf '%s\n' "resetprop    : $(resetprop_path)"
printf '%s\n' "Target       : $TARGET_NUMERIC / $TARGET_ISO / $TARGET_ALPHA"
printf '%s\n' "Slots        : $SLOT_COUNT"
echo

show_prop() {
  _prop="$1"
  _expected="$2"
  _current="$(getprop "$_prop")"
  if [ "$_current" = "$_expected" ]; then
    _state="OK"
  else
    _state="DIFF"
  fi
  printf '%-34s %s [%s]\n' "$_prop" "${_current:--}" "$_state"
}

show_prop gsm.sim.operator.numeric "$TARGET_NUMERIC_LIST"
show_prop gsm.sim.operator.iso-country "$TARGET_ISO_LIST"
show_prop gsm.sim.operator.alpha "$TARGET_ALPHA_LIST"
show_prop gsm.operator.numeric "$TARGET_NUMERIC_LIST"
show_prop gsm.operator.iso-country "$TARGET_ISO_LIST"
show_prop gsm.operator.alpha "$TARGET_ALPHA_LIST"

echo
if [ -f "$RUNTIME_STATE" ]; then
  echo "Runtime state:"
  cat "$RUNTIME_STATE"
fi
