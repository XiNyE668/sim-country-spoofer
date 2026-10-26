#!/system/bin/sh
ui_print '- XiNyE WebRTC Network Guard V1'
ui_print '- Dedicated UDP STUN filtering; does NOT edit singbox4root'
ui_print '- KernelSU Action toggles filtering ON/OFF'
[ -e "$MODPATH/guard.enabled" ] || touch "$MODPATH/guard.enabled"
set_perm_recursive "$MODPATH" 0 0 0755 0644
for x in common.sh service.sh action.sh uninstall.sh customize.sh; do
  set_perm "$MODPATH/$x" 0 0 0755
done
