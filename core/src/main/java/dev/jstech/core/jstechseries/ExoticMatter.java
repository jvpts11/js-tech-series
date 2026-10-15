/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.JsCore;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * The exotic forms of matter, a kind of their own, from real physics, observed or predicted, each in the state it calls
 * for: metallic hydrogen, a solid of the gas giants' cores; neutronium, the matter of a neutron star; the plasma of
 * quarks and gluons; strange matter; dark matter; the Bose-Einstein condensate; matter of negative mass; degenerate
 * matter; time crystals; photonic matter; and computronium, matter arranged to compute. J's Industrial makes them, J's
 * Space finds some in nature, and J's Computers' cosmological simulator makes them from a simulated universe; all
 * trade these same materials. What forms they take is decided with each one.
 */
@TextHolder
public final class ExoticMatter {

    public static final TextKey METALLIC_HYDROGEN_NAME = TextKey.of("jscore.material.metallic_hydrogen",
            "Metallic Hydrogen");
    public static final TextKey NEUTRONIUM_NAME = TextKey.of("jscore.material.neutronium", "Neutronium");
    public static final TextKey QUARK_GLUON_PLASMA_NAME = TextKey.of("jscore.material.quark_gluon_plasma",
            "Quark-Gluon Plasma");
    public static final TextKey STRANGE_MATTER_NAME = TextKey.of("jscore.material.strange_matter", "Strange Matter");
    public static final TextKey DARK_MATTER_NAME = TextKey.of("jscore.material.dark_matter", "Dark Matter");
    public static final TextKey BOSE_EINSTEIN_CONDENSATE_NAME = TextKey.of(
            "jscore.material.bose_einstein_condensate", "Bose-Einstein Condensate");
    public static final TextKey NEGATIVE_MASS_MATTER_NAME = TextKey.of("jscore.material.negative_mass_matter",
            "Negative-Mass Matter");
    public static final TextKey DEGENERATE_MATTER_NAME = TextKey.of("jscore.material.degenerate_matter",
            "Degenerate Matter");
    public static final TextKey TIME_CRYSTAL_NAME = TextKey.of("jscore.material.time_crystal", "Time Crystal");
    public static final TextKey PHOTONIC_MATTER_NAME = TextKey.of("jscore.material.photonic_matter",
            "Photonic Matter");
    public static final TextKey COMPUTRONIUM_NAME = TextKey.of("jscore.material.computronium", "Computronium");

    public static final Material METALLIC_HYDROGEN = exotic("metallic_hydrogen", METALLIC_HYDROGEN_NAME,
            MatterState.SOLID, 0xC9D3DE);
    public static final Material NEUTRONIUM = exotic("neutronium", NEUTRONIUM_NAME, MatterState.EXOTIC, 0x3C4150);
    public static final Material QUARK_GLUON_PLASMA = exotic("quark_gluon_plasma", QUARK_GLUON_PLASMA_NAME,
            MatterState.PLASMA, 0xFF8A3D);
    public static final Material STRANGE_MATTER = exotic("strange_matter", STRANGE_MATTER_NAME, MatterState.EXOTIC,
            0x7A4FD0);
    public static final Material DARK_MATTER = exotic("dark_matter", DARK_MATTER_NAME, MatterState.EXOTIC, 0x1A1426);
    public static final Material BOSE_EINSTEIN_CONDENSATE = exotic("bose_einstein_condensate",
            BOSE_EINSTEIN_CONDENSATE_NAME, MatterState.EXOTIC, 0x9FE3FF);
    public static final Material NEGATIVE_MASS_MATTER = exotic("negative_mass_matter", NEGATIVE_MASS_MATTER_NAME,
            MatterState.EXOTIC, 0x2DE0C0);
    public static final Material DEGENERATE_MATTER = exotic("degenerate_matter", DEGENERATE_MATTER_NAME,
            MatterState.EXOTIC, 0xEDEFF5);
    public static final Material TIME_CRYSTAL = exotic("time_crystal", TIME_CRYSTAL_NAME, MatterState.SOLID,
            0xB8F0E8);
    public static final Material PHOTONIC_MATTER = exotic("photonic_matter", PHOTONIC_MATTER_NAME,
            MatterState.EXOTIC, 0xFFF6C2);
    public static final Material COMPUTRONIUM = exotic("computronium", COMPUTRONIUM_NAME, MatterState.SOLID,
            0x39D6C4);

    private ExoticMatter() {
    }

    /** Declares them, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every exotic form of matter of the catalogue, in the order above. */
    public static List<Material> all() {
        return List.of(METALLIC_HYDROGEN, NEUTRONIUM, QUARK_GLUON_PLASMA, STRANGE_MATTER, DARK_MATTER,
                BOSE_EINSTEIN_CONDENSATE, NEGATIVE_MASS_MATTER, DEGENERATE_MATTER, TIME_CRYSTAL, PHOTONIC_MATTER,
                COMPUTRONIUM);
    }

    private static Material exotic(final String path, final TextKey name, final MatterState state, final int colour) {
        return Materials.material(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path), MaterialKind.EXOTIC,
                name).at(state).colour(colour).declare();
    }
}
