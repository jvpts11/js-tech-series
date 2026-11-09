/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.core.audio.media.MediaId;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * A picture in a portable's gallery (computers/docs/design/wireless.md): the PNG its camera took, which the server's
 * store keeps as it keeps a recording, how large it is, the day it was taken, and a small copy of it, which is all its
 * player's game is sent to show the gallery.
 *
 * @param number    its number in the gallery, which names its file: {@code picture_0007.png}
 * @param media     the PNG the store keeps
 * @param width     how wide it is, in pixels
 * @param height    how tall
 * @param day       the world's day it was taken, counting from one
 * @param thumbnail the small copy, a PNG of its own
 */
public record PortablePicture(int number, MediaId media, int width, int height, long day, byte[] thumbnail) {

    /** The most bytes a small copy is kept at; one larger is no small copy. */
    public static final int MOST_THUMBNAIL = 24 * 1024;
    /** How wide a small copy is, four to every three across. */
    public static final int THUMBNAIL_WIDTH = 72;
    /** How tall. */
    public static final int THUMBNAIL_HEIGHT = 54;

    public PortablePicture {
        Objects.requireNonNull(media, "media");
        thumbnail = thumbnail.clone();
    }

    /** The picture read back from what {@link #write} wrote, or null for one that cannot be. */
    @Nullable
    public static PortablePicture read(final CompoundTag tag) {
        try {
            final MediaId media = new MediaId(tag.getString("Hash"), tag.getString("Format"), tag.getLong("Bytes"));
            return new PortablePicture(tag.getInt("Number"), media, tag.getInt("Width"), tag.getInt("Height"),
                    tag.getLong("Day"), tag.getByteArray("Thumbnail"));
        } catch (final IllegalArgumentException malformed) {
            return null;
        }
    }

    /** The name of the file of the picture numbered {@code number}: {@code picture_0007.png}. */
    public static String fileNameOf(final int number) {
        return String.format(Locale.ROOT, "picture_%04d.png", number);
    }

    /** Its file's name, as the gallery and a computer it is sent to call it. */
    public String fileName() {
        return fileNameOf(number);
    }

    /** The small copy, a copy of it. */
    @Override
    public byte[] thumbnail() {
        return thumbnail.clone();
    }

    /** The picture as it is saved. */
    public CompoundTag write() {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("Number", number);
        tag.putString("Hash", media.hash());
        tag.putString("Format", media.format());
        tag.putLong("Bytes", media.bytes());
        tag.putInt("Width", width);
        tag.putInt("Height", height);
        tag.putLong("Day", day);
        tag.putByteArray("Thumbnail", thumbnail);
        return tag;
    }
}
