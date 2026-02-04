package zh.qiushui.mod.multiyggdrasil.auth;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.util.UUIDTypeAdapter;
import org.jetbrains.annotations.NotNull;
import zh.qiushui.mod.multiyggdrasil.util.RequestUtil;

import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

//#if AUTHLIB < 10600
//$$ import zh.qiushui.mod.multiyggdrasil.util.vap.Environment;
//#else
import com.mojang.authlib.Environment;
//#endif

//#if AUTHLIB < 31100
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import org.apache.commons.io.IOUtils;
import zh.qiushui.mod.multiyggdrasil.util.ParseUtil;
import java.security.PublicKey;
import java.util.Objects;
import java.util.stream.Collectors;
//#else
//$$ import com.mojang.authlib.minecraft.InsecurePublicKeyException;
//#endif

//#if AUTHLIB < 40000
import com.mojang.authlib.minecraft.InsecureTextureException;
//#else
//$$ import com.mojang.authlib.yggdrasil.ServicesKeyType;
//#endif

//#if AUTHLIB < 50000
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import com.mojang.authlib.yggdrasil.response.Response;
//#else
//$$ import com.mojang.authlib.exceptions.MinecraftClientException;
//$$ import com.mojang.authlib.minecraft.MinecraftSessionService;
//$$ import com.mojang.authlib.minecraft.client.MinecraftClient;
//$$ import com.mojang.authlib.yggdrasil.ProfileActionType;
//$$ import com.mojang.authlib.yggdrasil.ProfileResult;
//$$ import com.mojang.authlib.yggdrasil.ServicesKeySet;
//$$ import com.mojang.authlib.yggdrasil.response.ProfileAction;
//$$ import java.util.Optional;
//$$ import java.util.Set;
//$$ import java.util.UUID;
//$$ import java.util.stream.Collectors;
//$$ import javax.annotation.Nullable;
//#endif

//#if MC < 11800
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//#else
//$$ import com.mojang.logging.LogUtils;
//$$ import org.slf4j.Logger;
//#endif

