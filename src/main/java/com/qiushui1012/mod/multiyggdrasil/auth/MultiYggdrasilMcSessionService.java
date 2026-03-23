package com.qiushui1012.mod.multiyggdrasil.auth;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.util.UUIDTypeAdapter;
import org.jetbrains.annotations.NotNull;
import com.qiushui1012.mod.multiyggdrasil.util.RequestUtil;

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

//#if AUTHLIB < 31100
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.qiushui1012.mod.multiyggdrasil.util.ParseUtil;
import org.apache.commons.io.IOUtils;

import java.security.PublicKey;
import java.util.Objects;
//#else
//$$ import com.mojang.authlib.minecraft.InsecurePublicKeyException;
//#endif

//#if AUTHLIB < 40000
import com.mojang.authlib.minecraft.InsecureTextureException;
//#else
//$$ import com.mojang.authlib.yggdrasil.ServicesKeyType;
//#endif

//#if MC < 11800
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//#else
//$$ import com.mojang.logging.LogUtils;
//$$ import org.slf4j.Logger;
//#endif

public class MultiYggdrasilMcSessionService extends HttpMinecraftSessionService {
    private static final Logger LOGGER = LogManager.getLogger();

    private final List<String> baseUrls = new ArrayList<>();
    private final List<URL> joinUrls = new ArrayList<>();
    private final List<URL> checkUrls = new ArrayList<>();

    //#if AUTHLIB < 31100
    private final PublicKey publicKey;
    //#endif
    private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
    private final LoadingCache<GameProfile, GameProfile> insecureProfiles = CacheBuilder
        .newBuilder()
        .expireAfterWrite(6, TimeUnit.HOURS)
        .build(new CacheLoader<>() {
            @Override
            public @NotNull GameProfile load(final @NotNull GameProfile key) {
                return fillGameProfile(key, false);
            }
        });

    public MultiYggdrasilMcSessionService(final YggdrasilAuthenticationService service, final List<Environment> envs) {
        super(service);

        for (Environment env : envs) {
            String baseUrl = env.getSessionHost() + "/session/minecraft/";
            baseUrls.add(baseUrl);

            joinUrls.add(HttpAuthenticationService.constantURL(baseUrl + "join"));
            checkUrls.add(HttpAuthenticationService.constantURL(baseUrl + "hasJoined"));
        }

        //#if AUTHLIB < 31100
        try {
            this.publicKey = ParseUtil.parseX509PublicKey(IOUtils.toByteArray(
                Objects.requireNonNull(YggdrasilMinecraftSessionService.class.getResourceAsStream("/yggdrasil_session_pubkey.der"))
            ));
        } catch (Exception e) {
            throw new Error("Missing/invalid yggdrasil public key!");
        }
        //#endif
    }

    @Override
    public void joinServer(
        final GameProfile profile,
        final String authenticationToken,
        final String serverId
    ) throws AuthenticationException {
        final JoinMinecraftServerRequest request = new JoinMinecraftServerRequest();
        request.accessToken = authenticationToken;
        request.selectedProfile = profile.getId();
        request.serverId = serverId;

        RequestUtil.makeRequest(getAuthenticationService().getProxy(), joinUrls, request, Response.class);
    }

