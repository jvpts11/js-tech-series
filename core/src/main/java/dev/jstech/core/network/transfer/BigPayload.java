/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network.transfer;

import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * A kind of value too large for one message, declared with {@link BigPayloads#declare}: written with its codec,
 * packed, sent in pieces, and on the other side put together, unpacked and read back before the side that receives it
 * is handed it. What a side does with one is said on that side, so a player's game says it from its client code:
 *
 * <pre>{@code
 * BigPayload<Picture> PICTURE = BigPayloads.declare(id("picture"), Picture.STREAM_CODEC);
 * PICTURE.sendToPlayer(player, picture);              // on the server
 * PICTURE.onReceive((picture, context) -> show(picture)); // in the player's client code
 * }</pre>
 *
 * @param <T> the value
 */
public final class BigPayload<T> {

    private final ResourceLocation id;
    private final StreamCodec<RegistryFriendlyByteBuf, T> codec;
    private volatile @Nullable IReceiver<T> receiver;

    BigPayload(final ResourceLocation id, final StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        this.id = Objects.requireNonNull(id, "id");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    public ResourceLocation id() {
        return this.id;
    }

    /** What this side does with a value of this kind it receives. */
    public void onReceive(final IReceiver<T> handler) {
        this.receiver = Objects.requireNonNull(handler, "handler");
    }

    /** Sends {@code value} to {@code player}, a share of its pieces each tick. */
    public void sendToPlayer(final ServerPlayer player, final T value) {
        BigPayloads.queue(player, pieces(value, player.registryAccess()));
    }

    /** Sends {@code value} to the server, from a player's game. */
    public void sendToServer(final RegistryAccess registries, final T value) {
        for (final BigPiecePayload piece : pieces(value, registries)) {
            PacketDistributor.sendToServer(piece);
        }
    }

    /** {@code value} as the pieces it is sent in: written with the codec, packed, and cut. */
    public List<BigPiecePayload> pieces(final T value, final RegistryAccess registries) {
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            this.codec.encode(buffer, value);
            final byte[] raw = new byte[buffer.readableBytes()];
            buffer.readBytes(raw);
            if (raw.length > BigPayloads.MOST_UNPACKED) {
                throw new IllegalArgumentException("a " + this.id + " of " + raw.length
                        + " bytes is too large to send");
            }
            final byte[] packed = Compression.pack(raw);
            final int count = Pieces.count(packed.length, BigPayloads.PIECE_BYTES);
            final int transfer = BigPayloads.nextTransfer();
            final List<BigPiecePayload> pieces = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                final int start = (int) Pieces.start(index, BigPayloads.PIECE_BYTES);
                final int length = Pieces.length(index, packed.length, BigPayloads.PIECE_BYTES);
                final byte[] data = new byte[length];
                System.arraycopy(packed, start, data, 0, length);
                pieces.add(new BigPiecePayload(this.id, transfer, index, count, data));
            }
            return pieces;
        } finally {
            buffer.release();
        }
    }

    /**
     * The value packed bytes put together from the pieces stand for.
     *
     * @throws IOException when the bytes do not unpack within the limit, or do not read as a value of this kind
     */
    public T read(final byte[] packed, final RegistryAccess registries) throws IOException {
        final byte[] raw = Compression.unpack(packed, BigPayloads.MOST_UNPACKED);
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(raw), registries);
        try {
            final T value = this.codec.decode(buffer);
            if (buffer.isReadable()) {
                throw new IOException("a " + this.id + " left " + buffer.readableBytes() + " bytes unread");
            }
            return value;
        } catch (final RuntimeException e) {
            throw new IOException("the bytes do not read as a " + this.id, e);
        } finally {
            buffer.release();
        }
    }

    /* A value of this kind came, whole: what this side said to do with it. */
    void received(final T value, final IPayloadContext context) {
        final IReceiver<T> handler = this.receiver;
        if (handler != null) {
            handler.receive(value, context);
        }
    }

    /** What a side does with a value it receives, on its main thread. */
    @FunctionalInterface
    public interface IReceiver<T> {

        void receive(T value, IPayloadContext context);
    }
}
