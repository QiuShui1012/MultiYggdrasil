package zh.qiushui.mod.multiyggdrasil.auth;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ServicesKeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zh.qiushui.mod.multiyggdrasil.util.ParseUtil;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BetterYggdrasilServicesKeyInfo implements ServicesKeyInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(BetterYggdrasilServicesKeyInfo.class);
    public static final List<PublicKey> PUBLIC_KEYS = new CopyOnWriteArrayList<>();

    static {
        PUBLIC_KEYS.add(loadMojangPublicKey());
    }

    private static PublicKey loadMojangPublicKey() {
        try (InputStream in = BetterYggdrasilServicesKeyInfo.class.getResourceAsStream("/mojang_publickey.der")) {
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
        return new Signature("authlib-injector-dummy-verify") {
            {
                state = VERIFY;
            }

            @Override
            protected boolean engineVerify(byte[] sigBytes) {
                return true;
            }

            @Override
            protected void engineUpdate(byte[] b, int off, int len) {
            }

            @Override
            protected void engineUpdate(byte b) {
            }

            @Override
            protected byte[] engineSign() {
                throw new UnsupportedOperationException();
            }

            @Override
            @Deprecated
            protected void engineSetParameter(String param, Object value) {
            }

            @Override
            protected void engineInitVerify(PublicKey publicKey) {
            }

            @Override
            protected void engineInitSign(PrivateKey privateKey) {
                throw new UnsupportedOperationException();
            }

            @Override
            @Deprecated
            protected Object engineGetParameter(String param) {
                return null;
            }
        };
    }

    @Override
    public boolean validateProperty(Property property) {
        byte[] data = property.value().getBytes();
        byte[] sig = Base64.getDecoder().decode(property.signature());

        for (PublicKey customKey : PUBLIC_KEYS) {
            try {
                Signature signature = Signature.getInstance("SHA1withRSA");
                signature.initVerify(customKey);
                signature.update(data);
                if (signature.verify(sig))
                    return true;
            } catch (GeneralSecurityException e) {
                LOGGER.debug("Failed to verify signature with key {}", customKey, e);
            }
        }

        LOGGER.warn("Failed to verify property signature");
        return false;
    }
}
