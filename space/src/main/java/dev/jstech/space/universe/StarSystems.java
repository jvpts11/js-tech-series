/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import dev.jstech.core.data.DeclaredData;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The star systems of the universe: twenty-six in three galaxies, eighteen in the Milky Way, real stars at their real
 * distances, five in Andromeda and three in Triangulum. They are declared here and written to the files a data pack
 * writes over, {@code data/<namespace>/jsspace/star_systems/}, so a pack changes one or adds its own with no code.
 */
public final class StarSystems {

    /* The files a data pack writes, over the systems declared here. */
    private static final DeclaredData<StarSystem> SYSTEMS = new DeclaredData<>(id("star_systems"),
            "jsspace/star_systems", StarSystem.CODEC);
    /* Andromeda and Triangulum, from the Sun. */
    private static final double ANDROMEDA = 2_500_000;
    private static final double TRIANGULUM = 2_700_000;

    public static final ResourceLocation SOLAR_SYSTEM = declare("solar_system", UniverseTexts.SOLAR_SYSTEM,
            Galaxy.MILKY_WAY, 0, UniverseTexts.SOLAR_SYSTEM_STAR);
    public static final ResourceLocation BARNARDS_STAR = declare("barnards_star", UniverseTexts.BARNARDS_STAR,
            Galaxy.MILKY_WAY, 5.96, UniverseTexts.BARNARDS_STAR_STAR);
    public static final ResourceLocation ALPHA_CENTAURI = declare("alpha_centauri", UniverseTexts.ALPHA_CENTAURI,
            Galaxy.MILKY_WAY, 4.37, UniverseTexts.ALPHA_CENTAURI_STAR);
    public static final ResourceLocation EPSILON_ERIDANI = declare("epsilon_eridani", UniverseTexts.EPSILON_ERIDANI,
            Galaxy.MILKY_WAY, 10.5, UniverseTexts.EPSILON_ERIDANI_STAR);
    public static final ResourceLocation TAU_CETI = declare("tau_ceti", UniverseTexts.TAU_CETI, Galaxy.MILKY_WAY,
            11.9, UniverseTexts.TAU_CETI_STAR);
    public static final ResourceLocation GLIESE_667C = declare("gliese_667c", UniverseTexts.GLIESE_667C,
            Galaxy.MILKY_WAY, 23.6, UniverseTexts.GLIESE_667C_STAR);
    public static final ResourceLocation TRAPPIST_1 = declare("trappist_1", UniverseTexts.TRAPPIST_1,
            Galaxy.MILKY_WAY, 40.7, UniverseTexts.TRAPPIST_1_STAR);
    public static final ResourceLocation EIGHTY_TWO_ERIDANI = declare("82_eridani", UniverseTexts.EIGHTY_TWO_ERIDANI,
            Galaxy.MILKY_WAY, 19.7, UniverseTexts.EIGHTY_TWO_ERIDANI_STAR);
    public static final ResourceLocation DELTA_PAVONIS = declare("delta_pavonis", UniverseTexts.DELTA_PAVONIS,
            Galaxy.MILKY_WAY, 19.9, UniverseTexts.DELTA_PAVONIS_STAR);
    public static final ResourceLocation LICH = declare("lich", UniverseTexts.LICH, Galaxy.MILKY_WAY, 2_300,
            UniverseTexts.LICH_STAR);
    public static final ResourceLocation GALACTIC_CORE = declare("galactic_core", UniverseTexts.GALACTIC_CORE,
            Galaxy.MILKY_WAY, 26_000, UniverseTexts.GALACTIC_CORE_STAR);
    public static final ResourceLocation KEPLER_442 = declare("kepler_442", UniverseTexts.KEPLER_442,
            Galaxy.MILKY_WAY, 1_200, UniverseTexts.KEPLER_442_STAR);
    public static final ResourceLocation LUHMAN_16 = declare("luhman_16", UniverseTexts.LUHMAN_16, Galaxy.MILKY_WAY,
            6.5, UniverseTexts.LUHMAN_16_STAR);
    public static final ResourceLocation SIRIUS = declare("sirius", UniverseTexts.SIRIUS, Galaxy.MILKY_WAY, 8.6,
            UniverseTexts.SIRIUS_STAR);
    public static final ResourceLocation SIXTY_ONE_CYGNI = declare("61_cygni", UniverseTexts.SIXTY_ONE_CYGNI,
            Galaxy.MILKY_WAY, 11.4, UniverseTexts.SIXTY_ONE_CYGNI_STAR);
    public static final ResourceLocation EPSILON_INDI = declare("epsilon_indi", UniverseTexts.EPSILON_INDI,
            Galaxy.MILKY_WAY, 11.9, UniverseTexts.EPSILON_INDI_STAR);
    public static final ResourceLocation LHS_1140 = declare("lhs_1140", UniverseTexts.LHS_1140, Galaxy.MILKY_WAY, 49,
            UniverseTexts.LHS_1140_STAR);
    public static final ResourceLocation CYGNUS_X_1 = declare("cygnus_x_1", UniverseTexts.CYGNUS_X_1,
            Galaxy.MILKY_WAY, 7_200, UniverseTexts.CYGNUS_X_1_STAR);
    public static final ResourceLocation ANDROMEDA_PRIME = declare("andromeda_prime", UniverseTexts.ANDROMEDA_PRIME,
            Galaxy.ANDROMEDA, ANDROMEDA, null);
    public static final ResourceLocation ASHVAL = declare("ashval", UniverseTexts.ASHVAL, Galaxy.ANDROMEDA, ANDROMEDA,
            null);
    public static final ResourceLocation VERATH = declare("verath", UniverseTexts.VERATH, Galaxy.ANDROMEDA, ANDROMEDA,
            UniverseTexts.VERATH_STAR);
    public static final ResourceLocation CINDARA = declare("cindara", UniverseTexts.CINDARA, Galaxy.ANDROMEDA,
            ANDROMEDA, UniverseTexts.CINDARA_STAR);
    public static final ResourceLocation NEXARA = declare("nexara", UniverseTexts.NEXARA, Galaxy.ANDROMEDA, ANDROMEDA,
            UniverseTexts.NEXARA_STAR);
    public static final ResourceLocation VORNA = declare("vorna", UniverseTexts.VORNA, Galaxy.TRIANGULUM, TRIANGULUM,
            UniverseTexts.VORNA_STAR);
    public static final ResourceLocation ITHEL = declare("ithel", UniverseTexts.ITHEL, Galaxy.TRIANGULUM, TRIANGULUM,
            UniverseTexts.ITHEL_STAR);
    public static final ResourceLocation SOLUM_CADENS = declare("solum_cadens", UniverseTexts.SOLUM_CADENS,
            Galaxy.TRIANGULUM, TRIANGULUM, UniverseTexts.SOLUM_CADENS_STAR);

    private StarSystems() {
    }

    /** Declares the systems, before the game has loaded. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The system of {@code id} on the side {@code level} is on, or none. */
    public static Optional<StarSystem> get(final Level level, final ResourceLocation id) {
        return SYSTEMS.get(level, id);
    }

    /** Every system on the side {@code level} is on, by id: a data pack's over the declared. */
    public static Map<ResourceLocation, StarSystem> all(final Level level) {
        return SYSTEMS.all(level);
    }

    /** Every system declared in code, by id, for the data generation to write. */
    public static Map<ResourceLocation, StarSystem> declared() {
        return SYSTEMS.declared();
    }

    /* A system, {@code star} null where nothing is said of what shines at its heart. */
    private static ResourceLocation declare(final String path, final TextKey name, final Galaxy galaxy,
                                            final double lightYears, @Nullable final TextKey star) {
        final ResourceLocation id = id(path);
        SYSTEMS.declare(id, new StarSystem(name.key(), galaxy, lightYears, star == null ? "" : star.key()));
        return id;
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
