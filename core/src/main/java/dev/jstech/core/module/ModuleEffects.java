/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.module;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * Every kind of module effect, the Core's own and those mods add: a mod declares its kinds from its constructor, and a
 * module's data names each effect by its kind's id.
 */
public final class ModuleEffects {

    private static final Map<ResourceLocation, ModuleEffectType<?>> BY_ID = new ConcurrentHashMap<>();

    /** How any effect is read and written: its kind's id under {@code type}, then the kind's own fields. */
    public static final Codec<IModuleEffect> CODEC = Codec.lazyInitialized(() -> ResourceLocation.CODEC
            .comapFlatMap(ModuleEffects::find, ModuleEffectType::id)
            .dispatch("type", IModuleEffect::type, ModuleEffectType::codec));

    private ModuleEffects() {
    }

    /**
     * Declares a kind of effect.
     *
     * @throws IllegalStateException when a kind of that id is declared already
     */
    public static <E extends IModuleEffect> ModuleEffectType<E> declare(final ModuleEffectType<E> type) {
        if (BY_ID.putIfAbsent(type.id(), type) != null) {
            throw new IllegalStateException("the module effect " + type.id() + " is declared twice");
        }
        return type;
    }

    private static DataResult<ModuleEffectType<?>> find(final ResourceLocation id) {
        final ModuleEffectType<?> type = BY_ID.get(id);
        return type == null ? DataResult.error(() -> "no module effect is declared as " + id)
                : DataResult.success(type);
    }
}
