/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tools;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the player turned the wheel with the physics tool in the main hand, {@code steps} notches, up
 * positive; with shift held it changes the tool's strength, else its mode.
 *
 * @param steps    the notches turned
 * @param strength whether it changes the strength rather than the mode
 */
public record PhysicsToolScrollPayload(int steps, boolean strength) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PhysicsToolScrollPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "physics_tool_scroll"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhysicsToolScrollPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, PhysicsToolScrollPayload::steps, ByteBufCodecs.BOOL,
                    PhysicsToolScrollPayload::strength, PhysicsToolScrollPayload::new);

    @Override
    public CustomPacketPayload.Type<PhysicsToolScrollPayload> type() {
        return TYPE;
    }
}
