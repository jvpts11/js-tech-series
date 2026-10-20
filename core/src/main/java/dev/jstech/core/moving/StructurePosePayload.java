/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: where a moving structure stood at a tick of the server, and how fast it moved and turned then, so
 * the player's game draws it smoothly between the poses it is told and carries it on past the last one at the same
 * speed until the next comes.
 *
 * @param entity   the structure's entity
 * @param tick     the server's tick the pose is of
 * @param pose     where the structure's pivot stood, and how it was turned
 * @param velocity how far it moved that tick, in blocks
 * @param spin     how it turned that tick: an axis whose length is the angle, in radians
 */
public record StructurePosePayload(int entity, long tick, StructurePose pose, float[] velocity, float[] spin)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructurePosePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "structure_pose"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructurePosePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, StructurePosePayload::entity,
                    ByteBufCodecs.VAR_LONG, StructurePosePayload::tick,
                    StructureCodecs.POSE_STREAM, StructurePosePayload::pose,
                    StructureCodecs.VECTOR_STREAM, StructurePosePayload::velocity,
                    StructureCodecs.VECTOR_STREAM, StructurePosePayload::spin,
                    StructurePosePayload::new);

    public StructurePosePayload {
        velocity = velocity.clone();
        spin = spin.clone();
    }

    @Override
    public CustomPacketPayload.Type<StructurePosePayload> type() {
        return TYPE;
    }
}
