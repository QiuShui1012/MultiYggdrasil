package zh.qiushui.mod.multiyggdrasil.auth;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zh.qiushui.mod.multiyggdrasil.MultiYggdrasil;
import zh.qiushui.mod.multiyggdrasil.yggdrasil.IYggdrasilSource;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BetterYggdrasilAuthService extends YggdrasilAuthenticationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BetterYggdrasilAuthService.class);

    private final List<Environment> environments;
    @Getter
    private final ServicesKeySet servicesKeySet;

    public BetterYggdrasilAuthService(final Proxy proxy) {
        this(proxy, determineEnvironment());
    }

    public BetterYggdrasilAuthService(final Proxy proxy, List<Environment> environments) {
        super(proxy);
        this.environments = environments;
        LOGGER.info("Environments: {}", environments);

        servicesKeySet = ServicesKeySet.lazy(() -> type -> List.of(new BetterYggdrasilServicesKeyInfo()));
    }

    private static List<Environment> determineEnvironment() {
        List<IYggdrasilSource> envs = new ArrayList<>(MultiYggdrasil.SERVERS_CONFIG.sources());
        if (envs.isEmpty()) return List.of(YggdrasilEnvironment.PROD.getEnvironment());
        Collections.sort(envs);
        return ImmutableList.copyOf(Lists.transform(envs, IYggdrasilSource::toEnvironment));
    }

    @Override
    public MinecraftSessionService createMinecraftSessionService() {
        return new BetterYggdrasilMcSessionService(servicesKeySet, getProxy(), environments);
    }

    @Override
    public GameProfileRepository createProfileRepository() {
        return new BetterYggdrasilGPRepository(getProxy(), environments);
    }

    @Override
    public UserApiService createUserApiService(final String accessToken) {
        throw new RuntimeException("Should not use BetterYggdrasilAuthService$createUserApiService method.");
    }
}
