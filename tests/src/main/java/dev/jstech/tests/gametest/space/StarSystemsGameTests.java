/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.dimension.DimensionRules;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.space.universe.Galaxy;
import dev.jstech.space.universe.StarSystem;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.tests.JsTests;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Space's universe as the server holds it: twenty-six star systems in three galaxies, each named, the Solar System
 * at the heart of the Milky Way; and a system's space, made the first time someone gets there as a copy of the empty
 * template, vacuum with no gravity, and the same space every time after.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class StarSystemsGameTests {

    private static final String ARENA = "empty";
    /* The systems of each galaxy, as the design counts them. */
    private static final int MILKY_WAY = 18;
    private static final int ANDROMEDA = 5;
    private static final int TRIANGULUM = 3;

    private StarSystemsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void systems_areTwentySixInThreeGalaxiesEachNamed(final GameTestHelper helper) {
        final Map<ResourceLocation, StarSystem> systems = StarSystems.all(helper.getLevel());
        final Map<Galaxy, Integer> counted = new EnumMap<>(Galaxy.class);
        for (final Map.Entry<ResourceLocation, StarSystem> system : systems.entrySet()) {
            counted.merge(system.getValue().galaxy(), 1, Integer::sum);
            helper.assertTrue(Language.getInstance().has(system.getValue().name()),
                    system.getKey() + " has a name in the language file");
            helper.assertTrue(system.getValue().star().isEmpty()
                    || Language.getInstance().has(system.getValue().star()), system.getKey() + "'s star is written");
        }
        helper.assertTrue(counted.equals(Map.of(Galaxy.MILKY_WAY, MILKY_WAY, Galaxy.ANDROMEDA, ANDROMEDA,
                Galaxy.TRIANGULUM, TRIANGULUM)), "eighteen systems in the Milky Way, five in Andromeda and three in"
                + " Triangulum: " + counted);
        final StarSystem solar = StarSystems.get(helper.getLevel(), StarSystems.SOLAR_SYSTEM).orElseThrow();
        helper.assertTrue(solar.galaxy() == Galaxy.MILKY_WAY && solar.lightYears() == 0,
                "the Solar System is in the Milky Way, where the distances are counted from");
        helper.assertTrue(StarSystems.get(helper.getLevel(), StarSystems.ALPHA_CENTAURI).orElseThrow().lightYears()
                < StarSystems.get(helper.getLevel(), StarSystems.BARNARDS_STAR).orElseThrow().lightYears(),
                "Alpha Centauri is the nearest star, nearer than Barnard's");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void space_isMadeOnTheFirstVisitAsVacuumWithNoGravity(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(SystemSpaces.keyOf(StarSystems.SOLAR_SYSTEM).location().toString()
                .equals("jsspace:solar_system_space"), "a system's space is named after it");
        // A system of the test's own, so the first visit is a first visit whatever runs came before.
        final ResourceLocation system = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                "system_" + UUID.randomUUID().toString().substring(0, 8));
        final ResourceKey<Level> key = SystemSpaces.keyOf(system);
        helper.assertTrue(server.getLevel(key) == null, "no one has been there, so it has no space yet");
        final ServerLevel space = SystemSpaces.spaceOf(server, system);
        try {
            helper.assertTrue(space.dimension().equals(key), "the first visit makes its space");
            helper.assertTrue(SystemSpaces.systemOf(key).filter(system::equals).isPresent(),
                    "and the space knows its system");
            helper.assertTrue(space.getBlockState(BlockPos.ZERO).isAir()
                    && space.getBlockState(new BlockPos(0, -64, 0)).isAir(), "it is empty, top to bottom");
            final DimensionRules rules = DimensionRulesData.of(key);
            helper.assertTrue(rules.gravity() == 0 && !rules.breathable() && rules.pressure() == 0,
                    "it is vacuum with no gravity, by the template's rules: " + rules);
            helper.assertTrue(SystemSpaces.spaceOf(server, system) == space, "every visit after finds the same");
        } finally {
            RuntimeDimensions.remove(server, key);
        }
        helper.succeed();
    }
}
