package com.qiushui1012.mod.multiyggdrasil.auth;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import com.mojang.authlib.yggdrasil.response.NameAndId;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

public class MultiYggdrasilGameProfileRepo implements GameProfileRepository {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int ENTRIES_PER_PAGE = 2;
    private static final int MAX_FAIL_COUNT_PER_ENV = 3;
    private static final int DELAY_BETWEEN_PAGES = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;

    private final MinecraftClient client;
    private final List<String> searchPageUrls = new ArrayList<>();
    private final List<String> nameLookupUrls = new ArrayList<>();

    public MultiYggdrasilGameProfileRepo(Proxy proxy, List<Environment> envs) {
        this.client = MinecraftClient.unauthenticated(proxy);
        for (Environment env : envs) {
            this.searchPageUrls.add(env.profilesHost() + "/minecraft/profile/lookup/bulk/byname");
            this.nameLookupUrls.add(env.profilesHost() + "/minecraft/profile/lookup/name/");
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
            Map<String, NameAndId> results = findProfilesInSingleEnvByNames(
                criteria,
                searchPageUrl,
                !iterator.hasNext() ? callback : null
            );

            for (final Map.Entry<String, NameAndId> entry : results.entrySet()) {
                NameAndId result = entry.getValue();
                LOGGER.debug("Successfully looked up profile {}", result);
                criteria.remove(entry.getKey());
                callback.onProfileLookupSucceeded(result.name(), result.id());
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

    private Map<String, NameAndId> findProfilesInSingleEnvByNames(
        Set<String> criteria,
        String searchPageUrl,
        @Nullable ProfileLookupCallback callbackOnLast
    ) {
        final Set<String> removing = new HashSet<>();
        final Map<String, NameAndId> results = new HashMap<>();
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

                    for (NameAndId profile : response.profiles()) {
                        results.put(profile.name().toLowerCase(), profile);
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

    @Override
    public Optional<NameAndId> findProfileByName(final String name) {
        for (String nameLookupUrl : this.nameLookupUrls) {
            try {
                return Optional.ofNullable(client.get(
                    HttpAuthenticationService.constantURL(nameLookupUrl + name.toLowerCase(Locale.ROOT)),
                    NameAndId.class
                ));
            } catch (final MinecraftClientException ignored) {
            }
        }
        LOGGER.warn("Couldn't find profile with name: {}", name);
        return Optional.empty();
    }
}
