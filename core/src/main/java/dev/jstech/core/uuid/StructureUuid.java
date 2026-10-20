/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.uuid;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

/**
 * Strongly-typed wrapper for the UUID of a moving structure, which it keeps for its whole life: through a restart, a
 * change of dimension and every pose it takes.
 */
public record StructureUuid(UUID value) {

    public static final Codec<StructureUuid> CODEC = UUIDUtil.CODEC.xmap(StructureUuid::new, StructureUuid::value);
    public static final StreamCodec<ByteBuf, StructureUuid> STREAM_CODEC =
            UUIDUtil.STREAM_CODEC.map(StructureUuid::new, StructureUuid::value);

    public StructureUuid {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static StructureUuid random() {
        return new StructureUuid(UUID.randomUUID());
    }

    public String asString() {
        return value.toString();
    }
}
