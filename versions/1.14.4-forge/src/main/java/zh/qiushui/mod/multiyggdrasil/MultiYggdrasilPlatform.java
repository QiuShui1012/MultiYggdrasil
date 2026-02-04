package zh.qiushui.mod.multiyggdrasil;

import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import zh.qiushui.mod.multiyggdrasil.config.YggdrasilConfig;

@Mod(MultiYggdrasil.MOD_ID)
public class MultiYggdrasilPlatform {
    public MultiYggdrasilPlatform() {
    }

    @SubscribeEvent
    private static void onSave(WorldEvent.Save event) {
        YggdrasilConfig.save(MultiYggdrasil.SERVERS_CONFIG);
    }
}
