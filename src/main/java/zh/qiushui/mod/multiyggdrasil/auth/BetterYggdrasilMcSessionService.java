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
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileActionType;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.authlib.yggdrasil.response.ProfileAction;
import com.mojang.util.UUIDTypeAdapter;
import com.mojang.util.UndashedUuid;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class BetterYggdrasilMcSessionService implements MinecraftSessionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BetterYggdrasilMcSessionService.class);
    private final MinecraftClient client;
    private final ServicesKeySet servicesKeySet;
    private final List<String> baseUrls = new ArrayList<>();
    private final List<URL> joinUrls = new ArrayList<>();
    private final List<URL> checkUrls = new ArrayList<>();

    private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
    private final LoadingCache<UUID, Optional<ProfileResult>> insecureProfiles = CacheBuilder
        .newBuilder()
        .expireAfterWrite(6, TimeUnit.HOURS)
        .build(new CacheLoader<>() {
            @Override
            public @NotNull Optional<ProfileResult> load(final @NotNull UUID key) {
                return Optional.ofNullable(fetchProfileUncached(key, false));
            }
        });

    public BetterYggdrasilMcSessionService(final ServicesKeySet servicesKeySet, final Proxy proxy, final List<Environment> envs) {
        client = MinecraftClient.unauthenticated(proxy);
        this.servicesKeySet = servicesKeySet;
        for (Environment env : envs) {
            String baseUrl = env.sessionHost() + "/session/minecraft/";
            baseUrls.add(baseUrl);

            joinUrls.add(HttpAuthenticationService.constantURL(baseUrl + "join"));
            checkUrls.add(HttpAuthenticationService.constantURL(baseUrl + "hasJoined"));
        }
    }

    @Override
    public void joinServer(final UUID profileId, final String authToken, final String serverId) throws AuthenticationException {
        final JoinMinecraftServerRequest request = new JoinMinecraftServerRequest(authToken, profileId, serverId);

        AtomicReference<MinecraftClientException> e = new AtomicReference<>();
        for (URL joinUrl : joinUrls) {
            try {
                client.post(joinUrl, request, Void.class);
                return;
            } catch (final MinecraftClientException e1) {
                e.set(e1);
            }
        }
        throw e.get().toAuthenticationException();
    }

    @Override
    @Nullable
    public ProfileResult hasJoinedServer(final String profileName, final String serverId, @Nullable final InetAddress address) throws AuthenticationUnavailableException {
        final Map<String, Object> arguments = new HashMap<>();

        arguments.put("username", profileName);
        arguments.put("serverId", serverId);

        if (address != null) {
            arguments.put("ip", address.getHostAddress());
        }

        for (URL url : checkUrls) {
            url = HttpAuthenticationService.concatenateURL(url, HttpAuthenticationService.buildQuery(arguments));
            try {
                final HasJoinedMinecraftServerResponse response = client.get(url, HasJoinedMinecraftServerResponse.class);
                if (response == null || response.id() == null) continue;
                final GameProfile result = new GameProfile(response.id(), profileName);

                if (response.properties() != null) {
                    result.getProperties().putAll(response.properties());
                }

                final Set<ProfileActionType> profileActions = response.profileActions().stream()
                    .map(ProfileAction::type)
                    .collect(Collectors.toSet());
                return new ProfileResult(result, profileActions);
            } catch (final MinecraftClientException e) {
                if (e.toAuthenticationException() instanceof final AuthenticationUnavailableException unavailable) {
                    throw unavailable;
                }
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Property getPackedTextures(final GameProfile profile) {
        return Iterables.getFirst(profile.getProperties().get("textures"), null);
    }

    @Override
    public MinecraftProfileTextures unpackTextures(final Property packedTextures) {
        final String value = packedTextures.value();
        final SignatureState signatureState = getPropertySignatureState(packedTextures);

        final MinecraftTexturesPayload result;
        try {
            final String json = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            result = gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (final JsonParseException | IllegalArgumentException e) {
            LOGGER.error("Could not decode textures payload", e);
            return MinecraftProfileTextures.EMPTY;
        }

        if (result == null || result.textures() == null || result.textures().isEmpty()) {
            return MinecraftProfileTextures.EMPTY;
        }

        final Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = result.textures();

        return new MinecraftProfileTextures(
            textures.get(MinecraftProfileTexture.Type.SKIN),
            textures.get(MinecraftProfileTexture.Type.CAPE),
            textures.get(MinecraftProfileTexture.Type.ELYTRA),
            signatureState
        );
    }

    @Nullable
    @Override
    public ProfileResult fetchProfile(final UUID profileId, final boolean requireSecure) {
        if (!requireSecure) {
            return insecureProfiles.getUnchecked(profileId).orElse(null);
        }

        return fetchProfileUncached(profileId, true);
    }

    @Override
    public String getSecurePropertyValue(final Property property) throws InsecurePublicKeyException {
        return switch (getPropertySignatureState(property)) {
            case UNSIGNED ->
                throw new InsecurePublicKeyException.MissingException("Missing signature from \"" + property.name() + "\"");
            case INVALID ->
                throw new InsecurePublicKeyException.InvalidException("Property \"" + property.name() + "\" has been tampered with (signature invalid)");
            case SIGNED -> property.value();
        };
    }

    private SignatureState getPropertySignatureState(final Property property) {
        if (!property.hasSignature()) {
            return SignatureState.UNSIGNED;
        }
        if (servicesKeySet.keys(ServicesKeyType.PROFILE_PROPERTY).stream().noneMatch(key -> key.validateProperty(property))) {
            return SignatureState.INVALID;
        }
        return SignatureState.SIGNED;
    }

    @Nullable
    private ProfileResult fetchProfileUncached(final UUID profileId, final boolean requireSecure) {
        for (String baseUrl : baseUrls) {
            try {
                URL url = HttpAuthenticationService.constantURL(baseUrl + "profile/" + UndashedUuid.toString(profileId));
                url = HttpAuthenticationService.concatenateURL(url, "unsigned=" + !requireSecure);

                final MinecraftProfilePropertiesResponse response = client.get(url, MinecraftProfilePropertiesResponse.class);
                if (response == null) {
                    LOGGER.debug("Couldn't fetch profile properties for {} as the profile does not exist", profileId);
                    continue;
                }

                final GameProfile profile = response.toProfile();
                final Set<ProfileActionType> profileActions = response.profileActions().stream()
                    .map(ProfileAction::type)
                    .collect(Collectors.toSet());

                LOGGER.debug("Successfully fetched profile properties for {}", profile);
                return new ProfileResult(profile, profileActions);
            } catch (final MinecraftClientException | IllegalArgumentException e) {
                LOGGER.warn("Couldn't look up profile properties for {} on url {}.", profileId, baseUrl, e);
            }
        }
        LOGGER.warn("Couldn't look up profile properties for {}", profileId);
        return null;
    }
}

