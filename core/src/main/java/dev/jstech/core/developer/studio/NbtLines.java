/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * What a block entity saves, as lines a person reads: each value by its path ({@code Energy}, {@code Items.0.id}),
 * compounds opened and lists numbered, a long value cut short. Read by the studio's block inspector and its round trip
 * through the save.
 */
public final class NbtLines {

    /** The most lines read, and the longest a value is written. */
    public static final int MOST = 200;
    public static final int LONGEST = 96;

    private static final String CUT = "...";

    private NbtLines() {
    }

    /** {@code tag}'s values by their paths, in the order it holds them, {@link #MOST} at most. */
    public static Map<String, String> of(final CompoundTag tag) {
        final Map<String, String> out = new LinkedHashMap<>();
        walk("", tag, out);
        return out;
    }

    /** The lines as a list of their own, each a compound of its path and value, to travel. */
    public static ListTag listed(final Map<String, String> lines) {
        final ListTag out = new ListTag();
        lines.forEach((path, value) -> {
            final CompoundTag line = new CompoundTag();
            line.putString("path", path);
            line.putString("value", value);
            out.add(line);
        });
        return out;
    }

    /** The lines {@link #listed} wrote, back as a map. */
    public static Map<String, String> unlisted(final ListTag listed) {
        final Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i < listed.size(); i++) {
            final CompoundTag line = listed.getCompound(i);
            out.put(line.getString("path"), line.getString("value"));
        }
        return out;
    }

    private static void walk(final String prefix, final Tag tag, final Map<String, String> out) {
        if (out.size() >= MOST) {
            return;
        }
        if (tag instanceof CompoundTag compound) {
            for (final String key : compound.getAllKeys()) {
                walk(prefix.isEmpty() ? key : prefix + "." + key, compound.get(key), out);
            }
            if (compound.isEmpty() && !prefix.isEmpty()) {
                out.put(prefix, "{}");
            }
        } else if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                walk(prefix + "." + i, list.get(i), out);
            }
            if (list.isEmpty()) {
                out.put(prefix, "[]");
            }
        } else {
            final String value = tag instanceof StringTag string ? string.getAsString() : tag.toString();
            out.put(prefix, value.length() > LONGEST ? value.substring(0, LONGEST - CUT.length()) + CUT : value);
        }
    }
}
