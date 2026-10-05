/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.datagen;

import dev.jstech.core.datagen.ContentCueProvider;
import dev.jstech.core.datagen.ContentFluidTagsProvider;
import dev.jstech.core.datagen.ContentItemModelProvider;
import dev.jstech.core.datagen.ContentLanguageProvider;
import dev.jstech.core.datagen.ContentSoundProvider;
import dev.jstech.core.datagen.MultiblockPatternProvider;
import dev.jstech.core.datagen.WorldGenProvider;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestSounds;
import dev.jstech.tests.TestWorldGen;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Data generation for the test mod: the structure templates the GameTests run inside, the entries, subtitles and cue
 * bindings of the sounds the tests play, the names of its content, the models of its buckets and the tags of its
 * fluids. Its block states and its other item models are written by hand.
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
        generator.addProvider(event.includeServer(), new MultiblockPatternProvider(output, JsTests.MODID));
        TestWorldGen.declare();
        generator.addProvider(event.includeServer(),
                new WorldGenProvider(output, event.getLookupProvider(), JsTests.MODID));
        generator.addProvider(event.includeServer(), new ContentFluidTagsProvider(output,
                event.getLookupProvider(), TestSounds.CONTENT, event.getExistingFileHelper()));
        generator.addProvider(event.includeClient(),
                new ContentSoundProvider(output, TestSounds.CONTENT, event.getExistingFileHelper()));
        generator.addProvider(event.includeClient(), new ContentLanguageProvider(output, TestSounds.CONTENT));
        generator.addProvider(event.includeClient(), new ContentCueProvider(output, TestSounds.CONTENT));
        generator.addProvider(event.includeClient(),
                new ContentItemModelProvider(output, TestSounds.CONTENT, event.getExistingFileHelper()));
    }
}
