/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.jstechseries.hazard.Dissonance;
import dev.jstech.core.physics.Laws;
import dev.jstech.core.physics.Physics;
import dev.jstech.core.physics.PhysicsArea;
import dev.jstech.core.physics.Pull;
import dev.jstech.core.region.Box;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestHazards;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Dissonance on a server: how far laws are from ours, a pull's strength and way, time's pace, light that bends and
 * space that wraps counted and a planet's gravity not; and a body in an area of other laws building it up, faster the
 * further they are, but not in a bubble of normal laws, and falling back out of them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DissonanceGameTests {

    private static final String ARENA = "empty";
    private static final double CLOSE = 1.0E-6;
    private static final BlockPos FEET = new BlockPos(2, 1, 2);

    private DissonanceGameTests() {
    }

    @GameTest(template = ARENA)
    public static void deviation_countsHowFarTheLawsAreFromOurs(final GameTestHelper helper) {
        close(helper, 0, Dissonance.deviation(Laws.NONE), "our own laws");
        close(helper, 2, Dissonance.deviation(new Laws(Optional.empty(), Optional.of(2.0), Optional.empty(),
                Optional.empty(), Optional.empty())), "time twice as fast, counted twice");
        close(helper, 0.5, Dissonance.deviation(new Laws(Optional.of(new Pull.Scaled(0.5)), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty())), "a pull of half");
        close(helper, 2, Dissonance.deviation(new Laws(Optional.of(new Pull.Towards(0, 1, 0, 1)), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty())), "a pull straight up");
        close(helper, 0.5, Dissonance.deviation(new Laws(Optional.empty(), Optional.empty(),
                Optional.of(new Laws.Light(0xFFFFFF, 0, 0.5, 0, 0, 0)), Optional.empty(), Optional.empty())),
                "light bent by half");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void dissonance_buildsUpInOtherLawsButNotInABubble(final GameTestHelper helper) {
        final Player exposed = standing(helper);
        final Player shielded = standing(helper);
        final String id = "dissonance_test_" + helper.absolutePos(FEET).asLong();
        final BlockPos low = helper.absolutePos(BlockPos.ZERO);
        final BlockPos high = helper.absolutePos(new BlockPos(5, 5, 5));
        Physics.add(helper.getLevel(), new PhysicsArea(id, Box.between(low.getX(), low.getY(), low.getZ(),
                high.getX(), high.getY(), high.getZ()), new Laws(Optional.empty(), Optional.of(3.0),
                Optional.empty(), Optional.empty(), Optional.empty()), 0));
        TestHazards.shield(shielded, true);
        try {
            Hazards.second(exposed, helper.getLevel());
            Hazards.second(shielded, helper.getLevel());
            close(helper, 4, Hazards.level(exposed, Dissonance.KIND), "time three times as fast builds up four");
            close(helper, 0, Hazards.level(shielded, Dissonance.KIND), "nothing in a bubble of normal laws");
        } finally {
            Physics.remove(helper.getLevel(), id);
            TestHazards.shield(shielded, false);
        }
        Hazards.second(exposed, helper.getLevel());
        close(helper, 4 - (0.5 + 4 * 0.02), Hazards.level(exposed, Dissonance.KIND), "it falls back in normal laws");
        helper.succeed();
    }

    /* The game's mock player, standing in the test's arena. */
    private static Player standing(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(FEET));
        player.setPos(at.x, at.y, at.z);
        return player;
    }

    private static void close(final GameTestHelper helper, final double expected, final double actual,
                              final String what) {
        helper.assertTrue(Math.abs(expected - actual) < CLOSE + Math.abs(expected) * CLOSE,
                what + ": expected " + expected + ", got " + actual);
    }
}
