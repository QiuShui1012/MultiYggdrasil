package zh.qiushui.mod.multiyggdrasil.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zh.qiushui.mod.multiyggdrasil.MultiYggdrasil;
import zh.qiushui.mod.multiyggdrasil.config.YggdrasilConfig;

@Mixin(ServerLevel.class)
abstract class LogicalServerSaveMixin {
    @Inject(method = "save", at = @At("RETURN"))
    private void saveConfig(ProgressListener progressListener, boolean bl, boolean bl2, CallbackInfo ci) {
        YggdrasilConfig.save(MultiYggdrasil.SERVERS_CONFIG);
    }
}
