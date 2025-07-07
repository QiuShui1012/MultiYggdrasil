MultiYggdrasil
===========================
[简体中文](README.md) | **English**

A Minecraft mod that can make operators set multiple Yggdrasil sources to server.

Config format
===========================
A template:
```json5
{  // Will be in config/multi-yggdrasil.json
  "sources": [  // The outside array. Name must be 'sources'.
    {  // A single source.
      "type": "OFFICIAL",  // The type of the source. Now can be 'OFFICIAL' and 'BLESSING_SKIN' (case-insensitive.)
      "name": "FallenBreath's Mojang API Mirror",  // The name of the source. Can be anything (maybe).
      "sessionHost": "https://session.msp.fallenbreath.me",  // (optional) The session of sources with type 'OFFICIAL'. If not set, uses the default value (Mojang Official API).
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