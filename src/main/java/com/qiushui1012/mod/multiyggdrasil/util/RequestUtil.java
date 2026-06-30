package com.qiushui1012.mod.multiyggdrasil.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserMigratedException;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.util.UUIDTypeAdapter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
//#if MC < 11800
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//#else
//$$ import com.mojang.logging.LogUtils;
//$$ import org.slf4j.Logger;
//#endif

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class RequestUtil {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson gson;

    static {
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(GameProfile.class, new GameProfileSerializer());
        builder.registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer());
        builder.registerTypeAdapter(UUID.class, new UUIDTypeAdapter());
        builder.registerTypeAdapter(ProfileSearchResultsResponse.class, new ProfileSearchResultsResponse.Serializer());
        gson = builder.create();
    }

    public static <T extends Response> T makeRequest(final Proxy proxy, final List<URL> urls, final Object input, final Class<T> classOfT) throws AuthenticationException {
        AuthenticationException lastException = null;

        for (URL url : urls) {
            try {
                T result = makeRequest(proxy, url, input, classOfT);
                if (result == null) continue;
                return result;
            } catch (AuthenticationException e) {
                lastException = e;
            }
        }

        if (lastException != null) {
            throw lastException;
        }

        return null;
    }

    public static <T extends Response> T makeRequest(final Proxy proxy, final List<URL> urls, final Object input, final Class<T> classOfT, String auth) throws AuthenticationException {
        AuthenticationException exception = null;
        for (URL url : urls) {
            try {
                T result = makeRequest(proxy, url, input, classOfT);
                if (result == null) continue;
                return result;
            } catch (AuthenticationException e) {
                exception = e;
            }
        }
        if (exception != null) {
            throw exception;
        } else {
            throw new AuthenticationException("No valid URLs");
        }
    }

    public static <T extends Response> T makeRequest(final Proxy proxy, final URL url, final Object input, final Class<T> classOfT) throws AuthenticationException {
        try {
            final String jsonResult = input == null ? performGetRequest(proxy, url) : performPostRequest(proxy, url, gson.toJson(input), "application/json");
            final T result = gson.fromJson(jsonResult, classOfT);

            if (result == null) {
                return null;
            }

            if (StringUtils.isNotBlank(result.getError())) {
                if ("UserMigratedException".equals(result.getCause())) {
                    throw new UserMigratedException(result.getErrorMessage());
                } else if ("ForbiddenOperationException".equals(result.getError())) {
                    throw new InvalidCredentialsException(result.getErrorMessage());
                } else {
                    throw new AuthenticationException(result.getErrorMessage());
                }
            }

            return result;
        } catch (final IOException | IllegalStateException | JsonParseException e) {
            throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
        }
    }

    public static <T extends Response> T makeRequest(final Proxy proxy, final URL url, final Object input, final Class<T> classOfT, String auth) throws AuthenticationException {
        try {
            final String jsonResult = input == null ? performGetRequest(proxy, url, auth) : performPostRequest(proxy, url, gson.toJson(input), "application/json");
            final T result = gson.fromJson(jsonResult, classOfT);

            if (result == null) {
                return null;
            }

            if (StringUtils.isNotBlank(result.getError())) {
                if ("UserMigratedException".equals(result.getCause())) {
                    throw new UserMigratedException(result.getErrorMessage());
                } else if ("ForbiddenOperationException".equals(result.getError())) {
                    throw new InvalidCredentialsException(result.getErrorMessage());
                } else {
                    throw new AuthenticationException(result.getErrorMessage());
                }
            }

            return result;
        } catch (final IOException | IllegalStateException | JsonParseException e) {
            throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
        }
    }

    public static String performPostRequest(final Proxy proxy, final URL url, final String post, final String contentType) throws IOException {
        Validate.notNull(url);
        Validate.notNull(post);
        Validate.notNull(contentType);
        final HttpURLConnection connection = createUrlConnection(proxy, url);
        final byte[] postAsBytes = post.getBytes(StandardCharsets.UTF_8);

        connection.setRequestProperty("Content-Type", contentType + "; charset=utf-8");
        connection.setRequestProperty("Content-Length", "" + postAsBytes.length);
        connection.setDoOutput(true);

        LOGGER.debug("Writing POST data to {}: {}", url, post);

        OutputStream outputStream = null;
        try {
            outputStream = connection.getOutputStream();
            IOUtils.write(postAsBytes, outputStream);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

        return sendRequest(url, connection);
    }

    public static String performGetRequest(final Proxy proxy, final URL url) throws IOException {
        Validate.notNull(url);
        final HttpURLConnection connection = createUrlConnection(proxy, url);

        return sendRequest(url, connection);
    }

    public static String performGetRequest(final Proxy proxy, final URL url, final String auth) throws IOException {
        Validate.notNull(url);
        final HttpURLConnection connection = createUrlConnection(proxy, url);

        if (auth != null) {
            connection.setRequestProperty("Authorization", auth);
        }

        return sendRequest(url, connection);
    }

    private static String sendRequest(URL url, HttpURLConnection connection) throws IOException {
        LOGGER.debug("Reading data from {}", url);

        try (InputStream inputStream = connection.getInputStream()) {
            return readResponse(connection, inputStream);
        } catch (final IOException e) {
            InputStream errorStream = connection.getErrorStream();

            if (errorStream != null) {
                LOGGER.debug("Reading error page from {}", url);
                return readResponse(connection, errorStream);
            } else {
                LOGGER.debug("Request failed", e);
                throw e;
            }
        }
    }

    private static String readResponse(HttpURLConnection connection, InputStream errorStream) throws IOException {
        final String result = IOUtils.toString(errorStream, StandardCharsets.UTF_8);
        LOGGER.debug("Successful read, server response was {}", connection.getResponseCode());
        LOGGER.debug("Response: {}", result);
        return result;
    }

    public static HttpURLConnection createUrlConnection(final Proxy proxy, final URL url) throws IOException {
        Validate.notNull(url);
        LOGGER.debug("Opening connection to {}", url);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setUseCaches(false);
        return connection;
    }

    private static class GameProfileSerializer implements JsonSerializer<GameProfile>, JsonDeserializer<GameProfile> {
        @Override
        public GameProfile deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final JsonObject object = (JsonObject) json;
            final UUID id = object.has("id") ? context.deserialize(object.get("id"), UUID.class) : null;
            final String name = object.has("name") ? object.getAsJsonPrimitive("name").getAsString() : null;
            return new GameProfile(id, name);
        }

        @Override
        public JsonElement serialize(final GameProfile src, final Type typeOfSrc, final JsonSerializationContext context) {
            final JsonObject result = new JsonObject();
            if (src.getId() != null) {
                result.add("id", context.serialize(src.getId()));
            }
            if (src.getName() != null) {
                result.addProperty("name", src.getName());
            }
            return result;
        }
    }
}
