# SIM Country Spoofer A16

这是针对 Android 16 / LineageOS 23.2 / APatch 优化的 SIM / 运营商国家属性模块。

## 主要改进

- 使用 Android 16 当前标准的逗号分隔 TelephonyProperties：
  - `gsm.sim.operator.numeric`
  - `gsm.sim.operator.iso-country`
  - `gsm.sim.operator.alpha`
  - `gsm.operator.numeric`
  - `gsm.operator.iso-country`
  - `gsm.operator.alpha`
- APatch 下优先使用原生 `resetprop -w` 事件监听；Telephony/RIL 覆盖属性后自动恢复，不再只在开机后固定 120 秒重复写入。
- WebUI 使用单行 JSON 返回完整状态，修复 APatch 同步 `exec()` 只返回 stdout 最后一行时，界面只有最后一个属性可见的问题。
- WebUI 国家与运营商改为下拉预设，MCC/MNC 自动联动，不需要手工填写。
- 升级模块时保留现有 `config.conf`。
- 显示 Root 管理器、Android API、LineageOS 版本、resetprop 路径、监控模式和服务状态。

## 国家 / 运营商预设

- `us`：T-Mobile、AT&T、Verizon
- `au`：Telstra、Optus、Vodafone AU
- `ca`：Rogers、Bell、TELUS
- `de`：Telekom DE、Vodafone DE、O2 Germany
- `jp`：NTT DOCOMO、au (KDDI)、SoftBank、Rakuten Mobile
- `kr`：SK Telecom、KT、LG U+
- `uk`：EE、O2 UK、Vodafone UK、Three UK

英国在界面中保留用户熟悉的 `uk` 标识，但 Android Telephony 属性实际写入标准 ISO-3166 alpha-2 值 `gb`。

## APatch 运行机制

1. `system.prop` 在早期开机阶段提供初始属性。
2. `post-fs-data.sh` 同步当前配置并执行一次属性修复。
3. `service.sh` 等待系统启动和 Telephony 第一轮初始化。
4. APatch 支持 `resetprop -w` 时，为六个核心 Telephony 属性建立事件监听。
5. 任一属性被 Telephony/RIL 改动后，仅在需要时重新应用目标值。
6. 如果运行环境没有 property wait 能力，则使用低频回退轮询。

## 默认配置

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

WebUI 会根据国家和运营商自动生成前四项。

## 安装

在 APatch 管理器中安装模块 ZIP，然后重启。

升级同一个模块时，安装脚本会尽量保留已有的：

```text
/data/adb/modules/sim_country_spoofer/config.conf
```

## 检查状态

可以直接从 APatch 打开模块 WebUI，也可以执行：

```sh
sh /data/adb/modules/sim_country_spoofer/action.sh
```

或：

```sh
getprop gsm.sim.operator.numeric
getprop gsm.sim.operator.iso-country
getprop gsm.sim.operator.alpha
getprop gsm.operator.numeric
getprop gsm.operator.iso-country
getprop gsm.operator.alpha
```

## 说明

这是 property-level spoof。它不会修改真实 IMSI、ICCID、SubscriptionInfo、CarrierConfig、eSIM profile 或基带身份信息。应用如果使用这些来源、IP、GPS、账户地区或服务端地区信息，仍可能识别真实位置。

## License

MIT
