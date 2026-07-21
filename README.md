<div align="center">

# MultiYggdrasil

[![GitHub Release](https://img.shields.io/github/release/QiuShui1012/MultiYggdrasil.svg)](https://github.com/QiuShui1012/MultiYggdrasil/releases/)
[![Github License](https://img.shields.io/github/license/QiuShui1012/MultiYggdrasil)](https://github.com/QiuShui1012/MultiYggdrasil/blob/neoforge/LICENSE.txt)
[![QQ Group](https://img.shields.io/badge/QQ%20group-108917413-yellow)](https://qm.qq.com/q/XJGuYx9W6u)

**简体中文** | [English](README.en.md)

</div>

## 概述

MultiYggdrasil 是一个运行于 Forge | Fabric | NeoForge 的模组。  
它允许您设置多个Yggdrasil API来源，包括正版和其它外置登录。  
配置设计参考了[MultiLogin](https://github.com/CaaMoe/MultiLogin)，部分代码来自[authlib-injector](https://github.com/yushijinhun/authlib-injector/)，遵循APGL-3.0版权。  
~~缝合怪~~

## 安装

与其它大部分模组一致，仅需三步：
1. 从 [CurseForge](https://curseforge.com/minecraft/mc-mods/multiyggdrasil) | [Modrinth](https://modrinth.com/mod/multiyggdrasil/) 下载模组
2. 放入 `/mods` 文件夹
3. 启动游戏

## 配置

一个模板：
```ini
# 该配置路径位于 config/multi-yggdrasil.ini
[SomeRandomMirror]                     # *名称，可随意设置，无影响
type = OFFICIAL                        # *类型，目前支持“OFFICIAL”和“BLESSING_SKIN”
authHost = https://a.random.mirror     # “OFFICIAL”类型的特定值，需求见下表
accountsHost = https://a.random.mirror # “OFFICIAL”类型的特定值，需求见下表
sessionHost = https://a.random.mirror  # “OFFICIAL”类型的特定值，需求见下表
servicesHost = https://a.random.mirror # “OFFICIAL”类型的特定值，需求见下表
profilesHost = https://a.random.mirror # “OFFICIAL”类型的特定值，需求见下表
ordinal = 0                            # *序号，决定了该来源的使用顺序

[MojangOfficial]
type = OFFICIAL                        # “OFFICIAL”类型可以没有Host属性值，此时会使用官方API
ordinal = 1

[LittleSkin]
type = BLESSING_SKIN                   # ↙ *“BLESSING_SKIN”类型的特定值
apiRoot = https://littleskin.cn/api/yggdrasil/  
ordinal = 2
```

“OFFICIAL”类型的特定值的必需与否与MC版本有关，详见下表：

| authlib       | Minecraft     | authHost | accountsHost | sessionHost | servicesHost | profilesHost |
|:--------------|:--------------|:--------:|:------------:|:-----------:|:------------:|:------------:|
| 1.5.25~1.6.25 | 1.14.4~1.16.3 |    ✅     |      ✅       |      ✅      |      ❌       |      ❌       |
| 2.1.28~4.0.43 | 1.16.4~1.20.1 |    ✅     |      ✅       |      ✅      |      ✅       |      ❌       |
| 5.0.47        | 1.20.2        |    ❌     |      ✅       |      ✅      |      ✅       |      ❌       |
| 6.0.52~6.0.58 | 1.20.3~1.21.8 |    ❌     |      ❌       |      ✅      |      ✅       |      ❌       |
| 7.0.61~9.0.75 | 1.21.9~26.2   |    ❌     |      ❌       |      ✅      |      ✅       |      ✅       |


## 版本支持情况
| Minecraft     | Forge | Fabric | NeoForge |
|:--------------|:-----:|:------:|:--------:|
| 1.14.4-       |   ❌   |   ❌    |    ❌     |
| 1.14.4~1.19.4 |   ✅   |   ✅    |    ❌     |
| 1.20.1~26.2   |   ✅   |   ✅    |    ✅     |

