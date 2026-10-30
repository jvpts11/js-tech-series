/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.space.telemetry.AntennaSize;
import dev.jstech.space.telemetry.Contact;
import dev.jstech.space.telemetry.GroundSystem;
import dev.jstech.space.telemetry.GroundSystems;
import dev.jstech.testkit.gametest.ScenarioBuilder;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The telemetry line's topology: a stretch of it joining an antenna, a receiver, a decoder and a ground station is a
 * ground system that works, its antennas adding up; with a medium antenna it hears the Moon but not another world;
 * cut, each side is a system of its own, and the side with no receiver does not work.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class TelemetryGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 2;
    /* The devices in a row, the line laid along beside them. */
    private static final List<Block> DEVICES = List.of(Blocks.IRON_BLOCK, Blocks.COPPER_BLOCK, Blocks.CARTOGRAPHY_TABLE,
            Blocks.TARGET, Blocks.OBSERVER, Blocks.JUKEBOX);
    private static final int ROW = 2;
    private static final int LINE = 3;
    private static final int Y = 2;
    /* Where the line is cut: between the ground station and the receiver. */
    private static final int CUT = 3;

    private TelemetryGameTests() {
    }

    @GameTest(template = ARENA)
    public static void line_groupsItsDevicesIntoAGroundSystemThatHears(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        for (int x = 0; x < DEVICES.size(); x++) {
            helper.setBlock(new BlockPos(x + 1, Y, ROW), DEVICES.get(x));
            helper.assertTrue(ScenarioBuilder.layCable(level, helper.absolutePos(new BlockPos(x + 1, Y, LINE)),
                    TestCableTypes.TELEMETRY.get()), "the line is laid beside device " + x);
        }
        final BlockPos antenna = helper.absolutePos(new BlockPos(1, Y, ROW));
        final BlockPos recorder = helper.absolutePos(new BlockPos(DEVICES.size(), Y, ROW));
        helper.runAfterDelay(SETTLE, () -> {
            final Optional<GroundSystem> system = GroundSystems.of(level, antenna);
            helper.assertTrue(system.isPresent() && system.get().works() && system.get().keeps()
                    && system.get().devices().size() == DEVICES.size(),
                    "the stretch is one ground system that works and keeps its data: " + system);
            helper.assertTrue(system.get().rate() == AntennaSize.MEDIUM.rate() + AntennaSize.SMALL.rate()
                    && system.get().reach().equals(Optional.of(AntennaSize.Reach.FAR)),
                    "its antennas add up, the medium one setting how far it hears: " + system.get().rate());
            // The arena may stand far from the equator, where no geostationary satellite is in sight: the Moon is.
            helper.assertTrue(GroundSystems.hears(level, antenna, new Contact.Target(Contact.Where.MOON, 0, false)),
                    "its medium antenna hears the Moon");
            helper.assertTrue(!GroundSystems.hears(level, antenna, new Contact.Target(Contact.Where.DEEP_SPACE, 0,
                    false)), "but not a probe at another world, with no large antenna");
            helper.setBlock(new BlockPos(CUT + 1, Y, LINE), Blocks.AIR);
            helper.runAfterDelay(SETTLE, () -> {
                final Optional<GroundSystem> left = GroundSystems.of(level, antenna);
                final Optional<GroundSystem> right = GroundSystems.of(level, recorder);
                helper.assertTrue(left.isPresent() && right.isPresent() && !left.get().works()
                        && left.get().devices().size() == CUT, "cut, the side with no receiver does not work: "
                        + left);
                helper.succeed();
            });
        });
    }
}
