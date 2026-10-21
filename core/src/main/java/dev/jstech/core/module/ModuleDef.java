/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.module;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/**
 * A module, as a data pack or a mod declares it for an item: the kind of socket it goes in, the energy it spends while
 * it is on, and what it does for its wearer.
 *
 * @param kind    the kind of socket it fits, as its host names it: {@code visor}, {@code plate}, {@code core}
 * @param energy  what it spends while on, in FE a second; 0 for one that spends nothing
 * @param effects what it does
 */
public record ModuleDef(String kind, int energy, List<IModuleEffect> effects) {

    public static final Codec<ModuleDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("kind").forGetter(ModuleDef::kind),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("energy", 0).forGetter(ModuleDef::energy),
            ModuleEffects.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(ModuleDef::effects))
            .apply(instance, ModuleDef::new));

    public ModuleDef {
        effects = List.copyOf(effects);
    }
}
