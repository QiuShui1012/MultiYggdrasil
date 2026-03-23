package com.qiushui1012.mod.multiyggdrasil.mixin;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(YggdrasilAuthenticationService.class)
public interface YggdrasilAuthServiceAccessor {
    @Accessor(remap = false)
    String getClientToken();
}
