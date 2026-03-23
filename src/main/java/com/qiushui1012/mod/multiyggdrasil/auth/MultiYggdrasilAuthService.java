package com.qiushui1012.mod.multiyggdrasil.auth;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.response.Response;
import com.qiushui1012.mod.multiyggdrasil.MultiYggdrasil;
import com.qiushui1012.mod.multiyggdrasil.source.BaseYggdrasilSource;
import com.qiushui1012.mod.multiyggdrasil.util.RequestUtil;
import com.qiushui1012.mod.multiyggdrasil.util.vap.VersionUtil;

import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//#if AUTHLIB < 10600
//$$ import com.qiushui1012.mod.multiyggdrasil.util.vap.Environment;
//#else
import com.mojang.authlib.Environment;
import com.mojang.authlib.EnvironmentParser;
//#endif


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MultiYggdrasilAuthService extends YggdrasilAuthenticationService {
    private static final Logger LOGGER = LogManager.getLogger();

    private final List<Environment> environments;

    public MultiYggdrasilAuthService(
        final Proxy proxy
        ,final String clientToken
    ) {
        this(
            proxy,
            clientToken,
            determineEnvironment()
        );
    }

    private MultiYggdrasilAuthService(
        final Proxy proxy,
        final String clientToken,
        List<Environment> environments
    ) {
        super(proxy, clientToken);
        this.environments = environments;
        LOGGER.info("Environments: {}", environments);
    }

    private static List<Environment> determineEnvironment() {
        List<BaseYggdrasilSource> envs = MultiYggdrasil.SERVERS_CONFIG.getSources();
        if (envs.isEmpty()) return Lists.newArrayList(
            EnvironmentParser.getEnvironmentFromProperties().orElse(VersionUtil.DEFAULT)
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
    public UserAuthentication createUserAuthentication(Agent agent) {
        return new MultiYggdrasilUserAuth(this, getClientToken(), agent, environments);
    }

    @Override
    protected <T extends Response> T makeRequest(URL url, Object input, Class<T> classOfT) throws AuthenticationException {
        return RequestUtil.makeRequest(this.getProxy(), url, input, classOfT);
    }
}
