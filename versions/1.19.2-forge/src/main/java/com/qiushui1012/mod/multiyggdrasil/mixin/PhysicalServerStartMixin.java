package com.qiushui1012.mod.multiyggdrasil.mixin;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.qiushui1012.mod.multiyggdrasil.auth.MultiYggdrasilAuthService;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Main.class)
abstract class PhysicalServerStartMixin {
    @ModifyArg(
        method = "main",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/Services;create("
                     + "Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;Ljava/io/File;"
                     + ")Lnet/minecraft/server/Services;"
        ),
        remap = false
    )
    private static YggdrasilAuthenticationService createBetter(YggdrasilAuthenticationService value) {
        return new MultiYggdrasilAuthService(value.getProxy(), ((YggdrasilAuthServiceAccessor) value).getClientToken());
    }
}
