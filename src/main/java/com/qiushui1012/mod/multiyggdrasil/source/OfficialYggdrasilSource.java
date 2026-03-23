package com.qiushui1012.mod.multiyggdrasil.source;

import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OfficialYggdrasilSource extends BaseYggdrasilSource {
    private final String authHost;
    private final String accountsHost;
    private final String sessionHost;
    private final String servicesHost;

    public OfficialYggdrasilSource(
        String name,
        String authHost,
        String accountsHost,
        String sessionHost,
        String servicesHost,
        int ordinal
    ) {
        super(name, ordinal);
        this.authHost = authHost;
        this.accountsHost = accountsHost;
        this.sessionHost = sessionHost;
        this.servicesHost = servicesHost;
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
    public String getServicesRoot() {
        return this.servicesHost;
    }

    @Override
    public List<String> serialize() {
        List<String> result = new ArrayList<>();
        result.add("type = " + YggdrasilSourceType.OFFICIAL.name());
        if (!Objects.equals(this.authHost, YggdrasilEnvironment.PROD.getAuthHost())) {
            result.add("authHost = " + this.authHost);
        }
        if (!Objects.equals(this.accountsHost, YggdrasilEnvironment.PROD.getAccountsHost())) {
            result.add("accountHost = " + this.accountsHost);
        }
        if (!Objects.equals(this.sessionHost, YggdrasilEnvironment.PROD.getSessionHost())) {
            result.add("sessionHost = " + this.sessionHost);
        }
        if (!Objects.equals(this.servicesHost, YggdrasilEnvironment.PROD.getServicesHost())) {
            result.add("servicesHost = " + this.servicesHost);
        }
        result.add("ordinal = " + this.ordinal);
        return result;
    }
}
