/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * Writes a mod's English: every sentence it declares beside the code that says it, the names of its declared blocks,
 * items and tabs, and whatever else the mod names in registries of its own. A key written twice fails the run, so
 * two declarations cannot quietly claim one sentence.
 */
public final class ContentLanguageProvider extends LanguageProvider {

    private final ModContent content;
    private final List<Consumer<BiConsumer<String, String>>> more = new ArrayList<>();

    public ContentLanguageProvider(final PackOutput output, final ModContent content) {
        super(output, content.modid(), "en_us");
        this.content = content;
    }

    /** Adds names the mod keeps elsewhere, handed over as key and English. */
    void alsoNaming(final Consumer<BiConsumer<String, String>> names) {
        more.add(names);
    }

    @Override
    protected void addTranslations() {
        for (final TextKey key : DeclaredTexts.of(content.modid())) {
            add(key.key(), key.english());
        }
        for (final ContentTab tab : content.declaredTabs()) {
            add(tab.title().key(), tab.title().english());
        }
        for (final BlockEntry<?> block : content.declaredBlocks()) {
            add(block.get().getDescriptionId(), block.english());
        }
        for (final ItemEntry<?> item : content.declaredItems()) {
            add(item.get().getDescriptionId(), item.english());
        }
        more.forEach(names -> names.accept(this::add));
    }
}
