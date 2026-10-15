/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

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
 * The damage of electricity, the Core's own: what a live bare wire does to whoever touches it, which no armour stops.
 * Its type is data, {@code jscore:electric}, so a pack can change it.
 */
@TextHolder
public final class EnergyDamage {

    /** The type of electric damage. */
    public static final ResourceKey<DamageType> ELECTRIC = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "electric"));

    public static final TextKey DIED = TextKey.of("death.attack.jscore.electric", "%1$s was electrocuted");
    public static final TextKey DIED_FIGHTING = TextKey.of("death.attack.jscore.electric.player",
            "%1$s was electrocuted while fighting %2$s");

    private EnergyDamage() {
    }

    /** Electric damage in {@code level}. */
    public static DamageSource electric(final Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ELECTRIC));
    }
}
