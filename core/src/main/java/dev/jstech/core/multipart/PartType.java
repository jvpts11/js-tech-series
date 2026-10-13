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
 * <p>A kind may have a second model for while a part of it is at work ({@link IFacePart#busy}), the same part with
 * its lamps lit, say. Its lamps can blink through an animated texture, which the game runs by itself, so the block's
 * mesh is built again only when the part starts or stops working, never at each blink.
 *
 * @param <P> the parts it makes
 */
public final class PartType<P extends IFacePart> {

    private final Supplier<P> factory;
    private final TextKey name;
    private final ResourceLocation model;
    private final ResourceLocation busyModel;

    /**
     * A kind drawn the same whether its parts are at work or not.
     *
     * @param factory what makes a new part of this kind, with nothing set yet
     * @param name    what a player calls it
     * @param model   the standalone block model it is drawn with, facing north
     */
    public PartType(final Supplier<P> factory, final TextKey name, final ResourceLocation model) {
        this(factory, name, model, model);
    }

    /**
     * @param factory   what makes a new part of this kind, with nothing set yet
     * @param name      what a player calls it
     * @param model     the standalone block model it is drawn with while idle, facing north
     * @param busyModel the standalone block model it is drawn with while at work, facing north
     */
    public PartType(final Supplier<P> factory, final TextKey name, final ResourceLocation model,
                    final ResourceLocation busyModel) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.name = Objects.requireNonNull(name, "name");
        this.model = Objects.requireNonNull(model, "model");
        this.busyModel = Objects.requireNonNull(busyModel, "busyModel");
    }

    /** A new part of this kind. */
    public P create() {
        return this.factory.get();
    }

    /** What a player calls this kind of part. */
    public Text text() {
        return this.name.text();
    }

    /** The standalone block model the part is drawn with while idle, facing north; its item wears it too. */
    public ResourceLocation model() {
        return this.model;
    }

    /** The standalone block model the part is drawn with, facing north, while it is at work or while idle. */
    public ResourceLocation model(final boolean busy) {
        return busy ? this.busyModel : this.model;
    }

    /** Whether the kind has a model of its own for while it is at work. */
    public boolean hasBusyModel() {
        return !this.busyModel.equals(this.model);
    }

    /** The id this kind is registered under, or null when it is not registered. */
    public ResourceLocation id() {
        return CoreParts.REGISTRY.getKey(this);
    }
}
