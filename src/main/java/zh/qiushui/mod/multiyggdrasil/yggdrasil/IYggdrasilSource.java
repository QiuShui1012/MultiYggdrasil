package zh.qiushui.mod.multiyggdrasil.yggdrasil;

import com.mojang.authlib.Environment;
import org.jetbrains.annotations.NotNull;

public interface IYggdrasilSource extends Comparable<IYggdrasilSource> {
    YggdrasilSourceType type();

    String name();

    String sessionRoot();

    int ordinal();

    @Override
    default int compareTo(@NotNull IYggdrasilSource o) {
        return Integer.compare(this.ordinal(), o.ordinal());
    }

    default Environment toEnvironment() {
        return new Environment(this.sessionRoot(), null, this.name());
    }

    static IYggdrasilSource of(YggdrasilSourceType type, String name, String sessionRoot, int ordinal) {
        return new Simple(type, name, sessionRoot, ordinal);
    }

    record Simple(YggdrasilSourceType type, String name, String sessionRoot, int ordinal) implements IYggdrasilSource {
    }
}
