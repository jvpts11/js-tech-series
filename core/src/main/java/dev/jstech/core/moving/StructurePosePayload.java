/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.JsCore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: where a moving structure stood at a tick of the server, and how fast it moved and turned then, so
 * the player's game draws it smoothly between the poses it is told and carries it on past the last one at the same
 * speed until the next comes. A structure held by another is also told where it stands on its parent, so the
 * player's game draws it on its parent as the parent is drawn, never a hair apart; and what stands on it, where on
 * it, so a mob, an item or another player on a deck is drawn on the deck as the deck is drawn.
 *
 * @param entity   the structure's entity
 * @param tick     the server's tick the pose is of
 * @param pose     where the structure's pivot stood, and how it was turned
 * @param velocity how far it moved that tick, in blocks
 * @param spin     how it turned that tick: an axis whose length is the angle, in radians
 * @param parent   the entity of the structure holding it, or -1 for none
 * @param local    where it stood on its parent, in the parent's yard; its pose when it has none
 * @param riders   what stood on it then, and where on it
 */
public record StructurePosePayload(int entity, long tick, StructurePose pose, float[] velocity, float[] spin,
                                   int parent, StructurePose local, List<Rider> riders)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructurePosePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "structure_pose"));
    /** The most riders told with one pose. */
    public static final int MOST_RIDERS = 512;

    public static final StreamCodec<RegistryFriendlyByteBuf, StructurePosePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.entity());
                buffer.writeVarLong(payload.tick());
                StructureCodecs.POSE_STREAM.encode(buffer, payload.pose());
                StructureCodecs.VECTOR_STREAM.encode(buffer, payload.velocity());
                StructureCodecs.VECTOR_STREAM.encode(buffer, payload.spin());
                buffer.writeVarInt(payload.parent() + 1);
                if (payload.parent() >= 0) {
                    StructureCodecs.POSE_STREAM.encode(buffer, payload.local());
                }
                buffer.writeVarInt(payload.riders().size());
                for (final Rider rider : payload.riders()) {
                    buffer.writeVarInt(rider.entity());
                    buffer.writeFloat(rider.x());
                    buffer.writeFloat(rider.y());
                    buffer.writeFloat(rider.z());
                    buffer.writeFloat(rider.yaw());
                }
            },
            buffer -> {
                final int entity = buffer.readVarInt();
                final long tick = buffer.readVarLong();
                final StructurePose pose = StructureCodecs.POSE_STREAM.decode(buffer);
                final float[] velocity = StructureCodecs.VECTOR_STREAM.decode(buffer);
                final float[] spin = StructureCodecs.VECTOR_STREAM.decode(buffer);
                final int parent = buffer.readVarInt() - 1;
                final StructurePose local = parent >= 0 ? StructureCodecs.POSE_STREAM.decode(buffer) : pose;
                final int count = Math.min(buffer.readVarInt(), MOST_RIDERS);
                final List<Rider> riders = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    riders.add(new Rider(buffer.readVarInt(), buffer.readFloat(), buffer.readFloat(),
                            buffer.readFloat(), buffer.readFloat()));
                }
                return new StructurePosePayload(entity, tick, pose, velocity, spin, parent, local, riders);
            });

    public StructurePosePayload {
        velocity = velocity.clone();
        spin = spin.clone();
        riders = List.copyOf(riders);
    }

    /** Whether it was held by another structure then. */
    public boolean held() {
        return parent >= 0;
    }

    @Override
    public CustomPacketPayload.Type<StructurePosePayload> type() {
        return TYPE;
    }

    /**
     * Something standing on the structure: its entity, where its feet are from the structure's pivot, in the yard's
     * axes, and how it looked from the structure's forward.
     *
     * @param entity the entity
     * @param x      its feet's east from the pivot, in the yard
     * @param y      their up
     * @param z      their south
     * @param yaw    how it looked, in degrees from the structure's forward
     */
    public record Rider(int entity, float x, float y, float z, float yaw) {
    }
}
