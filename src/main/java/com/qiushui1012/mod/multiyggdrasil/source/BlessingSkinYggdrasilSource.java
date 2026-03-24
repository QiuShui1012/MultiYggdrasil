package com.qiushui1012.mod.multiyggdrasil.source;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
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
    public String getSessionRoot() {
        return this.apiRoot + "sessionserver";
    }

    @Override
    public String getServicesRoot() {
        return this.apiRoot + "services";
    }

    @Override
    public List<String> serialize() {
        List<String> result = new ArrayList<>();
        result.add("type = " + YggdrasilSourceType.BLESSING_SKIN.name());
        result.add("apiRoot = " + this.apiRoot);
        result.add("ordinal = " + this.ordinal);
        return result;
    }
}
