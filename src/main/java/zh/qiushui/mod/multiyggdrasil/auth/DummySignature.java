package zh.qiushui.mod.multiyggdrasil.auth;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

public class DummySignature extends Signature {
    protected DummySignature() {
        super("dummy-verify");
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
}
