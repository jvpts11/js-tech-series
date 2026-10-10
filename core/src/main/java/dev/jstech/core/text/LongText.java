/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Text of any length kept in a save: a file, a program's listing, a string a running program holds.
 *
 * <p>A string tag holds at most 65,535 bytes of modified UTF-8. Past that a save writes it as an empty string, so the
 * text is lost without a word, and the wire refuses it and drops the player. Text that fits is kept as a string tag,
 * as before, so saves made earlier read the same; text that does not is kept as its UTF-8 bytes, whose length has
 * no such cap. Reading takes either.
 */
public final class LongText {

    /** A string of any length, kept as a string while it fits one and as its UTF-8 bytes once it does not. */
    public static final Codec<String> CODEC = new Codec<>() {

        @Override
        public <T> DataResult<Pair<String, T>> decode(final DynamicOps<T> ops, final T input) {
            final DataResult<String> string = ops.getStringValue(input);
            if (string.result().isPresent()) {
                return string.map(text -> Pair.of(text, ops.empty()));
            }
            return ops.getByteBuffer(input).map(bytes -> Pair.of(utf8(bytes), ops.empty()));
        }

        @Override
        public <T> DataResult<T> encode(final String input, final DynamicOps<T> ops, final T prefix) {
            if (TextBounds.fitsTag(input)) {
                return ops.mergeToPrimitive(prefix, ops.createString(input));
            }
            return ops.mergeToPrimitive(prefix,
                    ops.createByteList(ByteBuffer.wrap(input.getBytes(StandardCharsets.UTF_8))));
        }

        @Override
        public String toString() {
            return "LongText";
        }
    };

    private LongText() {
    }

    /** Puts the text under {@code key}, as a string while it fits one. */
    public static void put(final CompoundTag tag, final String key, final String text) {
        if (TextBounds.fitsTag(text)) {
            tag.putString(key, text);
        } else {
            tag.putByteArray(key, text.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** The text under {@code key}, kept either way; empty when there is none. */
    public static String get(final CompoundTag tag, final String key) {
        final Tag held = tag.get(key);
        if (held instanceof StringTag string) {
            return string.getAsString();
        }
        if (held instanceof ByteArrayTag bytes) {
            return new String(bytes.getAsByteArray(), StandardCharsets.UTF_8);
        }
        return "";
    }

    /** Whether the tag holds text under {@code key}, kept either way. */
    public static boolean has(final CompoundTag tag, final String key) {
        final Tag held = tag.get(key);
        return held instanceof StringTag || held instanceof ByteArrayTag;
    }

    /**
     * Whether every string in the tag, and every key of every compound in it, fits a string tag: one that does not
     * would be written empty to a save, and would throw on the wire.
     */
    public static boolean fits(final Tag tag) {
        if (tag instanceof StringTag string) {
            return TextBounds.fitsTag(string.getAsString());
        }
        if (tag instanceof CompoundTag compound) {
            for (final String key : compound.getAllKeys()) {
                if (!TextBounds.fitsTag(key) || !fits(compound.get(key))) {
                    return false;
                }
            }
            return true;
        }
        if (tag instanceof ListTag list) {
            for (final Tag element : list) {
                if (!fits(element)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String utf8(final ByteBuffer bytes) {
        final ByteBuffer copy = bytes.duplicate();
        final byte[] array = new byte[copy.remaining()];
        copy.get(array);
        return new String(array, StandardCharsets.UTF_8);
    }
}
