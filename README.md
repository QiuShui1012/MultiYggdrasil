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
部分代码来自[authlib-injector](https://github.com/yushijinhun/authlib-injector/)  
~~缝合怪~~

## 安装

Java需求跟随Minecraft版本，无需安装 `authlib-injector` ，没有任何前置模组，也不需要添加和更改 `JVM` 参数

与其它大部分模组一致，仅需三步：
1. [下载](https://github.com/QiuShui1012/MultiYggdrasil/releases/latest) 模组
2. 丢进 mods
3. 启动服务器

## 配置

一个模板：
```json5
{  // 将会于 config/multi-yggdrasil.json
  "sources": [  // 最外部的array。名称必须是“sources”。
    {  // 一个来源。
      "type": "OFFICIAL",  // 来源的类型。现在可为“OFFICIAL”和“BLESSING_SKIN”（大小写不敏感）。
      "name": "Some Random Mirror",  // 来源的名称。（也许）可为任何东西。
      "sessionHost": "https://a.random.session.server.mirror/",  // (可选) “OFFICIAL”类型下来源的session。如果不设置，则使用默认值（Mojang官方API）。
      "ordinal": 0  // 来源的序号。控制着服务器发送请求的顺序。必须大于-1。
    },
    {
      "type": "OFFICIAL",
      "name": "Mojang Official API",
      "ordinal": 1
    },
    {
      "type": "BLESSING_SKIN",
      "name": "LittleSkin",
      "apiRoot": "https://littleskin.cn/api/yggdrasil/",  // “BLESSING_SKIN”类型下来源的根API网址。
      "ordinal": 2
    }
  ]
}
```