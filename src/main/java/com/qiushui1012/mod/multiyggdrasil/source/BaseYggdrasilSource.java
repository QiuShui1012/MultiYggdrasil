package com.qiushui1012.mod.multiyggdrasil.source;

import com.mojang.authlib.Environment;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import com.qiushui1012.mod.multiyggdrasil.util.vap.VersionUtil;

import java.util.List;

@Getter
public abstract class BaseYggdrasilSource implements Comparable<BaseYggdrasilSource> {
    protected final String name;
    protected final int ordinal;

    protected BaseYggdrasilSource(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    public abstract YggdrasilSourceType getType();

    public abstract String getAuthRoot();

    public abstract String getAccountRoot();

    public abstract String getSessionRoot();

    public abstract String getServicesRoot();

    public abstract List<String> serialize();

    @Override
    public int compareTo(@NotNull BaseYggdrasilSource that) {
        return Integer.compare(this.ordinal, that.ordinal);
    }

    public Environment toEnvironment() {
        return VersionUtil.createEnv(
            this.getAuthRoot(),
            this.getAccountRoot(),
            this.getSessionRoot(),
            this.getServicesRoot(),
            this.getName()
        );
    }
}
