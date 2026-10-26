#!/system/bin/sh
MODDIR="${0%/*}"
. "$MODDIR/common.sh"
(
  sleep 16
  while [ -d "$MODDIR" ]; do
    if [ -e "$MODDIR/disable" ]; then
      remove_rules
      break
    fi
    ensure_rules
    sleep 45
  done
) >/dev/null 2>&1 &
