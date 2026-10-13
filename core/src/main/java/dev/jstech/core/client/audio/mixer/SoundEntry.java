/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.mixer;

import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.SoundKeys;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A sound as the mixer lists it: by its subtitle, which is how a player knows it, with its id under it.
 *
 * @param id     the sound's id
 * @param name   its subtitle, or null when it has none
 * @param series whether a mod of the series declared it
 * @param made   whether it is made as it plays, and so has no file to preview
 */
record SoundEntry(ResourceLocation id, @Nullable Component name, boolean series, boolean made) {

    /** What the list shows as its name: the subtitle, or the id when there is none. */
    Component shown() {
        return name != null ? name : GameText.component(Text.literal(id.toString()));
    }

    /** Whether it answers a search, by its name or its id, case aside. */
    boolean matches(final String query) {
        final String wanted = query.toLowerCase(Locale.ROOT);
        return wanted.isEmpty() || id.toString().contains(wanted)
                || name != null && name.getString().toLowerCase(Locale.ROOT).contains(wanted);
    }

    /**
     * Every sound the game knows, the game's own, every mod's and the series' own made as they play, sorted by name,
     * the ones with no subtitle after the rest.
     */
    static List<SoundEntry> all(final SoundManager sounds) {
        final Map<ResourceLocation, SoundEntry> byId = new LinkedHashMap<>();
        for (final ResourceLocation id : sounds.getAvailableSounds()) {
            final WeighedSoundEvents events = sounds.getSoundEvent(id);
            byId.put(id, new SoundEntry(id, events == null ? null : events.getSubtitle(), SoundKeys.find(id) != null,
                    false));
        }
        for (final SoundKey key : SoundKeys.all()) {
            if (key.spec().made()) {
                byId.put(key.id(), new SoundEntry(key.id(), GameText.component(key.subtitle()), true, true));
            }
        }
        final List<SoundEntry> out = new ArrayList<>(byId.values());
        out.sort(Comparator.comparing((SoundEntry one) -> one.name == null)
                .thenComparing(one -> one.shown().getString().toLowerCase(Locale.ROOT))
                .thenComparing(one -> one.id.toString()));
        return out;
    }
}
