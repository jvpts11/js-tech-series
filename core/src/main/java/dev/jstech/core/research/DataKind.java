/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.research;

import dev.jstech.core.text.TextKey;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * A kind of data a discovery asks for: what an instrument gives, as a mass spectrometer gives what a sample is made of.
 * A mod declares its kinds ({@link DataKinds#declare}) and an item for each ({@link IResearchData}).
 *
 * @param id   its id, which a discovery's cost names: {@code myaddon:crystal}
 * @param name its name as a player reads it: "Crystal data"
 */
public record DataKind(ResourceLocation id, TextKey name) {

    public DataKind {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
