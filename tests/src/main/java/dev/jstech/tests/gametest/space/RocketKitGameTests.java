/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.moving.physics.BlockPhysics;
import dev.jstech.core.room.Seal;
import dev.jstech.core.room.Seals;
import dev.jstech.space.api.RocketKit;
import dev.jstech.space.api.RocketKits;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.JsTests;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The rocket parts' block kits: every part of the skeleton built of its kit, as wide as its diameter and as tall as it
 * stands, every block of the kits used and every kit's block one of them; the counts the approved kits hold; a tank
 * built as tall as it is wanted, on its skirt over an engine; the crews' pressure shell holding against the vacuum, and
 * every block weighing its share of its parts.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RocketKitGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_rocket_kits";

    private RocketKitGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void everyPart_isBuiltOfItsKitAsWideAndAsTallAsItStands(final GameTestHelper helper) {
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(helper.getLevel());
        final Map<ResourceLocation, RocketKit> kits = RocketKits.all(helper.getLevel());
        final Set<ResourceLocation> blocks = new HashSet<>();
        for (final BlockEntry<Block> block : RocketKitBlocks.all()) {
            blocks.add(block.getId());
        }
        final Set<ResourceLocation> used = new HashSet<>();
        for (final Map.Entry<ResourceLocation, RocketPart> part : parts.entrySet()) {
            final RocketKit kit = kits.get(part.getKey());
            helper.assertTrue(kit != null, "a kit for " + part.getKey());
            helper.assertTrue(kit.width() == part.getValue().diameter(), part.getKey() + " as wide as it is");
            final int layers = kit.least();
            final int tall = kit.built(layers, false).orElseThrow().size();
            helper.assertTrue(tall == part.getValue().height() * (part.getValue().stretches() ? layers : 1),
                    part.getKey() + " as tall as it stands: " + tall);
            helper.assertTrue(blocks.containsAll(kit.key().values()), part.getKey() + "'s blocks are the kits'");
            used.addAll(kit.key().values());
        }
        helper.assertTrue(used.equals(blocks), "every block of the kits is in a kit");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void kits_holdTheApprovedCounts(final GameTestHelper helper) {
        final Map<ResourceLocation, RocketKit> kits = RocketKits.all(helper.getLevel());
        final Map<ResourceLocation, Integer> capsule3 = counts(kits, "capsule_3");
        helper.assertTrue(total(capsule3) == 27 && capsule3.size() == 9, "the capsule of three: 27 of 9 kinds");
        helper.assertTrue(count(capsule3, RocketKitBlocks.HEAT_SHIELD) == 9
                && count(capsule3, RocketKitBlocks.CAPSULE_HULL) == 3
                && count(capsule3, RocketKitBlocks.CAPSULE_WINDOW) == 3
                && count(capsule3, RocketKitBlocks.THRUSTER_QUAD) == 4
                && count(capsule3, RocketKitBlocks.PARACHUTE_BAY) == 4
                && count(capsule3, RocketKitBlocks.FLIGHT_COMPUTER) == 1, "its heat shield, hull, windows, head");
        final Map<ResourceLocation, Integer> capsule5 = counts(kits, "capsule_5");
        helper.assertTrue(total(capsule5) == 49 && count(capsule5, RocketKitBlocks.HEAT_SHIELD) == 21
                && count(capsule5, RocketKitBlocks.CAPSULE_HULL) == 11
                && count(capsule5, RocketKitBlocks.CREW_SEAT) == 3, "the capsule of five: 49, three seats");
        helper.assertTrue(total(counts(kits, "instrument_ring_3")) == 9
                && total(counts(kits, "instrument_ring_5")) == 17, "the instrument rings: 9 and 17");
        helper.assertTrue(total(counts(kits, "separation_ring_3")) == 8
                && total(counts(kits, "separation_ring_5")) == 16, "the separation rings: 8 and 16");
        final Map<ResourceLocation, Integer> legs5 = counts(kits, "landing_legs_5");
        helper.assertTrue(count(legs5, RocketKitBlocks.LANDING_LEG) == 4
                && count(legs5, RocketKitBlocks.LEG_ACTUATOR) == 8
                && count(legs5, RocketKitBlocks.LEG_MOUNT) == 4, "the legs of five");
        final Set<ResourceLocation> lander = new HashSet<>(counts(kits, "lunar_lander_descent").keySet());
        lander.addAll(counts(kits, "lunar_lander_ascent").keySet());
        helper.assertTrue(lander.size() == 15, "the lander: 15 kinds, " + lander.size());
        helper.assertTrue(total(counts(kits, "lunar_lander_descent")) + total(counts(kits,
                "lunar_lander_ascent")) == 59, "and 59 blocks");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void tank_isBuiltAsTallAsItIsWantedOnItsSkirtOverAnEngine(final GameTestHelper helper) {
        final RocketKit tank = RocketKits.get(helper.getLevel(), SkeletonRocketParts.id("tank_3")).orElseThrow();
        helper.assertTrue(tank.least() == 1, "a tank may be one layer");
        final Map<ResourceLocation, Integer> alone = tank.counts(1, false);
        helper.assertTrue(count(alone, RocketKitBlocks.FILL_PORT) == 1 && count(alone, RocketKitBlocks.VENT_VALVE) == 1,
                "one layer holds its fill port and its vent valve");
        final List<List<String>> tall = tank.built(18, false).orElseThrow();
        final Map<ResourceLocation, Integer> eighteen = tank.counts(18, false);
        helper.assertTrue(tall.size() == 18 && count(eighteen, RocketKitBlocks.TANK_DOME) == 2
                && count(eighteen, RocketKitBlocks.CABLE_RACEWAY) == 18, "eighteen layers, a dome at each end");
        final Map<ResourceLocation, Integer> first = tank.counts(18, true);
        helper.assertTrue(tank.built(18, true).orElseThrow().size() == 18
                && count(first, RocketKitBlocks.TANK_SKIRT) == 8, "over an engine it stands on its skirt");
        helper.assertTrue(count(tank.counts(1, true), RocketKitBlocks.TANK_SKIRT) == 0,
                "a single layer has no room for one");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void shell_holdsACrewsAirAndEveryBlockWeighsItsShare(final GameTestHelper helper) {
        final BlockPos window = new BlockPos(1, 1, 1);
        helper.setBlock(window, RocketKitBlocks.CAPSULE_WINDOW.get());
        final Seal seal = Seals.of(helper.getLevel(), helper.absolutePos(window), helper.getBlockState(window));
        helper.assertTrue(seal.wall() && seal.leak() == 0 && seal.rating().orElse(0.0) >= 300,
                "a capsule window holds an atmosphere against a vacuum: " + seal);
        for (final BlockEntry<Block> block : RocketKitBlocks.all()) {
            final double mass = BlockPhysics.of(block.get().defaultBlockState()).mass().orElse(-1.0);
            helper.assertTrue(mass > 0, block.getId() + " weighs its share: " + mass);
        }
        helper.succeed();
    }

    private static Map<ResourceLocation, Integer> counts(final Map<ResourceLocation, RocketKit> kits,
                                                         final String part) {
        final RocketKit kit = kits.get(SkeletonRocketParts.id(part));
        return kit.counts(kit.least(), false);
    }

    private static int count(final Map<ResourceLocation, Integer> counts, final BlockEntry<Block> block) {
        return counts.getOrDefault(block.getId(), 0);
    }

    private static int total(final Map<ResourceLocation, Integer> counts) {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }
}
