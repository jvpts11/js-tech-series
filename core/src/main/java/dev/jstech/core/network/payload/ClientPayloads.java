/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network.payload;

import com.mojang.logging.LogUtils;
import dev.jstech.core.JsCore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Where every payload a client sends is registered with its gate. Nothing is let through by default: the gate is
 * asked on the server thread before the handler runs, and a payload it refuses is dropped, the server log saying so
 * once in a while per player. A payload that acts on the menu the player has open is best registered with
 * {@link #onMenu}, whose handler is handed that menu, already checked, and needs to check nothing again.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class ClientPayloads {

    private static final Logger LOGGER = LogUtils.getLogger();
    /** How long a player's refused payloads stay out of the log after one is written, in milliseconds. */
    private static final long QUIET_MILLIS = 10_000L;

    /** When each player last had a refusal written down; the server thread is the only one that touches it. */
    private static final Map<UUID, Long> LAST_LOGGED = new HashMap<>();

    private ClientPayloads() {
    }

    /** Registers a payload a client sends, handled only for a sender its gate admits. */
    public static <P extends CustomPacketPayload> void accept(final PayloadRegistrar registrar,
                                                              final CustomPacketPayload.Type<P> type,
                                                              final StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
                                                              final IPayloadGate<P> gate,
                                                              final IServerPayloadHandler<P> handler) {
        accept(registrar, type, codec, gate, handler, null);
    }

    /** The same, for a payload whose sender has to be told when it is refused. */
    public static <P extends CustomPacketPayload> void accept(final PayloadRegistrar registrar,
                                                              final CustomPacketPayload.Type<P> type,
                                                              final StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
                                                              final IPayloadGate<P> gate,
                                                              final IServerPayloadHandler<P> handler,
                                                              @Nullable final IPayloadRefusal<P> refusal) {
        registrar.playToServer(type, codec, guarded(type, gate, handler, refusal));
    }

    /**
     * Registers a payload that acts on the menu the player has open: admitted only while a menu of that kind is
     * open, still holds for the player and stands on the block the payload names, and handled with that menu.
     */
    public static <P extends CustomPacketPayload, M extends AbstractContainerMenu> void onMenu(
            final PayloadRegistrar registrar,
            final CustomPacketPayload.Type<P> type,
            final StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
            final Class<M> kind,
            final Function<M, BlockPos> at,
            final Function<P, BlockPos> pos,
            final IMenuPayloadHandler<P, M> handler) {
        onMenu(registrar, type, codec, kind, at, pos, handler, null);
    }

    /** The same, for a payload whose sender has to be told when it is refused. */
    public static <P extends CustomPacketPayload, M extends AbstractContainerMenu> void onMenu(
            final PayloadRegistrar registrar,
            final CustomPacketPayload.Type<P> type,
            final StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
            final Class<M> kind,
            final Function<M, BlockPos> at,
            final Function<P, BlockPos> pos,
            final IMenuPayloadHandler<P, M> handler,
            @Nullable final IPayloadRefusal<P> refusal) {
        accept(registrar, type, codec, IPayloadGate.menu(kind, at, pos),
                (payload, player, level) -> handler.handle(payload, kind.cast(player.containerMenu), player, level),
                refusal);
    }

    /** The same, for a payload that names nothing the menu does not already hold. */
    public static <P extends CustomPacketPayload, M extends AbstractContainerMenu> void onMenu(
            final PayloadRegistrar registrar,
            final CustomPacketPayload.Type<P> type,
            final StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
            final Class<M> kind,
            final IMenuPayloadHandler<P, M> handler) {
        accept(registrar, type, codec, IPayloadGate.menu(kind),
                (payload, player, level) -> handler.handle(payload, kind.cast(player.containerMenu), player, level));
    }

    /** The gate around a handler, for a payload registered some other way, such as one that travels both ways. */
    public static <P extends CustomPacketPayload> IPayloadHandler<P> guarded(final CustomPacketPayload.Type<P> type,
                                                                            final IPayloadGate<P> gate,
                                                                            final IServerPayloadHandler<P> handler,
                                                                            @Nullable final IPayloadRefusal<P> refusal) {
        return (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && gate.admits(player, payload)) {
                handler.handle(payload, player, player.serverLevel());
                return;
            }
            refused(context.player(), type);
            if (refusal != null && context.player() instanceof ServerPlayer player) {
                refusal.tell(player, payload);
            }
        });
    }

    @SubscribeEvent
    public static void loggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_LOGGED.remove(event.getEntity().getUUID());
    }

    /*
     * A refusal is written down once in a while per player: often enough to notice a screen sending what its
     * gate does not expect, rarely enough that a client sending nothing else cannot fill the log.
     */
    private static void refused(@Nullable final Player player, final CustomPacketPayload.Type<?> type) {
        if (player == null) {
            return;
        }
        final long now = System.currentTimeMillis();
        final Long last = LAST_LOGGED.get(player.getUUID());
        if (last != null && now - last < QUIET_MILLIS) {
            return;
        }
        LAST_LOGGED.put(player.getUUID(), now);
        LOGGER.warn("Ignored {} from {}: it did not come from a screen open on what it names",
                type.id(), player.getName().getString());
    }

    /**
     * What the server does with a payload a client sent: on the server thread, after the gate admitted the sender,
     * with the sender and the level the sender is in.
     *
     * @param <P> the payload
     */
    @FunctionalInterface
    public interface IServerPayloadHandler<P> {
        void handle(P payload, ServerPlayer player, ServerLevel level);
    }

    /**
     * What the server does with a payload that acts on the menu the player has open, handed that menu.
     *
     * @param <P> the payload
     * @param <M> the menu
     */
    @FunctionalInterface
    public interface IMenuPayloadHandler<P, M extends AbstractContainerMenu> {
        void handle(P payload, M menu, ServerPlayer player, ServerLevel level);
    }

    /**
     * What a screen is told when its gate refuses what it sent. Most screens need nothing, but one that writes what
     * was typed at once and waits for the answer, such as a terminal, would otherwise be left waiting.
     *
     * @param <P> the payload
     */
    @FunctionalInterface
    public interface IPayloadRefusal<P> {
        void tell(ServerPlayer player, P payload);
    }
}
