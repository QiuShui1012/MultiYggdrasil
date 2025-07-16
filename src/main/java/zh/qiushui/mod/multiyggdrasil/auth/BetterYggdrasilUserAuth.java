package zh.qiushui.mod.multiyggdrasil.auth;

import com.mojang.authlib.Agent;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.HttpUserAuthentication;
import com.mojang.authlib.UserType;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.yggdrasil.request.AuthenticationRequest;
import com.mojang.authlib.yggdrasil.request.RefreshRequest;
import com.mojang.authlib.yggdrasil.request.ValidateRequest;
import com.mojang.authlib.yggdrasil.response.AuthenticationResponse;
import com.mojang.authlib.yggdrasil.response.RefreshResponse;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.authlib.yggdrasil.response.User;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BetterYggdrasilUserAuth extends HttpUserAuthentication {
    private static final Logger LOGGER = LoggerFactory.getLogger(BetterYggdrasilUserAuth.class);

    private final List<URL> routesAuthenticate = new ArrayList<>();
    private final List<URL> routesRefresh = new ArrayList<>();
    private final List<URL> routesValidate = new ArrayList<>();
    // The two below is exist in com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication, but they didn't be used.
    // We will keep them.
    @SuppressWarnings({"FieldCanBeLocal", "MismatchedQueryAndUpdateOfCollection"})
    private final List<URL> routesInvalidate = new ArrayList<>();
    @SuppressWarnings({"FieldCanBeLocal", "MismatchedQueryAndUpdateOfCollection"})
    private final List<URL> routesSignout = new ArrayList<>();

    private static final String STORAGE_KEY_ACCESS_TOKEN = "accessToken";

    @Getter
    private final Agent agent;
    private GameProfile[] profiles;
    private final String clientToken;
    private String accessToken;
    private boolean isOnline;

    public BetterYggdrasilUserAuth(
        final BetterYggdrasilAuthService service, final String clientToken, final Agent agent, List<Environment> envs) {
        super(service);
        this.clientToken = clientToken;
        this.agent = agent;

        for (Environment env : envs) {
            LOGGER.info("Environment: {}. AuthHost: {}", env.getName(), env.getAuthHost());
            this.routesAuthenticate.add(HttpAuthenticationService.constantURL(env.getAuthHost() + "/authenticate"));
            this.routesRefresh.add(HttpAuthenticationService.constantURL(env.getAuthHost() + "/refresh"));
            this.routesValidate.add(HttpAuthenticationService.constantURL(env.getAuthHost() + "/validate"));
            this.routesInvalidate.add(HttpAuthenticationService.constantURL(env.getAuthHost() + "/invalidate"));
            this.routesSignout.add(HttpAuthenticationService.constantURL(env.getAuthHost() + "/signout"));
        }
    }

    @Override
    public boolean canLogIn() {
        return !canPlayOnline()
               && StringUtils.isNotBlank(getUsername())
               && (StringUtils.isNotBlank(getPassword())
                   || StringUtils.isNotBlank(getAuthenticatedToken()));
    }

    @Override
    public void logIn() throws AuthenticationException {
        if (StringUtils.isBlank(getUsername())) {
            throw new InvalidCredentialsException("Invalid username");
        }

        if (StringUtils.isNotBlank(getAuthenticatedToken())) {
            logInWithToken();
        } else if (StringUtils.isNotBlank(getPassword())) {
            logInWithPassword();
        } else {
            throw new InvalidCredentialsException("Invalid password");
        }
    }

    protected void logInWithPassword() throws AuthenticationException {
        if (StringUtils.isBlank(getUsername())) {
            throw new InvalidCredentialsException("Invalid username");
        }
        if (StringUtils.isBlank(getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        LOGGER.info("Logging in with username & password");

        final AuthenticationRequest request = new AuthenticationRequest(getAgent(), getUsername(), getPassword(), clientToken);
        final AuthenticationResponse response = getAuthenticationService().makeRequest(
            routesAuthenticate, request, AuthenticationResponse.class);

        if (!response.getClientToken().equals(clientToken)) {
            throw new AuthenticationException("Server requested we change our client token. Don't know how to handle this!");
        }

        if (response.getSelectedProfile() != null) {
            setUserType(response.getSelectedProfile().isLegacy() ? UserType.LEGACY : UserType.MOJANG);
        } else if (ArrayUtils.isNotEmpty(response.getAvailableProfiles())) {
            setUserType(response.getAvailableProfiles()[0].isLegacy() ? UserType.LEGACY : UserType.MOJANG);
        }

        final User user = response.getUser();

        if (user != null && user.getId() != null) {
            setUserid(user.getId());
        } else {
            setUserid(getUsername());
        }

        isOnline = true;
        accessToken = response.getAccessToken();
        profiles = response.getAvailableProfiles();
        setSelectedProfile(response.getSelectedProfile());
        getModifiableUserProperties().clear();

        updateUserProperties(user);
    }

    protected void updateUserProperties(final User user) {
        if (user == null) {
            return;
        }

        if (user.getProperties() != null) {
            getModifiableUserProperties().putAll(user.getProperties());
        }
    }

    protected void logInWithToken() throws AuthenticationException {
        if (StringUtils.isBlank(getUserID())) {
            if (StringUtils.isBlank(getUsername())) {
                setUserid(getUsername());
            } else {
                throw new InvalidCredentialsException("Invalid uuid & username");
            }
        }
        if (StringUtils.isBlank(getAuthenticatedToken())) {
            throw new InvalidCredentialsException("Invalid access token");
        }

        LOGGER.info("Logging in with access token");

        if (checkTokenValidity()) {
            LOGGER.debug("Skipping refresh call as we're safely logged in.");
            isOnline = true;
            return;
        }

        final RefreshRequest request = new RefreshRequest(getAuthenticatedToken(), clientToken);
        final RefreshResponse response = getAuthenticationService().makeRequest(routesRefresh, request, RefreshResponse.class);

        if (!response.getClientToken().equals(clientToken)) {
            throw new AuthenticationException("Server requested we change our client token. Don't know how to handle this!");
        }

        if (response.getSelectedProfile() != null) {
            setUserType(response.getSelectedProfile().isLegacy() ? UserType.LEGACY : UserType.MOJANG);
        } else if (ArrayUtils.isNotEmpty(response.getAvailableProfiles())) {
            setUserType(response.getAvailableProfiles()[0].isLegacy() ? UserType.LEGACY : UserType.MOJANG);
        }

        if (response.getUser() != null && response.getUser().getId() != null) {
            setUserid(response.getUser().getId());
        } else {
            setUserid(getUsername());
        }

        isOnline = true;
        accessToken = response.getAccessToken();
        profiles = response.getAvailableProfiles();
        setSelectedProfile(response.getSelectedProfile());
        getModifiableUserProperties().clear();

        updateUserProperties(response.getUser());
    }

    protected boolean checkTokenValidity(){
        final ValidateRequest request = new ValidateRequest(getAuthenticatedToken(), clientToken);
        try {
            getAuthenticationService().makeRequest(routesValidate, request, Response.class);
            return true;
        } catch (final AuthenticationException ignored) {
            return false;
        }
    }

    @Override
    public void logOut() {
        super.logOut();

        accessToken = null;
        profiles = null;
        isOnline = false;
    }

    @Override
    public GameProfile[] getAvailableProfiles() {
        return profiles;
    }

    @Override
    public boolean isLoggedIn() {
        return StringUtils.isNotBlank(accessToken);
    }

    @Override
    public boolean canPlayOnline() {
        return isLoggedIn() && getSelectedProfile() != null && isOnline;
    }

    @Override
    public void selectGameProfile(final GameProfile profile) throws AuthenticationException {
        if (!isLoggedIn()) {
            throw new AuthenticationException("Cannot change game profile whilst not logged in");
        }
        if (getSelectedProfile() != null) {
            throw new AuthenticationException("Cannot change game profile. You must log out and back in.");
        }
        if (profile == null || !ArrayUtils.contains(profiles, profile)) {
            throw new IllegalArgumentException("Invalid profile '" + profile + "'");
        }

        final RefreshRequest request = new RefreshRequest(getAuthenticatedToken(), clientToken, profile);
        final RefreshResponse response = getAuthenticationService().makeRequest(routesRefresh, request, RefreshResponse.class);

        if (!response.getClientToken().equals(clientToken)) {
            throw new AuthenticationException("Server requested we change our client token. Don't know how to handle this!");
        }

        isOnline = true;
        accessToken = response.getAccessToken();
        setSelectedProfile(response.getSelectedProfile());
    }

    @Override
    public void loadFromStorage(final Map<String, Object> credentials) {
        super.loadFromStorage(credentials);

        accessToken = String.valueOf(credentials.get(STORAGE_KEY_ACCESS_TOKEN));
    }

    @Override
    public Map<String, Object> saveForStorage() {
        final Map<String, Object> result = super.saveForStorage();

        if (StringUtils.isNotBlank(getAuthenticatedToken())) {
            result.put(STORAGE_KEY_ACCESS_TOKEN, getAuthenticatedToken());
        }

        return result;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String getSessionToken() {
        if (isLoggedIn() && getSelectedProfile() != null && canPlayOnline()) {
            return String.format("token:%s:%s", getAuthenticatedToken(), getSelectedProfile().getId());
        } else {
            return null;
        }
    }

    @Override
    public String getAuthenticatedToken() {
        return accessToken;
    }

    @Override
    public String toString() {
        return "BetterYggdrasilAuthService{" +
               "agent=" + agent +
               ", profiles=" + Arrays.toString(profiles) +
               ", selectedProfile=" + getSelectedProfile() +
               ", username='" + getUsername() + '\'' +
               ", isLoggedIn=" + isLoggedIn() +
               ", userType=" + getUserType() +
               ", canPlayOnline=" + canPlayOnline() +
               ", accessToken='" + accessToken + '\'' +
               ", clientToken='" + clientToken + '\'' +
               '}';
    }

    @Override
    public BetterYggdrasilAuthService getAuthenticationService() {
        return (BetterYggdrasilAuthService) super.getAuthenticationService();
    }
}
