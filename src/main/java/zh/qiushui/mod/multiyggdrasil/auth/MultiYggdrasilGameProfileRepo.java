package zh.qiushui.mod.multiyggdrasil.auth;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

//#if AUTHLIB < 10600
//$$ import zh.qiushui.mod.multiyggdrasil.util.vap.Environment;
//#else
import com.mojang.authlib.Environment;
//#endif

//#if AUTHLIB < 50000
import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;
import zh.qiushui.mod.multiyggdrasil.util.RequestUtil;
//#else
//$$ import com.mojang.authlib.exceptions.MinecraftClientException;
//$$ import com.mojang.authlib.minecraft.client.MinecraftClient;
//#endif

//#if AUTHLIB >= 70000
//$$ import com.mojang.authlib.yggdrasil.response.NameAndId;
//$$ import java.util.Locale;
//$$ import java.util.Optional;
//#endif

//#if MC < 11800
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//#else
//$$ import com.mojang.logging.LogUtils;
//$$ import org.slf4j.Logger;
//#endif

public class MultiYggdrasilGameProfileRepo implements GameProfileRepository {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int ENTRIES_PER_PAGE = 2;
    private static final int MAX_FAIL_COUNT_PER_ENV = 3;
    private static final int DELAY_BETWEEN_PAGES = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;

    //#if AUTHLIB < 50000
    private final Proxy proxy;
    //#else
    //$$ private final MinecraftClient client;
    //#endif
    private final List<String> searchPageUrls = new ArrayList<>();
    //#if AUTHLIB >= 70000
    //$$ private final List<String> nameLookupUrls = new ArrayList<>();
    //#endif

    public MultiYggdrasilGameProfileRepo(Proxy proxy, List<Environment> envs) {
        //#if AUTHLIB < 50000
        this.proxy = proxy;
        //#else
        //$$ this.client = MinecraftClient.unauthenticated(proxy);
        //#endif
        for (Environment env : envs) {
            this.searchPageUrls.add(
                //#if AUTHLIB < 50000
                env.getAccountsHost() + "/profiles/"
                //#elseif AUTHLIB < 60000
                //$$ env.accountsHost() + "/profiles/"
                //#elseif AUTHLIB < 70000
                //$$ env.servicesHost() + "/minecraft/profile/lookup/bulk/byname"
                //#else
                //$$ env.profilesHost() + "/minecraft/profile/lookup/bulk/byname"
                //#endif
            );
            //#if AUTHLIB >= 70000
            //$$ this.nameLookupUrls.add(env.profilesHost() + "/minecraft/profile/lookup/name");
            //#endif
        }
    }

    @Override
    public void findProfilesByNames(
        final String[] names,
        //#if AUTHLIB < 50000
        final Agent agent,
        //#endif
        final ProfileLookupCallback callback
    ) {
        final Set<String> criteria = Sets.newHashSet();

        for (final String name : names) {
            if (!Strings.isNullOrEmpty(name)) {
                criteria.add(name.toLowerCase());
            }
        }

        for (Iterator<String> iterator = this.searchPageUrls.iterator(); iterator.hasNext(); ) {
            String searchPageUrl = iterator.next();
            Map<String, GameProfile> results = findProfilesInSingleEnvByNames(
                //#if AUTHLIB < 50000
                agent,
                //#endif
                criteria,
                searchPageUrl,
                !iterator.hasNext() ? callback : null
            );

            for (final Map.Entry<String, GameProfile> entry : results.entrySet()) {
                LOGGER.debug("Successfully looked up profile {}", entry.getValue());
                criteria.remove(entry.getKey());
                callback.onProfileLookupSucceeded(entry.getValue());
            }
        }

        for (final String name : criteria) { // Still missing
            LOGGER.debug("Couldn't find profile {}", name);
            callback.onProfileLookupFailed(
                //#if AUTHLIB < 50000
                new GameProfile(null, name),
                //#else
                //$$ name,
                //#endif
                new ProfileNotFoundException("All servers did not find the requested profile")
            );
        }
    }

