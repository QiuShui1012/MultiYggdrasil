package com.qiushui1012.mod.multiyggdrasil.mixin;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.qiushui1012.mod.multiyggdrasil.auth.MultiYggdrasilAuthService;

@Mixin(Main.class)
abstract class PhysicalServerStartMixin {
    @SuppressWarnings("ModifyVariableMayUseName")
    @ModifyVariable(
        method = "main",
        at = @At("STORE"),
        ordinal = 0,
        remap = false
    )
    private static YggdrasilAuthenticationService createBetter(YggdrasilAuthenticationService value) {
        return new MultiYggdrasilAuthService(value.getProxy(), value.getClientToken());
    }
}
