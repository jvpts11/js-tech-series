/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import dev.jstech.core.JsCore;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: readings of the player's map of hazards in the dimension they are in, all of them as they arrive
 * in it (the nearest, up to a limit) or each new one as it is read.
 *
 * @param dimension the dimension the readings are of
 * @param replace   whether these are all the player's map holds there, to stand for what the game knew
 * @param readings  the readings
 */
public record HazardMapPayload(ResourceLocation dimension, boolean replace, List<Entry> readings)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HazardMapPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "hazard_map"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HazardMapPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, HazardMapPayload::dimension,
            ByteBufCodecs.BOOL, HazardMapPayload::replace,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), HazardMapPayload::readings,
            HazardMapPayload::new);

    public HazardMapPayload {
        readings = List.copyOf(readings);
    }

    @Override
    public CustomPacketPayload.Type<HazardMapPayload> type() {
        return TYPE;
    }

    /**
     * One reading.
     *
     * @param kind  the hazard read
     * @param chunk the chunk's place, packed as the game packs it
     * @param value the rate read, in the unit the hazard's rate is shown in
     * @param time  the game time it was read at
     */
    public record Entry(ResourceLocation kind, long chunk, float value, long time) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Entry::kind,
                ByteBufCodecs.VAR_LONG, Entry::chunk,
                ByteBufCodecs.FLOAT, Entry::value,
                ByteBufCodecs.VAR_LONG, Entry::time,
                Entry::new);
    }
}
