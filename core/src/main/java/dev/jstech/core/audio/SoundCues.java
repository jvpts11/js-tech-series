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
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Every cue the mods of the series and their addons declared, looked up by id. */
public final class SoundCues {

    private SoundCues() {
    }

    /** The declared cue with that id, or null for a cue no mod declared. */
    @Nullable
    public static SoundCue find(final ResourceLocation id) {
        for (final ModContent content : ModContent.all()) {
            for (final SoundCue cue : content.declaredCues()) {
                if (cue.id().equals(id)) {
                    return cue;
                }
            }
        }
        return null;
    }

    /** Every declared cue, mod by mod in the order they were declared. */
    public static List<SoundCue> all() {
        final List<SoundCue> out = new ArrayList<>();
        for (final ModContent content : ModContent.all()) {
            out.addAll(content.declaredCues());
        }
        return out;
    }
}
