package zh.qiushui.mod.multiyggdrasil.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zh.qiushui.mod.multiyggdrasil.auth.MultiYggdrasilAuthService;

@Mixin(Minecraft.class)
public class PhysicalClientStartMixin {
    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;"
                     + "createMinecraftSessionService()"
                     + "Lcom/mojang/authlib/minecraft/MinecraftSessionService;",
            remap = false
        )
    )
    private MinecraftSessionService createBetter(
        YggdrasilAuthenticationService instance,
        Operation<MinecraftSessionService> original
    ) {
        return original.call(new MultiYggdrasilAuthService(
            instance.getProxy()
            //#if AUTHLIB < 50000
            ,((YggdrasilAuthServiceAccessor) instance).getClientToken()
            //#endif
        ));
    }
}
