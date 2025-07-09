package zh.qiushui.mod.multiyggdrasil.yggdrasil;

import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class OfficialYggdrasilSource extends BaseYggdrasilSource {
    private final String sessionHost;

    public OfficialYggdrasilSource(String name, String sessionHost, int ordinal) {
        super(name, ordinal);
        this.sessionHost = sessionHost;
    }

    @Override
    public YggdrasilSourceType getType() {
        return YggdrasilSourceType.OFFICIAL;
    }

    @Override
    public String getSessionRoot() {
        return this.sessionHost;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", this.getType().name());
        if (!Objects.equals(this.sessionHost, YggdrasilEnvironment.PROD.getEnvironment().sessionHost())) {
            data.put("sessionHost", this.sessionHost);
        }
        data.put("ordinal", this.ordinal);
        return Map.of(this.name, data);
    }
}
