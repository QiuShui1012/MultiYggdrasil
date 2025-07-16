package zh.qiushui.mod.multiyggdrasil.auth;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.authlib.yggdrasil.TextureUrlChecker;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.util.UUIDTypeAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BetterYggdrasilMcSessionService extends HttpMinecraftSessionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BetterYggdrasilMcSessionService.class);
    private final List<String> baseUrls = new ArrayList<>();
    private final List<URL> joinUrls = new ArrayList<>();
    private final List<URL> checkUrls = new ArrayList<>();

    private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
    private final LoadingCache<GameProfile, GameProfile> insecureProfiles = CacheBuilder
        .newBuilder()
        .expireAfterWrite(6, TimeUnit.HOURS)
        .build(new CacheLoader<>() {
            @Override
            public @NotNull GameProfile load(@NotNull final GameProfile key) {
                return fillGameProfile(key, false);
            }
        });

    public BetterYggdrasilMcSessionService(final BetterYggdrasilAuthService service, List<Environment> envs) {
        super(service);
        for (Environment env : envs) {
            String baseUrl = env.getSessionHost() + "/session/minecraft/";
            baseUrls.add(baseUrl);

            joinUrls.add(HttpAuthenticationService.constantURL(baseUrl + "join"));
            checkUrls.add(HttpAuthenticationService.constantURL(baseUrl + "hasJoined"));
        }
    }

    @Override
    public void joinServer(
        final GameProfile profile, final String authenticationToken, final String serverId) throws AuthenticationException {
        final JoinMinecraftServerRequest request = new JoinMinecraftServerRequest();
        request.accessToken = authenticationToken;
        request.selectedProfile = profile.getId();
        request.serverId = serverId;

        getAuthenticationService().makeRequest(joinUrls, request, Response.class);
    }

    @Override
    @Nullable
    public GameProfile hasJoinedServer(
        final GameProfile user, final String serverId, final InetAddress address
    ) throws AuthenticationUnavailableException {
        final Map<String, Object> arguments = new HashMap<>();

        arguments.put("username", user.getName());
        arguments.put("serverId", serverId);

        if (address != null) {
            arguments.put("ip", address.getHostAddress());
        }

        for (URL url : checkUrls) {
            url = HttpAuthenticationService.concatenateURL(url, HttpAuthenticationService.buildQuery(arguments));
            try {
                final HasJoinedMinecraftServerResponse response = getAuthenticationService().makeRequest(
                    url, null, HasJoinedMinecraftServerResponse.class);

                if (response == null || response.getId() == null) continue;
                final GameProfile result = new GameProfile(response.getId(), user.getName());

                if (response.getProperties() != null) {
                    result.getProperties().putAll(response.getProperties());
                }

                return result;
            } catch (final AuthenticationUnavailableException e) {
                throw e;
            } catch (final AuthenticationException ignored) {
            }
        }
        return null;
    }

    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(
        final GameProfile profile, final boolean requireSecure) throws InsecurePublicKeyException {
        final Property textureProperty = Iterables.getFirst(profile.getProperties().get("textures"), null);

        if (textureProperty == null) {
            return new HashMap<>();
        }

        final String value = requireSecure ? getSecurePropertyValue(textureProperty) : textureProperty.getValue();

        final MinecraftTexturesPayload result;
        try {
            final String json = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            result = gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (final JsonParseException e) {
            LOGGER.error("Could not decode textures payload", e);
            return new HashMap<>();
        }

        if (result == null || result.getTextures() == null) {
            return new HashMap<>();
        }

        for (final Map.Entry<MinecraftProfileTexture.Type, MinecraftProfileTexture> entry : result.getTextures().entrySet()) {
            final String url = entry.getValue().getUrl();
            if (!TextureUrlChecker.isAllowedTextureDomain(url)) {
                LOGGER.error("Textures payload contains blocked domain: {}", url);
                return new HashMap<>();
            }
        }

        return result.getTextures();
    }

    @Override
    public GameProfile fillProfileProperties(final GameProfile profile, final boolean requireSecure) {
        if (profile.getId() == null) {
            return profile;
        }

        if (!requireSecure) {
            return insecureProfiles.getUnchecked(profile);
        }

        return fillGameProfile(profile, true);
    }

    @Override
    public String getSecurePropertyValue(final Property property) throws InsecurePublicKeyException {
        if (!property.hasSignature()) {
            LOGGER.error("Signature is missing from Property {}", property.getName());
            throw new InsecurePublicKeyException.MissingException();
        }

        final ServicesKeySet servicesKeySet = getAuthenticationService().getServicesKeySet();
        if (servicesKeySet.keys(ServicesKeyType.PROFILE_PROPERTY).stream().noneMatch(key -> key.validateProperty(property))) {
            LOGGER.error("Property {} has been tampered with (signature invalid)", property.getName());
            throw new InsecurePublicKeyException.InvalidException("Property has been tampered with (signature invalid)");
        }

        return property.getValue();
    }

    private GameProfile fillGameProfile(final GameProfile profile, final boolean requireSecure) {
        for (String baseUrl : baseUrls) {
            try {
                URL url = HttpAuthenticationService.constantURL(baseUrl + "profile/" + UUIDTypeAdapter.fromUUID(profile.getId()));
                url = HttpAuthenticationService.concatenateURL(url, "unsigned=" + !requireSecure);
                final MinecraftProfilePropertiesResponse response = getAuthenticationService().makeRequest(
                    url, null, MinecraftProfilePropertiesResponse.class);

                if (response == null) {
                    LOGGER.debug("Couldn't fetch profile properties for {} as the profile does not exist", profile);
                    return profile;
                } else {
                    final GameProfile result = new GameProfile(response.getId(), response.getName());
                    result.getProperties().putAll(response.getProperties());
                    profile.getProperties().putAll(response.getProperties());

                    LOGGER.debug("Successfully fetched profile properties for {}", result);
                    return result;
                }
            } catch (final AuthenticationException | IllegalArgumentException e) {
                LOGGER.warn("Couldn't look up profile properties for {} on url {}.", profile.getId(), baseUrl, e);
            }
        }
        LOGGER.warn("Couldn't look up profile properties for {}", profile.getId());
        return profile;
    }

    @Override
    public BetterYggdrasilAuthService getAuthenticationService() {
        return (BetterYggdrasilAuthService) super.getAuthenticationService();
    }
}

