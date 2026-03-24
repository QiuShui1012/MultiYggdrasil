package com.qiushui1012.mod.multiyggdrasil.auth;

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
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileActionType;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.authlib.yggdrasil.response.ProfileAction;
import com.mojang.util.UUIDTypeAdapter;

import java.net.InetAddress;
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
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class MultiYggdrasilMcSessionService implements MinecraftSessionService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final List<String> baseUrls = new ArrayList<>();
    private final List<URL> joinUrls = new ArrayList<>();
    private final List<URL> checkUrls = new ArrayList<>();

    private final MinecraftClient client;
    private final ServicesKeySet servicesKeySet;
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

    public MultiYggdrasilMcSessionService(final YggdrasilAuthenticationService service, final List<Environment> envs) {
        client = MinecraftClient.unauthenticated(service.getProxy());
        servicesKeySet = service.getServicesKeySet();

        for (Environment env : envs) {
            String baseUrl = env.sessionHost() + "/session/minecraft/";
            baseUrls.add(baseUrl);

            joinUrls.add(HttpAuthenticationService.constantURL(baseUrl + "join"));
            checkUrls.add(HttpAuthenticationService.constantURL(baseUrl + "hasJoined"));
        }
    }

    @Override
    public void joinServer(
        final UUID profileId,
        final String authenticationToken,
        final String serverId
    ) throws AuthenticationException {
        final JoinMinecraftServerRequest request = new JoinMinecraftServerRequest(authenticationToken, profileId, serverId);
        boolean success = false;
        MinecraftClientException e = new MinecraftClientException(
            MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE,
            "No valid environments",
            new AuthenticationException()
        );
        for (final URL joinUrl : joinUrls) {
            try {
                client.post(joinUrl, request, Void.class);
                success = true;
            } catch (final MinecraftClientException ex) {
                e = ex;
            }
        }
        if (!success) throw e.toAuthenticationException();
    }

    @Override
    public ProfileResult hasJoinedServer(
        final String profileName,
        final String serverId,
        final InetAddress address
    ) throws AuthenticationUnavailableException {
        final Map<String, Object> arguments = new HashMap<>();

        arguments.put("username", profileName);
        arguments.put("serverId", serverId);

        if (address != null) {
            arguments.put("ip", address.getHostAddress());
        }

        try {
            HasJoinedMinecraftServerResponse response;
            response = null;
            for (URL checkUrl : checkUrls) {
                final URL url = HttpAuthenticationService.concatenateURL(checkUrl, HttpAuthenticationService.buildQuery(arguments));
                HasJoinedMinecraftServerResponse responseCache = client.get(url, HasJoinedMinecraftServerResponse.class);
                if (responseCache == null) continue;
                response = responseCache;
                break;
            }

            if (response != null && response.id() != null) {
                final GameProfile result = new GameProfile(response.id(), profileName);

                if (response.properties() != null) {
                    result.getProperties().putAll(response.properties());
                }

                final Set<ProfileActionType> profileActions = response.profileActions().stream()
                    .map(ProfileAction::type)
                    .collect(Collectors.toSet());
                return new ProfileResult(result, profileActions);
            } else {
                return null;
            }
        } catch (final MinecraftClientException e) {
            if (e.toAuthenticationException() instanceof final AuthenticationUnavailableException unavailable) {
                throw unavailable;
            }
            return null;
        }
    }

    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(
        final GameProfile profile,
        final boolean requireSecure
    ) throws InsecurePublicKeyException {
        final Property textureProperty = Iterables.getFirst(profile.getProperties().get("textures"), null);

        if (textureProperty == null) {
            return new HashMap<>();
        }

        String texture;
        if (requireSecure) {
            texture = this.getSecurePropertyValue(textureProperty);
        } else {
            texture = textureProperty.value();
        }

        final MinecraftTexturesPayload result;
        try {
            final String json = new String(Base64.getDecoder().decode(texture), StandardCharsets.UTF_8);
            result = gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (final JsonParseException e) {
            LOGGER.error("Could not decode textures payload", e);
            return new HashMap<>();
        }

        if (result == null || result.textures() == null) {
            return new HashMap<>();
        }

        return result.textures();
    }

    @Nullable
    @Override
    public ProfileResult fetchProfile(final UUID profileId, final boolean requireSecure) {
        if (!requireSecure) {
            return insecureProfiles.getUnchecked(profileId).orElse(null);
        }

        return fetchProfileUncached(profileId, true);
    }

    @Nullable
    private ProfileResult fetchProfileUncached(final UUID profileId, final boolean requireSecure) {
        try {
            MinecraftProfilePropertiesResponse response = null;
            for (String baseUrl : baseUrls) {
                URL url = HttpAuthenticationService.constantURL(baseUrl + "profile/" + profileId.toString().replace('-', ' '));
                url = HttpAuthenticationService.concatenateURL(url, "unsigned=" + !requireSecure);
                MinecraftProfilePropertiesResponse responseCache = client.get(url, MinecraftProfilePropertiesResponse.class);
                if (responseCache == null) continue;
                response = responseCache;
                break;
            }

            if (response == null) {
                LOGGER.debug("Couldn't fetch profile properties for {} as the profile does not exist", profileId);
                return null;
            }

            final GameProfile profile = response.toProfile();
            final Set<ProfileActionType> profileActions = response.profileActions().stream()
                .map(ProfileAction::type)
                .collect(Collectors.toSet());

            LOGGER.debug("Successfully fetched profile properties for {}", profile);
            return new ProfileResult(profile, profileActions);
        } catch (final MinecraftClientException | IllegalArgumentException e) {
            LOGGER.warn("Couldn't look up profile properties for {}", profileId, e);
            return null;
        }
    }

    @Override
    public String getSecurePropertyValue(Property property) throws InsecurePublicKeyException {
        if (!property.hasSignature()) {
            LOGGER.error("Signature is missing from Property {}", property.name());
            throw new InsecurePublicKeyException.MissingException();
        }

        if (servicesKeySet.keys(null).iterator().next().validateProperty(property)) {
            LOGGER.error("Property {} has been tampered with (signature invalid)", property.name());
            throw new InsecurePublicKeyException.InvalidException("Property has been tampered with (signature invalid)");
        }

        return property.value();
    }
}

