package zh.qiushui.mod.multiyggdrasil.yggdrasil;

import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
public class OfficialYggdrasilSource extends BaseYggdrasilSource {
    private final String accountsHost;
    private final String sessionHost;

    public OfficialYggdrasilSource(String name, String accountsHost, String sessionHost, int ordinal) {
        super(name, ordinal);
        this.accountsHost = accountsHost;
        this.sessionHost = sessionHost;
    }

    @Override
    public YggdrasilSourceType getType() {
        return YggdrasilSourceType.OFFICIAL;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", this.getType().name());
        if (!Objects.equals(this.accountsHost, YggdrasilEnvironment.PROD.getEnvironment().accountsHost())) {
            data.put("accountsHost", this.accountsHost);
        }
        if (!Objects.equals(this.sessionHost, YggdrasilEnvironment.PROD.getEnvironment().sessionHost())) {
            data.put("sessionHost", this.sessionHost);
        }
        data.put("ordinal", this.ordinal);
        return Map.of(this.name, data);
    }
}
