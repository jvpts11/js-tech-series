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
 * Client to server, each tick a player sits at a helm and what they hold changes, and once a second besides: the
 * controls they hold, forward, back, left, right, up and down, for the structure the helm is on.
 *
 * @param seat the helm's seat
 * @param keys the controls held, one bit each in the order of {@link StructureControls.ControlAxis}
 */
public record StructureHelmPayload(int seat, int keys) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructureHelmPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "structure_helm"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureHelmPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, StructureHelmPayload::seat,
                    ByteBufCodecs.VAR_INT, StructureHelmPayload::keys,
                    StructureHelmPayload::new);

    /** The bit of {@code axis} among the keys. */
    public static int bit(final StructureControls.ControlAxis axis) {
        return switch (axis) {
            case FORWARD -> 1;
            case BACK -> 2;
            case LEFT -> 4;
            case RIGHT -> 8;
            case UP -> 16;
            case DOWN -> 32;
        };
    }

    @Override
    public CustomPacketPayload.Type<StructureHelmPayload> type() {
        return TYPE;
    }
}
