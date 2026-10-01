/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config.format;

import com.mojang.serialization.JavaOps;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/**
 * NBT, the game's own binary format, compressed as the game compresses its saves. It is for settings nobody edits by
 * hand, so it has no comments; a boolean is kept as the byte 0 or 1, as the game keeps them, and read back as one by
 * the setting that holds it.
 *
 * <p>Apart from the other formats, which need nothing of the game, this one is reached through {@link #INSTANCE}.
 */
public final class NbtConfigFormat implements IConfigFormat {

    public static final NbtConfigFormat INSTANCE = new NbtConfigFormat();

    /** The most a file may take to read, in bytes of what it holds, so a damaged one cannot take the memory. */
    private static final long MOST_BYTES = 16L << 20;

    private NbtConfigFormat() {
    }

    @Override
    public String extension() {
        return "nbt";
    }

    @Override
    public boolean keepsComments() {
        return false;
    }

    @Override
    public Map<String, Object> read(final byte[] file) throws ConfigFormatException {
        final CompoundTag tag;
        try {
            tag = NbtIo.readCompressed(new ByteArrayInputStream(file), NbtAccounter.create(MOST_BYTES));
        } catch (final IOException | RuntimeException e) {
            throw new ConfigFormatException("not compressed NBT: " + e.getMessage(), e);
        }
        final Object plain = NbtOps.INSTANCE.convertTo(JavaOps.INSTANCE, tag);
        return plain instanceof Map<?, ?> map ? PlainValues.map(map) : Map.of();
    }

    @Override
    public byte[] write(final Map<String, Object> values, final IConfigComments comments) {
        final Tag tag = JavaOps.INSTANCE.convertTo(NbtOps.INSTANCE, values);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(tag instanceof CompoundTag compound ? compound : new CompoundTag(), out);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }
}
