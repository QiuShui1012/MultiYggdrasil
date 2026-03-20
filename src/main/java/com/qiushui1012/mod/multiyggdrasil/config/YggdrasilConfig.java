package com.qiushui1012.mod.multiyggdrasil.config;

import com.google.common.collect.Lists;
//#if AUTHLIB < 10600
//$$ import com.qiushui1012.mod.multiyggdrasil.util.vap.VersionUtil;
//#else
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
//#endif
import com.mojang.datafixers.util.Pair;
import io.github.wasabithumb.jtoml.JToml;
import io.github.wasabithumb.jtoml.document.TomlDocument;
import io.github.wasabithumb.jtoml.key.TomlKey;
import io.github.wasabithumb.jtoml.value.TomlValue;
import io.github.wasabithumb.jtoml.value.primitive.TomlPrimitive;
import io.github.wasabithumb.jtoml.value.table.TomlTable;
import lombok.Getter;
import com.qiushui1012.mod.multiyggdrasil.MultiYggdrasil;
//#if AUTHLIB >= 31100
//$$ import com.qiushui1012.mod.multiyggdrasil.auth.MultiYggdrasilServicesKeyInfo;
//$$ import com.qiushui1012.mod.multiyggdrasil.util.ParseUtil;
//#endif
import com.qiushui1012.mod.multiyggdrasil.source.BaseYggdrasilSource;
import com.qiushui1012.mod.multiyggdrasil.source.BlessingSkinYggdrasilSource;
import com.qiushui1012.mod.multiyggdrasil.source.OfficialYggdrasilSource;
import com.qiushui1012.mod.multiyggdrasil.source.YggdrasilSourceType;

