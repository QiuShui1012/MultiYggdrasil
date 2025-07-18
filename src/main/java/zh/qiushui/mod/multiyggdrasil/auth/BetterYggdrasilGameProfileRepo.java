package zh.qiushui.mod.multiyggdrasil.auth;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.util.tuples.Pair;

import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class BetterYggdrasilGameProfileRepo implements GameProfileRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(BetterYggdrasilGameProfileRepo.class);
    private static final int ENTRIES_PER_PAGE = 2;
    private static final int MAX_FAIL_COUNT = 3;
    private static final int DELAY_BETWEEN_PAGES = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;

    private final MinecraftClient client;
    private final List<URL> searchPageUrls = new ArrayList<>();

    public BetterYggdrasilGameProfileRepo(final Proxy proxy, final List<Environment> envs) {
        this.client = MinecraftClient.unauthenticated(proxy);
        for (Environment env : envs) {
            this.searchPageUrls.add(HttpAuthenticationService.constantURL(env.accountsHost() + "/profiles/"));
        }
    }

    @Override
    public void findProfilesByNames(final String[] names, final ProfileLookupCallback callback) {
        final Set<String> criteria = Arrays.stream(names)
            .filter(name -> !Strings.isNullOrEmpty(name))
            .collect(Collectors.toSet());

        for (final List<String> request : Iterables.partition(criteria, ENTRIES_PER_PAGE)) {
            final List<String> normalizedRequest = request.stream().map(BetterYggdrasilGameProfileRepo::normalizeName).toList();
            final Set<Pair<String, AuthenticationException>> serverError = new HashSet<>();
            final Set<String> missing = new HashSet<>(request);
            for (URL searchPageUrl : searchPageUrls) {
                findFromSingleSource(searchPageUrl, normalizedRequest, callback, serverError, missing);
            }
            for (final var error : serverError) {
                callback.onProfileLookupFailed(error.getA(), error.getB());
            }
            for (final String name : missing) {
                callback.onProfileLookupFailed(name, new ProfileNotFoundException("Servers did not find the requested profile"));
            }
        }
    }

    private void findFromSingleSource(
        final URL searchPageUrl, final List<String> request,
        final ProfileLookupCallback callback, final Set<Pair<String, AuthenticationException>> serverError, final Set<String> missing
    ) {
        int failCount = 0;
        boolean failed;

        do {
            failed = false;

            try {
                final ProfileSearchResultsResponse response = client.post(searchPageUrl, request, ProfileSearchResultsResponse.class);
                final List<GameProfile> profiles = response != null ? response.profiles() : List.of();
                failCount = 0;

                LOGGER.debug("Page 0 returned {} results, parsing", profiles.size());

                for (final GameProfile profile : profiles) {
                    LOGGER.debug("Successfully looked up profile {}", profile);
                    missing.remove(profile.getName().toLowerCase());
                    callback.onProfileLookupSucceeded(profile);
                }

                for (final String name : missing) {
                    LOGGER.debug("Couldn't find profile {} on {}", name, searchPageUrl);
                }

                try {
                    Thread.sleep(DELAY_BETWEEN_PAGES);
                } catch (final InterruptedException ignored) {
                }
            } catch (final MinecraftClientException e) {
                failCount++;

                if (failCount == MAX_FAIL_COUNT) {
                    for (final String name : request) {
                        LOGGER.debug("Couldn't find profile {} on {} because of a server error", name, searchPageUrl);
                        serverError.add(new Pair<>(name, e.toAuthenticationException()));
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

    private static String normalizeName(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}