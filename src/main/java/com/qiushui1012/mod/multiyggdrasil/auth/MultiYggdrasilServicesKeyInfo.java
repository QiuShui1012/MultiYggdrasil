package com.qiushui1012.mod.multiyggdrasil.auth;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ServicesKeyInfo;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import com.qiushui1012.mod.multiyggdrasil.util.ParseUtil;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MultiYggdrasilServicesKeyInfo implements ServicesKeyInfo {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final List<PublicKey> PUBLIC_KEYS = new CopyOnWriteArrayList<>();

    static {
        PUBLIC_KEYS.add(loadMojangPublicKey());
    }

    private static PublicKey loadMojangPublicKey() {
        try (
            InputStream in =
                //#if AUTHLIB != 40043
                ServicesKeyInfo
                    //#else
                    //$$ MultiYggdrasilServicesKeyInfo
                    //#endif
                    .class
                    .getResourceAsStream("/yggdrasil_session_pubkey.der")
        ) {
            return ParseUtil.parseX509PublicKey(ParseUtil.asBytes(in));
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to load Mojang public key", e);
        }
    }

    @Override
    public int keyBitCount() {
        return 4096;
    }

    @Override
    public Signature signature() {
        return new DummySignature();
    }

    @Override
    public boolean validateProperty(Property property) {
        byte[] data = property.value().getBytes();
        byte[] sig = Base64.getDecoder().decode(property.signature());

        for (PublicKey key : PUBLIC_KEYS) try {
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(key);
            signature.update(data);
            if (signature.verify(sig)) return true;
        } catch (GeneralSecurityException e) {
            LOGGER.debug("Failed to verify signature with a key.", e);
        }

        LOGGER.debug("Failed to verify signature with all keys.");
        LOGGER.warn("Failed to verify property signature");
        return false;
    }
}