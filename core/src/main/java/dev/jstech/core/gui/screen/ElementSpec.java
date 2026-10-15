/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.screen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One component of a screen's file as it was read: its kind, the fields every component has, those of a component
 * that holds others, its own, and what it holds.
 *
 * @param type     its kind
 * @param common   what every component says
 * @param layout   how it lays out what it holds, when it holds anything
 * @param props    its own fields, read into what its kind declares
 * @param children what it holds
 */
public record ElementSpec(ElementType<?> type, CommonProps common, ContainerProps layout, Object props,
                          List<ElementSpec> children) {

    /** A component as a file writes it, its kind named by {@code "type"}. */
    public static final Codec<ElementSpec> CODEC = Codec.recursive("element", self ->
            ElementTypes.TYPE_CODEC.dispatch("type", ElementSpec::type, type -> type.specCodec(self))
                    .validate(ElementSpec::check));

    public ElementSpec {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(props, "props");
        children = List.copyOf(children);
    }

    /** The props read as what {@code kind} declares them, for the code that draws this kind. */
    @SuppressWarnings("unchecked")
    public <P> P props(final ElementType<P> kind) {
        if (kind != type) {
            throw new IllegalArgumentException("a " + type + " read as a " + kind);
        }
        return (P) props;
    }

    /** This component and every one inside it, parents first. */
    public List<ElementSpec> all() {
        final List<ElementSpec> out = new ArrayList<>();
        collect(this, out);
        return out;
    }

    private static void collect(final ElementSpec spec, final List<ElementSpec> out) {
        out.add(spec);
        for (final ElementSpec child : spec.children()) {
            collect(child, out);
        }
    }

    /* A component that holds nothing written with children is a slip the file's writer should hear of. */
    private static DataResult<ElementSpec> check(final ElementSpec spec) {
        if (!spec.type().holds() && !spec.children().isEmpty()) {
            return DataResult.error(() -> "a " + spec.type() + " holds nothing, and this one has children; put them "
                    + "in a row, a column or a panel", spec);
        }
        return DataResult.success(spec);
    }
}
