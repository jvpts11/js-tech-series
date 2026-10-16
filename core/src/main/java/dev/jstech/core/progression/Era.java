/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import dev.jstech.core.text.Text;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * An era of hardware, the generation of the computers a player builds: the series' seven, from Vintage to Singularity,
 * and any an add-on brings, after the last or between two. Saved by its id; where it stands in the order is the
 * registry's ({@link Eras}).
 *
 * @param id     the era's id, saved in worlds: {@code jscore:legacy}
 * @param name   its name as a player reads it: "Legacy"
 * @param colour the colour its screens are remembered by, as RGB
 */
public record Era(ResourceLocation id, Text name, int colour) {

    public Era {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
