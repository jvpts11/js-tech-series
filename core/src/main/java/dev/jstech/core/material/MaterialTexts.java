/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The sentences of the catalogue of materials that are no form's, property's or rock's. */
@TextHolder
public final class MaterialTexts {

    /** An isotope named after its element: "Uranium-235". */
    public static final TextKey ISOTOPE = TextKey.of("jscore.material.isotope", "%s-%s");
    /** A solid melted: "Molten Iron". */
    public static final TextKey MOLTEN = TextKey.of("jscore.material.molten", "Molten %s");
    /** A gas made liquid: "Liquid Oxygen". */
    public static final TextKey LIQUEFIED = TextKey.of("jscore.material.liquefied", "Liquid %s");
    /** A bucket of a liquid, as the game names its own: "Gasoline Bucket". */
    public static final TextKey BUCKET = TextKey.of("jscore.material.bucket", "%s Bucket");

    private MaterialTexts() {
    }
}
