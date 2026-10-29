package com.securepay.common.crypto;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Publishes the {@link LegacyCrypt} bean into the static holder at startup. */
@Component
public class LegacyCryptInitializer {

    private final LegacyCrypt legacyCrypt;

    public LegacyCryptInitializer(LegacyCrypt legacyCrypt) {
        this.legacyCrypt = legacyCrypt;
        // Set eagerly in the constructor too, so converters used very early still resolve.
        LegacyCryptHolder.set(legacyCrypt);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        LegacyCryptHolder.set(legacyCrypt);
    }
}
