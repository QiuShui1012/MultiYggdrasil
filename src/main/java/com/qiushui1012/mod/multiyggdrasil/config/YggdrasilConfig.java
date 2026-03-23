package com.qiushui1012.mod.multiyggdrasil.config;

import com.google.common.collect.Lists;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import lombok.Getter;
import com.qiushui1012.mod.multiyggdrasil.source.BaseYggdrasilSource;
import com.qiushui1012.mod.multiyggdrasil.source.BlessingSkinYggdrasilSource;
import com.qiushui1012.mod.multiyggdrasil.source.OfficialYggdrasilSource;

//#if FABRIC
import net.fabricmc.loader.api.FabricLoader;
//#elseif FORGE
//$$ import net.minecraftforge.fml.loading.FMLPaths;
//#elseif NEOFORGE
//$$ import net.neoforged.fml.loading.FMLPaths;
//#endif

import java.nio.file.Path;
import java.util.List;

public class YggdrasilConfig {
    public static final YggdrasilConfig DEFAULT = new YggdrasilConfig(Lists.newArrayList(
        new OfficialYggdrasilSource(
            "MojangOfficial",
            YggdrasilEnvironment.PROD.getAuthHost(),
            YggdrasilEnvironment.PROD.getAccountsHost(),
            YggdrasilEnvironment.PROD.getSessionHost(),
            YggdrasilEnvironment.PROD.getServicesHost(),
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

    public void save() {
        ConfigAgent.save(PATH, this);
    }

    public static YggdrasilConfig load() {
        return ConfigAgent.parse(PATH);
    }
}
