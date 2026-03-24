package com.qiushui1012.mod.multiyggdrasil.mixin;

import com.qiushui1012.mod.multiyggdrasil.MultiYggdrasil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
abstract class LogicalServerSaveMixin {
    @Inject(method = "saveAllChunks", at = @At("RETURN"))
    private void saveConfig(boolean bl, boolean bl2, boolean bl3, CallbackInfoReturnable<Boolean> cir) {
        MultiYggdrasil.SERVERS_CONFIG.save();
    }
}
