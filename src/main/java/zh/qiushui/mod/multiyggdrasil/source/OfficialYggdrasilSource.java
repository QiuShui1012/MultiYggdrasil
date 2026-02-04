package zh.qiushui.mod.multiyggdrasil.source;

//#if AUTHLIB < 10600
//$$ import zh.qiushui.mod.multiyggdrasil.util.vap.VersionUtil;
//#else
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
//#endif
import com.mojang.datafixers.util.Pair;
import io.github.wasabithumb.jtoml.value.table.TomlTable;

import java.util.Objects;

public class OfficialYggdrasilSource extends BaseYggdrasilSource {
    //#if AUTHLIB < 50000
    private final String authHost;
    //#endif
    //#if AUTHLIB < 60000
    private final String accountsHost;
    //#endif
    private final String sessionHost;
    //#if AUTHLIB >= 20000
    //$$ private final String servicesHost;
    //#endif
    //#if AUTHLIB >= 70000
    //$$ private final String profilesHost;
    //#endif

    public OfficialYggdrasilSource(
        String name,
        //#if AUTHLIB < 50000
        String authHost,
        //#endif
        //#if AUTHLIB < 60000
        String accountsHost,
        //#endif
        String sessionHost,
        //#if AUTHLIB >= 20000
        //$$ String servicesHost,
        //#endif
        //#if AUTHLIB >= 70000
        //$$ String profilesHost,
        //#endif
        int ordinal
    ) {
        super(name, ordinal);
        //#if AUTHLIB < 50000
        this.authHost = authHost;
        //#endif
        //#if AUTHLIB < 60000
        this.accountsHost = accountsHost;
        //#endif
        this.sessionHost = sessionHost;
        //#if AUTHLIB >= 20000
        //$$ this.servicesHost = servicesHost;
        //#endif
        //#if AUTHLIB >= 70000
        //$$ this.profilesHost = profilesHost;
        //#endif
    }

    @Override
    public YggdrasilSourceType getType() {
        return YggdrasilSourceType.OFFICIAL;
    }

    //#if AUTHLIB < 50000
    @Override
    public String getAuthRoot() {
        return this.authHost;
    }
    //#endif

    //#if AUTHLIB < 60000
    @Override
    public String getAccountRoot() {
        return this.accountsHost;
    }
    //#endif

    @Override
    public String getSessionRoot() {
        return this.sessionHost;
    }

    //#if AUTHLIB >= 20000
    //$$ @Override
    //$$ public String getServicesRoot() {
    //$$     return this.servicesHost;
    //$$ }
    //#endif

    //#if AUTHLIB >= 70000
    //$$ @Override
    //$$ public String getProfilesRoot() {
    //$$     return this.profilesHost;
    //$$ }
    //#endif

    @Override
    public Pair<String, TomlTable> serialize() {
        TomlTable data = TomlTable.create();
        data.put("type", this.getType().name());
        //#if AUTHLIB < 50000
        if (!Objects.equals(this.authHost, YggdrasilEnvironment.PROD.getAuthHost())) {
            data.put("authHost", this.authHost);
        }
        //#endif
        //#if AUTHLIB < 60000
        if (!Objects.equals(this.accountsHost, YggdrasilEnvironment.PROD.getAccountsHost())) {
            data.put("accountHost", this.accountsHost);
        }
        //#endif
        if (!Objects.equals(this.sessionHost, YggdrasilEnvironment.PROD.getSessionHost())) {
            data.put("sessionHost", this.sessionHost);
        }
        //#if AUTHLIB >= 20000
        //$$ if (!Objects.equals(this.servicesHost, YggdrasilEnvironment.PROD.getServicesHost())) {
        //$$     data.put("servicesHost", this.servicesHost);
        //$$ }
        //#endif
        //#if AUTHLIB >= 70000
        //$$ if (!Objects.equals(this.profilesHost, YggdrasilEnvironment.PROD.profilesHost())) {
        //$$     data.put("profilesHost", this.profilesHost);
        //$$ }
        //#endif
        data.put("ordinal", this.ordinal);
        return Pair.of(this.name, data);
    }
}
