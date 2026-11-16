/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.machine.MachineStructure;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A structure held, as a dock holds itself while its crane and its floor are away moving: it holds every place,
 * so its blocks carried off keep themselves, and it is not checked, so a block of it gone undoes nothing; let go, it
 * is checked again and stands as its blocks stand.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineStructureHoldGameTests {

    private static final String ARENA = "empty";
    /* The test pod: its thruster, its terminal on it, a light block south of each. */
    private static final BlockPos THRUSTER = new BlockPos(4, 2, 4);
    private static final BlockPos TERMINAL = THRUSTER.above();
    /* A place none of the pod's blocks stands in, as a block carried off in a moving structure stands. */
    private static final BlockPos AWAY = new BlockPos(1, 2, 1);
    /* How long it is watched held, in ticks: well past the settling a change waits before it is checked. */
    private static final int WATCHED = 40;

    private MachineStructureHoldGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void hold_aHeldStructureHoldsWhatIsAwayAndIsNotCheckedTillLetGo(final GameTestHelper helper) {
        helper.setBlock(THRUSTER, TestPhysics.THRUSTER.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.UP));
        helper.setBlock(THRUSTER.south(), TestPhysics.LIGHT.get());
        helper.setBlock(TERMINAL.south(), TestPhysics.LIGHT.get());
        helper.setBlock(TERMINAL, TestPhysics.POD.get());
        final boolean[] held = new boolean[1];
        final MachineStructure[] pod = new MachineStructure[1];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    pod[0] = ((TestPhysics.PodEntity) helper.getBlockEntity(TERMINAL)).structure();
                    helper.assertTrue(pod[0].formed() && pod[0].merged(), "the pod forms and merges its blocks");
                })
                .thenExecute(() -> {
                    pod[0].holdWhile(() -> held[0]);
                    helper.assertFalse(pod[0].holdsPlace(helper.absolutePos(AWAY)), "let go, it holds no place it"
                            + " does not stand in");
                    held[0] = true;
                    helper.assertTrue(pod[0].holdsPlace(helper.absolutePos(AWAY)), "held, it holds a place away from"
                            + " it, as its blocks carried off stand in");
                    helper.setBlock(THRUSTER.south(), Blocks.AIR);
                })
                .thenIdle(WATCHED)
                .thenExecute(() -> {
                    helper.assertTrue(pod[0].formed(), "held, a block of it gone undoes nothing: it is not checked");
                    held[0] = false;
                })
                .thenWaitUntil(() -> helper.assertFalse(pod[0].formed(), "let go, it is checked again and undone"))
                .thenSucceed();
    }
}
