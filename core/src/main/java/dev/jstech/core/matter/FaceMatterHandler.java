/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.matter;

import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.inventory.NamedSubject;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import java.util.Objects;
import java.util.Optional;

/**
 * A store of matter as one face of a block gives it out: the block's own, filled only through a face that lets things
 * in and only with materials the filter lets pass (by their id), and emptied only through a face that lets things out.
 */
public final class FaceMatterHandler implements IMatterHandler {

    private final IMatterHandler inner;
    private final FaceMode mode;
    private final ItemFilter filter;

    /**
     * @param inner  the block's own store
     * @param mode   what the face lets through
     * @param filter what may go in
     */
    public FaceMatterHandler(final IMatterHandler inner, final FaceMode mode, final ItemFilter filter) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.filter = Objects.requireNonNull(filter, "filter");
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
    public long insert(final MatterStack stack, final boolean simulate) {
        if (!this.mode.takesIn() || !this.filter.allows(new NamedSubject(stack.material().id().toString()))) {
            return 0L;
        }
        return this.inner.insert(stack, simulate);
    }

    @Override
    public long extract(final Material material, final long amount, final boolean simulate) {
        return this.mode.givesOut() ? this.inner.extract(material, amount, simulate) : 0L;
    }

    @Override
    public Optional<MatterStack> extractAny(final long amount, final boolean simulate) {
        return this.mode.givesOut() ? this.inner.extractAny(amount, simulate) : Optional.empty();
    }
}
