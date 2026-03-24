<div align="center">

# MultiYggdrasil

[![GitHub Release](https://img.shields.io/github/release/QiuShui1012/MultiYggdrasil.svg)](https://github.com/QiuShui1012/MultiYggdrasil/releases/)
[![Github License](https://img.shields.io/github/license/QiuShui1012/MultiYggdrasil)](https://github.com/QiuShui1012/MultiYggdrasil/blob/neoforge/LICENSE.txt)
[![QQ Group](https://img.shields.io/badge/QQ%20group-108917413-yellow)](https://qm.qq.com/q/XJGuYx9W6u)

[简体中文](README.md) | **English**

</div>

## Summary

MultiYggdrasil is a Forge | Fabric | NeoForge mod.  
It allows you to set multiple Yggdrasil API sources, including the official and other external authentication servers.  
Config design is inspired by [MultiLogin](https://github.com/CaaMoe/MultiLogin),
and some codes are borrowed from [authlib-injector](https://github.com/yushijinhun/authlib-injector/) under the AGPL-3.0 license.  
~~patchwork~~

## Deploy

Same as other mods, there are only three steps:
1. Download mod from [CurseForge](https://curseforge.com/minecraft/mc-mods/multiyggdrasil) | [Modrinth](https://modrinth.com/mod/multiyggdrasil/)
2. Throw it into `/mods`
3. Launch the server

## Configuration

A template:
```ini
# This config is located at config/multi-yggdrasil.ini
[SomeRandomMirror]                     # The name, can be set freely, has no impacts.
type = OFFICIAL                        # The type, now has 2 types, "OFFICIAL" and "BLESSING_SKIN".
authHost = https://a.random.mirror     # |- The specific value of "OFFICIAL" type.
accountsHost = https://a.random.mirror # |  Requirements in below.
sessionHost = https://a.random.mirror  # |
servicesHost = https://a.random.mirror # |
profilesHost = https://a.random.mirror # |
ordinal = 0                            # The ordinal, decided the order of use for this source

[MojangOfficial]
type = OFFICIAL                        # When using "OFFICIAL" type, there can be no host-like property,
ordinal = 1                            # and it will use the official API.

[LittleSkin]
type = BLESSING_SKIN                   # ↙ The specific value of "BLESSING_SKIN" type.
apiRoot = https://littleskin.cn/api/yggdrasil/
ordinal = 2
```

The requirements of these host-like specific values for “OFFICIAL” type are depending on the MC version, see the table below:

| authlib       | Minecraft      | authHost | accountsHost | sessionHost | servicesHost | profilesHost |
|:--------------|:---------------|:--------:|:------------:|:-----------:|:------------:|:------------:|
| 1.5.25~1.6.25 | 1.14.4~1.16.3  |    ✅     |      ✅       |      ✅      |      ❌       |      ❌       |
| 2.1.28~4.0.43 | 1.16.4~1.20.1  |    ✅     |      ✅       |      ✅      |      ✅       |      ❌       |
| 5.0.47        | 1.20.2         |    ❌     |      ✅       |      ✅      |      ✅       |      ❌       |
| 6.0.52~6.0.58 | 1.20.3~1.21.8  |    ❌     |      ❌       |      ✅      |      ✅       |      ❌       |
| 7.0.61        | 1.21.9~1.21.11 |    ❌     |      ❌       |      ✅      |      ✅       |      ✅       |

## Version Supporting
|   Minecraft    | Forge | Fabric | NeoForge |
|:--------------:|:-----:|:------:|:--------:|
|    1.14.4-     |   ❌   |   ❌    |    ❌     |
| 1.14.4~1.20.1  |   ✅   |   ✅    |    ❌     |
| 1.20.1~1.20.6  |   ✅   |   ✅    |    ✅     |
| 1.21.1~1.21.11 |   ❌   |   ✅    |    ✅     |
