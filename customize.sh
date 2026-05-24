SKIPUNZIP=0

ui_print " "
ui_print "SIM Country Spoofer"
ui_print "Default target: US / T-Mobile, MCCMNC 310260"
ui_print "Open this module in KsuWebUI to change MCC/MNC/ISO from a visual interface."
ui_print "You can also edit /data/adb/modules/sim_country_spoofer/config.conf manually."
ui_print " "

set_perm "$MODPATH" 0 0 0755
set_perm "$MODPATH/common.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/post-fs-data.sh" 0 0 0755
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/webui.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
if [ -d "$MODPATH/webroot" ]; then
  set_perm_recursive "$MODPATH/webroot" 0 0 0755 0644
fi
