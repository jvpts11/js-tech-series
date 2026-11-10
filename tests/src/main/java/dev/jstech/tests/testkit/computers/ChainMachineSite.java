/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit.computers;

import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.computers.chain.machine.ChainMachines;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A chain machine stood up in a test's arena (computers/docs/design/survival.md): its blocks laid as its shape has
 * them, its front to the north and its front west corner at {@link #CORNER}, and the Machine Terminal made its own.
 */
public final class ChainMachineSite {

    /** Where a machine's front west corner stands, its front to the north. */
    public static final BlockPos CORNER = new BlockPos(2, 1, 2);
    /* The most any machine stands, every way, which clearing the arena covers. */
    private static final int LONGEST = 4;

    private ChainMachineSite() {
    }

    /** The machine of {@code id}. */
    public static ChainMachine machine(final String id) {
        return ChainMachines.byId(id).orElseThrow();
    }

    /** Builds {@code machine} and makes its Machine Terminal its own; its terminal's place. */
    public static BlockPos formed(final GameTestHelper helper, final ChainMachine machine) {
        final BlockPos terminal = build(helper, machine);
        helper.assertTrue(ChainMachines.formAt(helper.getLevel(), helper.absolutePos(terminal), Direction.NORTH),
                machine.id() + " forms");
        return terminal;
    }

    /** Lays {@code machine}'s blocks at {@link #CORNER} facing north, the Machine Terminal at its place; that place. */
    public static BlockPos build(final GameTestHelper helper, final ChainMachine machine) {
        BlockPos terminal = CORNER;
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final String row = layers.get(y).get(z);
                for (int x = 0; x < row.length(); x++) {
                    final BlockPos at = CORNER.offset(x, y, z);
                    final char letter = row.charAt(x);
                    if (letter == '#') {
                        terminal = at;
                        helper.setBlock(at, ChainMachineBlocks.TERMINAL.get().defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    } else {
                        final Block block = ChainMachineBlocks.byId(machine.legend().get(letter)).get();
                        final BlockState state = block.defaultBlockState();
                        helper.setBlock(at, state.hasProperty(HorizontalDirectionalBlock.FACING)
                                ? state.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH) : state);
                    }
                }
            }
        }
        return terminal;
    }

    /** The place of the first block of {@code id} in {@code machine}'s shape, as {@link #build} lays it. */
    public static BlockPos place(final ChainMachine machine, final String id) {
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final String row = layers.get(y).get(z);
                for (int x = 0; x < row.length(); x++) {
                    if (id.equals(machine.legend().get(row.charAt(x)))) {
                        return CORNER.offset(x, y, z);
                    }
                }
            }
        }
        throw new IllegalArgumentException(machine.id() + " has no " + id);
    }

    /** Clears the arena where a machine stood. */
    public static void clear(final GameTestHelper helper) {
        for (int y = 0; y < LONGEST; y++) {
            for (int z = 0; z < LONGEST; z++) {
                for (int x = 0; x < LONGEST; x++) {
                    helper.setBlock(CORNER.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    /** The machine whose terminal is at {@code terminal}. */
    public static ChainMachineBlockEntity machineAt(final GameTestHelper helper, final BlockPos terminal) {
        return (ChainMachineBlockEntity) helper.getBlockEntity(terminal);
    }

    /** Whether the machine's outputs hold at least {@code count} of {@code item}. */
    public static boolean holds(final ChainMachineBlockEntity machine, final Item item, final int count) {
        return made(machine, item).getCount() >= count;
    }

    /** What the machine's outputs hold of {@code item}, all of it; empty for none. */
    public static ItemStack made(final ChainMachineBlockEntity machine, final Item item) {
        int found = 0;
        ItemStack first = ItemStack.EMPTY;
        for (int slot = 0; slot < machine.getInventory().getSlots(); slot++) {
            final ItemStack stack = machine.getInventory().getStackInSlot(slot);
            if (stack.is(item) && machine.layout().isOutput(slot)) {
                found += stack.getCount();
                first = first.isEmpty() ? stack : first;
            }
        }
        return found == 0 ? ItemStack.EMPTY : first.copyWithCount(found);
    }
}
