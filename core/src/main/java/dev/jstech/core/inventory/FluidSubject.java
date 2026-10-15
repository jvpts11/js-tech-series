/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inventory;

import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A fluid of the game as an {@link ItemFilter} looks at it: by its id, its tags and, for an exact rule, its components.
 *
 * @param fluid the fluid; its amount does not matter
 */
public record FluidSubject(FluidStack fluid) implements IFilterSubject {

    public FluidSubject {
        Objects.requireNonNull(fluid, "fluid");
    }

    /** The fluid {@code fluid} to filter. */
    public static FluidSubject of(final FluidStack fluid) {
        return new FluidSubject(fluid);
    }

    /* FluidStack has identity equality, so two subjects of equal fluids compare by fluid and components instead. */
    @Override
    public boolean equals(final Object other) {
        return other instanceof FluidSubject subject && FluidStack.isSameFluidSameComponents(this.fluid, subject.fluid);
    }

    @Override
    public int hashCode() {
        return FluidStack.hashFluidAndComponents(this.fluid);
    }

    @Override
    public String itemId() {
        return BuiltInRegistries.FLUID.getKey(this.fluid.getFluid()).toString();
    }

    @Override
    public boolean hasTag(final String tag) {
        final ResourceLocation id = ResourceLocation.tryParse(tag);
        return id != null && this.fluid.is(TagKey.create(Registries.FLUID, id));
    }

    @Override
    public boolean sameAs(final IFilterSubject other) {
        return other instanceof FluidSubject subject && FluidStack.isSameFluidSameComponents(this.fluid, subject.fluid);
    }
}
