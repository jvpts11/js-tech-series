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
import dev.jstech.industrial.crc.CrcPart;
import dev.jstech.industrial.crc.CrcStructure;
import dev.jstech.industrial.research.ResearchLink;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Computational Research Centre's formed model on a player's game: each entry drawn in the cell of the rear it
 * stands in, west where it stands west, read off the screen; and the model seen from behind, along its foot, from its
 * front and from underneath, with cables run to its entries, to set beside what a player sees.
 */
public final class CrcModelClientTests {

    /* The terminal in the middle layer, the body running south of it, its rear face at z 11. */
    private static final BlockPos TERMINAL = new BlockPos(0, 3, 2);
    private static final BlockPos BEHIND = new BlockPos(0, 2, 14);
    /* A second one standing in the air, its foot four blocks up, to be seen from underneath. */
    private static final BlockPos AIRBORNE = new BlockPos(0, 7, 2);
    /* Ticks for the puffs of its blocks merging to clear. */
    private static final int PUFFS_CLEAR = 40;
    /* The places as the approved design draws them, section after section: R a research processor, I an integrated
     * one, S a heat sink, L a liquid cooler. */
    private static final String PLACES = "RSRSRSRSR" + "LILILILIL" + "SRSSRSSRS";
    private static final int WAIT = 200;
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
                .thenServer(0, level -> build(ctx, level, TERMINAL, true))
                .thenWaitUntilServer(level -> crc(ctx, level, TERMINAL).formed(), WAIT, "the CRC to form",
                        level -> String.valueOf(crc(ctx, level, TERMINAL).structure().failure()))
                .thenWaitUntil(() -> formedOnClient(ctx, rear(ctx, TERMINAL, 0, 0))
                        && formedOnClient(ctx, rear(ctx, TERMINAL, 2, 1)), WAIT,
                        "its entries to be drawn by the formed model")
                .then(0, () -> ctx.mc().options.hideGui = true)
                .then(0, () -> ctx.lookAt(connection(ctx, 0, 0)))
                .then(3, () -> seen[0] = brightness(ctx.pixel(centreX(ctx), centreY(ctx))))
                .then(0, () -> ctx.lookAt(connection(ctx, 2, 0)))
                .then(3, () -> seen[1] = brightness(ctx.pixel(centreX(ctx), centreY(ctx))))
                .thenAssert(0, () -> seen[0] <= DARK && seen[1] >= BRIGHT, "the power entry, standing in the west"
                        + " cell, is drawn there, the gland's black at its middle, and the east cell bare: "
                        + seen[0] + " against " + seen[1])
                .thenServer(0, level -> cable(ctx, level))
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
                .thenTeleport(0, new BlockPos(-4, 2, 3), Direction.EAST)
                .then(1, () -> ctx.lookAt(new Vec3(ctx.abs(TERMINAL).getX() - 1.0, ctx.abs(TERMINAL).getY() + 0.3,
                        ctx.abs(TERMINAL).getZ() + 3.5)))
                .thenScreenshot(3, "crc-doors")
                .then(0, () -> ctx.mc().options.hideGui = false)
                .thenServer(0, level -> clear(ctx, level, TERMINAL));
    }

    /* The underside of a CRC standing in the air, seen from the ground beside it, as a player flying under one does. */
    @ClientTest(timeoutTicks = 600)
    public static void formedModel_seenFromUnderneath(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().options.hideGui = false);
        ctx.thenTeleport(0, new BlockPos(3, 2, 3), Direction.SOUTH)
                .thenServer(0, level -> build(ctx, level, AIRBORNE, false))
                .thenWaitUntilServer(level -> crc(ctx, level, AIRBORNE).formed(), WAIT, "the CRC to form",
                        level -> String.valueOf(crc(ctx, level, AIRBORNE).structure().failure()))
                .then(0, () -> ctx.mc().options.hideGui = true)
                .then(PUFFS_CLEAR, () -> ctx.lookAt(new Vec3(ctx.abs(AIRBORNE).getX() + 0.5,
                        ctx.abs(AIRBORNE).getY() - 1.0, ctx.abs(AIRBORNE).getZ() + 5.0)))
                .thenScreenshot(3, "crc-underside")
                .then(0, () -> ctx.mc().options.hideGui = false)
                .thenServer(0, level -> clear(ctx, level, AIRBORNE));
    }

    /*
     * The CRC facing north with its terminal at {@code terminal}, its Power Entry in the rear's west bottom place;
     * with {@code cabled}, its Link Socket in the east upper one too, and an energy cable and a Research Link Cable
     * run up to them from behind.
     */
    private static void build(final ClientTestContext ctx, final ServerLevel level, final BlockPos terminal,
                              final boolean cabled) {
        final MultiblockPattern pattern = CrcStructure.PATTERN;
        for (int y = 0; y < pattern.sizeY(); y++) {
            for (int z = 0; z < pattern.sizeZ(); z++) {
                for (int x = 0; x < pattern.sizeX(); x++) {
                    final BlockPos at = ctx.abs(terminal.offset(x - pattern.terminalX(), y - pattern.terminalY(),
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
        level.setBlockAndUpdate(rear(ctx, terminal, 0, 0), CrcContent.POWER_ENTRY.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        if (!cabled) {
            return;
        }
        // Its places filled as the approved design draws them, so its parts show through the glass.
        final CrcBlockEntity crc = crc(ctx, level, terminal);
        for (int place = 0; place < CrcBlockEntity.PLACES; place++) {
            final CrcPart part = switch (PLACES.charAt(place)) {
                case 'R' -> CrcPart.RESEARCH_PROCESSOR;
                case 'I' -> CrcPart.INTEGRATED_RESEARCH_PROCESSOR;
                case 'L' -> CrcPart.LIQUID_COOLER;
                default -> CrcPart.HEAT_SINK;
            };
            crc.getInventory().setStackInSlot(place, new ItemStack(CrcContent.PARTS.get(part).get()));
        }
        level.setBlockAndUpdate(rear(ctx, terminal, 2, 1), CrcContent.LINK_SOCKET.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
    }

    /*
     * An energy cable and a Research Link Cable run up from behind to the first CRC's Power Entry and Link Socket,
     * laid after its entries were read off the screen, so neither stands between the camera and an entry then.
     */
    private static void cable(final ClientTestContext ctx, final ServerLevel level) {
        for (int back = 1; back <= 2; back++) {
            CableItem.lay(level, rear(ctx, TERMINAL, 0, 0).south(back), SharedEnergy.INSULATED_COPPER_CABLE.get());
            CableItem.lay(level, rear(ctx, TERMINAL, 2, 1).south(back), ResearchLink.CABLE.get());
        }
    }

    /* Takes the CRC whose terminal is at {@code terminal} away, and the cables behind it. */
    private static void clear(final ClientTestContext ctx, final ServerLevel level, final BlockPos terminal) {
        final MultiblockPattern pattern = CrcStructure.PATTERN;
        final BlockPos corner = terminal.offset(-pattern.terminalX(), -pattern.terminalY(), -pattern.terminalZ());
        for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(corner), ctx.abs(corner.offset(pattern.sizeX() - 1,
                pattern.sizeY() - 1, pattern.sizeZ() + 1)))) {
            level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
        }
    }

    /* The rear place of {@code column} (0 west) and {@code row} (0 bottom) of the CRC at {@code terminal}. */
    private static BlockPos rear(final ClientTestContext ctx, final BlockPos terminal, final int column,
                                 final int row) {
        return CrcStructure.rearPlace(ctx.abs(terminal), Orientation.NORTH, column, row);
    }

    /*
     * The middle of the connection of an entry standing in the rear place of {@code column} and {@code row}: the
     * middle of its block's outer face, where a cable laid behind the block runs.
     */
    private static Vec3 connection(final ClientTestContext ctx, final int column, final int row) {
        final BlockPos place = rear(ctx, TERMINAL, column, row);
        return new Vec3(place.getX() + 0.5, place.getY() + 0.5, place.getZ() + 1.0);
    }

    private static boolean formedOnClient(final ClientTestContext ctx, final BlockPos at) {
        final BlockState state = ctx.mc().level == null ? null : ctx.mc().level.getBlockState(at);
        return state != null && state.hasProperty(CrcBlock.FORMED) && state.getValue(CrcBlock.FORMED);
    }

    private static CrcBlockEntity crc(final ClientTestContext ctx, final ServerLevel level, final BlockPos terminal) {
        if (!(level.getBlockEntity(ctx.abs(terminal)) instanceof CrcBlockEntity crc)) {
            throw new ClientTestFailure("no CRC terminal at " + terminal);
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
