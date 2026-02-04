package zh.qiushui.mod.multiyggdrasil.config;

import com.google.common.collect.Lists;
//#if AUTHLIB < 10600
//$$ import zh.qiushui.mod.multiyggdrasil.util.vap.VersionUtil;
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
import zh.qiushui.mod.multiyggdrasil.MultiYggdrasil;
//#if AUTHLIB >= 31100
//$$ import zh.qiushui.mod.multiyggdrasil.auth.MultiYggdrasilServicesKeyInfo;
//$$ import zh.qiushui.mod.multiyggdrasil.util.ParseUtil;
//#endif
import zh.qiushui.mod.multiyggdrasil.source.BaseYggdrasilSource;
import zh.qiushui.mod.multiyggdrasil.source.BlessingSkinYggdrasilSource;
import zh.qiushui.mod.multiyggdrasil.source.OfficialYggdrasilSource;
import zh.qiushui.mod.multiyggdrasil.source.YggdrasilSourceType;

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
            //#if AUTHLIB < 50000
            YggdrasilEnvironment.PROD.getAuthHost(),
            //#endif
            //#if AUTHLIB < 60000
            YggdrasilEnvironment.PROD.getAccountsHost(),
            //#endif
            YggdrasilEnvironment.PROD.getSessionHost(),
            //#if AUTHLIB >= 20000
            //$$ YggdrasilEnvironment.PROD.getServicesHost(),
            //#endif
            //#if AUTHLIB >= 70000
            //$$ YggdrasilEnvironment.PROD.getProfilesHost(),
            //#endif
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
                    //#if AUTHLIB < 50000
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
                    //#endif

                    //#if AUTHLIB < 60000
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
                    //#endif

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

                    //#if AUTHLIB >= 20000
                    //$$ String servicesHost;
                    //$$ TomlValue servicesHostRaw = config.get(TomlKey.join(sourceKey, TomlKey.parse("servicesHost")));
                    //$$ if (servicesHostRaw == null) {
                    //$$     servicesHost = YggdrasilEnvironment.PROD.getServicesHost();
                    //$$ } else if (!(servicesHostRaw instanceof TomlPrimitive) || !servicesHostRaw.asPrimitive().isString()) {
                    //$$     MultiYggdrasil.LOGGER.warn("The servicesHost of Official Yggdrasil source {} is not a valid string. Skipped.", name);
                    //$$     continue;
                    //$$ } else {
                    //$$     servicesHost = servicesHostRaw.asPrimitive().asString();
                    //$$ }
                    //$$ if (servicesHost.endsWith("/")) {
                    //$$     servicesHost = servicesHost.substring(0, servicesHost.length() - 1);
                    //$$ }
                    //#endif

                    //#if AUTHLIB >= 70000
                    //$$ String profilesHost;
                    //$$ TomlValue profilesHostRaw = config.get(TomlKey.join(sourceKey, TomlKey.parse("profilesHost")));
                    //$$ if (profilesHostRaw == null) {
                    //$$     profilesHost = YggdrasilEnvironment.PROD.profilesHost();
                    //$$ } else if (!(profilesHostRaw instanceof TomlPrimitive) || !profilesHostRaw.asPrimitive().isString()) {
                    //$$     MultiYggdrasil.LOGGER.warn("The profilesHost of Official Yggdrasil source {} is not a valid string. Skipped.", name);
                    //$$     continue;
                    //$$ } else {
                    //$$     profilesHost = profilesHostRaw.asPrimitive().asString();
                    //$$ }
                    //$$ if (profilesHost.endsWith("/")) {
                    //$$     profilesHost = profilesHost.substring(0, profilesHost.length() - 1);
                    //$$ }
                    //#endif

                    sourceList.add(new OfficialYggdrasilSource(
                        name,
                        //#if AUTHLIB < 50000
                        authHost,
                        //#endif
                        //#if AUTHLIB < 60000
                        accountsHost,
                        //#endif
                        sessionHost,
                        //#if AUTHLIB >= 20000
                        //$$ servicesHost,
                        //#endif
                        //#if AUTHLIB >= 70000
                        //$$ profilesHost,
                        //#endif
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
                    //#if AUTHLIB >= 31100
                    //$$ ParseUtil.getPublicKey(apiRoot).ifPresent(MultiYggdrasilServicesKeyInfo.PUBLIC_KEYS::add);
                    //#endif
                    sourceList.add(new BlessingSkinYggdrasilSource(name, apiRoot, ordinal));
            }
        }
        return new YggdrasilConfig(sourceList);
    }
}
