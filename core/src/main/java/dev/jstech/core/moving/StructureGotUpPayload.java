/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.JsCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

/**
 * Server to client: the player is getting up from a seat of a moving structure, and stands at {@code (x, y, z)} of its
 * yard, as {@code pose} has them; their game stands them there on the structure as it draws it. Told before the game
 * tells it they are off the seat, it is kept till then.
 *
 * @param x    where they stand in the yard, across
 * @param y    up
 * @param z    along
 * @param pose standing, crouching or crawling, as the room there has them
 */
record StructureGotUpPayload(double x, double y, double z, Pose pose) implements CustomPacketPayload {

    static final CustomPacketPayload.Type<StructureGotUpPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "structure_got_up"));
    static final StreamCodec<ByteBuf, StructureGotUpPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, StructureGotUpPayload::x,
            ByteBufCodecs.DOUBLE, StructureGotUpPayload::y,
            ByteBufCodecs.DOUBLE, StructureGotUpPayload::z,
            Pose.STREAM_CODEC, StructureGotUpPayload::pose,
            StructureGotUpPayload::new);

    /** Where they stand, in the yard. */
    Vec3 place() {
        return new Vec3(x, y, z);
    }

    @Override
    public CustomPacketPayload.Type<StructureGotUpPayload> type() {
        return TYPE;
    }
}
