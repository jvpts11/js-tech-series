/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.menu;

import io.netty.buffer.Unpooled;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * A value of any kind an open menu shows that the server works out and the client's copy of the menu receives
 * whenever it changes: a name, a list, a record of the machine's state. Read through {@link #get()} on both sides: the
 * server works it out each time, the client answers what it last received. Declared with {@link CoreMenu#synced},
 * under a name a screen's file binds its components to.
 *
 * <p>Unlike the game's own menu data, which carries a short each, it carries whatever its codec writes, so a number
 * past 32767 arrives whole.
 *
 * @param <T> what it holds
 */
public final class SyncedValue<T> {

    private final String name;
    private final StreamCodec<? super RegistryFriendlyByteBuf, T> codec;
    private final Supplier<T> onServer;
    private final boolean onClient;
    private T received;
    private T sent;
    private boolean everSent;

    SyncedValue(final String name, final StreamCodec<? super RegistryFriendlyByteBuf, T> codec, final T initial,
                final Supplier<T> onServer, final boolean onClient) {
        this.name = Objects.requireNonNull(name, "name");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.received = Objects.requireNonNull(initial, "initial");
        this.onServer = Objects.requireNonNull(onServer, "onServer");
        this.onClient = onClient;
    }

    /** The name a screen binds to. */
    public String name() {
        return name;
    }

    /** The value: worked out on the server, as last received on the client. */
    public T get() {
        return onClient ? received : onServer.get();
    }

    /*
     * The value written for the client when it is not what was sent last, or {@code null} when it is; {@code all}
     * writes it whatever was sent, as a menu does when it opens.
     */
    byte[] pending(final RegistryAccess access, final boolean all) {
        final T now = onServer.get();
        if (!all && everSent && Objects.equals(now, sent)) {
            return null;
        }
        sent = now;
        everSent = true;
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        try {
            codec.encode(buffer, now);
            final byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }

    /* What the server wrote, read into the client's copy. */
    void receive(final byte[] bytes, final RegistryAccess access) {
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), access);
        try {
            received = codec.decode(buffer);
        } finally {
            buffer.release();
        }
    }
}
