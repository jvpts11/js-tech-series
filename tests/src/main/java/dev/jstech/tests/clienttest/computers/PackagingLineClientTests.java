/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.computers.chain.machine.WaferWork;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The Packaging Line at work as its approved model draws it: the strips ride its belt left to right and the cleats
 * across the belt ride with them, every one moving on through the loop and none sliding back where the loop starts
 * again. The shots follow one two-second loop, from before the belt and close over it.
 */
public final class PackagingLineClientTests {

    private static final int SETTLE = 4;
    private static final int WAIT = 400;
    /* One shot every quarter second through the line's two-second loop. */
    private static final int SHOTS = 8;
    private static final int APART = 5;
    private static final BlockPos CORNER = new BlockPos(2, 2, 4);
    /* Two blocks before the line's middle, looking down at its belt. */
    private static final BlockPos IN_FRONT = CORNER.offset(1, 0, -2);
    /* The belt's middle, in blocks from the line's corner: along it, at the bench's top, near the front. */
    private static final Vec3 BELT = new Vec3(1.5, 0.95, 0.4);
    private static final int CHIPS = 8;
    private static final int LAYERS = 2;

    private PackagingLineClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void belt_carriesTheStripsAndItsCleatsOnThroughTheLoop(final ClientTestContext ctx) {
        final ChainMachine machine = ChainMachines.byId("packaging_line").orElseThrow();
        final BlockPos terminal = terminalOf(machine);
        ctx.thenServer(0, level -> ctx.serverPlayer().getInventory().clearContent())
                .thenBuild(0, world -> build(world::setBlock, machine))
                .thenWaitUntilServer(level -> line(ctx, level, terminal).formed(), SETTLE * 30,
                        "the Packaging Line to form", level -> "it stands unformed")
                .thenServer(SETTLE, level -> feed(line(ctx, level, terminal).getInventory()))
                .thenWaitUntilServer(level -> line(ctx, level, terminal).status() == MachineStatus.WORKING, WAIT,
                        "the line to package the wafers", level -> "status " + line(ctx, level, terminal).status())
                .thenTeleport(SETTLE, IN_FRONT, Direction.SOUTH)
                .then(SETTLE, () -> ctx.lookAt(Vec3.atLowerCornerOf(ctx.abs(CORNER)).add(BELT)))
                .then(SETTLE * 5, () -> ctx.mc().getToasts().clear());
        for (int shot = 0; shot < SHOTS; shot++) {
            ctx.thenScreenshot(shot == 0 ? 1 : APART, "packaging-belt-" + shot);
        }
    }

    /* Two finished memory wafers of the Vintage with the packages and the bonding wire their chips take. */
    private static void feed(final IItemHandler in) {
        final ItemStack finished = new ItemStack(ChainParts.FINISHED_WAFERS.getFirst().get(), 2);
        finished.set(ComputingComponents.WAFER_WORK.get(), new WaferWork(BuiltInRegistries.ITEM.getKey(
                ChainParts.MEMORY_CHIPS.getFirst().get()), LAYERS, LAYERS, WaferWork.Step.PACKAGING));
        in.insertItem(0, finished, false);
        in.insertItem(1, new ItemStack(ChainParts.CERAMIC_PACKAGE.get(), CHIPS), false);
        in.insertItem(2, new ItemStack(ChainParts.ALUMINIUM_BONDING_WIRE.get(), 4 * CHIPS), false);
        in.insertItem(3, new ItemStack(ChainParts.CERAMIC_PACKAGE.get(), CHIPS), false);
    }

    /* Lays {@code machine}'s blocks from {@link #CORNER} facing north, its Machine Terminal at its place. */
    private static void build(final Placer world, final ChainMachine machine) {
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final String row = layers.get(y).get(z);
                for (int x = 0; x < row.length(); x++) {
                    final char letter = row.charAt(x);
                    final Block block = letter == '#' ? ChainMachines.terminalOf(machine).get()
                            : ChainMachineBlocks.byId(machine.legend().get(letter)).get();
                    final BlockState state = block.defaultBlockState();
                    world.place(CORNER.offset(x, y, z), state.hasProperty(HorizontalDirectionalBlock.FACING)
                            ? state.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH) : state);
                }
            }
        }
    }

    /* Where {@link #build} lays the Machine Terminal of {@code machine}. */
    private static BlockPos terminalOf(final ChainMachine machine) {
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final int x = layers.get(y).get(z).indexOf('#');
                if (x >= 0) {
                    return CORNER.offset(x, y, z);
                }
            }
        }
        throw new IllegalArgumentException(machine.id() + " has no terminal");
    }

    private static ChainMachineBlockEntity line(final ClientTestContext ctx, final ServerLevel level,
                                                final BlockPos terminal) {
        return (ChainMachineBlockEntity) level.getBlockEntity(ctx.abs(terminal));
    }

    /* Where the machine's blocks are set: the test's builder. */
    @FunctionalInterface
    private interface Placer {
        void place(BlockPos at, BlockState state);
    }
}
