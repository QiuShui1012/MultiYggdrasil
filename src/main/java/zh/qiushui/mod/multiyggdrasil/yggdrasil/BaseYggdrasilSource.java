package zh.qiushui.mod.multiyggdrasil.yggdrasil;

import com.mojang.authlib.Environment;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Getter
public abstract class BaseYggdrasilSource implements Comparable<BaseYggdrasilSource> {
    protected final String name;
    protected final int ordinal;

    protected BaseYggdrasilSource(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    public abstract YggdrasilSourceType getType();

    public abstract String getSessionRoot();

    public abstract Map<String, Object> serialize();

    @Override
    public int compareTo(@NotNull BaseYggdrasilSource o) {
        return Integer.compare(ordinal, o.ordinal);
    }

    public Environment toEnvironment() {
        return new Environment(this.getSessionRoot(), null, this.getName());
    }
}
