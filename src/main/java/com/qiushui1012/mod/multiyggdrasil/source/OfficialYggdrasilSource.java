package com.qiushui1012.mod.multiyggdrasil.source;

import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OfficialYggdrasilSource extends BaseYggdrasilSource {
    private final String sessionHost;
    private final String servicesHost;
    private final String profilesHost;

    public OfficialYggdrasilSource(
        String name,
        String sessionHost,
        String servicesHost,
        String profilesHost,
        int ordinal
    ) {
        super(name, ordinal);
        this.sessionHost = sessionHost;
        this.servicesHost = servicesHost;
        this.profilesHost = profilesHost;
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
    public String getServicesRoot() {
        return this.servicesHost;
    }

    @Override
    public String getProfilesRoot() {
        return this.profilesHost;
    }

    @Override
    public List<String> serialize() {
        List<String> result = new ArrayList<>();
        result.add("type = " + YggdrasilSourceType.OFFICIAL.name());
        if (!Objects.equals(this.sessionHost, YggdrasilEnvironment.PROD.getEnvironment().sessionHost())) {
            result.add("sessionHost = " + this.sessionHost);
        }
        if (!Objects.equals(this.servicesHost, YggdrasilEnvironment.PROD.getEnvironment().servicesHost())) {
            result.add("servicesHost = " + this.servicesHost);
        }
        if (!Objects.equals(this.profilesHost, YggdrasilEnvironment.PROD.getEnvironment().profilesHost())) {
            result.add("profilesHost = " + this.profilesHost);
        }
        result.add("ordinal = " + this.ordinal);
        return result;
    }
}
