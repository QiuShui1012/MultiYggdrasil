package com.qiushui1012.mod.multiyggdrasil.mixin;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.qiushui1012.mod.multiyggdrasil.auth.MultiYggdrasilAuthService;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.Proxy;

@Mixin(value = Minecraft.class, priority = 50)
abstract class PhysicalClientStartMixin {
    @Redirect(
        method = "<init>",
        at = @At(
            value = "NEW",
            target = "(Ljava/net/Proxy;)Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;"
        ),
        remap = false
    )
    private YggdrasilAuthenticationService createBetter(Proxy proxy) {
        return new MultiYggdrasilAuthService(proxy);
    }
}
