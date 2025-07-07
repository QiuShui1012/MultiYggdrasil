package zh.qiushui.mod.multiyggdrasil.auth;

import com.mojang.authlib.GameProfile;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.players.GameProfileCache;

import java.util.Date;

@Getter
@Setter
public class BetterGameProfileInfo extends GameProfileCache.GameProfileInfo {
    private volatile String source;

    public BetterGameProfileInfo(GameProfile profile, Date expirationDate) {
        super(profile, expirationDate);
    }
}
