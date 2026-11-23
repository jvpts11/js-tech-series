/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.cable.CableItem;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.jstechseries.lines.SharedPipes;
import dev.jstech.tests.JsTests;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Laying a cable by hand: on a block that gives way, a snow layer, it is laid in its place and never above it; a new
 * cable joins the one beside it the moment it is laid; and a pipe sounds as metal, a cable as wool.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CableLayingGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos GROUND = new BlockPos(2, 1, 2);
    private static final BlockPos SNOW = GROUND.above();
    private static final int STACK = 16;

    private CableLayingGameTests() {
    }

    /**
     * A cable block laid in water stands full of it, and a liquid flowing over cables leaves them standing: water and
     * lava broke them as they do a torch, cutting their wires without a word.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void liquid_flowingOverCablesLeavesThemStandingAndWaterStandsInThem(final GameTestHelper helper) {
        final CableType cable = SharedEnergy.INSULATED_COPPER_CABLE.get();
        final BlockPos inWater = new BlockPos(1, 2, 1);
        final BlockPos underWater = new BlockPos(1, 2, 4);
        final BlockPos underLava = new BlockPos(5, 2, 4);
        helper.setBlock(inWater, Blocks.WATER);
        helper.assertTrue(CableItem.lay(helper.getLevel(), helper.absolutePos(inWater), cable), "laid in water");
        helper.assertTrue(CableItem.lay(helper.getLevel(), helper.absolutePos(underWater), cable)
                && CableItem.lay(helper.getLevel(), helper.absolutePos(underLava), cable), "laid in the open");
        helper.setBlock(underWater.above(), Blocks.WATER);
        helper.setBlock(underLava.above(), Blocks.LAVA);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(helper.getBlockState(inWater).getValue(BlockStateProperties.WATERLOGGED)
                    && helper.getBlockState(inWater).getFluidState().is(Fluids.WATER),
                    "the cable block laid in water stands full of it");
            helper.assertTrue(helper.getBlockEntity(underWater) instanceof CableBlockEntity,
                    "water flowing over a cable leaves it standing");
            helper.assertTrue(helper.getBlockEntity(underLava) instanceof CableBlockEntity,
                    "and so does lava");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void click_onASnowLayerLaysTheCableInItsPlace(final GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.STONE);
        helper.setBlock(SNOW, Blocks.SNOW);
        final FakePlayer player = player(helper, "lay_snow");
        final BlockHitResult hit = topOf(helper, SNOW);
        final Optional<BlockPos> landing = CableItem.landing(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                hit, SharedPipes.COPPER_PIPE.get());
        helper.assertTrue(landing.equals(Optional.of(helper.absolutePos(SNOW))),
                "the player's game and the server both lay it in the snow's place: " + landing);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(helper.getBlockEntity(SNOW) instanceof CableBlockEntity cable
                && cable.holds(SharedPipes.COPPER_PIPE.get()), "the pipe takes the snow's place");
        helper.assertTrue(helper.getBlockState(SNOW.above()).isAir(), "and nothing is laid above it");
        helper.assertTrue(player.getMainHandItem().getCount() == STACK - 1, "one pipe spent");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void laying_joinsTheCableBesideAtOnce(final GameTestHelper helper) {
        final CableType pipe = SharedPipes.COPPER_PIPE.get();
        final BlockPos first = new BlockPos(1, 2, 1);
        final BlockPos second = first.east();
        helper.assertTrue(CableItem.lay(helper.getLevel(), helper.absolutePos(first), pipe), "the first pipe laid");
        helper.assertTrue(CableItem.lay(helper.getLevel(), helper.absolutePos(second), pipe), "the second laid");
        helper.assertTrue(helper.getBlockEntity(first) instanceof CableBlockEntity a && a.crosses(pipe, Direction.EAST),
                "the first joins the second as it is laid, in the same tick");
        helper.assertTrue(helper.getBlockEntity(second) instanceof CableBlockEntity b
                && b.crosses(pipe, Direction.WEST), "and the second the first");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void pipesSoundAsMetal_cablesAsWool(final GameTestHelper helper) {
        final BlockPos pipeAt = new BlockPos(1, 2, 1);
        final BlockPos cableAt = new BlockPos(3, 2, 1);
        CableItem.lay(helper.getLevel(), helper.absolutePos(pipeAt), SharedPipes.COPPER_PIPE.get());
        CableItem.lay(helper.getLevel(), helper.absolutePos(cableAt), SharedEnergy.INSULATED_COPPER_CABLE.get());
        helper.assertTrue(soundAt(helper, pipeAt) == SoundType.METAL, "a copper pipe's block sounds as metal");
        helper.assertTrue(soundAt(helper, cableAt) == SoundType.WOOL, "a copper cable's block sounds as wool");
        final List<CableEntry> pipes = List.of(SharedPipes.COPPER_PIPE, SharedPipes.IRON_GAS_PIPE,
                SharedPipes.VACUUM_JACKETED_PIPE);
        final List<String> notMetal = pipes.stream().filter(pipe -> pipe.get().sound() != SoundType.METAL)
                .map(pipe -> String.valueOf(pipe.id())).toList();
        helper.assertTrue(notMetal.isEmpty(), "every pipe of the series sounds as metal: " + notMetal);
        helper.succeed();
    }

    /* A player standing beside the arena holding a stack of copper pipe. */
    private static FakePlayer player(final GameTestHelper helper, final String name) {
        final FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(
                name.getBytes(StandardCharsets.UTF_8)), name));
        final BlockPos stand = helper.absolutePos(GROUND).north(2).above();
        player.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, SharedPipes.COPPER_PIPE.stack(STACK));
        return player;
    }

    private static SoundType soundAt(final GameTestHelper helper, final BlockPos relative) {
        return helper.getBlockState(relative).getSoundType(helper.getLevel(), helper.absolutePos(relative), null);
    }

    /* A click on the top of the block at {@code relative}, a snow layer's top being two pixels up. */
    private static BlockHitResult topOf(final GameTestHelper helper, final BlockPos relative) {
        final BlockPos pos = helper.absolutePos(relative);
        return new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 2.0 / 16.0, pos.getZ() + 0.5), Direction.UP,
                pos, false);
    }
}
