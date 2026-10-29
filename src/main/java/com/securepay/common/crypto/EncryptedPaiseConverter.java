package com.securepay.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * JPA {@code AttributeConverter} for the encrypted integer-paise wallet columns
 * ({@code w1/w2/w3/lien} on {@code users}), ports of the legacy accessor/mutator behaviour (doc 10 §10.1).
 *
 * <ul>
 *   <li><b>Read</b>: decrypt ciphertext → numeric value (paise). Decrypt failure → {@code 0}
 *       (replicates the graceful-zero try/catch in the source {@code User} model).</li>
 *   <li><b>Write</b>: stringify the paise value then encrypt with the legacy-compatible {@link LegacyCrypt}.</li>
 * </ul>
 *
 * <p>The stored value is "already in paise" (everything is ×100), so we keep it as {@code long}.
 * The source casts to {@code (float)} on read; we parse the decrypted string tolerantly.</p>
 */
@Converter
public class EncryptedPaiseConverter implements AttributeConverter<Long, String> {

    private static final Logger log = LoggerFactory.getLogger(EncryptedPaiseConverter.class);

    // Hibernate instantiates converters directly; Spring injects the shared LegacyCrypt bean
    // via container integration. As a fallback, a static holder is wired at startup
    // (see LegacyCryptInitializer).
    @Autowired
    private LegacyCrypt legacyCrypt;

    private LegacyCrypt crypt() {
        return legacyCrypt != null ? legacyCrypt : LegacyCryptHolder.get();
    }

    @Override
    public String convertToDatabaseColumn(Long attribute) {
        long value = attribute == null ? 0L : attribute;
        return crypt().encryptString(Long.toString(value));
    }

    @Override
    public Long convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return 0L;
        }
        try {
            String plain = crypt().decryptString(dbData);
            // Source casts to float; values are integer paise, but tolerate a decimal form.
            return (long) Math.floor(Double.parseDouble(plain.trim()));
        } catch (Exception e) {
            log.debug("Wallet decrypt failed, defaulting to 0 (graceful-zero parity)", e);
            return 0L;
        }
    }
}
