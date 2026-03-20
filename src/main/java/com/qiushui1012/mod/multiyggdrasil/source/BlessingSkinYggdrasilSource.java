package com.qiushui1012.mod.multiyggdrasil.source;

import com.mojang.datafixers.util.Pair;
import io.github.wasabithumb.jtoml.value.table.TomlTable;

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
    public String getAuthRoot() {
        return this.apiRoot + "authserver";
    }

    @Override
    public String getAccountRoot() {
        return this.apiRoot + "api";
    }

    @Override
    public String getSessionRoot() {
        return this.apiRoot + "sessionserver";
    }

    @Override
    public Pair<String, TomlTable> serialize() {
        TomlTable data = TomlTable.create();
        data.put("type", this.getType().name());
        data.put("apiRoot", this.apiRoot);
        data.put("ordinal", this.ordinal);
        return Pair.of(this.name, data);
    }
}
