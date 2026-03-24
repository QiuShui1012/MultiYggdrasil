package com.qiushui1012.mod.multiyggdrasil.auth;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.authlib.Environment;
import com.mojang.authlib.EnvironmentParser;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.qiushui1012.mod.multiyggdrasil.MultiYggdrasil;
import com.qiushui1012.mod.multiyggdrasil.source.BaseYggdrasilSource;
import com.qiushui1012.mod.multiyggdrasil.util.vap.Patterns;
import lombok.Getter;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//#if MC < 11800
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//#else
//$$ import com.mojang.logging.LogUtils;
//$$ import org.slf4j.Logger;
//#endif

public class MultiYggdrasilAuthService extends YggdrasilAuthenticationService {
    private static final Logger LOGGER = LogManager.getLogger();

    private final List<Environment> environments;
    @Getter
    private final ServicesKeySet servicesKeySet = x -> List.of(new MultiYggdrasilServicesKeyInfo());

    public MultiYggdrasilAuthService(final Proxy proxy) {
        this(proxy, determineEnvironment());
    }

    private MultiYggdrasilAuthService(final Proxy proxy, List<Environment> environments) {
        super(proxy);
        this.environments = environments;
        LOGGER.info("Environments: {}", environments);
    }

    private static List<Environment> determineEnvironment() {
        List<BaseYggdrasilSource> envs = MultiYggdrasil.SERVERS_CONFIG.sources();
        if (envs.isEmpty()) return Lists.newArrayList(
            EnvironmentParser.getEnvironmentFromProperties().orElse(Patterns.getDefaultEnv())
        );
        Collections.sort(envs);
        List<Environment> result = new ArrayList<>();
        for (BaseYggdrasilSource source : envs) {
            result.add(source.toEnvironment());
        }
        return ImmutableList.copyOf(result);
    }

    @Override
    public GameProfileRepository createProfileRepository() {
        return new MultiYggdrasilGameProfileRepo(this.getProxy(), environments);
    }

    @Override
    public MinecraftSessionService createMinecraftSessionService() {
        return new MultiYggdrasilMcSessionService(this, environments);
    }

    @Override
    public UserApiService createUserApiService(final String accessToken) {
        return UserApiService.OFFLINE;
    }
}
