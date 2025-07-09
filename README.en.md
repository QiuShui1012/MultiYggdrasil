<div align="center">

# MultiYggdrasil

[![GitHub Release](https://img.shields.io/github/release/QiuShui1012/MultiYggdrasil.svg)](https://github.com/QiuShui1012/MultiYggdrasil/releases/)
[![GitHub License](https://img.shields.io/github/license/QiuShui1012/MultiYggdrasil?style=flat-square)](https://github.com/QiuShui1012/MultiYggdrasil/blob/neoforge/LICENSE.txt)
[![QQ Group](https://img.shields.io/badge/QQ%20group-108917413-yellow?style=flat-square)](https://qm.qq.com/q/XJGuYx9W6u)

[简体中文](README.md) | **English**

</div>

## Summary

MultiYggdrasil is a ~~Forge | Fabric |~~ NeoForge server-side only mod,
this means some functions may have bugs when running LAN server on client, for example, [MC-52974](https://bugs.mojang.com/browse/MC/issues/MC-52974).  
It allows the server to set multiple Yggdrasil API sources, including the official and other external authentication servers.  
Config design is inspired by [MultiLogin](https://github.com/CaaMoe/MultiLogin),
Some codes are borrowed from [authlib-injector](https://github.com/yushijinhun/authlib-injector/) under the AGPL-3.0 license.  
~~patchwork~~

## Deploy

Java requirement follows Minecraft versions, no need to install `authlib-injector`, no pre mods, and no need to add or modify `JVM` arguments.

Same as the most part of other mods, there are only three steps:
1. [Download](https://github.com/QiuShui1012/MultiYggdrasil/releases/latest) mod
2. Throw it into mods
3. Launch the server

## Configuration

A template:
```toml
# This config is located at config/multi-yggdrasil.toml
[SomeRandomMirror]                       # The name, can be set freely, has no impacts.
type = "OFFICIAL"                        # The type, now has 2 types, "OFFICIAL" and "BLESSING_SKIN".
sessionHost = "https://a.random.mirror"  # The specific value of "OFFICIAL" type. The tail of URL *should not* has '/'.
ordinal = 0                              # The ordinal, decided the order of use for this source

[MojangOfficialAPI]
type = "OFFICIAL"                        # When using "OFFICIAL" type, there can be no sessionHost property,
ordinal = 1                              # and it will use the official API.

[LittleSkin]
type = "BLESSING_SKIN"                   # ↙ The specific value of "BLESSING_SKIN" type. The tail of URL *should* has '/'.
apiRoot = "https://littleskin.cn/api/yggdrasil/"  
ordinal = 2
```
