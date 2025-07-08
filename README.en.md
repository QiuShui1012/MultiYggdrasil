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
```json5
{  // Will be in config/multi-yggdrasil.json
  "sources": [  // The outside array. Name must be 'sources'.
    {  // A single source.
      "type": "OFFICIAL",  // The type of the source. Now can be 'OFFICIAL' and 'BLESSING_SKIN' (case-insensitive.)
      "name": "Some Random Mirror",  // The name of the source. Can be anything (maybe).
      "sessionHost": "https://a.random.session.server.mirror/",  // (optional) The session of sources with type 'OFFICIAL'. If not set, uses the default value (Mojang Official API).
      "ordinal": 0  // The ordinal of the source. Controlling the order that server sending request to. Must be larger than -1.
    },
    {
      "type": "OFFICIAL",
      "name": "Mojang Official API",
      "ordinal": 1
    },
    {
      "type": "BLESSING_SKIN",
      "name": "LittleSkin",
      "apiRoot": "https://littleskin.cn/api/yggdrasil/",  // The root api of sources with type 'BLESSING_SKIN'.
      "ordinal": 2
    }
  ]
}
```