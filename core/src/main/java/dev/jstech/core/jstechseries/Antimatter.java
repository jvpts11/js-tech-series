/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.MaterialProperty;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Antimatter in the series' catalogue: antihydrogen, the antimatter J's Industrial's factory cools and traps and J's
 * Space gathers from the antiproton belts, and the mark that says a material is antimatter, which only the Antimatter
 * Trap keeps. A data pack can mark another material, as it can set any material's properties.
 */
@TextHolder
public final class Antimatter {

    public static final TextKey ANTIMATTER_NAME = TextKey.of("jscore.material_property.antimatter", "Antimatter");
    public static final TextKey ANTIHYDROGEN_NAME = TextKey.of("jscore.material.antihydrogen", "Antihydrogen");

    /** Whether a material is antimatter, which annihilates with matter and is kept only in a Penning trap. */
    public static final MaterialProperty<Boolean> ANTIMATTER = MaterialProperties.property(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "antimatter"), Codec.BOOL, ANTIMATTER_NAME)
            .shown(MaterialProperty.Shown.NEVER).declare();
    /** Antihydrogen: an antiproton and a positron. */
    public static final Material ANTIHYDROGEN = Materials.material(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "antihydrogen"), MaterialKind.EXOTIC,
                    ANTIHYDROGEN_NAME)
            .at(MatterState.EXOTIC).colour(0xFF8ADF).with(ANTIMATTER, true).declare();

    private Antimatter() {
    }

    /** Declares it, from the Core's constructor, with the rest of the catalogue. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Whether {@code material} is antimatter. */
    public static boolean is(final Material material) {
        return material.value(ANTIMATTER).orElse(false);
    }
}
