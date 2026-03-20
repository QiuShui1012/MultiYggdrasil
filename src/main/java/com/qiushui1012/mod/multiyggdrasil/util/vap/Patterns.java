package com.qiushui1012.mod.multiyggdrasil.util.vap;

//#if AUTHLIB >= 10600
import com.mojang.authlib.Environment;
import com.mojang.authlib.EnvironmentParser;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
//#endif

@SuppressWarnings("unused")
public class Patterns {
    @VAPPattern
    public static Environment getDefaultEnv() {
        //#if AUTHLIB < 10600
        //$$ return VersionUtil.createEnv("https://authserver.mojang.com", "https://api.mojang.com", "https://sessionserver.mojang.com", "MojangOfficial");
        //#else
        return YggdrasilEnvironment.PROD;
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
}
