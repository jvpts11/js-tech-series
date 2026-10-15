/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import java.util.Objects;

/**
 * A material in one form: iron as dust. What an item of a material is.
 *
 * @param material the material
 * @param form     the form it is in
 */
public record MaterialThing(Material material, Form form) {

    public MaterialThing {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(form, "form");
    }

    @Override
    public String toString() {
        return this.material + "/" + this.form;
    }
}
