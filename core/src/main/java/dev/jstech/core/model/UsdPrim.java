/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jetbrains.annotations.Nullable;

/**
 * A prim of a USD layer as its file, text or binary, writes it: its name, its type, its children, its properties and
 * its metadata. Values are kept plainly: true and false, numbers as doubles, strings, tokens and asset paths as
 * strings, tuples (vectors, quaternions written x, y, z then w, matrices row by row) as arrays of doubles, arrays of
 * those as arrays, and paths as strings. Pure.
 */
final class UsdPrim {

    final String name;
    final String path;
    final List<UsdPrim> children = new ArrayList<>();
    final Map<String, Property> properties = new LinkedHashMap<>();
    final Map<String, Object> metadata = new LinkedHashMap<>();
    String type = "";
    String specifier = "def";

    UsdPrim(final String name, final String path) {
        this.name = name;
        this.path = path;
    }

    /** The child named {@code wanted}, made if it has none. */
    UsdPrim child(final String wanted) {
        for (final UsdPrim child : this.children) {
            if (child.name.equals(wanted)) {
                return child;
            }
        }
        final UsdPrim made = new UsdPrim(wanted, this.path.equals("/") ? "/" + wanted : this.path + "/" + wanted);
        this.children.add(made);
        return made;
    }

    /** The property named {@code wanted}, made if it has none. */
    Property property(final String wanted) {
        return this.properties.computeIfAbsent(wanted, key -> new Property());
    }

    /** The value of the property {@code wanted} at rest: its default, or its first sample; none for none. */
    @Nullable
    Object value(final String wanted) {
        final Property property = this.properties.get(wanted);
        if (property == null) {
            return null;
        }
        if (property.value != null) {
            return property.value;
        }
        return property.samples.isEmpty() ? null : property.samples.firstEntry().getValue();
    }

    /** The prim at {@code wanted}, a path from the layer's root, under this one; none for none. */
    @Nullable
    UsdPrim find(final String wanted) {
        if (wanted.equals(this.path)) {
            return this;
        }
        for (final UsdPrim child : this.children) {
            if (wanted.equals(child.path) || wanted.startsWith(child.path + "/")) {
                return child.find(wanted);
            }
        }
        return null;
    }

    /**
     * A property: its type as written, as {@code point3f[]}; its value at rest and at each time; what it is joined
     * to (a relationship's targets, an attribute's connections); and its metadata, as its interpolation.
     */
    static final class Property {

        final TreeMap<Double, Object> samples = new TreeMap<>();
        final List<String> targets = new ArrayList<>();
        final Map<String, Object> metadata = new LinkedHashMap<>();
        String typeName = "";
        @Nullable
        Object value;
        boolean relationship;
    }

    /** An asset path, as {@code @textures/wood.png@} writes one. */
    record Asset(String path) {
    }
}
