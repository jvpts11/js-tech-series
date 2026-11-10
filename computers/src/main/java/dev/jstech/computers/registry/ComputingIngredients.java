/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.registry;

import dev.jstech.computers.JsComputers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The kinds of ingredient J's Computers' recipes take besides the game's own: an empty computer. */
public final class ComputingIngredients {

    private static final DeferredRegister<IngredientType<?>> TYPES = DeferredRegister.create(
            NeoForgeRegistries.Keys.INGREDIENT_TYPES, JsComputers.MODID);

    public static final DeferredHolder<IngredientType<?>, IngredientType<EmptyComputerIngredient>> EMPTY_COMPUTER =
            TYPES.register("empty_computer", () -> new IngredientType<>(EmptyComputerIngredient.CODEC,
                    EmptyComputerIngredient.STREAM_CODEC));

    private ComputingIngredients() {
    }

    /** Hands the kinds to the mod's bus, from the mod's constructor. */
    public static void register(final IEventBus modBus) {
        TYPES.register(modBus);
    }
}
