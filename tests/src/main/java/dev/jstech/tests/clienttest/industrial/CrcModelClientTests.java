/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.industrial;

import dev.jstech.core.cable.CableItem;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.industrial.crc.CrcBlock;
import dev.jstech.industrial.crc.CrcBlockEntity;
import dev.jstech.industrial.crc.CrcContent;
import dev.jstech.industrial.crc.CrcStructure;
import dev.jstech.industrial.research.ResearchLink;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Computational Research Centre's formed model on a player's game: each entry drawn in the cell of the rear it
 * stands in, west where it stands west, read off the screen; and the model seen from behind, along its foot and from
 * its front, with cables run to its entries, to set beside what a player sees.
 */
public final class CrcModelClientTests {

    /* The terminal in the middle layer, the body running south of it, its rear face at z 11. */
    private static final BlockPos TERMINAL = new BlockPos(0, 3, 2);
    private static final BlockPos BEHIND = new BlockPos(0, 2, 14);
    private static final int WAIT = 200;
    /*
     * The middle of an entry's connection, from the middle of its rear block's outer face: the cells of the two side
     * columns stand two and a half sixteenths in from their block's middle, clear of the side wall, and the bottom
     * row's three above it, clear of the plinth.
     */
    private static final double SIDE_IN = 2.5 / 16.0;
    private static final double BOTTOM_UP = 3.0 / 16.0;
    /* How dark the cable gland's black body is on the screen, and how bright the bare sheet of a cell, at most and at
     * least, of the three channels' sum. */
    private static final int DARK = 200;
    private static final int BRIGHT = 330;

