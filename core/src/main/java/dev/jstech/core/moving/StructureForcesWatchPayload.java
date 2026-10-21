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
 * Client to server: the player began, or stopped, looking at the forces on a moving structure, the diagram of forces
 * open or a physics tool holding it; while they look, they are told them a few times a second.
 *
 * @param entity   the structure's entity
 * @param watching whether they look now
 */
public record StructureForcesWatchPayload(int entity, boolean watching) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructureForcesWatchPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "structure_forces_watch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StructureForcesWatchPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, StructureForcesWatchPayload::entity, ByteBufCodecs.BOOL,
                    StructureForcesWatchPayload::watching, StructureForcesWatchPayload::new);

    @Override
    public CustomPacketPayload.Type<StructureForcesWatchPayload> type() {
        return TYPE;
    }
}
