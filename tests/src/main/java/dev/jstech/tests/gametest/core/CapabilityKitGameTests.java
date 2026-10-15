/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.capability.CapabilityCache;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.TestMachines;
import dev.jstech.tests.TestToolkit;
import dev.jstech.tests.testkit.TestCables;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A capability of a mod's own declared in one line: offered by a block entity on the face it says, kept in a cache
 * that follows the block as it is broken and placed again, and offered by a part on a cable through the cable block,
 * with nothing registered by the part.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CapabilityKitGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos KILN = new BlockPos(1, 2, 1);
    private static final BlockPos OWNER = new BlockPos(3, 2, 1);
    private static final BlockPos CABLE = new BlockPos(1, 2, 3);

    private CapabilityKitGameTests() {
    }

    @GameTest(template = ARENA)
    public static void kind_isOfferedByTheBlockEntityOnItsFace(final GameTestHelper helper) {
        helper.setBlock(KILN, TestMachines.KILN.get());
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(KILN);

        final TestToolkit.IProbe top = TestToolkit.PROBE.at(level, at, Direction.UP);
        helper.assertTrue(top != null && top.reading() == TestToolkit.KILN_READING, "the kiln reads 7 on its top");
        helper.assertTrue(TestToolkit.PROBE.at(level, at, Direction.NORTH) == null,
                "and offers nothing on a face it does not name");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cache_followsTheBlockAsItIsBrokenAndPlacedAgain(final GameTestHelper helper) {
        helper.setBlock(KILN, TestMachines.KILN.get());
        helper.setBlock(OWNER, TestMachines.KILN.get());
        final BlockEntity owner = helper.getBlockEntity(OWNER);
        final AtomicInteger stale = new AtomicInteger();
        final CapabilityCache<TestToolkit.IProbe> cache = CapabilityCache.of(TestToolkit.PROBE.block(),
                helper.getLevel(), helper.absolutePos(KILN), Direction.UP, owner, stale::incrementAndGet);

        helper.assertTrue(cache.get() != null, "the cache finds the kiln's probe");
        helper.setBlock(KILN, Blocks.AIR);
        helper.assertTrue(cache.get() == null && stale.get() > 0, "and is told, and finds nothing, once it is broken");
        helper.setBlock(KILN, TestMachines.KILN.get());
        final TestToolkit.IProbe again = cache.get();
        helper.assertTrue(again != null && again.reading() == TestToolkit.KILN_READING,
                "and finds it again once it is placed again");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void part_offersItsCapabilityThroughTheBlockItSitsOn(final GameTestHelper helper) {
        final CableBlockEntity cable = TestCables.lay(helper, CABLE, TestCableTypes.PLAIN_PIPE);
        cable.parts().add(Direction.EAST, TestToolkit.PROBE_PART.get().create());
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(CABLE);

        final TestToolkit.IProbe east = TestToolkit.PROBE.at(level, at, Direction.EAST);
        helper.assertTrue(east != null && east.reading() == TestToolkit.PART_READING,
                "the probe part reads 11 through the cable block, on its face");
        helper.assertTrue(TestToolkit.PROBE.at(level, at, Direction.WEST) == null, "and nothing on a bare face");
        helper.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK, at, Direction.EAST) == null,
                "a part that offers no items gives none on its face");
        cable.parts().remove(Direction.EAST);
        helper.assertTrue(TestToolkit.PROBE.at(level, at, Direction.EAST) == null,
                "and the face offers nothing once the part is taken off");
        helper.succeed();
    }
}
