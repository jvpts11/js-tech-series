/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hologram;

import dev.jstech.core.JsCore;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A hologram put up, moved, rewritten or taken down: the server's word to the players near it.
 *
 * @param id    the hologram's own id, which a later payload of the same id replaces
 * @param x     where its middle floats
 * @param y     where its lowest line sits
 * @param z     where its middle floats
 * @param lines what it says, top line first; none takes it down
 * @param ticks how long it stays up; 0 for as long as the player is near
 */
public record HologramPayload(ResourceLocation id, double x, double y, double z, List<Component> lines, int ticks)
        implements CustomPacketPayload {

    /** The most lines a hologram is sent with: more is a wall, not a label. */
    public static final int MOST_LINES = 16;

    public static final Type<HologramPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "hologram"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HologramPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, HologramPayload::id,
            ByteBufCodecs.DOUBLE, HologramPayload::x,
            ByteBufCodecs.DOUBLE, HologramPayload::y,
            ByteBufCodecs.DOUBLE, HologramPayload::z,
            ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list(MOST_LINES)), HologramPayload::lines,
            ByteBufCodecs.VAR_INT, HologramPayload::ticks,
            HologramPayload::new);

    public HologramPayload {
        lines = List.copyOf(lines);
    }

    @Override
    public Type<HologramPayload> type() {
        return TYPE;
    }
}
