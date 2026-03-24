package com.qiushui1012.mod.multiyggdrasil.auth;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

//#if AUTHLIB >= 60058
//$$ import java.util.Locale;
//$$ import java.util.Optional;
//#endif

public class MultiYggdrasilGameProfileRepo implements GameProfileRepository {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int ENTRIES_PER_PAGE = 2;
    private static final int MAX_FAIL_COUNT_PER_ENV = 3;
    private static final int DELAY_BETWEEN_PAGES = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;

    private final MinecraftClient client;
    private final List<String> searchPageUrls = new ArrayList<>();
    //#if AUTHLIB >= 60058
    //$$ private final List<String> nameLookupUrls = new ArrayList<>();
    //#endif

    public MultiYggdrasilGameProfileRepo(Proxy proxy, List<Environment> envs) {
        this.client = MinecraftClient.unauthenticated(proxy);
        for (Environment env : envs) {
            this.searchPageUrls.add(env.servicesHost() + "/minecraft/profile/lookup/bulk/byname");
            //#if AUTHLIB >= 60058
            //$$ this.nameLookupUrls.add(env.servicesHost() + "/minecraft/profile/lookup/name/");
            //#endif
        }
    }

    @Override
    public void findProfilesByNames(
        final String[] names,
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
                name,
                new ProfileNotFoundException("All servers did not find the requested profile")
            );
        }
    }

    private Map<String, GameProfile> findProfilesInSingleEnvByNames(
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
                    final ProfileSearchResultsResponse response = client.post(
                        HttpAuthenticationService.constantURL(searchPageUrl + "minecraft"),
                        request,
                        ProfileSearchResultsResponse.class
                    );
                    failCount = 0;

                    LOGGER.debug(
                        "Page {} returned {} results, parsing",
                        0,
                        response.profiles().size()
                    );

                    for (GameProfile profile : response.profiles()) {
                        results.put(profile.getName().toLowerCase(), profile);
                    }

                    try {
                        Thread.sleep(DELAY_BETWEEN_PAGES);
                    } catch (final InterruptedException ignored) {
                    }
                } catch (final MinecraftClientException e) {
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
                                callbackOnLast.onProfileLookupFailed(name, e.toAuthenticationException());
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

    //#if AUTHLIB >= 60058
    //$$ @Override
    //$$ public Optional<GameProfile> findProfileByName(final String name) {
    //$$     for (String nameLookupUrl : this.nameLookupUrls) {
    //$$         try {
    //$$             return Optional.ofNullable(client.get(
    //$$                 HttpAuthenticationService.constantURL(nameLookupUrl + name.toLowerCase(Locale.ROOT)),
    //$$                 GameProfile.class
    //$$             ));
    //$$         } catch (final MinecraftClientException ignored) {
    //$$         }
    //$$     }
    //$$     LOGGER.warn("Couldn't find profile with name: {}", name);
    //$$     return Optional.empty();
    //$$ }
    //#endif
}
