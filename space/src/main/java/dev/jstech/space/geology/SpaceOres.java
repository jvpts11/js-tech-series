/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.geology;

import dev.jstech.core.jstechseries.SeriesMinerals;
import dev.jstech.core.material.HostRock;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * The ores of the Moon and of Mars, in the rocks of those worlds, each drawn by J's Core with the specks of one of the
 * game's ores on its rock. The Moon's maria hold ilmenite, its titanium and its oxygen, in their basalt and in their
 * regolith, with armalcolite, tranquillityite and the merrillite that carries the rare earths of KREEP; its highlands
 * hold the native iron of meteorites. Mars holds hematite in its red regolith and in its lakebeds' mudstone, magnetite
 * and olivine in its basalt, and jarosite and gypsum, the proofs of its old water, in its mudstone.
 *
 * <p>Those rocks are host rocks of J's Core's registry, so any mod can lay its own ores in them.
 */
@TextHolder
public final class SpaceOres {

    public static final TextKey MARE_BASALT_ORE = TextKey.of("jsspace.host_rock.mare_basalt", "Mare Basalt %s Ore");
    public static final TextKey MARE_REGOLITH_ORE = TextKey.of("jsspace.host_rock.mare_regolith",
            "Mare Regolith %s Ore");
    public static final TextKey HIGHLAND_REGOLITH_ORE = TextKey.of("jsspace.host_rock.highland_regolith",
            "Highland Regolith %s Ore");
    public static final TextKey MARTIAN_REGOLITH_ORE = TextKey.of("jsspace.host_rock.martian_regolith",
            "Martian Regolith %s Ore");
    public static final TextKey MARTIAN_BASALT_ORE = TextKey.of("jsspace.host_rock.martian_basalt",
            "Martian Basalt %s Ore");
    public static final TextKey MUDSTONE_ORE = TextKey.of("jsspace.host_rock.mudstone", "Mudstone %s Ore");

    /** The Moon's dark basalt, of the maria. */
    public static final HostRock MARE_BASALT = rock("mare_basalt", SpaceRocks.MARE_BASALT, MARE_BASALT_ORE);
    /** The maria's regolith, richest in ilmenite. */
    public static final HostRock MARE_REGOLITH = rock("mare_regolith", SpaceRocks.MARE_REGOLITH, MARE_REGOLITH_ORE);
    /** The highlands' regolith, where the iron of meteorites lies. */
    public static final HostRock HIGHLAND_REGOLITH = rock("highland_regolith", SpaceRocks.HIGHLAND_REGOLITH,
            HIGHLAND_REGOLITH_ORE);
    /** Mars' red regolith. */
    public static final HostRock MARTIAN_REGOLITH = rock("martian_regolith", SpaceRocks.MARTIAN_REGOLITH,
            MARTIAN_REGOLITH_ORE);
    /** Mars' faceted basalt, the rock below its ground. */
    public static final HostRock MARTIAN_BASALT = rock("martian_basalt", SpaceRocks.MARTIAN_BASALT,
            MARTIAN_BASALT_ORE);
    /** The grey mudstone of Gale's lakebeds. */
    public static final HostRock MUDSTONE = rock("mudstone", SpaceRocks.MUDSTONE, MUDSTONE_ORE);

    private SpaceOres() {
    }

    /** Declares the rocks and the ores, once, from the mod's constructor, after the ground. */
    public static void declare() {
        // The Moon.
        ore(SeriesMinerals.ILMENITE, MARE_BASALT, MARE_REGOLITH);
        ore(SeriesMinerals.ARMALCOLITE, MARE_BASALT);
        ore(SeriesMinerals.TRANQUILLITYITE, MARE_BASALT);
        ore(SeriesMinerals.MERRILLITE, MARE_BASALT);
        ore(SeriesMinerals.NATIVE_IRON, HIGHLAND_REGOLITH);
        // Mars.
        ore(SeriesMinerals.HEMATITE, MARTIAN_REGOLITH, MUDSTONE);
        ore(SeriesMinerals.MAGNETITE, MARTIAN_BASALT);
        ore(SeriesMinerals.OLIVINE, MARTIAN_BASALT);
        ore(SeriesMinerals.JAROSITE, MUDSTONE);
        ore(SeriesMinerals.GYPSUM, MUDSTONE);
    }

    private static HostRock rock(final String path, final Supplier<? extends Block> block, final TextKey oreName) {
        return HostRocks.rock(ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path), block, oreName).declare();
    }

    private static void ore(final Material mineral, final HostRock... rocks) {
        MaterialUses.ore(JsSpace.MODID, mineral, rocks);
    }
}
