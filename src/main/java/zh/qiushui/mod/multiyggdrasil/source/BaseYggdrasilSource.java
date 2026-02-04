package zh.qiushui.mod.multiyggdrasil.source;

//#if AUTHLIB < 10600
//$$ import zh.qiushui.mod.multiyggdrasil.util.vap.Environment;
//#else
import com.mojang.authlib.Environment;
//#endif
import com.mojang.datafixers.util.Pair;
import io.github.wasabithumb.jtoml.value.table.TomlTable;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import zh.qiushui.mod.multiyggdrasil.util.vap.VersionUtil;

@Getter
public abstract class BaseYggdrasilSource implements Comparable<BaseYggdrasilSource> {
    protected final String name;
    protected final int ordinal;

    protected BaseYggdrasilSource(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    public abstract YggdrasilSourceType getType();

    //#if AUTHLIB < 50000
    public abstract String getAuthRoot();
    //#endif

    //#if AUTHLIB < 60000
    public abstract String getAccountRoot();
    //#endif

    public abstract String getSessionRoot();

    //#if AUTHLIB >= 20000
    //$$ public abstract String getServicesRoot();
    //#endif

    //#if AUTHLIB >= 70000
    //$$ public abstract String getProfilesRoot();
    //#endif

    public abstract Pair<String, TomlTable> serialize();

    @Override
    public int compareTo(@NotNull BaseYggdrasilSource that) {
        return Integer.compare(this.ordinal, that.ordinal);
    }

    public Environment toEnvironment() {
        return VersionUtil.createEnv(
            //#if AUTHLIB < 50000
            this.getAuthRoot(),
            //#endif
            //#if AUTHLIB < 60000
            this.getAccountRoot(),
            //#endif
            this.getSessionRoot(),
            //#if AUTHLIB >= 20000
            //$$ this.getServicesRoot(),
            //#endif
            //#if AUTHLIB >= 70000
            //$$ this.getProfilesRoot();
            //#endif
            this.getName()
        );
    }
}
