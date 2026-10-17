/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import dev.jstech.core.text.TextKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * A kind of harm, which worn things resist and hazards are protected from by: radiation, heat, acid, a blast, and the
 * kinds a mod brings. Its damage types are the tag {@code <namespace>:damage_kind/<path>}, so a data pack says which of
 * the game's and the mods' damage is of the kind, and a resistance to the kind takes off that damage.
 *
 * @param id   its id
 * @param name what it is called, as a tooltip and the body's screen name it
 */
public record DamageKind(ResourceLocation id, TextKey name) {

    /** The damage types of the kind. */
    public TagKey<DamageType> types() {
        return TagKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(this.id.getNamespace(), "damage_kind/" + this.id.getPath()));
    }
}
