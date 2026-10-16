/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.multipart.PartItem;
import dev.jstech.core.multipart.PartType;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A kind of part declared with {@link ModContent#part}: its kind, the item that mounts it, and how it looks.
 *
 * @param type the kind of part
 * @param item the item that mounts it
 * @param look how it looks
 * @param <P>  the parts it makes
 */
public record PartEntry<P extends IFacePart>(DeferredHolder<PartType<?>, PartType<P>> type, ItemEntry<PartItem> item,
                                             PartLook look) implements Supplier<PartType<P>> {

    public PartEntry {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(look, "look");
    }

    /** The kind of part. */
    @Override
    public PartType<P> get() {
        return this.type.get();
    }

    /** The id it is registered under, for its kind and its item alike. */
    public ResourceLocation id() {
        return this.type.getId();
    }
}
