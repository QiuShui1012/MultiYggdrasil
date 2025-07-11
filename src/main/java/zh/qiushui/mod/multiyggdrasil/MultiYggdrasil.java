package zh.qiushui.mod.multiyggdrasil;

import com.mojang.logging.LogUtils;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import zh.qiushui.mod.multiyggdrasil.config.YggdrasilServersConfig;

@Mod(MultiYggdrasil.MOD_ID)
public class MultiYggdrasil {
    public static final String MOD_ID = "multiyggdrasil";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final YggdrasilServersConfig SERVERS_CONFIG = YggdrasilServersConfig.load();

    public MultiYggdrasil(FMLJavaModLoadingContext ignored) {
        LevelEvent.Save.BUS.addListener(MultiYggdrasil::onServerSave);
    }

    private static void onServerSave(LevelEvent.Save event) {
        YggdrasilServersConfig.save(SERVERS_CONFIG);
    }
}
