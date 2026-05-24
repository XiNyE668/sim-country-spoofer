#!/system/bin/sh

MODDIR=${0%/*}

. "$MODDIR/common.sh"

load_config

until [ "$(getprop sys.boot_completed)" = "1" ]; do
  sleep 2
done

elapsed=0
while [ "$elapsed" -le "$REAPPLY_SECONDS" ]; do
  apply_sim_country_props
  sleep "$REAPPLY_INTERVAL"
  elapsed=$((elapsed + REAPPLY_INTERVAL))
done

log_msg "Finished boot reapply window."
