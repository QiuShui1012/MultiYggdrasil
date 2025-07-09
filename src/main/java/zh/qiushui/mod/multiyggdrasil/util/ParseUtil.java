/*
 * This file uses a lot of codes from authlib-injector project
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
package zh.qiushui.mod.multiyggdrasil.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import zh.qiushui.mod.multiyggdrasil.MultiYggdrasil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
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

    @SuppressWarnings("deprecation")
    public static Optional<PublicKey> getPublicKey(String url) {
        try (InputStream in = new URL(url).openConnection().getInputStream()) {
            return Optional.ofNullable(GSON.fromJson(asString(asBytes(in)), JsonObject.class).getAsJsonPrimitive("signaturePublickey"))
                .map(JsonPrimitive::getAsString)
                .map(ParseUtil::parseSignaturePublicKey);
        } catch (IOException e) {
            MultiYggdrasil.LOGGER.error("Failed to fetch metadata: {}", String.valueOf(e));
            throw new IllegalStateException(e);
        }
    }
}
