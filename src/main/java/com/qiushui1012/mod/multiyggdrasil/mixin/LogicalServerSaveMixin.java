package com.qiushui1012.mod.multiyggdrasil.mixin;

import com.qiushui1012.mod.multiyggdrasil.MultiYggdrasil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
abstract class LogicalServerSaveMixin {
    @Inject(method = "save", at = @At("RETURN"))
    private void saveConfig(ProgressListener progressListener, boolean bl, boolean bl2, CallbackInfo ci) {
        MultiYggdrasil.SERVERS_CONFIG.save();
    }
}
