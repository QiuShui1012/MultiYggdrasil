package zh.qiushui.mod.multiyggdrasil.mixin;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import zh.qiushui.mod.multiyggdrasil.auth.BetterYggdrasilAuthService;

@Mixin(Main.class)
public class MainMixin {
    @ModifyArg(
        method = "main",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/Services;create("
                     + "Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;Ljava/io/File;)"
                     + "Lnet/minecraft/server/Services;"))
    private static YggdrasilAuthenticationService createBetter(YggdrasilAuthenticationService authenticationService) {
        return new BetterYggdrasilAuthService(authenticationService.getProxy());
    }
}
