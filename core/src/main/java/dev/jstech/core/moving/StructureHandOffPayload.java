/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.JsCore;
import dev.jstech.core.region.Box;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: a structure's blocks are about to change hands with the world's, so the player's game hands the
 * drawing over in one frame rather than draw neither for a few. Arriving, the blocks leave {@code world} for the
 * structure's plot, and the land there is drawn as it was until the structure is drawn; landing, the structure is set
 * down at {@code pose} onto {@code world}, and it is drawn there until the land is drawn with its blocks.
 *
 * @param plot    the structure's plot of the yard
 * @param landing whether it is set down; otherwise its blocks arrive
 * @param pose    where it is set down, aligned to the grid; where it is, arriving
 * @param world   the box of the world its blocks leave, or land on
 */
public record StructureHandOffPayload(int plot, boolean landing, StructurePose pose, Box world)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructureHandOffPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "structure_hand_off"));

    private static final StreamCodec<ByteBuf, Box> BOX = StreamCodec.of((buffer, box) -> {
        buffer.writeInt(box.minX());
        buffer.writeInt(box.minY());
        buffer.writeInt(box.minZ());
        buffer.writeInt(box.maxX());
        buffer.writeInt(box.maxY());
        buffer.writeInt(box.maxZ());
    }, buffer -> new Box(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(),
            buffer.readInt()));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureHandOffPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, StructureHandOffPayload::plot,
                    ByteBufCodecs.BOOL, StructureHandOffPayload::landing,
                    StructureCodecs.POSE_STREAM, StructureHandOffPayload::pose,
                    BOX, StructureHandOffPayload::world,
                    StructureHandOffPayload::new);

    @Override
    public CustomPacketPayload.Type<StructureHandOffPayload> type() {
        return TYPE;
    }
}
