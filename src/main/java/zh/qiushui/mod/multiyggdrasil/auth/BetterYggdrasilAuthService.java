package zh.qiushui.mod.multiyggdrasil.auth;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.authlib.Environment;
import com.mojang.authlib.EnvironmentParser;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import com.mojang.authlib.yggdrasil.YggdrasilGameProfileRepository;
import com.mojang.datafixers.util.Pair;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zh.qiushui.mod.multiyggdrasil.MultiYggdrasil;
import zh.qiushui.mod.multiyggdrasil.yggdrasil.BaseYggdrasilSource;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BetterYggdrasilAuthService extends YggdrasilAuthenticationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BetterYggdrasilAuthService.class);

    private final List<Environment> environments;
    private final Environment servicesEnv;
    @Getter
    private final ServicesKeySet servicesKeySet = type -> List.of(new BetterYggdrasilServicesKeyInfo());

    public BetterYggdrasilAuthService(final Proxy proxy) {
        this(proxy, determineEnvironment());
    }

    private BetterYggdrasilAuthService(final Proxy proxy, Pair<List<Environment>, Environment> environments) {
        super(proxy);
        this.environments = environments.getFirst();
        this.servicesEnv = environments.getSecond();
        LOGGER.info("Environments: {}", environments);
    }

    private static Pair<List<Environment>, Environment> determineEnvironment() {
        List<BaseYggdrasilSource> envs = new ArrayList<>(MultiYggdrasil.SERVERS_CONFIG.sources());
        Environment servicesEnv = EnvironmentParser.getEnvironmentFromProperties()
            .orElse(YggdrasilEnvironment.PROD.getEnvironment());
        if (envs.isEmpty()) return new Pair<>(List.of(YggdrasilEnvironment.PROD.getEnvironment()), servicesEnv);
        Collections.sort(envs);
        return new Pair<>(ImmutableList.copyOf(Lists.transform(envs, BaseYggdrasilSource::toEnvironment)), servicesEnv);
    }

    @Override
    public MinecraftSessionService createMinecraftSessionService() {
        return new BetterYggdrasilMcSessionService(servicesKeySet, getProxy(), environments);
    }

    @Override
    public GameProfileRepository createProfileRepository() {
        return new YggdrasilGameProfileRepository(getProxy(), servicesEnv);
    }

    @Override
    public UserApiService createUserApiService(final String accessToken) {
        throw new UnsupportedOperationException("Should not use BetterYggdrasilAuthService$createUserApiService method.");
    }
}
