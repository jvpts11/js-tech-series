/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import dev.jstech.core.JsCore;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * The harm of air: oxygen breathed at too high a pressure, which poisons slowly, its damage the Core's own,
 * {@code jscore:oxygen_toxicity}, so a pack can change it.
 */
@TextHolder
public final class RoomHazards {

    /** The type of the damage of oxygen poisoning. */
    public static final ResourceKey<DamageType> OXYGEN_TOXICITY = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "oxygen_toxicity"));
    public static final TextKey DIED = TextKey.of("death.attack.jscore.oxygen_toxicity",
            "%1$s breathed too much oxygen");
    public static final TextKey DIED_FIGHTING = TextKey.of("death.attack.jscore.oxygen_toxicity.player",
            "%1$s breathed too much oxygen while fighting %2$s");
    /** How often oxygen poisoning harms, in ticks, and how much. */
    public static final int POISONS_EVERY = 200;
    public static final float POISON_DAMAGE = 1.0F;
    /** How long the nausea of carbon dioxide lasts once breathed, in ticks. */
    public static final int NAUSEA_TICKS = 200;

    private RoomHazards() {
    }

    /** Oxygen poisoning in {@code level}. */
    public static DamageSource oxygen(final Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(OXYGEN_TOXICITY));
    }
}
