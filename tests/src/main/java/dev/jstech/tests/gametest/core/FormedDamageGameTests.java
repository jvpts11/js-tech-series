/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.damage.CellMark;
import dev.jstech.core.multiblock.damage.DamageStamps;
import dev.jstech.core.multiblock.damage.FormedDamage;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestModels;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Damage to a formed structure's model, on the server: an impact on the test turbine's top is kept by every block it
 * reaches, the struck block is re-shaped from its faces as the impact left them so a ray where it struck goes into
 * the hole, the record is saved with the block, and erasing it mends the turbine; a plain block takes no strike.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FormedDamageGameTests {

    private static final String ARENA = "empty";
    /* The terminal, in the middle of the south side of a turbine built facing north; the middle of its top struck. */
    private static final BlockPos TERMINAL = new BlockPos(4, 1, 5);
    private static final BlockPos STRUCK = TERMINAL.offset(0, 1, -1);
    private static final int WAIT = 200;
    private static final double SIZE = 0.8;
    private static final long SEED = 7;
    /* How much lower than where it struck a ray must now stop to have gone into the hole, in blocks. */
    private static final double INTO = 1.0 / 16;
    /*
     * Where across the struck block it is struck, along x and along z: its plating, well clear of the hub, whose nose
     * rises above the plating, solid, so the strike holes the plating round it and leaves the hub standing.
     */
    private static final double ACROSS = 3.0 / 16;
    private static final double CLOSE = 1.0e-6;

    private FormedDamageGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = WAIT)
    public static void strike_holesTheBlocksItReachesAndEraseMendsThem(final GameTestHelper helper) {
        build(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(structure(helper).merged(), "the turbine forms and merges"))
                .thenExecute(() -> {
                    final ServerLevel level = helper.getLevel();
                    final BlockPos pos = helper.absolutePos(STRUCK);
                    final BlockHitResult top = down(level, pos);
                    helper.assertTrue(top.getType() == HitResult.Type.BLOCK && top.getBlockPos().equals(pos),
                            "a ray from above meets the struck block's top: " + top.getBlockPos());
                    final VoxelShape before = shape(level, pos);

                    final int reached = FormedDamage.strike(level, pos, DamageStamps.IMPACT, top.getLocation(),
                            new Vec3(0, 1, 0), SIZE, SEED);

                    helper.assertTrue(reached >= 2, "the impact reaches the struck block and those beside it: "
                            + reached);
                    final FormedPartBlockEntity part = (FormedPartBlockEntity) level.getBlockEntity(pos);
                    helper.assertTrue(part.damage().size() == 1 && part.damage().getFirst().on(part.place()),
                            "the struck block keeps the impact, laid on its own block of the model");
                    helper.assertTrue(FormedDamage.marks(level, pos, part.place()).size() == 1,
                            "the model has one strike, however many blocks keep it");
                    helper.assertTrue(Shapes.joinIsNotEmpty(before, shape(level, pos), BooleanOp.NOT_SAME),
                            "the struck block is re-shaped");
                    final BlockHitResult through = down(level, pos);
                    helper.assertTrue(through.getType() == HitResult.Type.MISS
                            || through.getLocation().y < top.getLocation().y - INTO, "a ray where it struck goes into"
                            + " the hole: " + through.getLocation() + " against " + top.getLocation());

                    helper.assertTrue(FormedDamage.erase(level, pos) == reached, "erasing lets every block go of it");
                    helper.assertTrue(part.damage().isEmpty(), "the struck block keeps nothing");
                    helper.assertFalse(Shapes.joinIsNotEmpty(before, shape(level, pos), BooleanOp.NOT_SAME),
                            "it stands as the model has it again");
                    helper.assertTrue(Math.abs(down(level, pos).getLocation().y - top.getLocation().y) < CLOSE,
                            "and a ray meets its top where it did");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = WAIT)
    public static void strike_isSavedWithTheBlocksItReached(final GameTestHelper helper) {
        build(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(structure(helper).merged(), "the turbine forms and merges"))
                .thenExecute(() -> {
                    final ServerLevel level = helper.getLevel();
                    final BlockPos pos = helper.absolutePos(STRUCK);
                    FormedDamage.strike(level, pos, DamageStamps.BURN, down(level, pos).getLocation(),
                            new Vec3(0, 1, 0), SIZE, SEED);
                    final FormedPartBlockEntity part = (FormedPartBlockEntity) level.getBlockEntity(pos);
                    final HolderLookup.Provider registries = level.registryAccess();

                    final CompoundTag saved = part.saveWithoutMetadata(registries);
                    final FormedPartBlockEntity read = new FormedPartBlockEntity(pos, part.getBlockState());
                    read.loadWithComponents(saved, registries);

                    final List<CellMark> kept = read.damage();
                    helper.assertTrue(kept.equals(part.damage()) && kept.size() == 1, "the burn reads back as it was"
                            + " saved: " + kept);
                    final FormedPlace place = read.place();
                    helper.assertTrue(place != null && kept.getFirst().on(place), "laid on the block it was");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void strike_onAPlainBlockLaysNothing(final GameTestHelper helper) {
        helper.setBlock(STRUCK, Blocks.IRON_BLOCK);
        final BlockPos pos = helper.absolutePos(STRUCK);

        final int reached = FormedDamage.strike(helper.getLevel(), pos, DamageStamps.IMPACT, Vec3.atCenterOf(pos),
                new Vec3(0, 1, 0), SIZE, SEED);

        helper.assertTrue(reached == 0, "a block of no formed model takes no strike");
        helper.succeed();
    }

    /*
     * Where a ray straight down through the block at {@code pos} first meets a block's shape: over its plating
     * between the arms of the hub's cross, in the middle of an eighth of the block, so the shape's boxes, in eighths
     * once the strike has cut them finer, never stand on the ray's line.
     */
    private static BlockHitResult down(final ServerLevel level, final BlockPos pos) {
        final Vec3 from = new Vec3(pos.getX() + ACROSS, pos.getY() + 3, pos.getZ() + ACROSS);
        final Vec3 to = new Vec3(pos.getX() + ACROSS, pos.getY() - 1.5, pos.getZ() + ACROSS);
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                CollisionContext.empty()));
    }

    private static VoxelShape shape(final ServerLevel level, final BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos);
    }

    /* The turbine, as the formed model tests build it: three by three, two high, its terminal at the south side. */
    private static void build(final GameTestHelper helper) {
        for (int y = 0; y < 2; y++) {
            for (int a = -1; a <= 1; a++) {
                for (int b = 0; b <= 2; b++) {
                    final BlockPos at = TERMINAL.offset(a, y, -b);
                    if (!at.equals(TERMINAL)) {
                        helper.setBlock(at, Blocks.IRON_BLOCK);
                    }
                }
            }
        }
        helper.setBlock(TERMINAL, TestModels.TURBINE_BLOCK.get());
    }

    private static MachineStructure structure(final GameTestHelper helper) {
        final TestModels.TurbineEntity turbine = helper.getBlockEntity(TERMINAL);
        return turbine.structure();
    }
}
