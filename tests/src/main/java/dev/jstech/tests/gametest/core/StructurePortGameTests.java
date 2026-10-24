/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.machine.ComparatorReads;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.multiblock.MultiblockPortBlockEntity;
import dev.jstech.core.multiblock.RedstonePortBlock;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestStructures;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Ports a player places on a multiblock's shell: ports of one colour feed the lane of their colour and never another,
 * energy goes in through its own port, a dye gives a port its colour and takes it off, a redstone port reads the
 * signal round it for the terminal or gives the terminal's signal out, and the structure runs as many recipes at
 * once as it says.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class StructurePortGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos LOW = new BlockPos(1, 1, 1);
    private static final BlockPos HIGH = new BlockPos(3, 3, 3);
    private static final BlockPos TERMINAL = new BlockPos(2, 2, 1);
    private static final BlockPos WEST = new BlockPos(1, 2, 2);
    private static final BlockPos EAST = new BlockPos(3, 2, 2);
    private static final BlockPos TOP = new BlockPos(2, 3, 2);
    private static final BlockPos BOTTOM = new BlockPos(2, 1, 2);
    private static final BlockPos SOUTH = new BlockPos(2, 2, 3);

    private StructurePortGameTests() {
    }

    @GameTest(template = ARENA)
    public static void ports_feedTheLaneOfTheirColourAndNoOther(final GameTestHelper helper) {
        build(helper, TestStructures.ENERGY_INPUT.get());
        helper.setBlock(WEST, TestStructures.ITEM_INPUT.get());
        helper.setBlock(EAST, TestStructures.ITEM_OUTPUT.get());
        helper.setBlock(TOP, TestStructures.ITEM_INPUT.get());
        helper.setBlock(BOTTOM, TestStructures.ITEM_OUTPUT.get());
        colour(helper, WEST, DyeColor.RED);
        colour(helper, EAST, DyeColor.RED);
        colour(helper, TOP, DyeColor.BLUE);
        colour(helper, BOTTOM, DyeColor.BLUE);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    final MachineStructure structure = smelter(helper).structure();
                    helper.assertTrue(structure.formed() && structure.groups() == 3, "the smelter forms with three"
                            + " groups: no colour, blue and red; got " + structure.groups());
                    helper.assertTrue(structure.groupOf(helper.absolutePos(WEST)) == 2
                            && structure.groupOf(helper.absolutePos(TOP)) == 1
                            && structure.groupOf(helper.absolutePos(SOUTH)) == 0, "each port is in its colour's group");
                })
                .thenExecute(() -> {
                    final IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,
                            helper.absolutePos(SOUTH), Direction.SOUTH);
                    helper.assertTrue(energy != null && energy.receiveEnergy(5_000, false) == 5_000,
                            "energy goes in through the energy port");
                    final IItemHandler red = items(helper, WEST, Direction.WEST);
                    helper.assertTrue(red != null && red.insertItem(0, new ItemStack(Items.COBBLESTONE, 4), false)
                            .isEmpty(), "the red input takes cobblestone");
                    helper.assertTrue(smelter(helper).getInventory().getStackInSlot(TestStructures.SMELTER_LAYOUT
                            .laneInput(2, 0)).is(Items.COBBLESTONE), "into the red lane");
                    helper.assertTrue(smelter(helper).recipesAtOnce() == 2, "it runs two at once; got "
                            + smelter(helper).recipesAtOnce());
                })
                .thenWaitUntil(() -> {
                    final IItemHandler redOut = items(helper, EAST, Direction.EAST);
                    helper.assertTrue(redOut != null && redOut.extractItem(0, 64, true).is(Items.STONE),
                            "the stone comes out of the red output");
                })
                .thenExecute(() -> {
                    final IItemHandler blueOut = items(helper, BOTTOM, Direction.DOWN);
                    helper.assertTrue(blueOut != null && blueOut.extractItem(0, 64, true).isEmpty(),
                            "and none out of the blue one");
                    final IItemHandler redIn = items(helper, WEST, Direction.WEST);
                    helper.assertTrue(redIn.extractItem(0, 1, true).isEmpty(), "an input gives nothing out");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void dye_givesAPortItsColourAndTakesItOff(final GameTestHelper helper) {
        build(helper, TestStructures.ITEM_INPUT.get());
        final FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(
                "port_dyer".getBytes(StandardCharsets.UTF_8)), "port_dyer"));
        final BlockPos at = helper.absolutePos(SOUTH);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_DYE));
        final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.SOUTH, at, false);
        final BlockState state = helper.getLevel().getBlockState(at);
        state.useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        final MultiblockPortBlockEntity port = (MultiblockPortBlockEntity) helper.getBlockEntity(SOUTH);
        helper.assertTrue(port.colour() == DyeColor.RED, "red dye makes the port red; got " + port.colour());
        final List<Text> lines = new ArrayList<>();
        port.describe(lines, false);
        helper.assertTrue(lines.stream().anyMatch(line -> line.english().equals("Port: Items in"))
                && lines.stream().anyMatch(line -> line.english().equals("Group: Red")),
                "whoever looks at it is told its kind and its group; got " + lines);
        state.useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(port.colour() == null, "the same dye again takes the colour off");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void redstonePort_readsTheSignalRoundItForTheTerminal(final GameTestHelper helper) {
        build(helper, TestStructures.REDSTONE.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(smelter(helper).structure().formed(), "the smelter forms"))
                .thenExecute(() -> helper.setBlock(SOUTH.south(), Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> helper.assertTrue(smelter(helper).control().signal() == 15,
                        "the terminal hears the redstone beside its port; got " + smelter(helper).control()
                                .signal()))
                .thenExecute(() -> helper.setBlock(SOUTH.south(), Blocks.AIR))
                .thenWaitUntil(() -> helper.assertTrue(smelter(helper).control().signal() == 0,
                        "and stops hearing it once it goes"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void redstonePort_givesTheTerminalsSignalOut(final GameTestHelper helper) {
        build(helper, TestStructures.REDSTONE.get());
        helper.setBlock(SOUTH, TestStructures.REDSTONE.get().defaultBlockState().setValue(RedstonePortBlock.GIVES,
                true));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(smelter(helper).structure().formed(), "the smelter forms"))
                .thenExecute(() -> {
                    smelter(helper).control().setReads(ComparatorReads.ENERGY);
                    smelter(helper).getEnergy().receiveEnergy(10_000, false);
                })
                .thenWaitUntil(() -> {
                    final int given = helper.getLevel().getSignal(helper.absolutePos(SOUTH), Direction.NORTH);
                    helper.assertTrue(given > 0 && given == smelter(helper).strength(),
                            "the port gives what a comparator reads of the full terminal; got " + given);
                })
                .thenSucceed();
    }

    /* The smelter: iron on its edges, glass on its faces, the terminal north and {@code south} on its south. */
    private static void build(final GameTestHelper helper, final Block south) {
        for (int x = LOW.getX(); x <= HIGH.getX(); x++) {
            for (int y = LOW.getY(); y <= HIGH.getY(); y++) {
                for (int z = LOW.getZ(); z <= HIGH.getZ(); z++) {
                    final int bounds = (x == LOW.getX() || x == HIGH.getX() ? 1 : 0)
                            + (y == LOW.getY() || y == HIGH.getY() ? 1 : 0)
                            + (z == LOW.getZ() || z == HIGH.getZ() ? 1 : 0);
                    final BlockPos at = new BlockPos(x, y, z);
                    if (bounds >= 2) {
                        helper.setBlock(at, Blocks.IRON_BLOCK);
                    } else if (bounds == 1) {
                        helper.setBlock(at, Blocks.GLASS);
                    }
                }
            }
        }
        helper.setBlock(SOUTH, south);
        helper.setBlock(TERMINAL, TestStructures.SMELTER.get());
    }

    private static void colour(final GameTestHelper helper, final BlockPos at, final DyeColor colour) {
        ((MultiblockPortBlockEntity) helper.getBlockEntity(at)).setColour(colour);
    }

    private static IItemHandler items(final GameTestHelper helper, final BlockPos at, final Direction side) {
        return helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(at), side);
    }

    private static TestStructures.SmelterBlockEntity smelter(final GameTestHelper helper) {
        if (helper.getBlockEntity(TERMINAL) instanceof TestStructures.SmelterBlockEntity smelter) {
            return smelter;
        }
        throw new IllegalStateException("no smelter terminal where the test put it");
    }
}