public class MultiYggdrasilMcSessionService
    //#if AUTHLIB < 50000
    extends HttpMinecraftSessionService
    //#else
    //$$ implements MinecraftSessionService
    //#endif
{
    private static final Logger LOGGER = LogManager.getLogger();

    private final List<String> baseUrls = new ArrayList<>();
    private final List<URL> joinUrls = new ArrayList<>();
    private final List<URL> checkUrls = new ArrayList<>();

    //#if AUTHLIB < 31100
    private final PublicKey publicKey;
    //#elseif AUTHLIB >= 50000
    //$$ private final MinecraftClient client;
    //$$ private final ServicesKeySet servicesKeySet;
    //#endif
    private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
    private final LoadingCache<
        //#if AUTHLIB < 50000
        GameProfile, GameProfile
        //#else
        //$$ UUID, Optional<ProfileResult>
        //#endif
        > insecureProfiles = CacheBuilder
        .newBuilder()
        .expireAfterWrite(6, TimeUnit.HOURS)
        //#if AUTHLIB < 50000
        .build(new CacheLoader<GameProfile, GameProfile>() {
            @Override
            public @NotNull GameProfile load(final @NotNull GameProfile key) {
                return fillGameProfile(key, false);
            }
        });
        //#else
        //$$ .build(new CacheLoader<UUID, Optional<ProfileResult>>() {
        //$$     @Override
        //$$     public Optional<ProfileResult> load(final UUID key) {
        //$$         return Optional.ofNullable(fetchProfileUncached(key, false));
        //$$     }
        //$$ });
        //#endif

    public MultiYggdrasilMcSessionService(final YggdrasilAuthenticationService service, final List<Environment> envs) {
        //#if AUTHLIB < 50000
        super(service);
        //#else
        //$$ client = MinecraftClient.unauthenticated(service.getProxy());
        //$$ servicesKeySet = service.getServicesKeySet();
        //#endif

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
        //#if AUTHLIB < 50000
        final GameProfile profile,
        //#else
        //$$ final UUID profileId,
        //#endif
        final String authenticationToken,
        final String serverId
    ) throws AuthenticationException {
        //#if AUTHLIB < 50000
        final JoinMinecraftServerRequest request = new JoinMinecraftServerRequest();
        request.accessToken = authenticationToken;
        request.selectedProfile = profile.getId();
        request.serverId = serverId;

        RequestUtil.makeRequest(getAuthenticationService().getProxy(), joinUrls, request, Response.class);
        //#else
        //$$ final JoinMinecraftServerRequest request = new JoinMinecraftServerRequest(authenticationToken, profileId, serverId);
        //$$ boolean success = false;
        //$$ MinecraftClientException e = new MinecraftClientException(
        //$$     MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE,
        //$$     "No valid environments",
        //$$     new AuthenticationException()
        //$$ );
        //$$ for (final URL joinUrl : joinUrls) {
        //$$     try {
        //$$         client.post(joinUrl, request, Void.class);
        //$$         success = true;
        //$$     } catch (final MinecraftClientException ex) {
        //$$         e = ex;
        //$$     }
        //$$ }
        //$$ if (!success) throw e.toAuthenticationException();
        //#endif
    }

    @Override
    //#if AUTHLIB < 50000
    public GameProfile hasJoinedServer(
        final GameProfile user,
    //#else
    //$$ public ProfileResult hasJoinedServer(
    //$$    final String profileName,
    //#endif
        final String serverId,
        final InetAddress address
    ) throws AuthenticationUnavailableException {
        final Map<String, Object> arguments = new HashMap<>();

        arguments.put(
            "username",
            //#if AUTHLIB < 50000
            user.getName()
            //#else
            //$$ profileName
            //#endif
        );
        arguments.put("serverId", serverId);

        if (address != null) {
            arguments.put("ip", address.getHostAddress());
        }

        try {
            HasJoinedMinecraftServerResponse response;
            //#if AUTHLIB < 50000
            response = RequestUtil.makeRequest(
                getAuthenticationService().getProxy(),
                Lists.transform(
                    checkUrls,
                    url -> HttpAuthenticationService.concatenateURL(url, HttpAuthenticationService.buildQuery(arguments))
                ),
                null,
                HasJoinedMinecraftServerResponse.class
            );
            //#else
            //$$ response = null;
            //$$ for (URL checkUrl : checkUrls) {
            //$$     final URL url = HttpAuthenticationService.concatenateURL(checkUrl, HttpAuthenticationService.buildQuery(arguments));
            //$$     HasJoinedMinecraftServerResponse responseCache = client.get(url, HasJoinedMinecraftServerResponse.class);
            //$$     if (responseCache == null) continue;
            //$$     response = responseCache;
            //$$     break;
            //$$ }
            //#endif

            if (response != null && response.getId() != null) {
                final GameProfile result = new GameProfile(
                    response.getId(),
                    //#if AUTHLIB < 50000
                    user.getName()
                    //#else
                    //$$ profileName
                    //#endif
                );

                if (response.getProperties() != null) {
                    result.getProperties().putAll(response.getProperties());
                }

                //#if AUTHLIB < 50000
                return result;
                //#else
                //$$ final Set<ProfileActionType> profileActions = response.profileActions().stream()
                //$$     .map(ProfileAction::type)
                //$$     .collect(Collectors.toSet());
                //$$ return new ProfileResult(result, profileActions);
                //#endif
            } else {
                return null;
            }
        }
        //#if AUTHLIB < 50000
        catch (final AuthenticationUnavailableException e) {
            throw e;
        } catch (final AuthenticationException ignored) {
            return null;
        }
        //#else
        //$$ catch (final MinecraftClientException e) {
        //$$     if (e.toAuthenticationException() instanceof final AuthenticationUnavailableException unavailable) {
        //$$         throw unavailable;
        //$$     }
        //$$     return null;
        //$$ }
        //#endif
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
                //#else
                //$$ servicesKeySet.keys(null).iterator().next().validateProperty(textureProperty)
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

    //#if AUTHLIB < 50000
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
    //#else
    //$$ @Nullable
    //$$ @Override
    //$$ public ProfileResult fetchProfile(final UUID profileId, final boolean requireSecure) {
    //$$     if (!requireSecure) {
    //$$         return insecureProfiles.getUnchecked(profileId).orElse(null);
    //$$     }
    //$$
    //$$     return fetchProfileUncached(profileId, true);
    //$$ }
    //$$
    //$$ @Nullable
    //$$ private ProfileResult fetchProfileUncached(final UUID profileId, final boolean requireSecure) {
    //$$     try {
    //$$         MinecraftProfilePropertiesResponse response = null;
    //$$         for (String baseUrl : baseUrls) {
    //$$             URL url = HttpAuthenticationService.constantURL(baseUrl + "profile/" + profileId.toString().replace('-', ' '));
    //$$             url = HttpAuthenticationService.concatenateURL(url, "unsigned=" + !requireSecure);
    //$$             MinecraftProfilePropertiesResponse responseCache = client.get(url, MinecraftProfilePropertiesResponse.class);
    //$$             if (responseCache == null) continue;
    //$$             response = responseCache;
    //$$             break;
    //$$         }
    //$$
    //$$         if (response == null) {
    //$$             LOGGER.debug("Couldn't fetch profile properties for {} as the profile does not exist", profileId);
    //$$             return null;
    //$$         }
    //$$
    //$$         final GameProfile profile = response.toProfile();
    //$$         final Set<ProfileActionType> profileActions = response.profileActions().stream()
    //$$             .map(ProfileAction::type)
    //$$             .collect(Collectors.toSet());
    //$$
    //$$         LOGGER.debug("Successfully fetched profile properties for {}", profile);
    //$$         return new ProfileResult(profile, profileActions);
    //$$     } catch (final MinecraftClientException | IllegalArgumentException e) {
    //$$         LOGGER.warn("Couldn't look up profile properties for {}", profileId, e);
    //$$         return null;
    //$$     }
    //$$ }
    //#endif

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
            //#elseif AUTHLIB >= 40000 && AUTHLIB < 50000
            //$$ !getAuthenticationService().getServicesKeySet().keys(null).iterator().next()
            //#elseif AUTHLIB >= 50000
            //$$ servicesKeySet.keys(null).iterator().next()
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

    //#if AUTHLIB < 50000
    @Override
    public YggdrasilAuthenticationService getAuthenticationService() {
        return (YggdrasilAuthenticationService) super.getAuthenticationService();
    }
    //#endif
}

