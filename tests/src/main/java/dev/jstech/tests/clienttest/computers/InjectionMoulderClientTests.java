/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.chain.BoardParts;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.core.client.machine.MachineScreen;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialUses;
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
import org.lwjgl.glfw.GLFW;

/**
 * The Injection Moulder shows what it moulds (computers/docs/design/survival.md): moulding the Socket 7's body from
 * PVC, the part it ejects and carries out on its belt is that body, both halves of its mould show that body's mould,
 * its hopper's pellets are the PVC's colour, and its screen's face shows the same body leaving the mould.
 */
public final class InjectionMoulderClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    private static final int WAIT = 400;
    /* Shots through one loop of the moulder's four seconds, the part in the mould, ejected, dropping, carried. */
    private static final int SHOTS = 8;
    private static final int APART = 10;
    private static final int MOULD_SHOTS = 12;
    private static final int MOULD_APART = 7;
    private static final BlockPos CORNER = new BlockPos(2, 2, 4);
    /* Before the machine, west of its tower, looking down at the mould and the belt under it. */
    private static final BlockPos IN_FRONT = CORNER.offset(1, 0, -2);
    /* West of the mould and before it, and the middle of the mould, in blocks from the machine's corner. */
    private static final BlockPos INTO_THE_GAP = CORNER.offset(0, 0, -2);
    private static final Vec3 MOULD = new Vec3(1.6, 1.4, 1.2);
    private static final int PLASTIC = 64;

    private InjectionMoulderClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void moulder_showsTheBodyOfItsRecipe(final ClientTestContext ctx) {
        final ChainMachine machine = ChainMachines.byId("injection_moulder").orElseThrow();
        final BlockPos terminal = terminalOf(machine);
        final BoardParts.Component socket7 = BoardParts.byPath("socket_socket_7");
        ctx.thenServer(0, level -> ctx.serverPlayer().getInventory().clearContent())
                .thenBuild(0, world -> build(world::setBlock, machine))
                .thenWaitUntilServer(level -> moulder(ctx, level, terminal).formed(), SETTLE * 30,
                        "the Injection Moulder to form", level -> "it stands unformed")
                .thenServer(SETTLE, level -> {
                    final ChainMachineBlockEntity moulder = moulder(ctx, level, terminal);
                    moulder.getInventory().insertItem(0, new ItemStack(socket7.mould().get()), false);
                    moulder.getInventory().insertItem(1, new ItemStack(MaterialUses.item(SeriesCompounds.PVC,
                            Forms.SHEET).get(), PLASTIC), false);
                })
                .thenWaitUntilServer(level -> moulder(ctx, level, terminal).status() == MachineStatus.WORKING
                                && moulder(ctx, level, terminal).makes().contains(BuiltInRegistries.ITEM.getKey(
                                socket7.body().get())), WAIT,
                        "the moulder to mould the Socket 7's body", level -> "status "
                                + moulder(ctx, level, terminal).status() + ", makes "
                                + moulder(ctx, level, terminal).makes())
                .thenTeleport(SETTLE, IN_FRONT, Direction.SOUTH)
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(CORNER.offset(1, 1, 0))).add(0, -0.4, 0)))
                .then(SETTLE * 5, () -> ctx.mc().getToasts().clear());
        for (int shot = 0; shot < SHOTS; shot++) {
            ctx.thenScreenshot(shot == 0 ? 1 : APART, "moulder-world-" + shot);
        }
        // Into the mould's gap from the front and the west, where the moving half's face and the part on it show.
        ctx.thenTeleport(SETTLE, INTO_THE_GAP, Direction.SOUTH)
                .then(SETTLE, () -> ctx.lookAt(Vec3.atLowerCornerOf(ctx.abs(CORNER)).add(MOULD)));
        // Closer together than its opening lasts, so one of them catches the part on the moving half.
        for (int shot = 0; shot < MOULD_SHOTS; shot++) {
            ctx.thenScreenshot(shot == 0 ? SETTLE : MOULD_APART, "moulder-mould-" + shot);
        }
        ctx.thenTeleport(SETTLE, terminal.north(2), Direction.SOUTH)
                .thenRightClick(SETTLE, terminal)
                .thenAwaitScreen(MachineScreen.class, SCREEN_WAIT)
                .then(SETTLE, () -> ctx.mc().getToasts().clear());
        for (int shot = 0; shot < SHOTS; shot++) {
            ctx.thenScreenshot(shot == 0 ? SETTLE : APART, "moulder-face-" + shot);
        }
        ctx.then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
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

    private static ChainMachineBlockEntity moulder(final ClientTestContext ctx, final ServerLevel level,
                                                   final BlockPos terminal) {
        return (ChainMachineBlockEntity) level.getBlockEntity(ctx.abs(terminal));
    }

    /* Where the machine's blocks are set: the test's builder. */
    @FunctionalInterface
    private interface Placer {
        void place(BlockPos at, BlockState state);
    }
}
