package com.qiushui1012.mod.multiyggdrasil.auth;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.authlib.Agent;
import com.mojang.authlib.Environment;
import com.mojang.authlib.EnvironmentParser;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import com.mojang.authlib.yggdrasil.response.Response;
import com.qiushui1012.mod.multiyggdrasil.MultiYggdrasil;
import com.qiushui1012.mod.multiyggdrasil.util.RequestUtil;
import com.qiushui1012.mod.multiyggdrasil.source.BaseYggdrasilSource;
import lombok.Getter;

import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//#if AUTHLIB >= 20000 && AUTHLIB < 30000
import com.mojang.authlib.yggdrasil.YggdrasilSocialInteractionsService;
//#elseif AUTHLIB >= 30000
//$$ import com.mojang.authlib.minecraft.UserApiService;
//#endif

//#if AUTHLIB >= 31100 && AUTHLIB < 40000
//$$ import com.mojang.authlib.yggdrasil.ServicesKeyInfo;
//#elseif AUTHLIB >= 40000
//$$ import com.mojang.authlib.yggdrasil.ServicesKeySet;
//#endif

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
    //#if AUTHLIB >= 31100 && AUTHLIB < 40000
    //$$ @Getter
    //$$ private final ServicesKeyInfo servicesKey = new MultiYggdrasilServicesKeyInfo();
    //#elseif AUTHLIB >= 40000
    //$$ @Getter
    //$$ private final ServicesKeySet servicesKeySet = x -> List.of(new MultiYggdrasilServicesKeyInfo());
    //#endif
    @Getter
    private final String clientToken;

    public MultiYggdrasilAuthService(final Proxy proxy) {
        this(proxy, null, determineEnvironment());
    }

    private MultiYggdrasilAuthService(
        final Proxy proxy,
        final String clientToken,
        List<Environment> environments
    ) {
        super(proxy, clientToken);
        this.clientToken = clientToken;
        this.environments = environments;
        LOGGER.info("Environments: {}", environments);
    }

    private static List<Environment> determineEnvironment() {
        List<BaseYggdrasilSource> envs = MultiYggdrasil.SERVERS_CONFIG.getSources();
        if (envs.isEmpty()) return Lists.newArrayList(
            EnvironmentParser.getEnvironmentFromProperties().orElse(YggdrasilEnvironment.PROD)
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

    //#if AUTHLIB >= 20000 && AUTHLIB < 30000
    @Override
    public YggdrasilSocialInteractionsService createSocialInteractionsService(final String accessToken) throws AuthenticationException {
        throw new AuthenticationException("Forced enabling multiplay permission.");
    }
    //#elseif AUTHLIB >= 30000
    //$$ @Override
    //$$ public UserApiService createUserApiService(final String accessToken) throws AuthenticationException {
    //$$     return UserApiService.OFFLINE;
    //$$ }
    //#endif

    @Override
    public UserAuthentication createUserAuthentication(Agent agent) {
        return new MultiYggdrasilUserAuth(this, getClientToken(), agent, environments);
    }

    @Override
    protected <T extends Response> T makeRequest(URL url, Object input, Class<T> classOfT) throws AuthenticationException {
        return RequestUtil.makeRequest(this.getProxy(), url, input, classOfT);
    }
}
