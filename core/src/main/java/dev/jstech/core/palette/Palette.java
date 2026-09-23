/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.palette;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * One declared palette: the colours it is declared with in the code, and the colours it has now, which a resource
 * pack may have changed. Whatever paints with it reads {@link #get()} each time and never keeps a copy, so a change
 * of pack reaches every screen at once.
 *
 * <p>Made only by {@link Palettes#declare}, which is what lets the palette be found to be written out and read back.
 *
 * @param <P> the record the palette is, one colour to a component
 */
public final class Palette<P extends Record> {

    private final ResourceLocation id;
    private final P declared;
    private volatile P current;

    Palette(final ResourceLocation id, final P declared) {
        this.id = id;
        this.declared = declared;
        this.current = declared;
    }

    /** The colours to paint with now. */
    public P get() {
        return this.current;
    }

    /** The colours the code declares, which is what the palette's file is written from. */
    public P declared() {
        return this.declared;
    }

    public ResourceLocation id() {
        return this.id;
    }

    /** Where a resource pack keeps this palette: {@code assets/<namespace>/palettes/<path>.json}. */
    public ResourceLocation file() {
        return ResourceLocation.fromNamespaceAndPath(this.id.getNamespace(), "palettes/" + this.id.getPath() + ".json");
    }

    /** Takes the colours a pack gives, over the declared ones; a role it does not name keeps its declared colour. */
    void load(final Map<String, Integer> overrides) {
        this.current = PaletteRoles.with(this.declared, overrides);
    }

    /** Goes back to the declared colours, which is what a palette no pack speaks for has. */
    void reset() {
        this.current = this.declared;
    }
}
