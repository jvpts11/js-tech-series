/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.kubejs;

import dev.jstech.core.content.ModContent;
import dev.jstech.core.content.ScriptBinding;
import dev.jstech.core.machine.ProcessingKind;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;

/**
 * J's Core's plugin for KubeJS. KubeJS finds it through the {@code kubejs.plugins.txt} at the root of the Core's jar
 * and loads it itself; nothing else names this class, so without KubeJS it never loads. It hands every script the
 * bindings the mods built on the Core declared, each under its own name, the Core's own {@code JsCore} among them; the
 * Core's events under {@code JsCoreEvents} ({@link CoreKubeEvents}); and a recipe function for every kind of
 * processing machine any mod declared, under the kind's id ({@link ProcessingKubeSchema}).
 */
public final class CoreKubeJsPlugin implements KubeJSPlugin {

    @Override
    public void init() {
        CoreKubeEvents.listen();
    }

    @Override
    public void registerEvents(final EventGroupRegistry registry) {
        registry.register(CoreKubeEvents.GROUP);
    }

    @Override
    public void registerBindings(final BindingRegistry bindings) {
        for (final ModContent content : ModContent.all()) {
            for (final ScriptBinding binding : content.declaredScriptBindings()) {
                bindings.add(binding.name(), binding.statics());
            }
        }
    }

    @Override
    public void registerRecipeSchemas(final RecipeSchemaRegistry registry) {
        for (final ModContent content : ModContent.all()) {
            for (final ProcessingKind kind : content.declaredProcessing()) {
                registry.register(kind.id(), ProcessingKubeSchema.SCHEMA);
            }
        }
    }
}
