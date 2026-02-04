package zh.qiushui.mod.multiyggdrasil.auth;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import zh.qiushui.mod.multiyggdrasil.MultiYggdrasil;
import zh.qiushui.mod.multiyggdrasil.util.RequestUtil;
import zh.qiushui.mod.multiyggdrasil.source.BaseYggdrasilSource;
import zh.qiushui.mod.multiyggdrasil.util.vap.Patterns;

import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//#if AUTHLIB < 10600
//$$ import zh.qiushui.mod.multiyggdrasil.util.vap.Environment;
//#else
import com.mojang.authlib.Environment;
import com.mojang.authlib.EnvironmentParser;
//#endif

//#if AUTHLIB >= 20000
//$$ import lombok.Getter;
//#endif

//#if AUTHLIB >= 20000 && AUTHLIB < 30000
//$$ import com.mojang.authlib.yggdrasil.YggdrasilSocialInteractionsService;
//#elseif AUTHLIB >= 30000
//$$ import com.mojang.authlib.minecraft.UserApiService;
//#endif

//#if AUTHLIB >= 31100 && AUTHLIB < 40000
//$$ import com.mojang.authlib.yggdrasil.ServicesKeyInfo;
//#elseif AUTHLIB >= 40000
//$$ import com.mojang.authlib.yggdrasil.ServicesKeySet;
//#endif

//#if AUTHLIB < 50000
import com.mojang.authlib.Agent;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.yggdrasil.response.Response;
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
    //#if AUTHLIB >= 20000 && AUTHLIB < 50000
    //$$ @Getter
    //$$ private final String clientToken;
    //#endif

    public MultiYggdrasilAuthService(
        final Proxy proxy
        //#if AUTHLIB < 50000
        ,final String clientToken
        //#endif
    ) {
        this(
            proxy,
            //#if AUTHLIB < 50000
            clientToken,
            //#endif
            determineEnvironment()
        );
    }

    private MultiYggdrasilAuthService(
        final Proxy proxy,
        //#if AUTHLIB < 50000
        final String clientToken,
        //#endif
        List<Environment> environments
    ) {
        super(
            proxy
            //#if AUTHLIB < 50000
            ,clientToken
            //#endif
        );
        //#if AUTHLIB >= 20000 && AUTHLIB < 50000
        //$$ this.clientToken = clientToken;
        //#endif
        this.environments = environments;
        LOGGER.info("Environments: {}", environments);
    }

    private static List<Environment> determineEnvironment() {
        List<BaseYggdrasilSource> envs = MultiYggdrasil.SERVERS_CONFIG.getSources();
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

    //#if AUTHLIB >= 20000 && AUTHLIB < 30000
    //$$ @Override
    //$$ public YggdrasilSocialInteractionsService createSocialInteractionsService(final String accessToken) throws AuthenticationException {
    //$$     throw new AuthenticationException("Forced enabling multiplay permission.");
    //$$ }
    //#elseif AUTHLIB >= 30000
    //$$ @Override
    //$$ public UserApiService createUserApiService(final String accessToken) throws AuthenticationException {
    //$$     return UserApiService.OFFLINE;
    //$$ }
    //#endif

    //#if AUTHLIB < 50000
    @Override
    public UserAuthentication createUserAuthentication(Agent agent) {
        return new MultiYggdrasilUserAuth(this, getClientToken(), agent, environments);
    }

    @Override
    protected <T extends Response> T makeRequest(URL url, Object input, Class<T> classOfT) throws AuthenticationException {
        return RequestUtil.makeRequest(this.getProxy(), url, input, classOfT);
    }
    //#endif
}
