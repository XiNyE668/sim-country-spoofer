# SIM Country Spoofer A16

Android 16 / LineageOS 23.2 / APatch optimized SIM and operator country-property spoofer.

## Highlights

- Uses the canonical Android 16 comma-separated TelephonyProperties:
  - `gsm.sim.operator.numeric`
  - `gsm.sim.operator.iso-country`
  - `gsm.sim.operator.alpha`
  - `gsm.operator.numeric`
  - `gsm.operator.iso-country`
  - `gsm.operator.alpha`
- Uses APatch native `resetprop -w` event watching when available, so Telephony/RIL changes are repaired after boot instead of only during a fixed 120-second window.
- Returns WebUI status as one-line JSON, fixing APatch synchronous `exec()` environments that only expose the final stdout line.
- Country and carrier are selected from presets; MCC/MNC update automatically.
- Preserves existing `config.conf` during module upgrades.
- Shows root manager, Android API, LineageOS version, resetprop path, watcher mode, and service state.

## Country / carrier presets

- `us`: T-Mobile, AT&T, Verizon
- `au`: Telstra, Optus, Vodafone AU
- `ca`: Rogers, Bell, TELUS
- `de`: Telekom DE, Vodafone DE, O2 Germany
- `jp`: NTT DOCOMO, au (KDDI), SoftBank, Rakuten Mobile
- `kr`: SK Telecom, KT, LG U+
- `uk`: EE, O2 UK, Vodafone UK, Three UK

The UI keeps `uk` as a friendly selector key, while Android Telephony receives the ISO-3166 alpha-2 value `gb`.

## Runtime model

1. `system.prop` provides early boot values.
2. `post-fs-data.sh` synchronizes the current configuration and repairs properties once.
3. `service.sh` waits for Android boot and the initial Telephony snapshot.
4. On APatch with property-wait support, six event watchers wait for Telephony property changes.
5. A changed property triggers a guarded repair of the configured identity.
6. Other root environments fall back to low-frequency polling.

## Default configuration

```sh
TARGET_MCC=310
TARGET_MNC=260
TARGET_ISO=us
TARGET_ALPHA='T-Mobile'
SLOT_COUNT=2
WATCH_INTERVAL=15
TELEPHONY_SETTLE_SECONDS=8
LEGACY_SLOT_PROPS=0
```

The WebUI generates the first four values from the selected country/carrier preset.

## Install

Install the module ZIP in APatch Manager and reboot.

For upgrades, the installer attempts to preserve:

```text
/data/adb/modules/sim_country_spoofer/config.conf
```

## Status

Open the module WebUI in APatch, or run:

```sh
sh /data/adb/modules/sim_country_spoofer/action.sh
```

You can also inspect the live properties directly:

```sh
getprop gsm.sim.operator.numeric
getprop gsm.sim.operator.iso-country
getprop gsm.sim.operator.alpha
getprop gsm.operator.numeric
getprop gsm.operator.iso-country
getprop gsm.operator.alpha
```

## Scope

This is property-level spoofing. It does not alter the real IMSI, ICCID, SubscriptionInfo, CarrierConfig, eSIM profile, or modem identity. Apps using those sources, IP, GPS, account region, or server-side signals may still identify the real region.

## License

MIT
