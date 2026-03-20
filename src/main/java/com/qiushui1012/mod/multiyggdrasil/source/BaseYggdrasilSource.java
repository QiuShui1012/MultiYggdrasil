package com.qiushui1012.mod.multiyggdrasil.source;

//#if AUTHLIB < 10600
//$$ import com.qiushui1012.mod.multiyggdrasil.util.vap.Environment;
//#else
import com.mojang.authlib.Environment;
//#endif
import com.mojang.datafixers.util.Pair;
import io.github.wasabithumb.jtoml.value.table.TomlTable;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import com.qiushui1012.mod.multiyggdrasil.util.vap.VersionUtil;

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

    public abstract Pair<String, TomlTable> serialize();

    @Override
    public int compareTo(@NotNull BaseYggdrasilSource that) {
        return Integer.compare(this.ordinal, that.ordinal);
    }

    public Environment toEnvironment() {
        return VersionUtil.createEnv(
            this.getAuthRoot(),
            this.getAccountRoot(),
            this.getSessionRoot(),
            this.getName()
        );
    }
}
