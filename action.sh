#!/system/bin/sh

MODDIR=${0%/*}

echo "SIM Country Spoofer"
echo "Module path: $MODDIR"
echo

if [ -d "$MODDIR/webroot" ] && [ -f "$MODDIR/webroot/index.html" ]; then
  echo "KsuWebUI files: OK"
else
  echo "KsuWebUI files: missing"
  echo "Expected: $MODDIR/webroot/index.html"
fi

echo
echo "Current config:"
if [ -f "$MODDIR/config.conf" ]; then
  cat "$MODDIR/config.conf"
else
  echo "config.conf missing"
fi

echo
echo "Current properties:"
getprop gsm.sim.operator.numeric
getprop gsm.sim.operator.iso-country
getprop gsm.operator.numeric
getprop gsm.operator.iso-country

echo
echo "Open KsuWebUI and refresh the module list after reboot."
