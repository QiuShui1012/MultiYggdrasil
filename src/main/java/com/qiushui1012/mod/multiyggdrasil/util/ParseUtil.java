/*
 * This file uses some codes from authlib-injector project
 * (https://github.com/yushijinhun/authlib-injector/) and has some
 * custom modifications.
 * Below is the License Header from original files.
 *
 *
 * Copyright (C) 2019  Haowei Wen <yushijinhun@gmail.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.qiushui1012.mod.multiyggdrasil.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.qiushui1012.mod.multiyggdrasil.MultiYggdrasil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ParseUtil {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String removeNewLines(String input) {
        return input.replace("\n", "")
            .replace("\r", "");
    }

    private static final String PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_KEY_END = "-----END PUBLIC KEY-----";

    public static byte[] decodePEMPublicKey(String pem) throws IllegalArgumentException {
        pem = removeNewLines(pem);
        if (!pem.startsWith(PUBLIC_KEY_HEADER) || !pem.endsWith(PUBLIC_KEY_END)) throw new IllegalArgumentException("Bad key format");
        return Base64.getDecoder().decode(pem.substring(PUBLIC_KEY_HEADER.length(), pem.length() - PUBLIC_KEY_END.length()));
    }

    public static PublicKey parseX509PublicKey(byte[] encodedKey) throws GeneralSecurityException {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encodedKey));
    }

    public static PublicKey parseSignaturePublicKey(String pem) throws UncheckedIOException {
        try {
            return parseX509PublicKey(decodePEMPublicKey(pem));
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new UncheckedIOException(new IOException("Bad signature public key", e));
        }
    }

    public static byte[] asBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transfer(in, out);
        return out.toByteArray();
    }

    public static void transfer(InputStream from, OutputStream to) throws IOException {
        byte[] buf = new byte[8192];
        int read;
        while ((read = from.read(buf)) != -1) {
            to.write(buf, 0, read);
        }
    }

    public static String asString(byte[] bytes) {
        return new String(bytes, UTF_8);
    }

    public static Optional<PublicKey> getPublicKey(String url) {
        MultiYggdrasil.LOGGER.info("Public Key URL: {}", url);

        Throwable ex = null;
        for (int i = 0; i < 5; i++) {
            HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)) // 这个URL可能会301跳转
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                // HttpClient已经自动完成了重定向，这里拿到的是最终响应

                String content = response.body();
                MultiYggdrasil.LOGGER.info("Response Code: {}", response.statusCode());
                MultiYggdrasil.LOGGER.info("Response Body: {}", content);
                return Optional.ofNullable(GSON.fromJson(content, JsonObject.class).getAsJsonPrimitive("signaturePublickey"))
                    .map(JsonPrimitive::getAsString)
                    .map(ParseUtil::parseSignaturePublicKey);
            } catch (IOException | JsonSyntaxException e) {
                MultiYggdrasil.LOGGER.error("Failed to fetch metadata", e);
                throw new IllegalStateException(e);
            } catch (InterruptedException e) {
                MultiYggdrasil.LOGGER.warn("Failed to fetch metadata. Retrying... ({}/5)", i);
                ex = e;
            }
            //#if MC >= 1_20_05
            //$$ finally {
            //$$     client.close();
            //$$ }
            //#endif
        }
        MultiYggdrasil.LOGGER.error("Failed to fetch metadata", ex);
        throw new IllegalStateException(ex);
    }
}
