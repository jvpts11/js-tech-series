/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

/**
 * A kind of part that mounts on a face of a block: what makes a new one, what a player calls it, and the model it is
 * drawn with. Registered in {@link CoreParts#REGISTRY} like any other registry entry, by a mod's own deferred
 * register, so a save names a part by its registry id and the players' games agree on which kind each id is.
 *
 * @param <P> the parts it makes
 */
public final class PartType<P extends IFacePart> {

    private final Supplier<P> factory;
    private final TextKey name;
    private final ResourceLocation model;

    /**
     * @param factory what makes a new part of this kind, with nothing set yet
     * @param name    what a player calls it
     * @param model   the standalone block model it is drawn with, facing north
     */
    public PartType(final Supplier<P> factory, final TextKey name, final ResourceLocation model) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.name = Objects.requireNonNull(name, "name");
        this.model = Objects.requireNonNull(model, "model");
    }

    /** A new part of this kind. */
    public P create() {
        return this.factory.get();
    }

    /** What a player calls this kind of part. */
    public Text text() {
        return this.name.text();
    }

    /** The standalone block model the part is drawn with, facing north. */
    public ResourceLocation model() {
        return this.model;
    }

    /** The id this kind is registered under, or null when it is not registered. */
    public ResourceLocation id() {
        return CoreParts.REGISTRY.getKey(this);
    }
}
