/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.datagen;

import dev.jstech.core.datagen.DeclaredTexts;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.JsIndustrial;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * Generates the English of the industrial mod: every sentence it declares beside the code that says it, and the
 * names of its blocks.
 */
public class JsIndustrialLanguageProvider extends LanguageProvider {

    public JsIndustrialLanguageProvider(final PackOutput output) {
        super(output, JsIndustrial.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        for (final TextKey key : DeclaredTexts.of(JsIndustrial.MODID)) {
            add(key.key(), key.english());
        }
        add("itemGroup.jsindustrial.industrial", "J's Industrial");
        addBlock(IndustrialModule.MACERATOR, "Macerator");
        addBlock(IndustrialModule.COAL_GENERATOR, "Coal Generator");
        addBlock(IndustrialModule.ELECTRIC_FURNACE, "Electric Furnace");
        addBlock(IndustrialModule.COMPRESSOR, "Compressor");
    }
}
