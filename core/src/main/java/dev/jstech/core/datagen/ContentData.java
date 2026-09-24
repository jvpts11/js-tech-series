/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.content.ModContent;
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
 * already say (block states, models, the English, palettes, loot, block tags, recipe machines), and the mod adds
 * only what is its own on top.
 *
 * <pre>{@code
 * @SubscribeEvent
 * public static void onGatherData(final GatherDataEvent event) {
 *     final ContentData data = ContentData.gather(event, MyMod.CONTENT);
 *     data.server(new MyRecipeProvider(data.output(), data.lookup()));
 * }
 * }</pre>
 */
public final class ContentData {

    private final GatherDataEvent event;
    private final ContentLanguageProvider language;

    private ContentData(final GatherDataEvent event, final ContentLanguageProvider language) {
        this.event = event;
        this.language = language;
    }

    /** Adds the providers every declared block and item needs, and hands back the rest of the mod's data. */
    public static ContentData gather(final GatherDataEvent event, final ModContent content) {
        final PackOutput output = event.getGenerator().getPackOutput();
        final ContentData data = new ContentData(event, new ContentLanguageProvider(output, content));
        data.client(new ContentBlockStateProvider(output, content, data.existingFiles()));
        data.client(new ContentItemModelProvider(output, content, data.existingFiles()));
        data.client(data.language);
        data.client(new PaletteProvider(output, content.modid()));
        data.client(new ContentSoundProvider(output, content, data.existingFiles()));
        data.client(new ContentCueProvider(output, content));
        data.server(new ContentLootProvider(output, data.lookup(), content));
        data.server(new ContentBlockTagsProvider(output, data.lookup(), content, data.existingFiles()));
        data.server(new RecipeMachinesProvider(output, content));
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
