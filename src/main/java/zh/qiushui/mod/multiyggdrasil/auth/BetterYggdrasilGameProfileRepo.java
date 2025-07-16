package zh.qiushui.mod.multiyggdrasil.auth;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mojang.authlib.Agent;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BetterYggdrasilGameProfileRepo implements GameProfileRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(BetterYggdrasilGameProfileRepo.class);
    private final List<String> searchPageUrls = new ArrayList<>();
    private static final int ENTRIES_PER_PAGE = 2;
    private static final int MAX_FAIL_COUNT = 3;
    private static final int DELAY_BETWEEN_PAGES = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;

    private final BetterYggdrasilAuthService authenticationService;

    public BetterYggdrasilGameProfileRepo(final BetterYggdrasilAuthService service, final List<Environment> envs) {
        this.authenticationService = service;
        for (Environment env : envs) {
            this.searchPageUrls.add(env.getAccountsHost() + "/profiles/");
        }
    }

    @Override
    public void findProfilesByNames(final String[] names, final Agent agent, final ProfileLookupCallback callback) {
        final Set<String> criteria = Sets.newHashSet();

        for (final String name : names) {
            if (!Strings.isNullOrEmpty(name)) {
                criteria.add(name.toLowerCase());
            }
        }

        for (final List<String> request : Iterables.partition(criteria, ENTRIES_PER_PAGE)) {
            final Set<Pair<String, AuthenticationException>> serverError = new HashSet<>();
            final Set<String> missing = new HashSet<>(request);
            for (String searchPageUrl : searchPageUrls) {
                findFromSingleSource(searchPageUrl, agent.getName().toLowerCase(Locale.ROOT), request, callback, serverError, missing);
            }
            for (final var error : serverError) {
                callback.onProfileLookupFailed(new GameProfile(null, error.getA()), error.getB());
            }
            for (final String name : missing) {
                callback.onProfileLookupFailed(
                    new GameProfile(null, name),
                    new ProfileNotFoundException("Servers did not find the requested profile"));
            }
        }
    }

    private void findFromSingleSource(
        final String searchPageUrl, final String agent, final List<String> request,
        final ProfileLookupCallback callback, final Set<Pair<String, AuthenticationException>> serverError, final Set<String> missing
    ) {
        int failCount = 0;
        boolean failed;

        do {
            failed = false;

            try {
                final ProfileSearchResultsResponse response = authenticationService.makeRequest(
                    HttpAuthenticationService.constantURL(searchPageUrl + agent),
                    request, ProfileSearchResultsResponse.class);
                failCount = 0;

                LOGGER.debug("Page 0 returned {} results, parsing", response.getProfiles().length);

                for (final GameProfile profile : response.getProfiles()) {
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
            } catch (final AuthenticationException e) {
                failCount++;

                if (failCount == MAX_FAIL_COUNT) {
                    for (final String name : request) {
                        LOGGER.debug("Couldn't find profile {} on {} because of a server error", name, searchPageUrl);
                        serverError.add(new Pair<>(name, e));
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
}
