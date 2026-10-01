/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network.transfer;

import dev.jstech.core.JsCore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Values too large for one message, sent in packed pieces either way. The server sends each player a share of their
 * pieces each tick, so a large sending never holds the others up; a side keeps only so many sendings from one sender
 * open at once, and refuses one whose pieces come out of order, come to more than a sending may, or unpack into more
 * than a value may, so a sender cannot fill its memory.
 *
 * <p>A kind of value is declared once, on both sides, while the mods load: see {@link BigPayload}.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class BigPayloads {

    /** The most bytes one piece carries, well inside what one message may. */
    public static final int PIECE_BYTES = 30 * 1024;
    /** The most bytes a sending comes to, packed. */
    public static final int MOST_PACKED = 16 * 1024 * 1024;
    /** The most bytes a value may unpack into. */
    public static final int MOST_UNPACKED = 64 * 1024 * 1024;
    /** The most sendings one sender may have open at once. */
    public static final int MOST_OPEN = 8;
    /** The most bytes the server sends one player in a tick. */
    public static final int BYTES_PER_TICK = 256 * 1024;

    private static final String NETWORK_VERSION = "1";
    private static final Map<ResourceLocation, BigPayload<?>> KINDS = new ConcurrentHashMap<>();
    private static final Map<UUID, Deque<BigPiecePayload>> OUTGOING = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Assembly>> INCOMING = new ConcurrentHashMap<>();
    private static final AtomicInteger TRANSFERS = new AtomicInteger();
    /* Whose pieces came from the server, on a player's game. */
    private static final UUID SERVER = new UUID(0L, 0L);

    private BigPayloads() {
    }

    /**
     * Declares a kind of value, written and read with {@code codec}.
     *
     * @throws IllegalStateException when a kind of that id was declared already
     */
    public static <T> BigPayload<T> declare(final ResourceLocation id,
                                            final StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        final BigPayload<T> kind = new BigPayload<>(id, codec);
        if (KINDS.putIfAbsent(id, kind) != null) {
            throw new IllegalStateException("the big payload " + id + " is declared twice");
        }
        return kind;
    }

    /**
     * Puts the pieces of one sending back together, as the side that receives them does, and reads the value.
     *
     * @throws IOException when the pieces are out of order, too many or too large, or do not read as a value
     */
    public static <T> T assemble(final BigPayload<T> kind, final List<BigPiecePayload> pieces,
                                 final RegistryAccess registries) throws IOException {
        if (pieces.isEmpty()) {
            throw new IOException("a sending comes in one piece at least");
        }
        final Assembly assembly = new Assembly(pieces.getFirst().count());
        for (final BigPiecePayload piece : pieces) {
            assembly.accept(piece);
        }
        if (!assembly.order.complete()) {
            throw new IOException("the sending lacks pieces");
        }
        return kind.read(assembly.bytes.toByteArray(), registries);
    }

    @SubscribeEvent
    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION).playBidirectional(BigPiecePayload.TYPE, BigPiecePayload.STREAM_CODEC,
                BigPayloads::onPiece);
    }

    /* Each player is sent a share of their pieces. */
    @SubscribeEvent
    public static void afterServerTick(final ServerTickEvent.Post event) {
        final MinecraftServer server = event.getServer();
        for (final Iterator<Map.Entry<UUID, Deque<BigPiecePayload>>> it = OUTGOING.entrySet().iterator();
             it.hasNext();) {
            final Map.Entry<UUID, Deque<BigPiecePayload>> entry = it.next();
            final ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }
            int budget = BYTES_PER_TICK;
            final Deque<BigPiecePayload> queue = entry.getValue();
            synchronized (queue) {
                while (budget > 0 && !queue.isEmpty()) {
                    final BigPiecePayload piece = queue.poll();
                    PacketDistributor.sendToPlayer(player, piece);
                    budget -= Math.max(1, piece.data().length);
                }
                if (queue.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        OUTGOING.remove(event.getEntity().getUUID());
        INCOMING.remove(event.getEntity().getUUID());
    }

    /** The player's game left the server: what it was sent and had not put together is forgotten. */
    public static void forgetServer() {
        INCOMING.remove(SERVER);
    }

    static int nextTransfer() {
        return TRANSFERS.incrementAndGet();
    }

    static void queue(final ServerPlayer player, final List<BigPiecePayload> pieces) {
        final Deque<BigPiecePayload> queue = OUTGOING.computeIfAbsent(player.getUUID(), key -> new ArrayDeque<>());
        synchronized (queue) {
            queue.addAll(pieces);
        }
    }

    /* A piece came, on either side; it is put with the others on the main thread. */
    private static void onPiece(final BigPiecePayload piece, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final BigPayload<?> kind = KINDS.get(piece.kind());
            if (kind == null) {
                return;
            }
            final Player player = context.player();
            final UUID sender = player instanceof ServerPlayer ? player.getUUID() : SERVER;
            final Map<String, Assembly> open = INCOMING.computeIfAbsent(sender, key -> new LinkedHashMap<>());
            final String key = piece.kind() + "#" + piece.transfer();
            try {
                Assembly assembly = open.get(key);
                if (assembly == null) {
                    if (open.size() >= MOST_OPEN || piece.index() != 0) {
                        JsCore.LOGGER.warn("Refused a {} from {}: too many open, or it began in the middle",
                                piece.kind(), sender);
                        return;
                    }
                    assembly = new Assembly(piece.count());
                    open.put(key, assembly);
                }
                assembly.accept(piece);
                if (assembly.order.complete()) {
                    open.remove(key);
                    deliver(kind, assembly.bytes.toByteArray(), context);
                }
            } catch (final IOException e) {
                open.remove(key);
                JsCore.LOGGER.warn("Refused a {} from {}: {}", piece.kind(), sender, e.getMessage());
            }
        });
    }

    private static <T> void deliver(final BigPayload<T> kind, final byte[] packed, final IPayloadContext context)
            throws IOException {
        kind.received(kind.read(packed, context.player().registryAccess()), context);
    }

    /* The pieces of one sending so far. */
    private static final class Assembly {

        private final OrderedPieces order;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final int count;

        private Assembly(final int count) throws IOException {
            if (count <= 0 || (long) count * PIECE_BYTES > (long) MOST_PACKED + PIECE_BYTES) {
                throw new IOException("a sending of " + count + " pieces is more than may be sent");
            }
            this.count = count;
            this.order = new OrderedPieces(count, MOST_PACKED);
        }

        private void accept(final BigPiecePayload piece) throws IOException {
            if (piece.count() != this.count) {
                throw new IOException("a piece says the sending has " + piece.count() + " pieces, not " + this.count);
            }
            this.order.accept(piece.index(), piece.data().length);
            this.bytes.write(Objects.requireNonNull(piece.data(), "data"));
        }
    }
}
