package com.qiushui1012.mod.multiyggdrasil.util.vap;

import com.mojang.authlib.Environment;

import java.util.Collection;
import java.util.StringJoiner;

public class VersionUtil {
    public static Environment createEnv(
        String auth,
        String account,
        String services,
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
            public String getServicesHost() {
                return services;
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
                    .add("servicesHost='" + getServicesHost() + "'")
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

    public static int getSize(Object o) {
        if (o instanceof Object[]) {
            return ((Object[]) o).length;
        } else if (o instanceof Collection<?>) {
            return ((Collection<?>) o).size();
        } else {
            throw new IllegalArgumentException("Cannot get this object (" + o + ")'s size!");
        }
    }
}
