/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.state;

import dev.jstech.core.JsCore;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The value of a synced state that is a player's to see, sent to their game: the state by its id, then the value as
 * the state's own stream codec writes it.
 *
 * @param state the state
 * @param value its value for this player
 */
record StateSyncPayload(CoreState<?> state, Object value) implements CustomPacketPayload {

    static final CustomPacketPayload.Type<StateSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "state"));

    static final StreamCodec<RegistryFriendlyByteBuf, StateSyncPayload> STREAM_CODEC =
            StreamCodec.of(StateSyncPayload::write, StateSyncPayload::read);

    @Override
    public CustomPacketPayload.Type<StateSyncPayload> type() {
        return TYPE;
    }

    private static void write(final RegistryFriendlyByteBuf buf, final StateSyncPayload payload) {
        ResourceLocation.STREAM_CODEC.encode(buf, payload.state().id());
        payload.state().writeValue(buf, payload.value());
    }

    private static StateSyncPayload read(final RegistryFriendlyByteBuf buf) {
        final ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
        final CoreState<?> state = CoreStates.byId(id);
        if (state == null || !state.synced()) {
            throw new DecoderException("the server sent a state this game does not keep: " + id);
        }
        return new StateSyncPayload(state, state.readValue(buf));
    }
}
