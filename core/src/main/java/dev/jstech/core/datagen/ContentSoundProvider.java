/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.SoundSpec;
import dev.jstech.core.content.ModContent;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

/**
 * A mod's {@code sounds.json}, written from the sounds it declared: each with its files, whether they are read as they
 * play, how far they carry and the key of its subtitle. A file named here that does not exist fails the generator,
 * so a sound cannot be declared ahead of the file it plays.
 */
public final class ContentSoundProvider extends SoundDefinitionsProvider {

    private final ModContent content;

    public ContentSoundProvider(final PackOutput output, final ModContent content, final ExistingFileHelper files) {
        super(output, content.modid(), files);
        this.content = content;
    }

    @Override
    public void registerSounds() {
        for (final SoundKey key : content.declaredSounds()) {
            final SoundSpec spec = key.spec();
            final SoundDefinition definition = definition().subtitle(key.subtitle().key());
            for (final ResourceLocation file : spec.files()) {
                definition.with(sound(file).stream(spec.stream()).attenuationDistance(spec.range()));
            }
            add(key.id(), definition);
        }
    }
}
