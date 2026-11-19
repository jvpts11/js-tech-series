/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.matter;

import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.multiblock.PortProducts;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * A store of matter that lets its own materials alone through, filled only with them and emptied only of them: a
 * machine's tanks of gas or plasma as one of its ports bound to those products gives them out (see
 * {@link PortProducts}).
 */
@ApiStatus.Experimental
public final class ProductMatterHandler implements IMatterHandler {

    private final IMatterHandler inner;
    private final Set<ResourceLocation> materials;

    /**
     * @param inner     the machine's own tanks of a state of matter
     * @param materials the ids of the materials it lets through
     */
    public ProductMatterHandler(final IMatterHandler inner, final Set<ResourceLocation> materials) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.materials = Set.copyOf(materials);
    }

    @Override
    public MatterState state() {
        return this.inner.state();
    }

    @Override
    public int tanks() {
        return this.inner.tanks();
    }

    @Override
    public Optional<MatterStack> stack(final int tank) {
        return this.inner.stack(tank);
    }

    @Override
    public long capacity(final int tank) {
        return this.inner.capacity(tank);
    }

    @Override
    public double pressureNow(final int tank) {
        return this.inner.pressureNow(tank);
    }

    @Override
    public long insert(final MatterStack stack, final boolean simulate) {
        return this.materials.contains(stack.material().id()) ? this.inner.insert(stack, simulate) : 0L;
    }

    @Override
    public long extract(final Material material, final long amount, final boolean simulate) {
        return this.materials.contains(material.id()) ? this.inner.extract(material, amount, simulate) : 0L;
    }

    /** Empties one of its own materials from the first tank that holds one, up to {@code amount}; nothing else. */
    @Override
    public Optional<MatterStack> extractAny(final long amount, final boolean simulate) {
        for (int tank = 0; tank < this.inner.tanks(); tank++) {
            final Optional<MatterStack> held = this.inner.stack(tank);
            if (held.isPresent() && this.materials.contains(held.get().material().id())) {
                final Material own = held.get().material();
                final long taken = this.inner.extract(own, amount, simulate);
                return taken > 0 ? Optional.of(new MatterStack(own, taken)) : Optional.empty();
            }
        }
        return Optional.empty();
    }
}