    private Map<String, GameProfile> findProfilesInSingleEnvByNames(
        //#if AUTHLIB < 50000
        Agent agent,
        //#endif
        Set<String> criteria,
        String searchPageUrl,
        @Nullable ProfileLookupCallback callbackOnLast
    ) {
        final Set<String> removing = new HashSet<>();
        final Map<String, GameProfile> results = new HashMap<>();
        for (final List<String> request : Iterables.partition(criteria, ENTRIES_PER_PAGE)) {
            int failCount = 0;
            boolean failed;

            do {
                failed = false;

                try {
                    //#if AUTHLIB < 50000
                    final ProfileSearchResultsResponse response = RequestUtil.makeRequest(
                        proxy,
                        HttpAuthenticationService.constantURL(searchPageUrl + agent.getName().toLowerCase()),
                        request,
                        ProfileSearchResultsResponse.class
                    );
                    //#else
                    //$$ final ProfileSearchResultsResponse response = client.post(
                    //$$     HttpAuthenticationService.constantURL(searchPageUrl + "minecraft"),
                    //$$     request,
                    //$$     ProfileSearchResultsResponse.class
                    //$$ );
                    //#endif
                    failCount = 0;

                    LOGGER.debug(
                        "Page {} returned {} results, parsing",
                        0,
                        //#if AUTHLIB < 50000
                        response.getProfiles().length
                        //#else
                        //$$ response.profiles().size()
                        //#endif
                    );

                    for (
                        GameProfile profile :
                        //#if AUTHLIB < 50000
                        response.getProfiles()
                        //#else
                        //$$ response.profiles()
                        //#endif
                    ) {
                        results.put(profile.getName().toLowerCase(), profile);
                    }

                    try {
                        Thread.sleep(DELAY_BETWEEN_PAGES);
                    } catch (final InterruptedException ignored) {
                    }
                } catch (
                    //#if AUTHLIB < 50000
                    final AuthenticationException e
                    //#else
                    //$$ final MinecraftClientException e
                    //#endif
                ) {
                    failCount++;

                    if (failCount == MAX_FAIL_COUNT_PER_ENV) {
                        boolean isLast = callbackOnLast != null;
                        for (final String name : request) {
                            LOGGER.debug(
                                "Couldn't find profile {} because of a server error.{}",
                                name,
                                isLast ? "" : " Trying another environment."
                            );
                            if (isLast) {
                                callbackOnLast.onProfileLookupFailed(
                                    //#if AUTHLIB < 50000
                                    new GameProfile(null, name),
                                    e
                                    //#else
                                    //$$ name,
                                    //$$ e.toAuthenticationException()
                                    //#endif
                                );
                                removing.add(name);
                            }
                        }
                    } else {
                        try {
                            Thread.sleep(DELAY_BETWEEN_FAILURES);
                        } catch (final InterruptedException ignored) {
                        }
                        failed = true;
                    }
                }
            } while (failed);
        }
        for (String name : removing) {
            criteria.remove(name);
        }
        return results;
    }

    //#if AUTHLIB >= 70000
    //$$ @Override
    //$$ public Optional<NameAndId> findProfileByName(final String name) {
    //$$     for (final String nameLookupUrl : nameLookupUrls) {
    //$$         try {
    //$$             return Optional.ofNullable(client.get(
    //$$                     HttpAuthenticationService.constantURL(nameLookupUrl + name.toLowerCase(Locale.ROOT)),
    //$$                     NameAndId.class
    //$$             ));
    //$$         } catch (final MinecraftClientException e) {
    //$$             LOGGER.warn("Couldn't find profile with name {} in a Yggdrasil source. Trying another...", name, e);
    //$$             return Optional.empty();
    //$$         }
    //$$     }
    //$$     LOGGER.warn("Couldn't find profile with name {} in all Yggdrasil sources.", name);
    //$$     return Optional.empty();
    //$$ }
    //#endif
}
