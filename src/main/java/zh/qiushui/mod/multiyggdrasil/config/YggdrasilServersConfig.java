package zh.qiushui.mod.multiyggdrasil.config;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import zh.qiushui.mod.multiyggdrasil.MultiYggdrasil;
import zh.qiushui.mod.multiyggdrasil.auth.BetterYggdrasilServicesKeyInfo;
import zh.qiushui.mod.multiyggdrasil.util.ParseUtil;
import zh.qiushui.mod.multiyggdrasil.yggdrasil.BaseYggdrasilSource;
import zh.qiushui.mod.multiyggdrasil.yggdrasil.BlessingSkinYggdrasilSource;
import zh.qiushui.mod.multiyggdrasil.yggdrasil.OfficialYggdrasilSource;
import zh.qiushui.mod.multiyggdrasil.yggdrasil.YggdrasilSourceType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public record YggdrasilServersConfig(List<BaseYggdrasilSource> sources) {
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("multi-yggdrasil.toml");

    public static void save(YggdrasilServersConfig config) {
        tryMkConfigDirs();
        TomlWriter writer = new TomlWriter();
        try {
            Map<String, Object> result = new TreeMap<>();
            for (BaseYggdrasilSource source : config.sources) {
                result.putAll(source.serialize());
            }
            writer.write(result, PATH.toFile());
        } catch (IOException e) {
            MultiYggdrasil.LOGGER.warn("Cannot save config to {}.", PATH, e);
        }
    }

    private static void tryMkConfigDirs() {
        if (!FMLPaths.CONFIGDIR.get().toFile().exists() && !FMLPaths.CONFIGDIR.get().toFile().mkdirs()) {
            MultiYggdrasil.LOGGER.warn("Cannot mk config dirs. Path: {}", FMLPaths.CONFIGDIR.get());
        }
    }

    public static YggdrasilServersConfig load() {
        Toml config;
        try {
            config = new Toml().read(PATH.toFile());
        } catch (IllegalStateException e) {
            MultiYggdrasil.LOGGER.warn(
                "Cannot load config. If you are first starting with this mod, you can ignore this warn, and finish your config in {}",
                PATH, e
            );
            return new YggdrasilServersConfig(List.of());
        }

        List<BaseYggdrasilSource> sourceList = new ArrayList<>();
        for (var sourceRaw : config.entrySet()) {
            String name = sourceRaw.getKey();
            YggdrasilSourceType type = YggdrasilSourceType.valueOf(config.getString(name.concat(".type")).toUpperCase(Locale.ROOT));
            int ordinal = config.getLong(name.concat(".ordinal")).intValue();
            if (ordinal < 0) throw new IllegalArgumentException("The ordinal cannot be lesser than 0! From source " + name);
            sourceList.add(switch (type) {
                case OFFICIAL -> {
                    String authHost = config.getString(name.concat(".authHost"));
                    if (authHost == null) {
                        authHost = YggdrasilEnvironment.PROD.getEnvironment().getAuthHost();
                    }
                    if (authHost.endsWith("/")) {
                        authHost = authHost.substring(0, authHost.length() - 1);
                    }
                    String accountsHost = config.getString(name.concat(".accountsHost"));
                    if (accountsHost == null) {
                        accountsHost = YggdrasilEnvironment.PROD.getEnvironment().getAccountsHost();
                    }
                    if (accountsHost.endsWith("/")) {
                        accountsHost = accountsHost.substring(0, accountsHost.length() - 1);
                    }
                    String sessionHost = config.getString(name.concat(".sessionHost"));
                    if (sessionHost == null) {
                        sessionHost = YggdrasilEnvironment.PROD.getEnvironment().getSessionHost();
                    }
                    if (sessionHost.endsWith("/")) {
                        sessionHost = sessionHost.substring(0, sessionHost.length() - 1);
                    }
                    yield new OfficialYggdrasilSource(name, authHost, accountsHost, sessionHost, ordinal);
                }
                case BLESSING_SKIN -> {
                    String apiRoot = config.getString(name.concat(".apiRoot"));
                    if (!apiRoot.endsWith("/")) {
                        apiRoot = apiRoot.concat("/");
                    }
                    ParseUtil.getPublicKey(apiRoot).ifPresent(BetterYggdrasilServicesKeyInfo.PUBLIC_KEYS::add);
                    yield new BlessingSkinYggdrasilSource(name, apiRoot, ordinal);
                }
            });
        }
        return new YggdrasilServersConfig(List.copyOf(sourceList));
    }
}
