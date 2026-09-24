/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.datagen;

import dev.jstech.core.datagen.ContentCueProvider;
import dev.jstech.core.datagen.ContentLanguageProvider;
import dev.jstech.core.datagen.ContentSoundProvider;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestSounds;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Data generation for the test mod: the structure templates the GameTests run inside, and the entries, subtitles and
 * cue bindings of the sounds the tests play.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class JsTestsDataGenerators {

    private JsTestsDataGenerators() {
    }

    @SubscribeEvent
    public static void onGatherData(final GatherDataEvent event) {
        final DataGenerator generator = event.getGenerator();
        final PackOutput output = generator.getPackOutput();
        generator.addProvider(event.includeServer(), new GameTestStructureProvider(output));
        generator.addProvider(event.includeClient(),
                new ContentSoundProvider(output, TestSounds.CONTENT, event.getExistingFileHelper()));
        generator.addProvider(event.includeClient(), new ContentLanguageProvider(output, TestSounds.CONTENT));
        generator.addProvider(event.includeClient(), new ContentCueProvider(output, TestSounds.CONTENT));
    }
}
