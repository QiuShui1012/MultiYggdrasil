package com.qiushui1012.mod.multiyggdrasil.util.vap;

import java.nio.file.Path;

//#if FABRIC
import net.fabricmc.loader.api.FabricLoader;
//#elseif FORGE
//$$ import net.minecraftforge.fml.loading.FMLPaths;
//#elseif NEOFORGE
//$$ import net.neoforged.fml.loading.FMLPaths;
//#endif

@SuppressWarnings("unused")
public class Patterns {
    @VAPPattern
    public static Path getConfigDir() {
        //#if FABRIC
        return FabricLoader.getInstance().getConfigDir();
        //#elseif FORGE || NEOFORGE
        //$$ return FMLPaths.CONFIGDIR.get();
        //#endif
    }
}
