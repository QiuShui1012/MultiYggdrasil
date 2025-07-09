<div align="center">

# MultiYggdrasil

[![GitHub Release](https://img.shields.io/github/release/QiuShui1012/MultiYggdrasil.svg)](https://github.com/QiuShui1012/MultiYggdrasil/releases/)
[![GitHub License](https://img.shields.io/github/license/QiuShui1012/MultiYggdrasil?style=flat-square)](https://github.com/QiuShui1012/MultiYggdrasil/blob/master/LICENSE)
[![QQ Group](https://img.shields.io/badge/QQ%20group-108917413-yellow?style=flat-square)](https://qm.qq.com/q/XJGuYx9W6u)

**简体中文** | [English](README.en.md)

</div>

## 概述

MultiYggdrasil 是一个运行于 ~~Forge | Fabric |~~ NeoForge 的仅服务端模组，
这意味着它在客户端使用局域网联机运行时某些功能可能无法正常运行，如[MC-52974](https://bugs.mojang.com/browse/MC/issues/MC-52974)。  
它允许服务器设置多个Yggdrasil API来源，包括正版和其它外置登录。  
配置设计参考了[MultiLogin](https://github.com/CaaMoe/MultiLogin)，
部分代码来自[authlib-injector](https://github.com/yushijinhun/authlib-injector/)，遵循APGL-3.0版权。  
~~缝合怪~~

## 安装

Java需求跟随Minecraft版本，无需安装 `authlib-injector` ，没有任何前置模组，也不需要添加和更改 `JVM` 参数

与其它大部分模组一致，仅需三步：
1. [下载](https://github.com/QiuShui1012/MultiYggdrasil/releases/latest) 模组
2. 丢进 mods
3. 启动服务器

## 配置

一个模板：
```toml
# 该配置路径位于 config/multi-yggdrasil.toml
[SomeRandomMirror]                       # 名称，可随意设置，无影响
type = "OFFICIAL"                        # 类型，目前支持“OFFICIAL”和“BLESSING_SKIN”
sessionHost = "https://a.random.mirror"  # “OFFICIAL”类型的特定值，URL结尾*不应有*/，但是有也可以
ordinal = 0                              # 序号，决定了该来源的使用顺序

[MojangOfficialAPI]
type = "OFFICIAL"                        # “OFFICIAL”类型可以没有sessionHost属性值，此时会使用官方API
ordinal = 1

[LittleSkin]
type = "BLESSING_SKIN"                   # ↙ “BLESSING_SKIN”类型的特定值，URL结尾*应有*/，但是没有也可以
apiRoot = "https://littleskin.cn/api/yggdrasil/"  
ordinal = 2
```