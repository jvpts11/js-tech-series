/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.text.Text;
import java.util.Optional;

/**
 * What a material is called now, when that can change as a world goes on: an element beyond the known table shows its
 * systematic name until it is discovered, then the name its discoverer gave it. Asked each time the name is read, on
 * either side; whatever it does not say, the material's declaration does.
 */
public interface IMaterialNamer {

    /** Its name now, or none for the name it was declared with. */
    Optional<Text> name(Material material);

    /** Its symbol now, or none for the one it was declared with. */
    Optional<String> symbol(Material material);
}