    @Override
    public GameProfile hasJoinedServer(
        final GameProfile user,
        final String serverId,
        final InetAddress address
    ) throws AuthenticationUnavailableException {
        final Map<String, Object> arguments = new HashMap<>();

        arguments.put("username", user.getName());
        arguments.put("serverId", serverId);

        if (address != null) {
            arguments.put("ip", address.getHostAddress());
        }

        try {
            HasJoinedMinecraftServerResponse response;
            response = RequestUtil.makeRequest(
                getAuthenticationService().getProxy(),
                Lists.transform(
                    checkUrls,
                    url -> HttpAuthenticationService.concatenateURL(url, HttpAuthenticationService.buildQuery(arguments))
                ),
                null,
                HasJoinedMinecraftServerResponse.class
            );

            if (response != null && response.getId() != null) {
                final GameProfile result = new GameProfile(response.getId(), user.getName());

                if (response.getProperties() != null) {
                    result.getProperties().putAll(response.getProperties());
                }

                return result;
            } else {
                return null;
            }
        } catch (final AuthenticationUnavailableException e) {
            throw e;
        } catch (final AuthenticationException ignored) {
            return null;
        }
    }

    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(
        final GameProfile profile,
        final boolean requireSecure
    )
        //#if AUTHLIB >= 40000
        //$$ throws InsecurePublicKeyException
        //#endif
    {
        final Property textureProperty = Iterables.getFirst(profile.getProperties().get("textures"), null);

        if (textureProperty == null) {
            return new HashMap<>();
        }

        if (requireSecure) {
            if (!textureProperty.hasSignature()) {
                //#if AUTHLIB < 40000
                LOGGER.error("Signature is missing from textures payload");
                throw new InsecureTextureException("Signature is missing from textures payload");
                //#else
                //$$ LOGGER.error("Signature is missing from Property {}", textureProperty.getName());
                //$$ throw new InsecurePublicKeyException.MissingException();
                //#endif
            }

            if (
                //#if AUTHLIB < 31100
                !textureProperty.isSignatureValid(publicKey)
                //#elseif AUTHLIB < 40000
                //$$ !getAuthenticationService().getServicesKey().validateProperty(textureProperty)
                //#elseif AUTHLIB < 50000
                //$$ !getAuthenticationService().getServicesKeySet().keys(null).iterator().next().validateProperty(textureProperty)
                //#endif
            ) {
                //#if AUTHLIB < 40000
                LOGGER.error("Textures payload has been tampered with (signature invalid)");
                throw new InsecureTextureException("Textures payload has been tampered with (signature invalid)");
                //#else
                //$$ LOGGER.error("Property {} has been tampered with (signature invalid)", textureProperty.getName());
                //$$ throw new InsecurePublicKeyException.InvalidException("Property has been tampered with (signature invalid)");
                //#endif
            }
        }

        final MinecraftTexturesPayload result;
        try {
            final String json = new String(Base64.getDecoder().decode(textureProperty.getValue()), StandardCharsets.UTF_8);
            result = gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (final JsonParseException e) {
            LOGGER.error("Could not decode textures payload", e);
            return new HashMap<>();
        }

        if (result == null || result.getTextures() == null) {
            return new HashMap<>();
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

    protected GameProfile fillGameProfile(final GameProfile profile, final boolean requireSecure) {
        try {
            final MinecraftProfilePropertiesResponse response = RequestUtil.makeRequest(
                getAuthenticationService().getProxy(),
                Lists.transform(baseUrls, base -> HttpAuthenticationService.concatenateURL(HttpAuthenticationService.constantURL(base), "profile/" + UUIDTypeAdapter.fromUUID(profile.getId()) + "?unsigned=" + !requireSecure)),
                null,
                MinecraftProfilePropertiesResponse.class
            );

            if (response == null) {
                LOGGER.debug("Couldn't fetch profile properties for {} as the profile does not exist", profile);
                return profile;
            } else {
                final GameProfile result = new GameProfile(response.getId(), response.getName());
                result.getProperties().putAll(response.getProperties());
                profile.getProperties().putAll(response.getProperties());
                LOGGER.debug("Successfully fetched profile properties for {}", profile);
                return result;
            }
        } catch (final AuthenticationException e) {
            LOGGER.warn("Couldn't look up profile properties for {}", profile, e);
            return profile;
        }
    }

    //#if AUTHLIB >= 31100
    //$$ @Override
    //$$ public String getSecurePropertyValue(Property property) throws InsecurePublicKeyException {
    //$$     if (!property.hasSignature()) {
    //$$         LOGGER.error("Signature is missing from Property {}", property.getName());
    //$$         throw new InsecurePublicKeyException.MissingException();
    //$$     }
    //$$
    //$$     if (
            //#if AUTHLIB >= 31100 && AUTHLIB < 40000
            //$$ !getAuthenticationService().getServicesKey()
            //#elseif AUTHLIB >= 40000
            //$$ !getAuthenticationService().getServicesKeySet().keys(null).iterator().next()
            //#endif
    //$$             .validateProperty(property)
    //$$     ) {
    //$$         LOGGER.error("Property {} has been tampered with (signature invalid)", property.getName());
    //$$         throw new InsecurePublicKeyException.InvalidException("Property has been tampered with (signature invalid)");
    //$$     }
    //$$
    //$$     return property.getValue();
    //$$ }
    //#endif

    @Override
    public YggdrasilAuthenticationService getAuthenticationService() {
        return (YggdrasilAuthenticationService) super.getAuthenticationService();
    }
}

