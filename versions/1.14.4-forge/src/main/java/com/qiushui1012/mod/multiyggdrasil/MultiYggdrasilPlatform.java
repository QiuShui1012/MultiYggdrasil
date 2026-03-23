package com.qiushui1012.mod.multiyggdrasil;

import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(MultiYggdrasil.MOD_ID)
public class MultiYggdrasilPlatform {
    public MultiYggdrasilPlatform() {
    }

    @SubscribeEvent
    public static void onSave(WorldEvent.Save event) {
        MultiYggdrasil.SERVERS_CONFIG.save();
    }
}
