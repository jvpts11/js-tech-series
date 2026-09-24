/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** The ambient fields the mods of the series and their addons declare. */
public final class AmbientFields {

    private static final Map<ResourceLocation, AmbientField> FIELDS = new LinkedHashMap<>();

    private AmbientFields() {
    }

    /** Adds a field; the same id twice is refused, so two mods cannot quietly make one place. */
    public static synchronized AmbientField register(final AmbientField field) {
        if (FIELDS.putIfAbsent(field.id(), field) != null) {
            throw new IllegalStateException("the ambient field " + field.id() + " is declared twice");
        }
        return field;
    }

    @Nullable
    public static synchronized AmbientField find(final ResourceLocation id) {
        return FIELDS.get(id);
    }

    public static synchronized Collection<AmbientField> all() {
        return Collections.unmodifiableCollection(FIELDS.values());
    }
}
