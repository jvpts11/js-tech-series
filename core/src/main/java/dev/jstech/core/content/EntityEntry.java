/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * A declared kind of entity: the entity type itself, reached like any registered one, and what was said about it
 * where it was declared.
 * @param <E> the entity's class
 */
public final class EntityEntry<E extends Entity> extends DeferredHolder<EntityType<?>, EntityType<E>> {

    private final String english;
    private final @Nullable Supplier<AttributeSupplier.Builder> attributes;
    private final boolean holdsEnergy;

    EntityEntry(final ResourceLocation id, final String english,
                final @Nullable Supplier<AttributeSupplier.Builder> attributes, final boolean holdsEnergy) {
        super(ResourceKey.create(Registries.ENTITY_TYPE, id));
        this.english = english;
        this.attributes = attributes;
        this.holdsEnergy = holdsEnergy;
    }

    /** What the entity is called in English. */
    public String english() {
        return english;
    }

    /** The attributes a living entity of this kind starts with, or null for an entity that is not living. */
    public @Nullable Supplier<AttributeSupplier.Builder> attributes() {
        return attributes;
    }

    /** Whether cables and chargers reach the energy it holds. */
    public boolean holdsEnergy() {
        return holdsEnergy;
    }
}
