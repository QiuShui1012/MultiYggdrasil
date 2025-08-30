package zh.qiushui.mod.multiyggdrasil.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zh.qiushui.mod.multiyggdrasil.auth.BetterYggdrasilAuthService;

import java.net.Proxy;

// The most important part of this mod. If this mixin did not work, this mod will do nothing.
@Mixin(value = Main.class, priority = 50)
public class MainMixin {
    @WrapOperation(
        method = "main",
        at = @At(value = "NEW", target = "(Ljava/net/Proxy;)Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;")
    )
    private static YggdrasilAuthenticationService createBetter(
        Proxy proxy, Operation<YggdrasilAuthenticationService> original
    ) {
        return new BetterYggdrasilAuthService(proxy);
    }
}
