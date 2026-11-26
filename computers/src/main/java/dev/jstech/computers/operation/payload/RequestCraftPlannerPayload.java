/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.engine.CraftRequest;
import dev.jstech.computers.storage.StorageKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Client to server: the Craft Planner asks for data. With no {@code target} it requests the craft catalogue (the
 * pickable results, replied with a {@link CraftCatalogPayload}); with a target, an item or a liquid or gas a machine
 * makes, it requests a plan for {@code quantity} of it in its own units (replied with a {@link CraftPlannerPayload}).
 * Proximity + monitor-link gated like the Network Interactor.
 */
public record RequestCraftPlannerPayload(BlockPos host, BlockPos monitorPos, Optional<StorageKey> target,
                                         long quantity) implements CustomPacketPayload {

    public RequestCraftPlannerPayload {
        quantity = Math.min(quantity, CraftRequest.MOST_DEMAND);
    }

    /** Asks for the catalogue alone. */
    public static RequestCraftPlannerPayload catalogue(final BlockPos host, final BlockPos monitorPos) {
        return new RequestCraftPlannerPayload(host, monitorPos, Optional.empty(), 0L);
    }

    public static final CustomPacketPayload.Type<RequestCraftPlannerPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "request_craft_planner"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestCraftPlannerPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestCraftPlannerPayload::host,
                    BlockPos.STREAM_CODEC, RequestCraftPlannerPayload::monitorPos,
                    ByteBufCodecs.optional(StorageKey.STREAM_CODEC), RequestCraftPlannerPayload::target,
                    ByteBufCodecs.VAR_LONG, RequestCraftPlannerPayload::quantity,
                    RequestCraftPlannerPayload::new);

    @Override
    public CustomPacketPayload.Type<RequestCraftPlannerPayload> type() {
        return TYPE;
    }
}
