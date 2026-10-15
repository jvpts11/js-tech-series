/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * The chemistry a mineral belongs to, which decides how it is processed: a sulfide is roasted, an oxide reduced, a
 * carbonate calcined. Declared once, with {@link MineralFamilies}.
 *
 * @param id   the family's id
 * @param name what it is called: "Sulfide"
 */
public record MineralFamily(ResourceLocation id, TextKey name) {

    public MineralFamily {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }

    /** What it is called. */
    public Text displayName() {
        return this.name.text();
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
