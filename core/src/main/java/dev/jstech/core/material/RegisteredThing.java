/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A thing the Core registered for a material because a mod uses it: an item, a block of the material, or its ore in
 * one rock. What its art, its tags and its drops are made for.
 *
 * @param thing the material in its form
 * @param rock  the rock, for an ore; null otherwise
 * @param id    the id it is registered under
 */
public record RegisteredThing(MaterialThing thing, @Nullable HostRock rock, ResourceLocation id) {

    public RegisteredThing {
        Objects.requireNonNull(thing, "thing");
        Objects.requireNonNull(id, "id");
    }

    /** The rock, for an ore. */
    public Optional<HostRock> inRock() {
        return Optional.ofNullable(this.rock);
    }

    /** Whether it is placed in the world: a block of the material or an ore. */
    public boolean isBlock() {
        return this.thing.form().shape() != Form.Shape.ITEM;
    }
}
