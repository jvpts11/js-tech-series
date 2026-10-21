/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Example.
 */
package dev.jstech.example;

import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.datagen.ContentData;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * The example mod's entry: it hands the game what it declared, its settings file and its state, and has its data
 * generated from the same declarations.
 */
@Mod(JsExample.MODID)
public final class JsExample {

    public static final String MODID = "jsexample";

    public JsExample(final IEventBus modBus, final ModContainer container) {
        // Everything declared registers at once: blocks, items, block entities, cables, the part, the fluid, the menu.
        ExampleContent.CONTENT.register(modBus);
        ConfigFiles.register(ExampleSettings.FILE, modBus, container);
        ExampleState.register();
        // Block states, models, the English names and the loot tables, all from what was declared.
        modBus.addListener(GatherDataEvent.class, event -> ContentData.gather(event, ExampleContent.CONTENT));
    }

    /** A name in the mod's namespace. */
    public static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
