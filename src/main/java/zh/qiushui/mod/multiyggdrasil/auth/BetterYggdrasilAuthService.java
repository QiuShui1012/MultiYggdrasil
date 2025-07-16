package zh.qiushui.mod.multiyggdrasil.auth;

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.Agent;
import com.mojang.authlib.Environment;
import com.mojang.authlib.EnvironmentParser;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InsufficientPrivilegesException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.UserBannedException;
import com.mojang.authlib.exceptions.UserMigratedException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import com.mojang.authlib.yggdrasil.response.Response;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zh.qiushui.mod.multiyggdrasil.MultiYggdrasil;
import zh.qiushui.mod.multiyggdrasil.yggdrasil.BaseYggdrasilSource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BetterYggdrasilAuthService extends YggdrasilAuthenticationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BetterYggdrasilAuthService.class);

    @Nullable
    private final String clientToken;
    private final ObjectMapper objectMapper = ObjectMapper.create();
    private final List<Environment> environments;
    @Getter
    private final ServicesKeySet servicesKeySet = type -> List.of(new BetterYggdrasilServicesKeyInfo());

    public BetterYggdrasilAuthService(final Proxy proxy) {
        this(proxy, null, determineEnvironment());
    }

    private BetterYggdrasilAuthService(
        final Proxy proxy, @Nullable final String clientToken, List<Environment> environments) {
        super(proxy);
        this.clientToken = clientToken;
        this.environments = environments;
        LOGGER.info("Environments: {}", environments);
    }

    private static List<Environment> determineEnvironment() {
        List<BaseYggdrasilSource> envs = new ArrayList<>(MultiYggdrasil.SERVERS_CONFIG.sources());
        Environment normalEnv = EnvironmentParser.getEnvironmentFromProperties()
            .orElse(YggdrasilEnvironment.PROD.getEnvironment());
        if (envs.isEmpty()) return List.of(normalEnv);
        Collections.sort(envs);
        return ImmutableList.copyOf(envs);
    }

    @Override
    public UserAuthentication createUserAuthentication(Agent agent) {
        return new BetterYggdrasilUserAuth(this, this.clientToken, agent, this.environments);
    }

    @Override
    public MinecraftSessionService createMinecraftSessionService() {
        return new BetterYggdrasilMcSessionService(this, environments);
    }

    @Override
    public GameProfileRepository createProfileRepository() {
        return new BetterYggdrasilGameProfileRepo(this, environments);
    }

    protected <T extends Response> T makeRequest(
        final List<URL> urls, final Object input, final Class<T> clazz) throws AuthenticationException {
        return makeRequest(urls, input, clazz, null);
    }

    @SuppressWarnings("SameParameterValue")
    protected <T extends Response> T makeRequest(
        final List<URL> urls, final Object input, final Class<T> clazz, @Nullable final String authentication
    ) throws AuthenticationException {
        T resultCache = null;
        boolean areAuthServersAvailable = false;
        for (URL url : urls) {
            try {
                final String jsonResult =
                    input == null
                    ? performGetRequest(url, authentication)
                    : performPostRequest(url, objectMapper.writeValueAsString(input), "application/json");
                areAuthServersAvailable = true;
                final T result = resultCache = objectMapper.readValue(jsonResult, clazz);

                if (result == null) continue;
                if (StringUtils.isBlank(result.getError())) return result;
            } catch (final IOException | IllegalStateException | MinecraftClientException e) {
                LOGGER.warn("Cannot contact authentication server from {}", url, e);
            }
        }
        if (!areAuthServersAvailable) throw new AuthenticationUnavailableException("Cannot contact all authentication servers");
        if (resultCache == null) return null;
        if ("UserMigratedException".equals(resultCache.getCause())) {
            throw new UserMigratedException(resultCache.getErrorMessage());
        } else if ("ForbiddenOperationException".equals(resultCache.getError())) {
            throw new InvalidCredentialsException(resultCache.getErrorMessage());
        } else if ("InsufficientPrivilegesException".equals(resultCache.getError())) {
            throw new InsufficientPrivilegesException(resultCache.getErrorMessage());
        } else if ("multiplayer.access.banned".equals(resultCache.getError())) {
            throw new UserBannedException();
        } else {
            throw new AuthenticationException(resultCache.getErrorMessage());
        }
    }

    protected <T extends Response> T makeRequest(
        final URL url, final Object input, final Class<T> classOfT
    ) throws AuthenticationException {
        return makeRequest(url, input, classOfT, null);
    }

    protected <T extends Response> T makeRequest(
        final URL url, final Object input, final Class<T> classOfT, @Nullable final String authentication
    ) throws AuthenticationException {
        try {
            final String jsonResult = input == null ? performGetRequest(url, authentication) : performPostRequest(
                url, objectMapper.writeValueAsString(input), "application/json");
            final T result = objectMapper.readValue(jsonResult, classOfT);

            if (result == null) {
                return null;
            }

            if (StringUtils.isNotBlank(result.getError())) {
                if ("UserMigratedException".equals(result.getCause())) {
                    throw new UserMigratedException(result.getErrorMessage());
                } else if ("ForbiddenOperationException".equals(result.getError())) {
                    throw new InvalidCredentialsException(result.getErrorMessage());
                } else if ("InsufficientPrivilegesException".equals(result.getError())) {
                    throw new InsufficientPrivilegesException(result.getErrorMessage());
                } else if ("multiplayer.access.banned".equals(result.getError())) {
                    throw new UserBannedException();
                } else {
                    throw new AuthenticationException(result.getErrorMessage());
                }
            }

            return result;
        } catch (final IOException | IllegalStateException | MinecraftClientException e) {
            throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
        }
    }
}
