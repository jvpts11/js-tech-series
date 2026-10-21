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
 * Client to server, just before the player's move: the moving structure the player stands on and where on it, in the
 * structure's own places; or, for a player driving a vehicle, where on the structure the vehicle stands. The server
 * puts the player, or the vehicle, there on its own structure, so a deck walked or driven on feels as steady as the
 * ground however fast it goes, and the server never takes the deck's motion for the player's.
 *
 * @param entity  the structure's entity, or -1 for standing on none
 * @param place   where the player's feet are, or the vehicle's underside, in the yard, {@code {x, y, z}}
 * @param vehicle whether it is the vehicle the player drives that stands there
 */
public record StructureFootingPayload(int entity, double[] place, boolean vehicle) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructureFootingPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "structure_footing"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureFootingPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, StructureFootingPayload::entity,
                    StructureCodecs.PLACE_STREAM, StructureFootingPayload::place,
                    ByteBufCodecs.BOOL, StructureFootingPayload::vehicle,
                    StructureFootingPayload::new);

    public StructureFootingPayload {
        place = place.clone();
    }

    /** Standing on no structure. */
    public static StructureFootingPayload none() {
        return new StructureFootingPayload(-1, new double[3], false);
    }

    /** The vehicle the player drives standing on no structure. */
    public static StructureFootingPayload noVehicle() {
        return new StructureFootingPayload(-1, new double[3], true);
    }

    @Override
    public CustomPacketPayload.Type<StructureFootingPayload> type() {
        return TYPE;
    }
}
