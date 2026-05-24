# SIM Country Spoofer

SIM Country Spoofer 是一个 Magisk / Magisk Alpha 模块，用来在开机时修改常见的 SIM / 运营商国家相关系统属性，并提供兼容 KsuWebUIStandalone 的可视化配置界面。

## 功能

- 修改常见 SIM / 运营商属性，例如 `gsm.sim.operator.numeric`、`gsm.sim.operator.iso-country`
- 开机自动应用，并在 Android 启动完成后短时间重复应用，减少被系统电话服务覆盖的概率
- 支持双 SIM 形式的逗号分隔属性
- 内置 `webroot/index.html`，可在 KsuWebUIStandalone 打开界面
- 仍然支持手动编辑 `config.conf`

## 默认配置

```text
MCC/MNC: 310260
ISO: us
运营商: T-Mobile
SIM 槽数量: 2
```

## 安装

1. 从 GitHub Releases 下载 ZIP。
2. 在 Magisk / Magisk Alpha 中安装。
3. 重启手机。
4. 打开 KsuWebUIStandalone，刷新模块列表。

## 手动配置

编辑：

```text
/data/adb/modules/sim_country_spoofer/config.conf
```

日本 NTT DOCOMO 示例：

```sh
TARGET_MCC=440
TARGET_MNC=10
TARGET_ISO=jp
TARGET_ALPHA="NTT DOCOMO"
SLOT_COUNT=2
REAPPLY_SECONDS=120
REAPPLY_INTERVAL=5
```

改完后重启，或在 root shell 中立即应用：

```sh
sh /data/adb/modules/sim_country_spoofer/service.sh
```

## 检查是否生效

```sh
getprop gsm.sim.operator.numeric
getprop gsm.sim.operator.iso-country
getprop gsm.operator.numeric
getprop gsm.operator.iso-country
```

## 注意

不同应用检测地区的方式不同。这个模块只修改常见的 SIM / 运营商属性；如果应用还检查 IP、GPS、账号地区、Google 服务、真实订阅信息或电话框架返回值，它可能仍然识别出真实地区。

请只在当地法律和相关服务规则允许的范围内使用。

## 许可证

MIT
