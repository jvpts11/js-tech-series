/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.space.geology.SpaceRocks;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The ground of the Moon and of Mars: its rocks, and the water ice of Mars' caps hard as rock in their cold, come away
 * whole only under a pickaxe, its loose regoliths and sands dig with a shovel and fall as sand does, and its frost and
 * icy regolith break with anything, giving themselves back.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SpaceRocksGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(1, 1, 1);
    /* Where a loose block is put to fall from, the stone it falls on, and where it lands on it. */
    private static final BlockPos HIGH = new BlockPos(1, 5, 1);
    private static final BlockPos STONE = new BlockPos(1, 1, 1);
    private static final BlockPos LANDED = new BlockPos(1, 2, 1);

    private static final List<BlockEntry<?>> ROCKS = List.of(SpaceRocks.ANORTHOSITE, SpaceRocks.MARE_BASALT,
            SpaceRocks.MARTIAN_BASALT, SpaceRocks.COLUMNAR_MARTIAN_BASALT, SpaceRocks.VESICULAR_MARTIAN_BASALT,
            SpaceRocks.MUDSTONE, SpaceRocks.BUFF_MUDSTONE, SpaceRocks.DUSTED_MUDSTONE, SpaceRocks.WATER_ICE);
    private static final List<BlockEntry<?>> LOOSE = List.of(SpaceRocks.HIGHLAND_REGOLITH, SpaceRocks.MARE_REGOLITH,
            SpaceRocks.MARTIAN_REGOLITH, SpaceRocks.HEMATITE_REGOLITH, SpaceRocks.BASALT_SAND);
    private static final List<BlockEntry<?>> FROZEN = List.of(SpaceRocks.ICY_REGOLITH, SpaceRocks.DRY_ICE);

    private SpaceRocksGameTests() {
    }

    @GameTest(template = ARENA)
    public static void rocks_comeAwayWholeOnlyUnderAPickaxe(final GameTestHelper helper) {
        for (final BlockEntry<?> rock : ROCKS) {
            final BlockState state = rock.get().defaultBlockState();
            helper.assertTrue(state.requiresCorrectToolForDrops(), rock.getId() + " needs its tool to drop");
            helper.assertTrue(dropsOf(helper, state, new ItemStack(Items.WOODEN_PICKAXE)).equals(List.of(rock.item())),
                    rock.getId() + " drops itself under a pickaxe");
            // The game drops a block for a player only when the tool in hand is right for it, so a hand gets nothing.
            helper.assertFalse(ItemStack.EMPTY.isCorrectToolForDrops(state), rock.getId() + " drops nothing by hand");
            helper.assertFalse(new ItemStack(Items.WOODEN_SHOVEL).isCorrectToolForDrops(state), rock.getId()
                    + " nor under a shovel");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void looseGround_digsWithAShovelAndDropsItself(final GameTestHelper helper) {
        final ItemStack shovel = new ItemStack(Items.WOODEN_SHOVEL);
        for (final BlockEntry<?> loose : LOOSE) {
            final BlockState state = loose.get().defaultBlockState();
            helper.assertTrue(shovel.isCorrectToolForDrops(state) || !state.requiresCorrectToolForDrops(),
                    loose.getId() + " comes away under a shovel");
            helper.assertTrue(shovel.getDestroySpeed(state) > 1.0F, loose.getId() + " digs faster with one");
            helper.assertTrue(dropsOf(helper, state, ItemStack.EMPTY).equals(List.of(loose.item())),
                    loose.getId() + " drops itself even by hand");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void frostAndIcyRegolith_breakWithAnythingAndGiveThemselvesBack(final GameTestHelper helper) {
        final ItemStack pickaxe = new ItemStack(Items.WOODEN_PICKAXE);
        for (final BlockEntry<?> ice : FROZEN) {
            final BlockState state = ice.get().defaultBlockState();
            helper.assertFalse(state.requiresCorrectToolForDrops(), ice.getId() + " needs no tool");
            helper.assertTrue(pickaxe.getDestroySpeed(state) > 1.0F, ice.getId() + " breaks faster with a pickaxe");
            helper.assertTrue(dropsOf(helper, state, ItemStack.EMPTY).equals(List.of(ice.item())),
                    ice.getId() + " drops itself by hand");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void regolith_fallsAsSandDoes(final GameTestHelper helper) {
        final Block regolith = SpaceRocks.MARTIAN_REGOLITH.get();
        helper.setBlock(STONE, Blocks.STONE);
        helper.setBlock(HIGH, regolith);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(regolith, LANDED);
            helper.assertBlockPresent(Blocks.AIR, HIGH);
        });
    }

    private static List<?> dropsOf(final GameTestHelper helper, final BlockState state, final ItemStack tool) {
        final BlockPos at = helper.absolutePos(AT);
        return Block.getDrops(state, helper.getLevel(), at, null, null, tool).stream()
                .map(stack -> stack.getCount() == 1 ? stack.getItem() : stack).toList();
    }
}
