package com.securepay.common.crypto;

/**
 * Static fallback holder so Hibernate-instantiated {@link EncryptedPaiseConverter} instances can
 * reach the Spring-managed {@link LegacyCrypt} bean even if container injection is unavailable.
 * Wired once at startup by {@link LegacyCryptInitializer}.
 */
public final class LegacyCryptHolder {

    private static volatile LegacyCrypt instance;

    private LegacyCryptHolder() {
    }

    static void set(LegacyCrypt crypt) {
        instance = crypt;
    }

    static LegacyCrypt get() {
        if (instance == null) {
            throw new CryptoException("LegacyCrypt not yet initialized");
        }
        return instance;
    }
}
