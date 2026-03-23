package com.qiushui1012.mod.multiyggdrasil.source;

import com.qiushui1012.mod.multiyggdrasil.util.vap.VersionUtil;

import java.util.ArrayList;
import java.util.List;
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
    public List<String> serialize() {
        List<String> result = new ArrayList<>();
        result.add("type = " + YggdrasilSourceType.OFFICIAL.name());
        if (!Objects.equals(this.authHost, VersionUtil.DEFAULT.getAuthHost())) {
            result.add("authHost = " + this.authHost);
        }
        if (!Objects.equals(this.accountsHost, VersionUtil.DEFAULT.getAccountsHost())) {
            result.add("accountHost = " + this.accountsHost);
        }
        if (!Objects.equals(this.sessionHost, VersionUtil.DEFAULT.getSessionHost())) {
            result.add("sessionHost = " + this.sessionHost);
        }
        result.add("ordinal = " + this.ordinal);
        return result;
    }
}
