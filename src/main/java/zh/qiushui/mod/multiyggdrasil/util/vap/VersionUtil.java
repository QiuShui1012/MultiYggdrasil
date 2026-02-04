package zh.qiushui.mod.multiyggdrasil.util.vap;

//#if AUTHLIB >= 10600
import com.mojang.authlib.Environment;
//#endif
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.StringJoiner;

public class VersionUtil {
    public static Environment createEnv(
        //#if AUTHLIB < 50000
        String auth,
        //#endif

        //#if AUTHLIB < 60000
        String account,
        //#endif

        //#if AUTHLIB >= 20000
        //$$ String services,
        //#endif

        //#if AUTHLIB >= 70000
        //$$ String profiles,
        //#endif

        String session,
        String name
    ) {
        //#if AUTHLIB < 50000
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

            //#if AUTHLIB >= 20000
            //$$ @Override
            //$$ public String getServicesHost() {
            //$$     return services;
            //$$ }
            //#endif

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
        //#elseif AUTHLIB < 60000
        //$$ return new Environment(account, session, services, name);
        //#elseif AUTHLIB < 70000
        //$$ return new Environment(session, services, name);
        //#elseif AUTHLIB < 80000
        //$$ return new Environment(session, services, profiles, name);
        //#endif
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
