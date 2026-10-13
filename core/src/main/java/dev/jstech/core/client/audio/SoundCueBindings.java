/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.JsCore;
import dev.jstech.core.audio.SoundCue;
import dev.jstech.core.audio.SoundCues;
import dev.jstech.core.audio.SoundSet;
import dev.jstech.core.audio.SoundSetJson;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;

/**
 * The rules each cue has on this client, read from the resource packs: the file the top-most pack holds for a cue
 * wins, as it does for a texture, and a cue no pack speaks for keeps the rules its mod declared. A file that cannot
 * be read is said so in the log, and its cue keeps its declared rules: a broken pack never silences a cue.
 */
public final class SoundCueBindings implements ResourceManagerReloadListener {

    private static final Map<ResourceLocation, SoundSet> BOUND = new ConcurrentHashMap<>();

    @Override
    public void onResourceManagerReload(final ResourceManager manager) {
        BOUND.clear();
        for (final SoundCue cue : SoundCues.all()) {
            final Optional<Resource> file = manager.getResource(cue.file());
            if (file.isEmpty()) {
                continue;
            }
            try (Reader reader = file.get().openAsReader()) {
                BOUND.put(cue.id(), SoundSetJson.read(GsonHelper.parse(reader)));
            } catch (final IOException | RuntimeException unreadable) {
                JsCore.LOGGER.warn("The sounds of the cue {} could not be read, so it keeps its own: {}", cue.id(),
                        unreadable.getMessage());
            }
        }
    }

    /** The rules a cue has on this client: its packs', or its own. */
    public static SoundSet of(final SoundCue cue) {
        return BOUND.getOrDefault(cue.id(), cue.defaults());
    }

    /** Binds a cue to those rules until the next reload, as a pack's file would. */
    public static void bind(final SoundCue cue, final SoundSet rules) {
        BOUND.put(cue.id(), rules);
    }

    /** Gives a cue back its declared rules until the next reload. */
    public static void unbind(final SoundCue cue) {
        BOUND.remove(cue.id());
    }
}
