package zh.qiushui.mod.multiyggdrasil;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import zh.qiushui.mod.multiyggdrasil.config.YggdrasilServersConfig;

@Mod(MultiYggdrasil.MOD_ID)
public class MultiYggdrasil {
    public static final String MOD_ID = "multiyggdrasil";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final YggdrasilServersConfig SERVERS_CONFIG = YggdrasilServersConfig.load();

    public MultiYggdrasil(IEventBus ignored, ModContainer ignored1) {
        NeoForge.EVENT_BUS.addListener(MultiYggdrasil::onServerSave);
    }

    private static void onServerSave(LevelEvent.Save event) {
        YggdrasilServersConfig.save(SERVERS_CONFIG);
    }
}
