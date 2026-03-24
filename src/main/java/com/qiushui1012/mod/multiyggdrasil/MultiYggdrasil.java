package com.qiushui1012.mod.multiyggdrasil;

import com.mojang.logging.LogUtils;
import com.qiushui1012.mod.multiyggdrasil.config.YggdrasilConfig;
import org.slf4j.Logger;

public class MultiYggdrasil {
    public static final String MOD_ID = "multiyggdrasil";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final YggdrasilConfig SERVERS_CONFIG = YggdrasilConfig.load();
}
