#!/system/bin/sh

MODDIR=${0%/*}
. "$MODDIR/common.sh"

load_config
# Keep the next boot's APatch/Magisk early property file synchronized with config.conf.
write_system_prop_file
# Also enforce once during the current early boot. Telephony may overwrite later; service.sh watches that.
apply_sim_country_props "post-fs-data"
