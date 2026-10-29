/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.moving.physics.BlockPhysics;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.region.Box;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestPhysics;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The blocks of a formed structure merged into its model, aboard a moving structure: they weigh what the blocks they
 * were weigh, and a thruster merged into the model still thrusts. The test mod's thrust pod, a thruster with its
 * terminal on top and a light block beside each, merges its thruster and light blocks once formed; made a moving
 * structure, it weighs its blocks' own masses, and lit by a lever aboard, it climbs.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FormedPhysicsGameTests {

    private static final String TALL = "tall";
    /* The pod's thruster, its terminal on top, the light blocks south of each, and the lever east of the thruster. */
    private static final BlockPos THRUSTER = new BlockPos(4, 2, 4);
    private static final BlockPos TERMINAL = THRUSTER.above();
    private static final BlockPos LEVER = THRUSTER.east();
    private static final int WAIT = 200;
    /* The masses the test mod's data map gives the thruster, a light block and the terminal, in kilograms. */
    private static final double THRUSTER_KILOGRAMS = 100;
    private static final double LIGHT_KILOGRAMS = 5;
    private static final double TERMINAL_KILOGRAMS = 20;
    /* How close the mass weighed must be, in kilograms, and how far the pod must climb once lit, in blocks. */
    private static final double SAME = 1.0e-6;
    private static final double CLIMBS = 1;
    private static final int LIT_FOR = 10;

    private FormedPhysicsGameTests() {
    }

    @GameTest(template = TALL, timeoutTicks = WAIT)
    public static void mergedBlocks_weighAndThrustAsTheBlocksTheyWere(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        build(helper);
        final List<MovingStructure> made = new ArrayList<>();
        final double[] start = new double[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(structure(helper).merged(), "the pod forms and merges"))
                .thenExecute(() -> {
                    helper.assertTrue(helper.getBlockState(THRUSTER).getBlock() instanceof FormedPartBlock,
                            "its thruster merged into it");
                    warm(level);
                    final StructureResult result = MovingStructures.assemble(level, helper.absolutePos(THRUSTER),
                            helper.absolutePos(new BlockPos(LEVER.getX(), TERMINAL.getY(), THRUSTER.getZ() + 1)));
                    helper.assertTrue(result.ok() && result.structure() != null, "the pod is made a structure: "
                            + (result.refusal() == null ? "done" : result.refusal().english()));
                    made.add(result.structure());
                    result.structure().move(PhysicsMotion.AT_REST);
                    start[0] = result.structure().worldBox().minY;
                })
                .thenWaitUntil(() -> {
                    final StructurePhysics physics = made.getFirst().physics();
                    helper.assertTrue(physics != null && physics.mass() > 0, "it moves by its physics");
                    final double lever = leverMass(level, made.getFirst());
                    final double wanted = THRUSTER_KILOGRAMS + 2 * LIGHT_KILOGRAMS + TERMINAL_KILOGRAMS + lever;
                    helper.assertTrue(Math.abs(physics.mass() - wanted) < SAME, "it weighs its blocks as they were, "
                            + wanted + " kg, not as merged blocks: " + physics.mass() + " kg");
                })
                .thenExecute(() -> {
                    final BlockPos lever = find(level, made.getFirst());
                    helper.assertTrue(lever != null, "the lever is aboard");
                    level.setBlock(lever, level.getBlockState(lever).setValue(LeverBlock.POWERED, true), 3);
                })
                .thenExecuteAfter(LIT_FOR, () -> {
                    final MovingStructure pod = made.getFirst();
                    helper.assertTrue(pod.worldBox().minY > start[0] + CLIMBS, "lit, its merged thruster lifts it: "
                            + pod.worldBox() + " from " + start[0]);
                    MovingStructures.takeAway(pod);
                })
                .thenSucceed();
    }

    /* The pod, its thruster facing up, and the lever, off, on the thruster's east side. */
    private static void build(final GameTestHelper helper) {
        helper.setBlock(THRUSTER, TestPhysics.THRUSTER.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.UP));
        helper.setBlock(THRUSTER.south(), TestPhysics.LIGHT.get());
        helper.setBlock(TERMINAL.south(), TestPhysics.LIGHT.get());
        helper.setBlock(TERMINAL, TestPhysics.POD.get());
        helper.setBlock(LEVER, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
                .setValue(LeverBlock.FACING, Direction.EAST).setValue(LeverBlock.POWERED, false));
    }

    private static MachineStructure structure(final GameTestHelper helper) {
        final TestPhysics.PodEntity pod = helper.getBlockEntity(TERMINAL);
        return pod.structure();
    }

    /* What the lever aboard weighs, as the game's own blocks are weighed. */
    private static double leverMass(final ServerLevel level, final MovingStructure structure) {
        final BlockPos lever = find(level, structure);
        return lever == null ? 0 : BlockPhysics.massOf(level, lever, level.getBlockState(lever));
    }

    /* The lever among the structure's blocks in the yard. */
    private static BlockPos find(final ServerLevel level, final MovingStructure structure) {
        final Box box = structure.bounds();
        for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            final BlockState state = level.getBlockState(at);
            if (state.is(Blocks.LEVER)) {
                return at.immutable();
            }
        }
        return null;
    }

    /* Loads the chunks round the yard's next plot, so a structure made comes into it at once. */
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
