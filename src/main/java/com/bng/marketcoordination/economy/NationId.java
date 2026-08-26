package com.bng.marketcoordination.economy;

import java.util.UUID;

public record NationId(UUID uuid) {
    public NationId {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid");
        }
    }

    public static NationId random() {
        return new NationId(UUID.randomUUID());
    }
}
