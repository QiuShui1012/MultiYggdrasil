package com.qiushui1012.mod.multiyggdrasil;

//#if MC < 11800
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//#else
//$$ import com.mojang.logging.LogUtils;
//$$ import org.slf4j.Logger;
//#endif
import com.qiushui1012.mod.multiyggdrasil.config.YggdrasilConfig;

public class MultiYggdrasil {
    public static final String MOD_ID = "multiyggdrasil";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final YggdrasilConfig SERVERS_CONFIG = YggdrasilConfig.load();
}
