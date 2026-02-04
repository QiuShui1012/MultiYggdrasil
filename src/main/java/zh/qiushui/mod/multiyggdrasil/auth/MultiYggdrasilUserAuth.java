package zh.qiushui.mod.multiyggdrasil.auth;

import com.mojang.authlib.Agent;
//#if AUTHLIB < 10600
//$$ import zh.qiushui.mod.multiyggdrasil.util.vap.Environment;
//#else
import com.mojang.authlib.Environment;
//#endif
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.UserType;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import com.mojang.authlib.yggdrasil.request.AuthenticationRequest;
import com.mojang.authlib.yggdrasil.request.RefreshRequest;
import com.mojang.authlib.yggdrasil.request.ValidateRequest;
import com.mojang.authlib.yggdrasil.response.AuthenticationResponse;
import com.mojang.authlib.yggdrasil.response.RefreshResponse;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.authlib.yggdrasil.response.User;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
//#if MC < 11800
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//#else
//$$ import com.mojang.logging.LogUtils;
//$$ import org.slf4j.Logger;
//#endif
import zh.qiushui.mod.multiyggdrasil.util.RequestUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MultiYggdrasilUserAuth extends YggdrasilUserAuthentication {
    private static final Logger LOGGER = LogManager.getLogger();

    private final List<URL> routesAuthenticate = new ArrayList<>();
    private final List<URL> routesRefresh = new ArrayList<>();
    private final List<URL> routesValidate = new ArrayList<>();

    private final String clientToken;
    private GameProfile[] profiles;
    private String accessToken;
    private boolean isOnline;

    public MultiYggdrasilUserAuth(
        final MultiYggdrasilAuthService authService,
        final String clientToken,
        final Agent agent,
        List<Environment> envs
    ) {
        super(
            authService,
            //#if AUTHLIB >= 20000
            //$$ clientToken,
            //#endif
            agent
        );
        this.clientToken = clientToken;

        for (Environment env : envs) {
            LOGGER.info("Environment: {}. AuthHost: {}", env.getName(), env.getAuthHost());
            this.routesAuthenticate.add(HttpAuthenticationService.constantURL(env.getAuthHost() + "/authenticate"));
            this.routesRefresh.add(HttpAuthenticationService.constantURL(env.getAuthHost() + "/refresh"));
            this.routesValidate.add(HttpAuthenticationService.constantURL(env.getAuthHost() + "/validate"));
        }
    }

    @Override
    public boolean canLogIn() {
        return !canPlayOnline()
               && StringUtils.isNotBlank(getUsername())
               && (StringUtils.isNotBlank(getPassword()) || StringUtils.isNotBlank(getAuthenticatedToken()));
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

    @Override
    protected void logInWithPassword() throws AuthenticationException {
        if (StringUtils.isBlank(getUsername())) {
            throw new InvalidCredentialsException("Invalid username");
        }
        if (StringUtils.isBlank(getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        LOGGER.info("Logging in with username & password");

        final AuthenticationRequest request = new AuthenticationRequest(
            //#if AUTHLIB < 20000
            this,
            this.getUsername(),
            this.getPassword()
            //#else
            //$$ getAgent(),
            //$$ this.getUsername(),
            //$$ this.getPassword(),
            //$$ clientToken
            //#endif
        );
        final AuthenticationResponse response = RequestUtil.makeRequest(
            this.getAuthenticationService().getProxy(),
            this.routesAuthenticate,
            request,
            AuthenticationResponse.class
        );

        if (!response.getClientToken().equals(this.clientToken)) {
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

    @Override
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

        final RefreshRequest request = new RefreshRequest(
            //#if AUTHLIB < 20000
            this,
            getSelectedProfile()
            //#else
            //$$ getAuthenticatedToken(),
            //$$ clientToken
            //#endif
        );
        final RefreshResponse response = RequestUtil.makeRequest(
            this.getAuthenticationService().getProxy(),
            this.routesRefresh,
            request,
            RefreshResponse.class
        );

        if (!response.getClientToken().equals(this.clientToken)) {
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

    protected boolean checkTokenValidity() {
        final ValidateRequest request = new ValidateRequest(
            //#if AUTHLIB < 20000
            this
            //#else
            //$$ getAuthenticatedToken(),
            //$$ clientToken
            //#endif
        );
        try {
            RequestUtil.makeRequest(this.getAuthenticationService().getProxy(), this.routesValidate, request, Response.class);
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

        final RefreshRequest request = new RefreshRequest(
            //#if AUTHLIB < 20000
            this,
            profile
            //#else
            //$$ getAuthenticatedToken(),
            //$$ clientToken,
            //$$ profile
            //#endif
        );
        final RefreshResponse response = RequestUtil.makeRequest(
            this.getAuthenticationService().getProxy(),
            this.routesRefresh,
            request,
            RefreshResponse.class
        );

        if (!response.getClientToken().equals(this.clientToken)) {
            throw new AuthenticationException("Server requested we change our client token. Don't know how to handle this!");
        }

        isOnline = true;
        accessToken = response.getAccessToken();
        setSelectedProfile(response.getSelectedProfile());
    }

    @Override
    public void loadFromStorage(final Map<String, Object> credentials) {
        super.loadFromStorage(credentials);

        accessToken = String.valueOf(credentials.get("accessToken"));
    }

    @Override
    public Map<String, Object> saveForStorage() {
        final Map<String, Object> result = super.saveForStorage();

        if (StringUtils.isNotBlank(getAuthenticatedToken())) {
            result.put("accessToken", getAuthenticatedToken());
        }

        return result;
    }

    @Override
    public String getAuthenticatedToken() {
        return accessToken;
    }

    @Override
    public String toString() {
        return "BetterYggdrasilAuthService{" +
               "agent=" + getAgent() +
               ", profiles=" + Arrays.toString(profiles) +
               ", selectedProfile=" + getSelectedProfile() +
               ", username='" + getUsername() + '\'' +
               ", isLoggedIn=" + isLoggedIn() +
               ", userType=" + getUserType() +
               ", canPlayOnline=" + canPlayOnline() +
               ", accessToken='" + accessToken + '\'' +
               ", clientToken='" + this.clientToken + '\'' +
               '}';
    }
}
