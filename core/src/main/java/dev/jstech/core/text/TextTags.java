/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Text in a save, in the form it was made in, so what a machine said before the world was closed is read in the
 * player's language when it is opened again.
 *
 * <p>Unlike the wire, a save keeps a sentence's English beside its key: the machine that reads it back is the one that
 * wrote it, and what it hands on as data, down a pipe or into a file, is that English. A save written by an older
 * version, whose sentence has since been reworded, still reads as what it said then.
 */
public final class TextTags {

    private static final String LITERAL = "l";
    private static final String KEY = "k";
    private static final String ENGLISH = "e";
    private static final String ARGS = "a";

    /** How deeply sentences sit inside each other, past which the rest is kept as the English it reads as. */
    private static final int MOST_DEPTH = 8;

    private TextTags() {
    }

    /** The text as a tag. */
    public static CompoundTag write(final Text text) {
        return write(text, 0);
    }

    /** The text a tag holds; a tag that holds none reads as nothing. */
    public static Text read(final CompoundTag tag) {
        return read(tag, 0);
    }

    /** A list of texts as a tag. */
    public static ListTag writeAll(final List<Text> texts) {
        final ListTag list = new ListTag();
        for (final Text text : texts) {
            list.add(write(text));
        }
        return list;
    }

    /** The texts a list tag holds. */
    public static List<Text> readAll(final ListTag list) {
        final List<Text> texts = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            texts.add(read(list.getCompound(i)));
        }
        return texts;
    }

    private static CompoundTag write(final Text text, final int depth) {
        final CompoundTag tag = new CompoundTag();
        if (text instanceof Text.Translated translated && depth < MOST_DEPTH) {
            tag.putString(KEY, translated.key().key());
            tag.putString(ENGLISH, translated.key().english());
            if (!translated.args().isEmpty()) {
                final ListTag args = new ListTag();
                for (final Text arg : translated.args()) {
                    args.add(write(arg, depth + 1));
                }
                tag.put(ARGS, args);
            }
            return tag;
        }
        tag.putString(LITERAL, text.english());
        return tag;
    }

    private static Text read(final CompoundTag tag, final int depth) {
        if (!tag.contains(KEY, Tag.TAG_STRING) || depth >= MOST_DEPTH) {
            return Text.literal(tag.getString(LITERAL));
        }
        final ListTag written = tag.getList(ARGS, Tag.TAG_COMPOUND);
        final List<Text> args = new ArrayList<>(written.size());
        for (int i = 0; i < written.size(); i++) {
            args.add(read(written.getCompound(i), depth + 1));
        }
        final String key = tag.getString(KEY);
        final String english = tag.getString(ENGLISH);
        try {
            return new Text.Translated(new TextKey(key, english.isBlank() ? key : english), args);
        } catch (final IllegalArgumentException notAKey) {
            return Text.literal(english.isBlank() ? key : english);
        }
    }
}
