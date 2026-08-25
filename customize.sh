SKIPUNZIP=0

ui_print " "
ui_print "SIM Country Spoofer A16"
ui_print "Optimized for APatch + Android 16 / LineageOS 23.2"
ui_print "Default target: US / T-Mobile, MCCMNC 310260"
ui_print "WebUI: APatch module page -> Open WebUI"
ui_print "Config: /data/adb/modules/sim_country_spoofer/config.conf"
ui_print " "

set_perm "$MODPATH" 0 0 0755
set_perm "$MODPATH/common.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/post-fs-data.sh" 0 0 0755
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/webui.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
if [ -f "$MODPATH/system.prop" ]; then
  set_perm "$MODPATH/system.prop" 0 0 0644
fi
if [ -d "$MODPATH/webroot" ]; then
  set_perm_recursive "$MODPATH/webroot" 0 0 0755 0644
fi
