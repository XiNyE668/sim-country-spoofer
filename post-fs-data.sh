#!/system/bin/sh

MODDIR=${0%/*}

. "$MODDIR/common.sh"

load_config
apply_sim_country_props
