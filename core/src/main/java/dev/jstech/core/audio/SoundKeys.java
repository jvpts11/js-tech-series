/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import dev.jstech.core.content.ModContent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Every sound the mods of the series and their addons declared, looked up by id: what the mixer asks when the game is
 * about to play a sound, to know which channel it belongs to.
 */
public final class SoundKeys {

    private static volatile Map<ResourceLocation, SoundKey> byId = Map.of();
    private static volatile int counted = -1;

    private SoundKeys() {
    }

    /** The declared sound with that id, or null for a sound no mod of the series declared. */
    @Nullable
    public static SoundKey find(final ResourceLocation id) {
        return current().get(id);
    }

    /** Every declared sound, mod by mod in the order they were declared. */
    public static List<SoundKey> all() {
        final List<SoundKey> out = new ArrayList<>();
        for (final ModContent content : ModContent.all()) {
            out.addAll(content.declaredSounds());
        }
        return out;
    }

    /*
     * Declarations are made while the mods load and not after, so the table is built once they are in; a count that
     * has moved since (an addon declaring late, a test mod) builds it again rather than answering from an old one.
     */
    private static Map<ResourceLocation, SoundKey> current() {
        int total = 0;
        for (final ModContent content : ModContent.all()) {
            total += content.declaredSounds().size();
        }
        if (total != counted) {
            final Map<ResourceLocation, SoundKey> table = new HashMap<>();
            for (final SoundKey sound : all()) {
                table.put(sound.id(), sound);
            }
            byId = Map.copyOf(table);
            counted = total;
        }
        return byId;
    }
}
