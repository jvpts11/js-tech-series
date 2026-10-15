/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.call;

import dev.jstech.core.JsCore;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Every call a screen makes travels in this one payload: the call's id, then what the call's own codec writes. */
record MenuCallPayload(MenuCall<?, ?> call, Object argument) implements CustomPacketPayload {

    static final Type<MenuCallPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "menu_call"));

    static final StreamCodec<RegistryFriendlyByteBuf, MenuCallPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                ResourceLocation.STREAM_CODEC.encode(buffer, payload.call().id());
                payload.call().encode(buffer, payload.argument());
            },
            buffer -> {
                final ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buffer);
                final MenuCall<?, ?> call = MenuCalls.byId(id)
                        .orElseThrow(() -> new DecoderException("no screen call is declared as " + id));
                return new MenuCallPayload(call, call.decode(buffer));
            });

    @Override
    public Type<MenuCallPayload> type() {
        return TYPE;
    }

    /** Hands the argument to the call it was made for, as that call's own type; whether it was handled. */
    boolean receivedFrom(final ServerPlayer player) {
        return receive(call, player, argument);
    }

    private static <M extends AbstractContainerMenu, A> boolean receive(final MenuCall<M, A> call,
                                                                        final ServerPlayer player,
                                                                        final Object argument) {
        return call.receive(player, call.cast(argument));
    }
}
