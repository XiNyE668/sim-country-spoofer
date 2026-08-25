# SIM Country Spoofer A16

面向 **APatch + Android 16 / LineageOS 23.2** 优化的 SIM / 运营商国家属性模块，同时保留 Magisk / KernelSU 的回退兼容路径。

## Android 16 版改动

- 修复 APatch WebUI 只返回命令最后一行导致状态页面仅显示“当前运营商 ISO”的问题。
- WebUI 后端改为单行 JSON 状态，APatch 同步 `exec()` 可以一次获得完整结果。
- 使用 Android 16 TelephonyProperties 的标准逗号分隔属性，不再默认依赖非标准 `.0/.1` 属性。
- 新增 `system.prop`，让 APatch / Magisk 在开机早期先加载目标属性。
- `post-fs-data.sh` 再进行一次早期同步。
- `service.sh` 等待 Android Telephony 完成第一轮初始化后再强制应用。
- APatch 下优先使用原生 `resetprop -w` 监听属性变化。Telephony、RIL、飞行模式或 SIM 重新初始化覆盖属性后，会立即修复，而不是旧版“每 5 秒重复 120 秒后停止”。
- 非 APatch / 不支持 `resetprop -w` 时使用低开销回退轮询。
- 只在属性实际不同的时候写入，减少无意义 resetprop 操作和日志。
- WebUI 增加 Root 管理器、Android API、LineageOS 版本、resetprop、监控模式和六个核心属性的实时 OK/DIFF 状态。

## 默认配置

```text
MCC: 310
MNC: 260
ISO: us
运营商: T-Mobile
SIM 槽数量: 2
回退检查间隔: 15 秒
```

配置文件：

```text
/data/adb/modules/sim_country_spoofer/config.conf
```

## 核心属性

Android 16 / LineageOS 23.2 默认维护以下标准 Telephony 属性：

```text
gsm.sim.operator.numeric
gsm.sim.operator.iso-country
gsm.sim.operator.alpha
gsm.operator.numeric
gsm.operator.iso-country
gsm.operator.alpha
```

双卡配置会使用标准的逗号分隔形式，例如：

```text
gsm.sim.operator.numeric=310260,310260
gsm.sim.operator.iso-country=us,us
```

旧版使用的 `gsm.*.0` / `gsm.*.1` 形式在本版默认关闭；如确实有特殊 ROM 需要，可在 `config.conf` 将 `LEGACY_SLOT_PROPS=1`。

## APatch 运行流程

```text
system.prop 早期加载
        ↓
post-fs-data 再同步一次
        ↓
等待 sys.boot_completed
        ↓
等待 Telephony 第一轮 SIM/网络属性
        ↓
应用目标 MCC/MNC/ISO/运营商
        ↓
APatch resetprop -w 事件监听
        ↓
属性被 Telephony/RIL 改回时立即修复
```

APatch 当前原生提供 Magisk-compatible `resetprop`，本版会自动检测其 `--wait` 能力；不支持时自动回退到轮询模式。

## 检查

在 APatch 中打开模块 WebUI，六个 Telephony 属性应显示 `OK`。

也可以使用：

```sh
getprop gsm.sim.operator.numeric
getprop gsm.sim.operator.iso-country
getprop gsm.sim.operator.alpha
getprop gsm.operator.numeric
getprop gsm.operator.iso-country
getprop gsm.operator.alpha
```

或者点击 APatch 模块的 Action 查看诊断信息。

## 边界

这是 **system property 层** 的 SIM / 运营商国家伪装。它不会修改真实 IMSI、ICCID、eSIM Profile、SubscriptionInfo、CarrierConfig、基带身份、IP、GPS 或账号地区。应用如果读取这些其它来源，仍可能得到真实信息。

## 许可证

MIT
