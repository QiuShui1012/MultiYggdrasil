package com.qiushui1012.mod.multiyggdrasil.util.vap;

//#if FABRIC
import net.fabricmc.loader.api.FabricLoader;
//#elseif FORGE
//$$ import net.minecraftforge.fml.loading.FMLPaths;
//#endif

import java.nio.file.Path;

@SuppressWarnings("unused")
public class PlatformUtil {
    @VAPPattern
    public static Path getConfigDir() {
        //#if FABRIC
        return FabricLoader.getInstance().getConfigDir();
        //#elseif FORGE
        //$$ return FMLPaths.CONFIGDIR.get();
        //#endif
    }
}
