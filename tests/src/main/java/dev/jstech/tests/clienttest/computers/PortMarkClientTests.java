/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.computers.chain.machine.PortMark;
import dev.jstech.computers.client.chain.PortMarkPainter;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A machine's ports bound to one product wear their pipe markings as a player sees them: the Siemens Reactor's two gas
 * inlets, each with a band in its gas's cylinder colour, its formula and an arrow into the machine.
 */
public final class PortMarkClientTests {

    private static final int SETTLE = 4;
    private static final int WAIT = 200;
    private static final BlockPos CORNER = new BlockPos(4, 2, 4);
    /* How far in front of a marked face the player stands, and the band's height above the face's foot. */
    private static final double BACK = 2.2;
    private static final double BAND = 2.6 / 16.0;
    private static final int SHOTS = 2;

    private PortMarkClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void siemens_inletsWearTheirGasesMarkings(final ClientTestContext ctx) {
        final ChainMachine machine = ChainMachines.byId("siemens_reactor").orElseThrow();
        final BlockPos terminal = terminalOf(machine);
        ctx.thenBuild(0, world -> build(world::setBlock, machine))
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(terminal))
                                instanceof ChainMachineBlockEntity reactor && reactor.formed(), WAIT,
                        "the Siemens Reactor to form", level -> "it stands unformed")
                .thenWaitUntil(() -> marks(ctx, terminal).size() >= SHOTS, WAIT,
                        "the reactor's markings to reach the client")
                .thenAssert(0, () -> marks(ctx, terminal).stream().map(PortMark::pos).distinct().count()
                        == marks(ctx, terminal).size(), "one marking for each inlet, on the face its model shows");
        for (int shot = 0; shot < SHOTS; shot++) {
            final int index = shot;
            ctx.thenServer(SETTLE, level -> stand(ctx, marks(ctx, terminal).get(index)))
                    .thenScreenshot(SETTLE * 4, "siemens-inlet-" + shot);
        }
    }

    /* Stands the player before {@code mark}'s face, looking at its band. */
    private static void stand(final ClientTestContext ctx, final PortMark mark) {
        final Vec3 normal = Vec3.atLowerCornerOf(mark.face().getNormal());
        final Vec3 port = Vec3.atCenterOf(BlockPos.of(mark.pos()));
        final Vec3 band = port.add(normal.scale(0.5)).add(0.0, BAND - 0.5, 0.0);
        // On the ground before it, whichever layer of the machine the port stands in.
        final Vec3 before = port.add(normal.scale(BACK));
        final Vec3 feet = new Vec3(before.x, ctx.abs(CORNER).getY(), before.z);
        final Vec3 look = band.subtract(feet.add(0.0, ctx.serverPlayer().getEyeHeight(), 0.0));
        final float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        final float pitch = (float) -Math.toDegrees(Math.atan2(look.y, look.horizontalDistance()));
        ctx.serverPlayer().teleportTo(ctx.serverLevel(), feet.x, feet.y, feet.z, yaw, pitch);
    }

    /* The markings the client's reactor draws. */
    private static List<PortMark> marks(final ClientTestContext ctx, final BlockPos terminal) {
        return ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(terminal))
                instanceof ChainMachineBlockEntity reactor ? PortMarkPainter.shown(reactor, ctx.mc().level)
                : List.of();
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

    /* Where the machine's blocks are set: the test's builder. */
    @FunctionalInterface
    private interface Placer {
        void place(BlockPos at, BlockState state);
    }
}
