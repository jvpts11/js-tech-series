/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.screen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * A kind of component a screen's file may hold, {@code "type": "jscore:button"}: what its own fields are, and whether
 * it holds other components. Declared once with {@link ElementTypes#declare}; what it draws is registered on the
 * client.
 *
 * @param <P> what its own fields are read into
 */
public final class ElementType<P> {

    private final ResourceLocation id;
    private final MapCodec<P> props;
    private final boolean holds;

    ElementType(final ResourceLocation id, final MapCodec<P> props, final boolean holds) {
        this.id = Objects.requireNonNull(id, "id");
        this.props = Objects.requireNonNull(props, "props");
        this.holds = holds;
    }

    public ResourceLocation id() {
        return id;
    }

    /** Its own fields as a file writes them. */
    public MapCodec<P> props() {
        return props;
    }

    /** Whether it holds other components, laid out by its container fields. */
    public boolean holds() {
        return holds;
    }

    @Override
    public String toString() {
        return id.toString();
    }

    /* A component of this kind as a file writes it: the common fields, its own, and those it holds. */
    @SuppressWarnings("unchecked")
    MapCodec<ElementSpec> specCodec(final Codec<ElementSpec> element) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                CommonProps.MAP_CODEC.forGetter(ElementSpec::common),
                ContainerProps.MAP_CODEC.forGetter(ElementSpec::layout),
                props.forGetter(spec -> (P) spec.props()),
                element.listOf().optionalFieldOf("children", List.of()).forGetter(ElementSpec::children)
        ).apply(i, (common, layout, own, children) -> new ElementSpec(this, common, layout, own, children)));
    }
}
