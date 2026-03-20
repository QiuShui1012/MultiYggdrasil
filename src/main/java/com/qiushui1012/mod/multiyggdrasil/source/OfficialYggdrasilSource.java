package com.qiushui1012.mod.multiyggdrasil.source;

//#if AUTHLIB < 10600
//$$ import com.qiushui1012.mod.multiyggdrasil.util.vap.VersionUtil;
//#else
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
//#endif
import com.mojang.datafixers.util.Pair;
import io.github.wasabithumb.jtoml.value.table.TomlTable;

import java.util.Objects;

public class OfficialYggdrasilSource extends BaseYggdrasilSource {
    private final String authHost;
    private final String accountsHost;
    private final String sessionHost;

    public OfficialYggdrasilSource(
        String name,
        String authHost,
        String accountsHost,
        String sessionHost,
        int ordinal
    ) {
        super(name, ordinal);
        this.authHost = authHost;
        this.accountsHost = accountsHost;
        this.sessionHost = sessionHost;
    }

    @Override
    public YggdrasilSourceType getType() {
        return YggdrasilSourceType.OFFICIAL;
    }

    @Override
    public String getAuthRoot() {
        return this.authHost;
    }

    @Override
    public String getAccountRoot() {
        return this.accountsHost;
    }

    @Override
    public String getSessionRoot() {
        return this.sessionHost;
    }

    @Override
    public Pair<String, TomlTable> serialize() {
        TomlTable data = TomlTable.create();
        data.put("type", this.getType().name());
        if (!Objects.equals(this.authHost, YggdrasilEnvironment.PROD.getAuthHost())) {
            data.put("authHost", this.authHost);
        }
        if (!Objects.equals(this.accountsHost, YggdrasilEnvironment.PROD.getAccountsHost())) {
            data.put("accountHost", this.accountsHost);
        }
        if (!Objects.equals(this.sessionHost, YggdrasilEnvironment.PROD.getSessionHost())) {
            data.put("sessionHost", this.sessionHost);
        }
        data.put("ordinal", this.ordinal);
        return Pair.of(this.name, data);
    }
}
