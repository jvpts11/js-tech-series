/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.audio.LoopRequest;
import dev.jstech.core.mechanical.MechanicalPartBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMechanical;
import dev.jstech.tests.TestSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The parts of mechanical power as the players see and hear them: a part drawn turning is drawn by its block entity,
 * not its block's model, and hides nothing beside it; its speed reaches the players' games; and a part with a sound
 * turns with it, louder and higher the faster it turns, and silent at rest.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class TurningPartsGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 40;
    private static final double CLOSE = 1.0e-4;

    private TurningPartsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void turningPart_isDrawnByItsBlockEntityNotItsModel(final GameTestHelper helper) {
        helper.assertValueEqual(TestMechanical.SHAFT.get().defaultBlockState().getRenderShape(),
                RenderShape.ENTITYBLOCK_ANIMATED, "a shaft drawn turning");
        helper.assertValueEqual(TestMechanical.LARGE_PULLEY.get().defaultBlockState().getRenderShape(),
                RenderShape.ENTITYBLOCK_ANIMATED, "a pulley drawn turning");
        helper.assertValueEqual(TestMechanical.GEARBOX.get().defaultBlockState().getRenderShape(), RenderShape.MODEL,
                "a gearbox its mod draws standing still keeps its model");
        helper.assertFalse(TestMechanical.SHAFT.get().defaultBlockState().canOcclude(),
                "a part drawn turning hides nothing beside it, so the faces around it are drawn");
        helper.assertTrue(TestMechanical.SHAFT.get().defaultBlockState().propagatesSkylightDown(helper.getLevel(),
                helper.absolutePos(BlockPos.ZERO)), "and lets the light through, so it is lit as its place is");
        helper.assertTrue(TestMechanical.GEARBOX.get().defaultBlockState().canOcclude(),
                "a gearbox drawn as its block is stays a whole block");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void turning_reachesThePlayersGames(final GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 2), TestMechanical.CRANK.get());
        place(helper, new BlockPos(2, 2, 2), TestMechanical.SHAFT.get());
        helper.runAfterDelay(SETTLE, () -> {
            final MechanicalPartBlockEntity shaft = part(helper, new BlockPos(2, 2, 2));
            final double told = shaft.getUpdateTag(helper.getLevel().registryAccess()).getDouble("Rpm");
            helper.assertTrue(shaft.rpm() > 1.0, "the shaft turns: " + shaft.rpm());
            helper.assertTrue(Math.abs(told - shaft.rpm()) < CLOSE, "the players' games are told how fast: " + told);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void turningSound_isLouderAndHigherTheFasterItTurns(final GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 2), TestMechanical.CRANK.get());
        place(helper, new BlockPos(2, 2, 2), TestMechanical.SHAFT.get());
        helper.runAfterDelay(SETTLE, () -> {
            final MechanicalPartBlockEntity shaft = part(helper, new BlockPos(2, 2, 2));
            final List<LoopRequest> loops = shaft.loops();
            helper.assertValueEqual(loops.size(), 1, "the shaft whirs as it turns");
            final LoopRequest whir = loops.getFirst();
            final double share = Math.min(1.0, Math.abs(shaft.rpm()) / TestMechanical.CRANK_TOP);
            helper.assertTrue(whir.sound() == TestSounds.WHIR, "with its block's sound");
            helper.assertTrue(Math.abs(whir.volume() - share) < CLOSE, "as loud as its share of its top speed: "
                    + whir.volume() + " at " + shaft.rpm() + " rpm");
            helper.assertTrue(Math.abs(whir.pitch() - (0.5 + share)) < CLOSE, "and higher with it: " + whir.pitch());
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void turningSound_isSilentAtRestAndForAPartWithoutOne(final GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 2), TestMechanical.SHAFT.get());
        place(helper, new BlockPos(1, 2, 4), TestMechanical.CRANK.get());
        place(helper, new BlockPos(2, 2, 4), TestMechanical.SMALL_GEAR.get());
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(part(helper, new BlockPos(1, 2, 2)).loops().isEmpty(), "a shaft at rest is silent");
            final MechanicalPartBlockEntity gear = part(helper, new BlockPos(2, 2, 4));
            helper.assertTrue(gear.rpm() > 1.0, "the gear turns: " + gear.rpm());
            helper.assertTrue(gear.loops().isEmpty(), "but its block has no sound");
            helper.succeed();
        });
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final Block block) {
        helper.setBlock(pos, block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
    }

    private static MechanicalPartBlockEntity part(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos);
    }
}
