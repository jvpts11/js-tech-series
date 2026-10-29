/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import dev.jstech.core.data.DeclaredData;
import dev.jstech.space.JsSpace;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The suits' modules, by id, the id of each its item's: those declared in code, by J's Space or an add-on, and those a
 * data pack's files say, {@code data/<namespace>/jsspace/suit_modules/}, a file winning over a declaration of its id.
 */
public final class SuitModules {

    private static final DeclaredData<SuitModule> MODULES = new DeclaredData<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "suit_modules"), "jsspace/suit_modules",
            SuitModule.CODEC);

    private SuitModules() {
    }

    /** Declares the module of {@code id}, its item's id, as it is while no data pack says otherwise. */
    public static SuitModule declare(final ResourceLocation id, final SuitModule module) {
        return MODULES.declare(id, module);
    }

    /** The module of {@code id} on the side {@code level} is on, or none. */
    public static Optional<SuitModule> get(final Level level, final ResourceLocation id) {
        return MODULES.get(level, id);
    }

    /** Every module on the side {@code level} is on, by id: a data pack's over the declared. */
    public static Map<ResourceLocation, SuitModule> all(final Level level) {
        return MODULES.all(level);
    }

    /** Every module declared in code, by id. */
    public static Map<ResourceLocation, SuitModule> declared() {
        return MODULES.declared();
    }
}