    private CrcModelClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void formedModel_drawsEachEntryWhereItStandsAndMeetsItsCables(final ClientTestContext ctx) {
        final int[] seen = new int[2];
        ctx.afterTest(() -> ctx.mc().options.hideGui = false);
        ctx.thenTeleport(0, BEHIND, Direction.NORTH)
                .thenServer(0, level -> build(ctx, level))
                .thenWaitUntilServer(level -> crc(ctx, level).formed(), WAIT, "the CRC to form",
                        level -> String.valueOf(crc(ctx, level).structure().failure()))
                .thenWaitUntil(() -> formedOnClient(ctx, rear(ctx, 0, 0)) && formedOnClient(ctx, rear(ctx, 2, 1)),
                        WAIT, "its entries to be drawn by the formed model")
                .then(0, () -> ctx.mc().options.hideGui = true)
                .then(0, () -> ctx.lookAt(connection(ctx, 0, 0)))
                .then(3, () -> seen[0] = brightness(ctx.pixel(centreX(ctx), centreY(ctx))))
                .then(0, () -> ctx.lookAt(connection(ctx, 2, 0)))
                .then(3, () -> seen[1] = brightness(ctx.pixel(centreX(ctx), centreY(ctx))))
                .thenAssert(0, () -> seen[0] <= DARK && seen[1] >= BRIGHT, "the power entry, standing in the west"
                        + " cell, is drawn there, the gland's black at its middle, and the east cell bare: "
                        + seen[0] + " against " + seen[1])
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(TERMINAL.offset(0, 0, 9)))))
                .thenScreenshot(3, "crc-rear")
                .thenTeleport(0, new BlockPos(-3, 2, 13), Direction.NORTH)
                .then(1, () -> ctx.lookAt(connection(ctx, 0, 0)))
                .thenScreenshot(3, "crc-power-cable")
                .thenTeleport(0, new BlockPos(4, 2, 7), Direction.WEST)
                .then(1, () -> ctx.lookAt(new Vec3(ctx.abs(TERMINAL).getX() + 2.0, ctx.abs(TERMINAL).getY() - 0.95,
                        ctx.abs(TERMINAL).getZ() + 4.5)))
                .thenScreenshot(3, "crc-base")
                .thenTeleport(0, new BlockPos(-3, 2, 9), Direction.EAST)
                .then(1, () -> ctx.lookAt(new Vec3(ctx.abs(TERMINAL).getX() - 1.0, ctx.abs(TERMINAL).getY() - 0.98,
                        ctx.abs(TERMINAL).getZ() + 8.5)))
                .thenScreenshot(3, "crc-base-doors")
                .thenTeleport(0, new BlockPos(1, 2, -2), Direction.SOUTH)
                .then(1, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(TERMINAL.below()))))
                .thenScreenshot(3, "crc-front")
                .then(0, () -> ctx.mc().options.hideGui = false)
                .thenServer(0, level -> clear(ctx, level));
    }

    /*
     * The CRC facing north, its Power Entry in the rear's west bottom place and its Link Socket in the east upper
     * one, an energy cable and a Research Link Cable run up to them from behind.
     */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        final MultiblockPattern pattern = CrcStructure.PATTERN;
        for (int y = 0; y < pattern.sizeY(); y++) {
            for (int z = 0; z < pattern.sizeZ(); z++) {
                for (int x = 0; x < pattern.sizeX(); x++) {
                    final BlockPos at = ctx.abs(TERMINAL.offset(x - pattern.terminalX(), y - pattern.terminalY(),
                            z - pattern.terminalZ()));
                    final char letter = pattern.charAt(x, y, z);
                    final BlockState state = letter == '#' ? CrcContent.TERMINAL.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                            : letter == 'G' ? CrcContent.GATE.get().defaultBlockState()
                            : letter == 'B' ? CrcContent.BUS.get().defaultBlockState()
                            : CrcContent.CASING.get().defaultBlockState();
                    level.setBlockAndUpdate(at, state);
                }
            }
        }
        level.setBlockAndUpdate(rear(ctx, 0, 0), CrcContent.POWER_ENTRY.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(rear(ctx, 2, 1), CrcContent.LINK_SOCKET.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        for (int back = 1; back <= 2; back++) {
            CableItem.lay(level, rear(ctx, 0, 0).south(back), SharedEnergy.INSULATED_COPPER_CABLE.get());
            CableItem.lay(level, rear(ctx, 2, 1).south(back), ResearchLink.CABLE.get());
        }
    }

    /* Takes the CRC and its cables away. */
    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        final MultiblockPattern pattern = CrcStructure.PATTERN;
        final BlockPos corner = TERMINAL.offset(-pattern.terminalX(), -pattern.terminalY(), -pattern.terminalZ());
        for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(corner), ctx.abs(corner.offset(pattern.sizeX() - 1,
                pattern.sizeY() - 1, pattern.sizeZ() + 1)))) {
            level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
        }
    }

    /* The rear place of {@code column} (0 west) and {@code row} (0 bottom), in the world. */
    private static BlockPos rear(final ClientTestContext ctx, final int column, final int row) {
        return CrcStructure.rearPlace(ctx.abs(TERMINAL), Orientation.NORTH, column, row);
    }

    /* The middle of the connection of an entry standing in the rear place of {@code column} and {@code row}. */
    private static Vec3 connection(final ClientTestContext ctx, final int column, final int row) {
        final BlockPos place = rear(ctx, column, row);
        final double across = column == 0 ? SIDE_IN : column == 2 ? -SIDE_IN : 0.0;
        final double up = row == 0 ? BOTTOM_UP : 0.0;
        return new Vec3(place.getX() + 0.5 + across, place.getY() + 0.5 + up, place.getZ() + 1.0);
    }

    private static boolean formedOnClient(final ClientTestContext ctx, final BlockPos at) {
        final BlockState state = ctx.mc().level == null ? null : ctx.mc().level.getBlockState(at);
        return state != null && state.hasProperty(CrcBlock.FORMED) && state.getValue(CrcBlock.FORMED);
    }

    private static CrcBlockEntity crc(final ClientTestContext ctx, final ServerLevel level) {
        if (!(level.getBlockEntity(ctx.abs(TERMINAL)) instanceof CrcBlockEntity crc)) {
            throw new ClientTestFailure("no CRC terminal at " + TERMINAL);
        }
        return crc;
    }

    private static int centreX(final ClientTestContext ctx) {
        return ctx.mc().getWindow().getGuiScaledWidth() / 2;
    }

    private static int centreY(final ClientTestContext ctx) {
        return ctx.mc().getWindow().getGuiScaledHeight() / 2;
    }

    /* The sum of an ARGB colour's red, green and blue. */
    private static int brightness(final int argb) {
        return (argb >> 16 & 0xFF) + (argb >> 8 & 0xFF) + (argb & 0xFF);
    }
}
