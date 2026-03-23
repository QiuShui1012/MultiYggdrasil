package com.qiushui1012.mod.multiyggdrasil.util.vap;

import java.nio.file.Path;

//#if AUTHLIB >= 10600
import com.mojang.authlib.Environment;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
//#endif

//#if MC < 11800
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//#else
//$$ import com.mojang.logging.LogUtils;
//$$ import org.slf4j.Logger;
//#endif

//#if FABRIC
import net.fabricmc.loader.api.FabricLoader;
//#elseif FORGE
//$$ import net.minecraftforge.fml.loading.FMLPaths;
//#endif

@SuppressWarnings("unused")
public class Patterns {
    @VAPPattern
    public static Environment getDefaultEnv() {
        //#if AUTHLIB < 30000
        return YggdrasilEnvironment.PROD;
        //#else
        //$$ return YggdrasilEnvironment.PROD.getEnvironment();
        //#endif
    }

    @VAPPattern
    public static Logger getLogger() {
        //#if MC < 11800
        return LogManager.getLogger();
        //#else
        //$$ return LogUtils.getLogger();
        //#endif
    }

    @VAPPattern
    public static Path getConfigDir() {
        //#if FABRIC
        return FabricLoader.getInstance().getConfigDir();
        //#elseif FORGE
        //$$ return FMLPaths.CONFIGDIR.get();
        //#endif
    }
}
