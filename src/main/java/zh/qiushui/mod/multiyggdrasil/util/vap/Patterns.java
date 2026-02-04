package zh.qiushui.mod.multiyggdrasil.util.vap;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;

import java.net.URL;
import java.util.Map;
import java.util.UUID;

//#if AUTHLIB >= 10600
import com.mojang.authlib.Environment;
import com.mojang.authlib.EnvironmentParser;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
//#endif

//#if MC < 11800
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//#else
//$$ import com.mojang.logging.LogUtils;
//$$ import org.slf4j.Logger;
//#endif

@SuppressWarnings("unused")
public class Patterns {
    @VAPPattern
    public static Environment getDefaultEnv() {
        //#if AUTHLIB < 10600
        //$$ return VersionUtil.createEnv("https://authserver.mojang.com", "https://api.mojang.com", "https://sessionserver.mojang.com", "MojangOfficial");
        //#elseif AUTHLIB < 30000
        return YggdrasilEnvironment.PROD;
        //#else
        //$$ return YggdrasilEnvironment.PROD.getEnvironment();
        //#endif
    }

    @VAPPattern
    public static String getAccountHost(Environment env) {
        //#if AUTHLIB < 50000
        return env.getAccountsHost();
        //#elseif AUTHLIB < 60000
        //$$ return env.accountsHost();
        //#else
        //$$ return "Environment don't have account host in authlib with version 7.0 or higher.";
        //#endif
    }

    @VAPPattern
    public static String getSessionHost(Environment env) {
        //#if AUTHLIB < 50000
        return env.getSessionHost();
        //#else
        //$$ return env.sessionHost();
        //#endif
    }

    @VAPPattern
    public static String getServicesHost(Environment env) {
        //#if AUTHLIB < 20000
        return "Environment don't have services host in authlib with version lower than 2.0.";
        //#elseif AUTHLIB < 50000
        //$$ return env.getServicesHost();
        //#else
        //$$ return env.servicesHost();
        //#endif
    }

    @VAPPattern
    public static Logger getLogger() {
        //#if MC < 11800
        return LogManager.getLogger();
        //#else
        //$$ return LogUtils.getLogger();
        //#endif
    }

    @VAPPattern
    public static Environment getEnvFromProperties() {
        //#if AUTHLIB < 10600
        //$$ return Patterns.getDefaultEnv();
        //#else
        return EnvironmentParser.getEnvironmentFromProperties().orElse(Patterns.getDefaultEnv());
        //#endif
    }

    @VAPPattern
    public static String getName(Property property) {
        //#if AUTHLIB < 50000
        return property.getName();
        //#else
        //$$ return property.name();
        //#endif
    }

    @VAPPattern
    public static String getValue(Property property) {
        //#if AUTHLIB < 50000
        return property.getValue();
        //#else
        //$$ return property.value();
        //#endif
    }

    @VAPPattern
    public static String getSignature(Property property) {
        //#if AUTHLIB < 50000
        return property.getSignature();
        //#else
        //$$ return property.signature();
        //#endif
    }

    @VAPPattern
    public static UUID getId(HasJoinedMinecraftServerResponse response) {
        //#if AUTHLIB < 50000
        return response.getId();
        //#else
        //$$ return response.id();
        //#endif
    }

    @VAPPattern
    public static PropertyMap getProperties(HasJoinedMinecraftServerResponse response) {
        //#if AUTHLIB < 50000
        return response.getProperties();
        //#else
        //$$ return response.properties();
        //#endif
    }
}
