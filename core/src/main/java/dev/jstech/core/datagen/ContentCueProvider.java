/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.audio.SoundCue;
import dev.jstech.core.audio.SoundSetJson;
import dev.jstech.core.content.ModContent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes every cue a mod declares to {@code assets/<mod>/sound_cues/}, one file each, from the rules the code
 * declares: the file a resource pack puts its own in place of to bind the cue to other sounds.
 */
public final class ContentCueProvider implements DataProvider {

    private final PackOutput output;
    private final ModContent content;

    public ContentCueProvider(final PackOutput output, final ModContent content) {
        this.output = output;
        this.content = content;
    }

    @Override
    public String getName() {
        return content.modid() + ":sound_cues";
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        final List<CompletableFuture<?>> written = new ArrayList<>();
        for (final SoundCue cue : content.declaredCues()) {
            written.add(DataProvider.saveStable(cache, SoundSetJson.write(cue.defaults()), output
                    .getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                    .resolve(cue.file().getNamespace()).resolve(cue.file().getPath())));
        }
        return CompletableFuture.allOf(written.toArray(CompletableFuture[]::new));
    }
}
