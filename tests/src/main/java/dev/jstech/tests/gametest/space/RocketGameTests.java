/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.orbit.Rocketry;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.RocketStages;
import dev.jstech.space.rocket.Rockets;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.StageMath;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The rockets' model: J's Space's parts stand in the diameters of 3 and 5 blocks while the registry knows 7 and 9 too;
 * a small rocket of them, two stages of the sea-level and vacuum engines and a capsule, reaches a low orbit from the
 * Overworld and lifts off it, and its parts fit where a part of another diameter would not; a rocket is a moving
 * structure that belongs to its builder and is used by their team.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RocketGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_rockets";
    /* The small rocket's tanks, in layers, and how tall it stands. */
    private static final int FIRST_TANK = 18;
    private static final int SECOND_TANK = 4;
    private static final int SMALL_HEIGHT = 2 + FIRST_TANK + 1 + 3 + SECOND_TANK + 1 + 3;

    private RocketGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void parts_standInTheFirstTwoDiametersAndTheRegistryKnowsFour(final GameTestHelper helper) {
        final Map<ResourceLocation, RocketPart> parts = RocketParts.declared();
        helper.assertTrue(parts.values().stream().allMatch(part -> part.diameter() == 3 || part.diameter() == 5),
                "J's Space's parts are of 3 and 5 blocks");
        helper.assertTrue(parts.values().stream().anyMatch(part -> part.diameter() == 3)
                && parts.values().stream().anyMatch(part -> part.diameter() == 5), "both");
        helper.assertTrue(RocketPart.DIAMETERS.equals(List.of(3, 5, 7, 9)), "the registry knows the four diameters");
        helper.assertTrue(parse("{\"name\":\"x\",\"kind\":\"tank\",\"diameter\":9,\"height\":1,\"mass\":1}")
                .isPresent(), "an add-on's part of 9 blocks reads");
        helper.assertTrue(parse("{\"name\":\"x\",\"kind\":\"tank\",\"diameter\":4,\"height\":1,\"mass\":1}")
                .isEmpty(), "and one of 4 does not");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void smallRocket_reachesALowOrbitAndLiftsOffTheOverworld(final GameTestHelper helper) {
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(helper.getLevel());
        final RocketStack small = small();
        final List<StageMath.Burn> burns = RocketStages.burns(parts, small).orElseThrow();
        final double deltaV = StageMath.deltaV(burns, 0);
        final double lift = StageMath.thrustToWeight(burns.getFirst(), 1, 1);
        helper.assertTrue(burns.size() == 2, "two stages, split at the separation ring: " + burns.size());
        helper.assertTrue(deltaV >= Rocketry.SURFACE_TO_LOW_ORBIT, "it reaches a low orbit: " + deltaV + " m/s");
        helper.assertTrue(lift > 1, "and lifts off the Overworld: " + lift);
        helper.assertTrue(StageMath.thrustToWeight(burns.getFirst(), 0.165, 0) > 5 * lift,
                "and would leap off the Moon");
        helper.assertTrue(RocketStages.height(parts, small) == SMALL_HEIGHT, "it stands "
                + RocketStages.height(parts, small) + " blocks tall");
        helper.assertTrue(RocketStages.misfit(parts, small).isEmpty(), "its parts fit");
        final RocketStack misfit = RocketStack.of(part("sea_level_engine_3"), part("tank_5", 2));
        helper.assertTrue(RocketStages.misfit(parts, misfit).equals(Optional.of(1)),
                "a tank of 5 does not fit an engine of 3");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void rocket_isAMovingStructureOfItsBuilderUsedByTheirTeam(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer builder = GameTestPlayers.join(level, UUID.randomUUID(), "rocket_builder");
        final ServerPlayer crew = GameTestPlayers.join(level, UUID.randomUUID(), "rocket_crew");
        final ServerPlayer stranger = GameTestPlayers.join(level, UUID.randomUUID(), "stranger");
        final Scoreboard scoreboard = level.getServer().getScoreboard();
        final PlayerTeam team = scoreboard.addPlayerTeam("jstests_rocket_" + builder.getUUID().toString()
                .substring(0, 8));
        scoreboard.addPlayerToTeam(builder.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(crew.getScoreboardName(), team);
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.IRON_BLOCK);
        warm(level);
        final StructureResult made = MovingStructures.assembleFor(level, helper.absolutePos(new BlockPos(2, 2, 2)),
                helper.absolutePos(new BlockPos(2, 2, 2)), builder.getUUID());
        try {
            helper.assertTrue(made.ok(), "the rocket's blocks are a moving structure");
            Rockets.register(level.getServer(), made.structure().id(), small(), builder.getUUID());
            final Rockets.Rocket rocket = Rockets.get(level.getServer(), made.structure().id()).orElseThrow();
            helper.assertTrue(rocket.stack().equals(small()), "it keeps its parts");
            helper.assertTrue(rocket.ownership().mayUse(builder) && rocket.ownership().mayUse(crew),
                    "its builder and their team may fly it");
            helper.assertTrue(!rocket.ownership().mayUse(stranger), "a stranger may not");
            helper.assertTrue(Rockets.forget(level.getServer(), made.structure().id())
                    && Rockets.get(level.getServer(), made.structure().id()).isEmpty(),
                    "taken apart, it is a rocket no more");
        } finally {
            if (made.ok()) {
                MovingStructures.takeAway(made.structure());
            }
            scoreboard.removePlayerTeam(team);
            GameTestPlayers.leave(builder);
            GameTestPlayers.leave(crew);
            GameTestPlayers.leave(stranger);
        }
        helper.succeed();
    }

    /* The small rocket: a sea-level engine under a tall tank, a separation ring, a vacuum engine under a short tank,
    the instrument ring and the capsule. */
    private static RocketStack small() {
        return RocketStack.of(part("sea_level_engine_3"), part("tank_3", FIRST_TANK), part("separation_ring_3"),
                part("vacuum_engine_3"), part("tank_3", SECOND_TANK), part("instrument_ring_3"), part("capsule_3"));
    }

    private static RocketStack.Part part(final String path) {
        return part(path, 1);
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }

    private static Optional<RocketPart> parse(final String json) {
        return RocketPart.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result();
    }

    /* Loads the chunks of the yard's next plot, so a structure made comes into it at once. */
    private static void warm(final ServerLevel level) {
        final int plot = StructureYard.nextPlot(level);
        final int cx = YardLayout.centreX(plot) >> 4;
        final int cz = YardLayout.centreZ(plot) >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk(cx + dx, cz + dz);
            }
        }
    }
}
