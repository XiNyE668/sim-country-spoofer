#!/system/bin/sh

MODDIR=${0%/*}
LOGFILE="$MODDIR/service.log"

echo "$(date '+%Y-%m-%d %H:%M:%S') Module removed. Reboot to let Android restore live SIM/operator properties." >> "$LOGFILE"
