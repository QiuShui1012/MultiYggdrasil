package com.qiushui1012.mod.multiyggdrasil.util.vap;

//#if AUTHLIB >= 10600
import com.mojang.authlib.Environment;
//#endif
import java.util.StringJoiner;

public class VersionUtil {
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
                return new StringJoiner(", ", "", "")
                    .add("authHost='" + getAuthHost() + "'")
                    .add("accountsHost='" + getAccountsHost() + "'")
                    .add("sessionHost='" + getSessionHost() + "'")
                    .add("name='" + getName() + "'")
                    .toString();
            }
        };
    }
}
