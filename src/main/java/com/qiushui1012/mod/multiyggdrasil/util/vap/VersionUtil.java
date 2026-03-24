package com.qiushui1012.mod.multiyggdrasil.util.vap;

import com.mojang.authlib.Environment;
import java.util.Collection;

public class VersionUtil {
    public static Environment createEnv(
        String services,
        String session,
        String name
    ) {
        return new Environment(session, services, name);
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