//#if FABRIC
import net.fabricmc.loader.api.FabricLoader;
//#elseif FORGE
//$$ import net.minecraftforge.fml.loading.FMLPaths;
//#elseif NEOFORGE
//$$ import net.neoforged.fml.loading.FMLPaths;
//#endif

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class YggdrasilConfig {
    public static final YggdrasilConfig DEFAULT = new YggdrasilConfig(Lists.newArrayList(
        new OfficialYggdrasilSource(
            "MojangOfficial",
            YggdrasilEnvironment.PROD.getAuthHost(),
            YggdrasilEnvironment.PROD.getAccountsHost(),
            YggdrasilEnvironment.PROD.getSessionHost(),
            0
        ),
        new BlessingSkinYggdrasilSource("LittleSkin", "https://littleskin.cn/api/yggdrasil/", 1),
        new BlessingSkinYggdrasilSource("ElyBy", "https://account.ely.by/api/authlib-injector/", 2)
    ));
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("multi-yggdrasil.toml");

    @Getter
    private final List<BaseYggdrasilSource> sources;

    public YggdrasilConfig(List<BaseYggdrasilSource> sources) {
        this.sources = sources;
    }

    public static void save(YggdrasilConfig config) {
        tryMkConfigDirs();
        try (OutputStream stream = Files.newOutputStream(PATH)) {
            TomlTable result = TomlTable.create();
            for (BaseYggdrasilSource source : config.sources) {
                Pair<String, TomlTable> data = source.serialize();
                result.put(data.getFirst(), data.getSecond());
            }
            JToml.jToml().write(stream, result);
        } catch (IOException e) {
            MultiYggdrasil.LOGGER.warn("Cannot save config to {}.", PATH, e);
        }
    }

    private static void tryMkConfigDirs() {
        if (
            !FabricLoader.getInstance().getConfigDir().toFile().exists()
            && !FabricLoader.getInstance().getConfigDir().toFile().mkdirs()
        ) {
            MultiYggdrasil.LOGGER.warn("Cannot mk config dirs. Path: {}", FabricLoader.getInstance().getConfigDir());
        }
    }

    public static YggdrasilConfig load() {
        TomlDocument config;
        try {
            config = JToml.jToml().read(PATH.toFile());
        } catch (Throwable e) {
            MultiYggdrasil.LOGGER.warn(
                "Cannot load config. Use default config. If you are first starting with this mod, you can ignore this warn, and modify config in {} if you want.",
                PATH,
                e
            );
            return DEFAULT;
        }

        List<BaseYggdrasilSource> sourceList = new ArrayList<>();
        for (TomlKey sourceKey : config.keys()) {
            String name = sourceKey.toString();

            TomlValue typeRaw = config.get(TomlKey.join(sourceKey, TomlKey.parse("type")));
            if (!(typeRaw instanceof TomlPrimitive) || !typeRaw.asPrimitive().isString()) {
                MultiYggdrasil.LOGGER.warn("The type of Yggdrasil source {} is not a valid string. Skipped.", name);
                continue;
            }
            YggdrasilSourceType type;
            try {
                type = YggdrasilSourceType.valueOf(typeRaw.asPrimitive().asString().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                MultiYggdrasil.LOGGER.warn("The type of Yggdrasil source {} is not a valid type. Skipped.", name, e);
                continue;
            }

            TomlValue ordinalRaw = config.get(TomlKey.join(sourceKey, TomlKey.parse("ordinal")));
            if (!(ordinalRaw instanceof TomlPrimitive) || !ordinalRaw.asPrimitive().isInteger()) {
                MultiYggdrasil.LOGGER.warn("The ordinal of Yggdrasil source {} is not a valid integer. Skipped.", name);
                continue;
            }
            int ordinal = ordinalRaw.asPrimitive().asInteger();
            if (ordinal < 0) {
                MultiYggdrasil.LOGGER.warn("The ordinal of Yggdrasil source {} cannot be lesser than 0. Skipped.", name);
                continue;
            }

            switch (type) {
                case OFFICIAL:
                    String authHost;
                    TomlValue authHostRaw = config.get(TomlKey.join(sourceKey, TomlKey.parse("authHost")));
                    if (authHostRaw == null) {
                        authHost = YggdrasilEnvironment.PROD.getAuthHost();
                    } else if (!(authHostRaw instanceof TomlPrimitive) || !authHostRaw.asPrimitive().isString()) {
                        MultiYggdrasil.LOGGER.warn("The authHost of Official Yggdrasil source {} is not a valid string. Skipped.", name);
                        continue;
                    } else {
                        authHost = authHostRaw.asPrimitive().asString();
                    }
                    if (authHost.endsWith("/")) {
                        authHost = authHost.substring(0, authHost.length() - 1);
                    }

                    String accountsHost;
                    TomlValue accountsHostRaw = config.get(TomlKey.join(sourceKey, TomlKey.parse("accountsHost")));
                    if (accountsHostRaw == null) {
                        accountsHost = YggdrasilEnvironment.PROD.getAccountsHost();
                    } else if (!(accountsHostRaw instanceof TomlPrimitive) || !accountsHostRaw.asPrimitive().isString()) {
                        MultiYggdrasil.LOGGER.warn("The accountsHost of Official Yggdrasil source {} is not a valid string. Skipped.", name);
                        continue;
                    } else {
                        accountsHost = accountsHostRaw.asPrimitive().asString();
                    }
                    if (accountsHost.endsWith("/")) {
                        accountsHost = accountsHost.substring(0, accountsHost.length() - 1);
                    }

                    String sessionHost;
                    TomlValue sessionHostRaw = config.get(TomlKey.join(sourceKey, TomlKey.parse("sessionHost")));
                    if (sessionHostRaw == null) {
                        sessionHost = YggdrasilEnvironment.PROD.getSessionHost();
                    } else if (!(sessionHostRaw instanceof TomlPrimitive) || !sessionHostRaw.asPrimitive().isString()) {
                        MultiYggdrasil.LOGGER.warn("The sessionHost of Official Yggdrasil source {} is not a valid string. Skipped.", name);
                        continue;
                    } else {
                        sessionHost = sessionHostRaw.asPrimitive().asString();
                    }
                    if (sessionHost.endsWith("/")) {
                        sessionHost = sessionHost.substring(0, sessionHost.length() - 1);
                    }

                    sourceList.add(new OfficialYggdrasilSource(
                        name,
                        authHost,
                        accountsHost,
                        sessionHost,
                        ordinal
                    ));
                    break;
                case BLESSING_SKIN:
                    String apiRoot;
                    TomlValue apiRootRaw = config.get(TomlKey.join(sourceKey, TomlKey.parse("apiRoot")));
                    if (!(apiRootRaw instanceof TomlPrimitive) || !apiRootRaw.asPrimitive().isString()) {
                        MultiYggdrasil.LOGGER.warn("The apiRoot of BlessingSkin Yggdrasil source {} is not a valid string. Skipped.", name);
                        continue;
                    } else {
                        apiRoot = apiRootRaw.asPrimitive().asString();
                    }
                    if (!apiRoot.endsWith("/")) {
                        apiRoot = apiRoot.concat("/");
                    }
                    sourceList.add(new BlessingSkinYggdrasilSource(name, apiRoot, ordinal));
            }
        }
        return new YggdrasilConfig(sourceList);
    }
}
