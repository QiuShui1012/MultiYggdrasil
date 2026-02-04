package zh.qiushui.mod.multiyggdrasil.util.vap;

/**
 * Used for authlib with version 1.5.25 or before,
 * as they don't support custom yggdrasil source.
 */
public interface Environment {
    String getAuthHost();

    String getAccountsHost();

    String getSessionHost();

    String getName();

    String asString();
}
