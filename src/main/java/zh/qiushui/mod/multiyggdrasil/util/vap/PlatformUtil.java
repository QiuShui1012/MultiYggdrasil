package zh.qiushui.mod.multiyggdrasil.util.vap;

//#if FABRIC
import net.fabricmc.loader.api.FabricLoader;
//#elseif FORGE
//$$ import net.minecraftforge.fml.loading.FMLPaths;
//#elseif NEOFORGE
//$$ import net.neoforged.fml.loading.FMLPaths;
//#endif

import java.nio.file.Path;

public class PlatformUtil {
    @VAPPattern
    public static Path getConfigDir() {
        //#if FABRIC
        return FabricLoader.getInstance().getConfigDir();
        //#elseif FORGE || NEOFORGE
        //$$ return FMLPaths.CONFIGDIR.get();
        //#endif
    }
}
