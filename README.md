# SIM Country Spoofer A16

An Android system-property SIM/operator country spoofer optimized for **APatch + Android 16 / LineageOS 23.2**, with fallback compatibility for Magisk/KernelSU environments.

## Android 16 / APatch changes

- Fixes the APatch WebUI issue where synchronous `exec()` only returns the command's final stdout line. Status is now emitted as one JSON line.
- Uses the canonical Android 16 TelephonyProperties keys and comma-separated per-phone values.
- Adds `system.prop` for early APatch/Magisk property loading.
- Re-applies once in `post-fs-data.sh`, then waits for the first Android Telephony snapshot after boot before enforcing the configured identity.
- On APatch, uses native `resetprop -w` property-change waiting. If Telephony/RIL overwrites a value after boot, the module repairs it instead of stopping after a fixed 120-second window.
- Falls back to a low-overhead polling watcher when `resetprop -w` is unavailable.
- Writes only properties whose current value differs from the configured value.
- WebUI displays root manager, Android API, LineageOS version, watcher mode, service state, and live OK/DIFF status for the six canonical properties.

## Canonical properties

```text
gsm.sim.operator.numeric
gsm.sim.operator.iso-country
gsm.sim.operator.alpha
gsm.operator.numeric
gsm.operator.iso-country
gsm.operator.alpha
```

For two phone slots, values use Android's normal comma-separated representation, for example:

```text
gsm.sim.operator.numeric=310260,310260
gsm.sim.operator.iso-country=us,us
```

Legacy `.0/.1` property names are disabled by default and can be enabled with `LEGACY_SLOT_PROPS=1` only for ROM-specific compatibility.

## Default configuration

```text
MCC=310
MNC=260
ISO=us
Carrier=T-Mobile
Slots=2
Fallback watcher interval=15 seconds
```

Configuration file:

```text
/data/adb/modules/sim_country_spoofer/config.conf
```

## Runtime flow on APatch

```text
system.prop early load
        ↓
post-fs-data synchronization
        ↓
wait for sys.boot_completed
        ↓
wait for first Telephony SIM/network snapshot
        ↓
enforce configured properties
        ↓
APatch resetprop -w event watchers
        ↓
repair if Telephony/RIL changes a property
```

## Verify

Open the module WebUI from APatch. All six Telephony property rows should show `OK`.

Or check manually:

```sh
getprop gsm.sim.operator.numeric
getprop gsm.sim.operator.iso-country
getprop gsm.sim.operator.alpha
getprop gsm.operator.numeric
getprop gsm.operator.iso-country
getprop gsm.operator.alpha
```

## Scope

This is a **system-property-level** spoofer. It does not modify the real IMSI, ICCID, eSIM profile, SubscriptionInfo, CarrierConfig, modem identity, IP address, GPS location, or account region.

## License

MIT
