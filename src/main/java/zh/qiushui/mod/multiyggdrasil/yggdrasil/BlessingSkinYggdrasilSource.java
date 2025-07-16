package zh.qiushui.mod.multiyggdrasil.yggdrasil;

import java.util.Map;

public class BlessingSkinYggdrasilSource extends BaseYggdrasilSource {
    private final String apiRoot;

    public BlessingSkinYggdrasilSource(String name, String apiRoot, int ordinal) {
        super(name, ordinal);
        this.apiRoot = apiRoot;
    }

    @Override
    public YggdrasilSourceType getType() {
        return YggdrasilSourceType.BLESSING_SKIN;
    }

    @Override
    public String getAuthHost() {
        return this.apiRoot + "authserver";
    }

    @Override
    public String getAccountsHost() {
        return this.apiRoot + "api";
    }

    @Override
    public String getSessionHost() {
        return this.apiRoot + "sessionserver";
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of(
            this.name, Map.of(
                "type", this.getType().name(),
                "apiRoot", this.apiRoot,
                "ordinal", this.ordinal
            )
        );
    }
}
