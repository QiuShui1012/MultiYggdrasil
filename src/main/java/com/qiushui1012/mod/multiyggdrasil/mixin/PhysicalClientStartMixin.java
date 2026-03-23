package com.qiushui1012.mod.multiyggdrasil.mixin;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.qiushui1012.mod.multiyggdrasil.auth.MultiYggdrasilAuthService;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public class PhysicalClientStartMixin {
    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;"
                     + "createMinecraftSessionService()"
                     + "Lcom/mojang/authlib/minecraft/MinecraftSessionService;",
            remap = false
        )
    )
    private MinecraftSessionService createBetter(YggdrasilAuthenticationService instance) {
        return new MultiYggdrasilAuthService(instance.getProxy(), instance.getClientToken()).createMinecraftSessionService();
    }
}
