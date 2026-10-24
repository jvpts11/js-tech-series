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
import dev.jstech.core.cable.CableItem;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.CoreCables;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Laying a cable by hand: on a block that gives way, a snow layer, it is laid in its place and never above it; a new
 * cable joins the one beside it the moment it is laid; and each cable sounds as what it is made of.
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
    public static void cables_soundAsWhatTheyAreMadeOf(final GameTestHelper helper) {
        final CableType pipe = SharedPipes.COPPER_PIPE.get();
        final BlockPos at = new BlockPos(1, 2, 1);
        CableItem.lay(helper.getLevel(), helper.absolutePos(at), pipe);
        final SoundType sound = helper.getBlockState(at).getSoundType(helper.getLevel(), helper.absolutePos(at),
                null);
        helper.assertTrue(pipe.sound() == SoundType.METAL && sound == SoundType.METAL,
                "a copper pipe sounds as metal, laid and in its block");
        final List<String> wool = CoreCables.REGISTRY.stream().filter(type -> type.sound() == SoundType.WOOL)
                .map(type -> String.valueOf(type.id())).toList();
        helper.assertTrue(wool.isEmpty(), "no cable sounds as wool: " + wool);
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

    /* A click on the top of the block at {@code relative}, a snow layer's top being two pixels up. */
    private static BlockHitResult topOf(final GameTestHelper helper, final BlockPos relative) {
        final BlockPos pos = helper.absolutePos(relative);
        return new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 2.0 / 16.0, pos.getZ() + 0.5), Direction.UP,
                pos, false);
    }
}
