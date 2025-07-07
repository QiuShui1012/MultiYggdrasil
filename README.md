MultiYggdrasil
===========================
**简体中文** | [English](README.md)

一个允许管理员为服务器设置多个Yggdrasil API来源的我的世界模组。

配置格式
===========================
一个模板：
```json5
{  // 将会于 config/multi-yggdrasil.json
  "sources": [  // 最外部的array。名称必须是“sources”。
    {  // 一个来源。
      "type": "OFFICIAL",  // 来源的类型。现在可为“OFFICIAL”和“BLESSING_SKIN”（大小写不敏感）。
      "name": "FallenBreath's Mojang API Mirror",  // 来源的名称。（也许）可为任何东西。
      "sessionHost": "https://session.msp.fallenbreath.me",  // (可选) “OFFICIAL”类型下来源的session。如果不设置，则使用默认值（Mojang官方API）。
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