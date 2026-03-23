package com.qiushui1012.mod.multiyggdrasil.util.vap;

import java.nio.file.Path;

//#if AUTHLIB >= 10600
import com.mojang.authlib.Environment;
import com.mojang.authlib.EnvironmentParser;
//#endif

//#if FABRIC
import net.fabricmc.loader.api.FabricLoader;
//#elseif FORGE
//$$ import net.minecraftforge.fml.loading.FMLPaths;
//#endif

@SuppressWarnings("unused")
public class Patterns {
    @VAPPattern
    public static Environment getEnvFromProperties() {
        //#if AUTHLIB < 10600
        //$$ return VersionUtil.DEFAULT;
        //#else
        return EnvironmentParser.getEnvironmentFromProperties().orElse(VersionUtil.DEFAULT);
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
