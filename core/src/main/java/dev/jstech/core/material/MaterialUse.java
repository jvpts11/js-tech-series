/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * One mod's use of one material in one form, and, for an ore, in one rock. A use the mod shares is one other mods may
 * trade: its thing shows in the creative tab of shared content, not the mod's own.
 *
 * @param modid    the mod that uses it
 * @param material the material
 * @param form     the form
 * @param rock     the rock, for an ore; null otherwise
 * @param shared   whether it is shared content
 */
public record MaterialUse(String modid, Material material, Form form, @Nullable HostRock rock, boolean shared) {

    public MaterialUse {
        Objects.requireNonNull(modid, "modid");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(form, "form");
    }

    /** The rock, for an ore. */
    public Optional<HostRock> inRock() {
        return Optional.ofNullable(this.rock);
    }

    /** The material in its form. */
    public MaterialThing thing() {
        return new MaterialThing(this.material, this.form);
    }
}
