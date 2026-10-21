/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.condition;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.JsCore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.ApiStatus;

/**
 * The conditions the Core gives every data file, beside NeoForge's own ({@code neoforge:mod_loaded},
 * {@code neoforge:not}, {@code neoforge:and}, {@code neoforge:or}, {@code neoforge:tag_empty}).
 */
public final class DataConditions {

    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS = DeferredRegister.create(
            NeoForgeRegistries.Keys.CONDITION_CODECS, JsCore.MODID);

    /** That a setting has a value: {@code jscore:setting}. */
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<SettingCondition>> SETTING =
            CONDITIONS.register("setting", () -> SettingCondition.CODEC);

    private DataConditions() {
    }

    /** Registers the conditions, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void register(final IEventBus modBus) {
        CONDITIONS.register(modBus);
    }
}
