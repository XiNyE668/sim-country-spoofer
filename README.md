# SIM Country Spoofer

SIM Country Spoofer is a Magisk / Magisk Alpha module that rewrites common Android SIM and operator country properties at boot. It also includes a KsuWebUIStandalone-compatible visual interface for editing MCC, MNC, ISO country, and carrier name.

## Features

- Spoofs common SIM/operator properties such as `gsm.sim.operator.numeric` and `gsm.sim.operator.iso-country`
- Applies values during boot and reapplies them shortly after Android finishes starting
- Supports dual-SIM style comma-separated properties
- Includes a KsuWebUIStandalone WebUI at `webroot/index.html`
- Can still be configured manually through `config.conf`

## Default Identity

```text
MCC/MNC: 310260
ISO: us
Carrier: T-Mobile
Slots: 2
```

## Install

1. Download the ZIP from GitHub Releases.
2. Install it in Magisk / Magisk Alpha.
3. Reboot.
4. Open KsuWebUIStandalone and refresh the module list.

## Manual Config

Edit:

```text
/data/adb/modules/sim_country_spoofer/config.conf
```

Example for Japan / NTT DOCOMO:

```sh
TARGET_MCC=440
TARGET_MNC=10
TARGET_ISO=jp
TARGET_ALPHA="NTT DOCOMO"
SLOT_COUNT=2
REAPPLY_SECONDS=120
REAPPLY_INTERVAL=5
```

Then reboot, or apply immediately as root:

```sh
sh /data/adb/modules/sim_country_spoofer/service.sh
```

## Check Values

```sh
getprop gsm.sim.operator.numeric
getprop gsm.sim.operator.iso-country
getprop gsm.operator.numeric
getprop gsm.operator.iso-country
```

## Notes

Some apps read Android system properties, while others check telephony framework responses, IP geolocation, GPS, account region, Google services, Wi-Fi, or real subscription data. This module only changes common SIM/operator properties, so it may not affect every region check.

Use this module only where it is lawful and consistent with the services you use.

## License

MIT
