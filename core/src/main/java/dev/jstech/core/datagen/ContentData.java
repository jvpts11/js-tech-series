/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.content.ModContent;
import dev.jstech.core.worldgen.WorldGen;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * A mod's data generation, started from its declared content: {@link #gather} adds everything the declarations
 * already say (block states, models, the English, palettes, sounds, fonts, loot, block tags, the item tags its tabs
 * name, recipe machines), and
 * the mod adds only what is its own on top.
 *
 * <pre>{@code
 * modBus.addListener(GatherDataEvent.class, MyDataGenerators::onGatherData);  // in the mod's constructor
 *
 * public static void onGatherData(final GatherDataEvent event) {
 *     final ContentData data = ContentData.gather(event, MyMod.CONTENT);
 *     data.server(new MyRecipeProvider(data.output(), data.lookup()));
 *     data.check();
 * }
 * }</pre>
 */
public final class ContentData {

    private final GatherDataEvent event;
    private final String modid;
    private final ContentLanguageProvider language;

    private ContentData(final GatherDataEvent event, final String modid, final ContentLanguageProvider language) {
        this.event = event;
        this.modid = modid;
        this.language = language;
    }

    /** Adds the providers every declared block and item needs, and hands back the rest of the mod's data. */
    public static ContentData gather(final GatherDataEvent event, final ModContent content) {
        final PackOutput output = event.getGenerator().getPackOutput();
        final ContentData data = new ContentData(event, content.modid(), new ContentLanguageProvider(output, content));
        data.client(new ContentBlockStateProvider(output, content, data.existingFiles()));
        data.client(new ContentItemModelProvider(output, content, data.existingFiles()));
        data.client(data.language);
        data.client(new PaletteProvider(output, content.modid()));
        data.client(new MotionProfileProvider(output, content.modid()));
        data.client(new ContentSoundProvider(output, content, data.existingFiles()));
        data.client(new ContentCueProvider(output, content));
        content.declaredGuides().forEach(guide -> data.client(new GuideProvider(output, guide)));
        if (!content.declaredFonts().isEmpty()) {
            data.client(new FontSheetProvider(output, content, data.existingFiles()));
        }
        data.server(new ContentLootProvider(output, data.lookup(), content));
        data.server(new ContentBlockTagsProvider(output, data.lookup(), content, data.existingFiles()));
        if (ContentItemTagsProvider.hasTags(content)) {
            data.server(new ContentItemTagsProvider(output, data.lookup(), content, data.existingFiles()));
        }
        if (!content.declaredFluids().isEmpty()) {
            data.server(new ContentFluidTagsProvider(output, data.lookup(), content, data.existingFiles()));
        }
        data.server(new MultiblockPatternProvider(output, content.modid()));
        if (WorldGen.declares(content.modid())) {
            data.server(new WorldGenProvider(output, data.lookup(), content.modid()));
        }
        return data;
    }

    /** Adds names the mod keeps in registries of its own to its English, handed over as key and English. */
    public ContentData alsoNaming(final Consumer<BiConsumer<String, String>> names) {
        language.alsoNaming(names);
        return this;
    }

    /** Adds a provider of assets: models, textures, languages. */
    public ContentData client(final DataProvider provider) {
        event.getGenerator().addProvider(event.includeClient(), provider);
        return this;
    }

    /** Adds a provider of data: recipes, loot, tags, advancements. */
    public ContentData server(final DataProvider provider) {
        event.getGenerator().addProvider(event.includeServer(), provider);
        return this;
    }

    /**
     * Adds, after everything the mod added, the check that fails the build when a block or an item the mod registered
     * has no English name, no block state, no model, a model whose texture is nowhere, or no loot table while it
     * drops something. Call it last: the steps run in the order they were added, and it reads what they wrote.
     */
    public void check() {
        if (event.includeClient() && event.includeServer()) {
            event.getGenerator().addProvider(true, new ContentCheckProvider(output(), existingFiles(), modid));
        }
    }

    public PackOutput output() {
        return event.getGenerator().getPackOutput();
    }

    public CompletableFuture<HolderLookup.Provider> lookup() {
        return event.getLookupProvider();
    }

    public ExistingFileHelper existingFiles() {
        return event.getExistingFileHelper();
    }
}
