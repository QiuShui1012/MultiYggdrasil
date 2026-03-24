package com.qiushui1012.mod.multiyggdrasil.mixin;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.qiushui1012.mod.multiyggdrasil.auth.MultiYggdrasilAuthService;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.Proxy;

@Mixin(value = Main.class, priority = 50)
abstract class PhysicalServerStartMixin {
    @Redirect(
        method = "main",
        at = @At(
            value = "NEW",
            target = "(Ljava/net/Proxy;)Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;"
        ),
        remap = false
    )
    private static YggdrasilAuthenticationService createBetter(Proxy proxy) {
        return new MultiYggdrasilAuthService(proxy);
    }
}
