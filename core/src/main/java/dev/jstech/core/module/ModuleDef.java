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
 * it is on, what it does for its wearer, how many of it one host takes, and what a host must be to take it.
 *
 * <pre>{@code
 * {"kind": "armour", "energy": 20, "most": 3, "needs": ["feet"], "effects": [ ... ]}
 * {"kind": "armour", "needs": ["sealed"], "effects": [ ... ]}   // a space module: only a sealed suit takes it
 * }</pre>
 *
 * @param kind    the kind of socket it fits, as its host names it: {@code visor}, {@code plate}, {@code core}
 * @param energy  what it spends while on, in FE: a second in a worn host, a use in a tool; 0 for one that spends
 *                nothing
 * @param effects what it does
 * @param most    how many of it one host takes, 0 for as many as it has sockets of its kind
 * @param needs   the traits a host must have to take it ({@link ModuleHostDef#traits}): the piece it goes on, a seal
 */
public record ModuleDef(String kind, int energy, List<IModuleEffect> effects, int most, List<String> needs) {

    public static final Codec<ModuleDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("kind").forGetter(ModuleDef::kind),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("energy", 0).forGetter(ModuleDef::energy),
            ModuleEffects.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(ModuleDef::effects),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("most", 0).forGetter(ModuleDef::most),
            Codec.STRING.listOf().optionalFieldOf("needs", List.of()).forGetter(ModuleDef::needs))
            .apply(instance, ModuleDef::new));

    public ModuleDef {
        effects = List.copyOf(effects);
        needs = List.copyOf(needs);
    }

    /** A module that any host with a socket of its kind takes, as many as it has. */
    public ModuleDef(final String kind, final int energy, final List<IModuleEffect> effects) {
        this(kind, energy, effects, 0, List.of());
    }

    /** The same, of which one host takes at most {@code count}. */
    public ModuleDef upTo(final int count) {
        return new ModuleDef(this.kind, this.energy, this.effects, count, this.needs);
    }

    /** The same, which only a host with every one of {@code traits} takes. */
    public ModuleDef needing(final String... traits) {
        return new ModuleDef(this.kind, this.energy, this.effects, this.most, List.of(traits));
    }
}
