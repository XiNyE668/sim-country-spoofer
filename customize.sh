SKIPUNZIP=0

ui_print " "
ui_print "SIM Country Spoofer A16"
ui_print "Android 16 / LineageOS 23.2 / APatch optimized build"
ui_print "Country/carrier presets with automatic MCC/MNC selection"
ui_print "Default target: US / T-Mobile, MCCMNC 310260"
ui_print "Open the module WebUI in APatch after reboot to select country and carrier."
ui_print " "

# Preserve the user's live configuration when upgrading the same module.
OLD_CONFIG="/data/adb/modules/sim_country_spoofer/config.conf"
if [ -f "$OLD_CONFIG" ]; then
  cp -f "$OLD_CONFIG" "$TMPDIR/sim_country_spoofer.config.conf" 2>/dev/null
fi

set_perm "$MODPATH" 0 0 0755
set_perm "$MODPATH/common.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/post-fs-data.sh" 0 0 0755
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/webui.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
set_perm "$MODPATH/system.prop" 0 0 0644
if [ -d "$MODPATH/webroot" ]; then
  set_perm_recursive "$MODPATH/webroot" 0 0 0755 0644
fi

if [ -f "$TMPDIR/sim_country_spoofer.config.conf" ]; then
  cp -f "$TMPDIR/sim_country_spoofer.config.conf" "$MODPATH/config.conf"
  ui_print "Preserved existing config.conf"
fi
set_perm "$MODPATH/config.conf" 0 0 0644
