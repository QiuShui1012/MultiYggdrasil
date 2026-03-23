package com.qiushui1012.mod.multiyggdrasil.util.vap;

//#if AUTHLIB >= 10600
import com.mojang.authlib.Environment;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
//#endif
import java.util.StringJoiner;

public class VersionUtil {
    public static Environment DEFAULT =
        //#if AUTHLIB >= 10600
        YggdrasilEnvironment.PROD;
        //#else
        //$$ VersionUtil.createEnv(
        //$$    "https://authserver.mojang.com",
        //$$    "https://api.mojang.com",
        //$$    "https://sessionserver.mojang.com",
        //$$    "MojangOfficial"
        //$$ );
        //#endif

    public static Environment createEnv(
        String auth,
        String account,
        String session,
        String name
    ) {
        return new Environment() {
            @Override
            public String getAuthHost() {
                return auth;
            }

            @Override
            public String getAccountsHost() {
                return account;
            }

            @Override
            public String getSessionHost() {
                return session;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String asString() {
                return new StringJoiner(", ", "{", "}")
                    .add("authHost='" + getAuthHost() + "'")
                    .add("accountsHost='" + getAccountsHost() + "'")
                    .add("sessionHost='" + getSessionHost() + "'")
                    .add("name='" + getName() + "'")
                    .toString();
            }

            @Override
            public String toString() {
                return this.asString();
            }
        };
    }
}
