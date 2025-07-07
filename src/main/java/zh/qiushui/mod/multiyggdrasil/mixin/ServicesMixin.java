package zh.qiushui.mod.multiyggdrasil.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.server.Services;
import net.minecraft.server.players.GameProfileCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import zh.qiushui.mod.multiyggdrasil.auth.BetterGameProfileCache;

@Mixin(Services.class)
public class ServicesMixin {
    @ModifyVariable(
        method = "create",
        at = @At("STORE"))
    private static GameProfileCache useBetter(GameProfileCache cache, @Local(argsOnly = true) YggdrasilAuthenticationService service) {
        return new BetterGameProfileCache(service.createProfileRepository(), cache.file);
    }
}
