package com.qiushui1012.mod.multiyggdrasil.config;

import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import com.qiushui1012.mod.multiyggdrasil.MultiYggdrasil;
import com.qiushui1012.mod.multiyggdrasil.source.BaseYggdrasilSource;
import com.qiushui1012.mod.multiyggdrasil.source.BlessingSkinYggdrasilSource;
import com.qiushui1012.mod.multiyggdrasil.source.OfficialYggdrasilSource;
import com.qiushui1012.mod.multiyggdrasil.source.YggdrasilSourceType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

//#if AUTHLIB >= 31100
//$$ import com.qiushui1012.mod.multiyggdrasil.auth.MultiYggdrasilServicesKeyInfo;
//$$ import com.qiushui1012.mod.multiyggdrasil.util.ParseUtil;
//#endif

//#if FABRIC
import net.fabricmc.loader.api.FabricLoader;
//#elseif FORGE
//$$ import net.minecraftforge.fml.loading.FMLPaths;
//#endif

public class ConfigAgent {
    public static void save(Path configPath, YggdrasilConfig config) {
        tryMkConfigDirs();
        try (BufferedWriter writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            for (BaseYggdrasilSource source : config.getSources()) {
                String name = source.getName();
                writer.write('[' + name + ']');
                writer.newLine();

                List<String> lines = source.serialize();
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            MultiYggdrasil.LOGGER.info("Successfully saved config");
        } catch (IOException e) {
            MultiYggdrasil.LOGGER.warn("Cannot save config to {}.", configPath, e);
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

    public static YggdrasilConfig parse(Path configPath) {
        Map<String, Map<String, String>> configRaw;
        try {
            configRaw = IniParser.parseRaw(configPath);
        } catch (IOException | IllegalArgumentException e) {
            MultiYggdrasil.LOGGER.warn("Cannot load config. Use default config.", e);
            //#if AUTHLIB >= 31100
            //$$ for (BaseYggdrasilSource source : YggdrasilConfig.DEFAULT.getSources()) {
            //$$     if (source instanceof BlessingSkinYggdrasilSource blessing) {
            //$$         ParseUtil.getPublicKey(blessing.getApiRoot()).ifPresent(MultiYggdrasilServicesKeyInfo.PUBLIC_KEYS::add);
            //$$     }
            //$$ }
            //#endif
            return YggdrasilConfig.DEFAULT;
        }

        List<BaseYggdrasilSource> sources = new ArrayList<>();
        for (String name : configRaw.keySet()) {
            Map<String, String> config = configRaw.get(name);
            if (name.contains(".")) MultiYggdrasil.LOGGER.warn("Yggdrasil source key should not contains '.', got {}", name);

            String typeRaw = config.get("type");
            if (typeRaw == null) {
                MultiYggdrasil.LOGGER.warn("The type of Yggdrasil source {} is not set. Skipped.", name);
                continue;
            }
            YggdrasilSourceType type;
            try {
                type = YggdrasilSourceType.valueOf(typeRaw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                MultiYggdrasil.LOGGER.warn("The value of Yggdrasil source {}'s type is not valid. Skipped.", name, e);
                continue;
            }

            String ordinalRaw = config.get("ordinal");
            if (ordinalRaw == null) {
                MultiYggdrasil.LOGGER.warn("The ordinal of Yggdrasil source {} is not set. Skipped.", name);
                continue;
            }
            int ordinal;
            try {
                ordinal = Integer.parseInt(ordinalRaw);
            } catch (NumberFormatException e) {
                MultiYggdrasil.LOGGER.warn("The ordinal of Yggdrasil source {} cannot be lesser than 0. Skipped.", name, e);
                continue;
            }

            switch (type) {
                case OFFICIAL:
                    String authHost = config.get("authHost");
                    if (authHost == null) {
                        authHost = YggdrasilEnvironment.PROD.getAuthHost();
                    }
                    if (!authHost.endsWith("/")) {
                        authHost = authHost.concat("/");
                    }

                    String accountsHost = config.get("accountsHost");
                    if (accountsHost == null) {
                        accountsHost = YggdrasilEnvironment.PROD.getAccountsHost();
                    }
                    if (!accountsHost.endsWith("/")) {
                        accountsHost = accountsHost.concat("/");
                    }

                    String sessionHost = config.get("sessionHost");
                    if (sessionHost == null) {
                        sessionHost = YggdrasilEnvironment.PROD.getSessionHost();
                    }
                    if (!sessionHost.endsWith("/")) {
                        sessionHost = sessionHost.concat("/");
                    }

                    String servicesHost = config.get("servicesHost");
                    if (servicesHost == null) {
                        servicesHost = YggdrasilEnvironment.PROD.getServicesHost();
                    }
                    if (!servicesHost.endsWith("/")) {
                        servicesHost = servicesHost.concat("/");
                    }

                    sources.add(new OfficialYggdrasilSource(
                        name,
                        authHost,
                        accountsHost,
                        sessionHost,
                        servicesHost,
                        ordinal
                    ));
                    break;
                case BLESSING_SKIN:
                    String apiRoot = config.get("apiRoot");
                    if (apiRoot == null) {
                        MultiYggdrasil.LOGGER.warn("The apiRoot of BlessingSkin Yggdrasil source {} is not set. Skipped.", name);
                        continue;
                    }
                    if (!apiRoot.endsWith("/")) {
                        apiRoot = apiRoot.concat("/");
                    }
                    //#if AUTHLIB >= 31100
                    //$$ ParseUtil.getPublicKey(apiRoot).ifPresent(MultiYggdrasilServicesKeyInfo.PUBLIC_KEYS::add);
                    //#endif
                    sources.add(new BlessingSkinYggdrasilSource(name, apiRoot, ordinal));
            }
        }
        return new YggdrasilConfig(sources);
    }
}