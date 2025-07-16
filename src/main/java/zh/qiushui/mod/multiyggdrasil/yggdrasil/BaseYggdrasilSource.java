package zh.qiushui.mod.multiyggdrasil.yggdrasil;

import com.mojang.authlib.Environment;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.StringJoiner;

@Getter
public abstract class BaseYggdrasilSource implements Environment, Comparable<BaseYggdrasilSource> {
    protected final String name;
    protected final int ordinal;

    protected BaseYggdrasilSource(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    public abstract YggdrasilSourceType getType();

    public abstract Map<String, Object> serialize();

    @Override
    public String getServicesHost() {
        throw new UnsupportedOperationException(
            "For compatibility, we do not use services host in MultiYggdrasil. Please check your codes."
        );
    }

    @Override
    public String asString() {
        return new StringJoiner(", ", "", "")
            .add("authHost='" + getAuthHost() + "'")
            .add("accountsHost='" + getAccountsHost() + "'")
            .add("sessionHost='" + getSessionHost() + "'")
            .add("name='" + getName() + "'")
            .toString();
    }

    @Override
    public int compareTo(@NotNull BaseYggdrasilSource o) {
        return Integer.compare(ordinal, o.ordinal);
    }
}
