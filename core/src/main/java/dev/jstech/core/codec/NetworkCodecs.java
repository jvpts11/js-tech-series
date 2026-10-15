/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.codec;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.ApiStatus;

/**
 * What travels between the server and the players, written the way it is saved: one codec to keep, and the network
 * codec taken from it, so the two never disagree.
 *
 * <pre>{@code
 * public static final StreamCodec<RegistryFriendlyByteBuf, Setting> STREAM_CODEC = NetworkCodecs.fromSave(CODEC);
 * }</pre>
 *
 * <p>It travels as the game's own compact tags. A value sent many times a second is better given a network codec of
 * its own, written field by field.
 */
@ApiStatus.Experimental
public final class NetworkCodecs {

    private NetworkCodecs() {
    }

    /** The network codec of what {@code save} writes, able to name the world's registries (items, blocks). */
    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromSave(final Codec<T> save) {
        return ByteBufCodecs.fromCodecWithRegistries(save);
    }

    /** The same, for a value that names nothing of the world's registries, sent where they are not known. */
    public static <T> StreamCodec<ByteBuf, T> fromSaveWithoutRegistries(final Codec<T> save) {
        return ByteBufCodecs.fromCodec(save);
    }
}
