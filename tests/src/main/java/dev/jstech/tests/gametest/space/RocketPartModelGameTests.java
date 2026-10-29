/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.moving.BlockAboard;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.space.api.RocketKit;
import dev.jstech.space.api.RocketKits;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketPartModels;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Which formed model each part of a rocket is drawn with, and where: a tank a model a layer, on its skirt over an
 * engine, its lowest, middle and highest layers, or one layer alone; a separation ring with its interstage skirt round
 * a vacuum engine standing on it and without one under a sea-level engine; the instrument ring of five with its adapter
 * round a lunar lander; the legs folded on the way up and spread to land; each model at the height its part stands at.
 * And the crew seat is a chair of its own and in the lander's cabin, and a couch its crew lie on in a capsule.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RocketPartModelGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_rocket_models";

    private RocketPartModelGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void rocket_isDrawnWithAModelForEachPartWhereItStands(final GameTestHelper helper) {
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(helper.getLevel());
        final Map<ResourceLocation, RocketKit> kits = RocketKits.all(helper.getLevel());
        final RocketStack small = RocketStack.of(part("sea_level_engine_3", 1), part("landing_legs_3", 1),
                part("tank_3", 4), part("separation_ring_3", 1), part("vacuum_engine_3", 1), part("tank_3", 1),
                part("instrument_ring_3", 1), part("capsule_3", 1));
        final List<RocketPartModels.Placed> up = RocketPartModels.of(parts, kits, small, false);
        helper.assertTrue(names(up).equals(List.of("sea_level_engine_3", "landing_legs_3_stowed", "tank_3_bottom",
                "tank_3_middle", "tank_3_middle", "tank_3_top", "separation_ring_3_skirt", "vacuum_engine_3",
                "tank_3_alone", "instrument_ring_3", "capsule_3")), "on the way up: " + names(up));
        helper.assertTrue(up.get(2).y() == 3 && up.get(6).y() == 7 && up.get(7).y() == 8 && up.get(10).y() == 13,
                "each at its height: " + up);
        helper.assertTrue(names(RocketPartModels.of(parts, kits, small, true)).get(1).equals("landing_legs_3"),
                "spread to land");
        final RocketStack first = RocketStack.of(part("sea_level_engine_5", 1), part("tank_5", 3),
                part("separation_ring_5", 1), part("sea_level_engine_5", 1), part("tank_5", 2),
                part("instrument_ring_5", 1), part("lunar_lander_descent", 1), part("lunar_lander_ascent", 1));
        final List<String> lunar = names(RocketPartModels.of(parts, kits, first, false));
        helper.assertTrue(lunar.equals(List.of("sea_level_engine_5", "tank_5_foot", "tank_5_bottom", "tank_5_top",
                "separation_ring_5", "sea_level_engine_5", "tank_5_foot", "tank_5_alone", "instrument_ring_5_adapter",
                "lunar_lander_descent_stowed", "lunar_lander_ascent")), "a tank over an engine stands on its skirt, "
                + "no interstage round a sea-level engine, the adapter round the lander: " + lunar);
        helper.assertTrue(RocketPartModels.names().stream().allMatch(name -> RocketPartModels.get(name).isPresent()),
                "every model is declared");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void crewSeat_isSatUprightAloneAndInTheLanderAndLainOnInACapsule(final GameTestHelper helper) {
        final Optional<BlockAboard.Seat> alone = BlockAboard.of(RocketKitBlocks.CREW_SEAT.get().defaultBlockState())
                .seat();
        helper.assertTrue(alone.isPresent() && !alone.get().lying() && !alone.get().helm(),
                "a crew seat of its own is sat on upright: " + alone);
        for (final String capsule : List.of("capsule_3", "capsule_5")) {
            final BlockAboard couch = FormedShapes.aboardDeclared(RocketPartModels.get(capsule).orElseThrow().id())
                    .get(RocketKitBlocks.CREW_SEAT.getId());
            helper.assertTrue(couch != null && couch.seat().isPresent() && couch.seat().get().lying(),
                    "in " + capsule + " its crew lie on it: " + couch);
        }
        helper.assertTrue(FormedShapes.aboardDeclared(RocketPartModels.get("lunar_lander_ascent").orElseThrow().id())
                .isEmpty(), "in the lander's cabin it stays a chair facing the Flight Computer");
        helper.succeed();
    }

    private static List<String> names(final List<RocketPartModels.Placed> placed) {
        return placed.stream().map(model -> model.model().getPath().substring("rocket/".length())).toList();
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }
}
