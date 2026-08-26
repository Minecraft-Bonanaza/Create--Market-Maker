package com.bng.marketcoordination.market;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public record MarketId(UUID uuid) {
    public MarketId {
        Objects.requireNonNull(uuid, "uuid");
    }

    public static MarketId random() {
        return new MarketId(UUID.randomUUID());
    }

    public static MarketId fromLedger(ResourceKey<Level> dimension, BlockPos ledgerPos) {
        ResourceLocation dimId = dimension.location();
        String seed = dimId + "@" + ledgerPos.asLong();
        return new MarketId(UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)));
    }
}
