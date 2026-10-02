/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.cable.Cables;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Each data line's cables, one era to a row from the Vintage at the front, to set beside the approved pages of the
 * cables: the jacket of each era, and the plug of its era where the row meets a device at either end.
 */
public final class CableLineClientTests {

    private static final BlockPos STAND = new BlockPos(0, 3, -2);
    private static final int FLOOR = 2;
    private static final int FIRST_ROW = 1;
    private static final int LENGTH = 2;
    private static final float LOOK_DOWN = 40.0F;

    private CableLineClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void access_eachEraInItsJacketAndPlug(final ClientTestContext ctx) {
        rows(ctx, "access", List.of(ComputingModule.THIN_COAX_CABLE, ComputingModule.ETHERNET_CABLE,
                ComputingModule.CAT5E_CABLE, ComputingModule.GIGABIT_CABLE, ComputingModule.CAT6A_CABLE));
    }

    @ClientTest(timeoutTicks = 400)
    public static void backbone_eachEraInItsJacketAndPlug(final ClientTestContext ctx) {
        rows(ctx, "backbone", List.of(ComputingModule.THICK_COAX_CABLE, ComputingModule.HBW_CABLE,
                ComputingModule.CX4_CABLE, ComputingModule.FIBRE_CABLE, ComputingModule.OM5_CABLE));
    }

    @ClientTest(timeoutTicks = 400)
    public static void longDistance_eachEraInItsJacketAndPlug(final ClientTestContext ctx) {
        rows(ctx, "long-distance", List.of(ComputingModule.TELEPHONE_LINE, ComputingModule.LEASED_LINE,
                ComputingModule.T3_LINE, ComputingModule.VLDC_CABLE, ComputingModule.DARK_FIBRE_CABLE));
    }

    @ClientTest(timeoutTicks = 400)
    public static void computeAndCrafting_eachInItsJacketAndPlug(final ClientTestContext ctx) {
        rows(ctx, "compute-crafting", List.of(ComputingModule.INFINIBAND_CABLE, ComputingModule.HPC_CABLE,
                ComputingModule.OSFP_CABLE, ComputingModule.CRAFTING_CABLE));
    }

    /*
     * Lays each of {@code cables} in a row of its own between two Server Routers, which take every line of every era,
     * and captures them once the last row has reached the router at its east end.
     */
    private static void rows(final ClientTestContext ctx, final String label, final List<CableEntry> cables) {
        final BlockPos lastEnd = new BlockPos(LENGTH, FLOOR, FIRST_ROW + cables.size() - 1);
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    for (int row = 0; row < cables.size(); row++) {
                        final int z = FIRST_ROW + row;
                        level.setBlockAndUpdate(ctx.abs(new BlockPos(-LENGTH - 1, FLOOR, z)),
                                ComputingModule.SERVER_ROUTER.get().defaultBlockState());
                        level.setBlockAndUpdate(ctx.abs(new BlockPos(LENGTH + 1, FLOOR, z)),
                                ComputingModule.SERVER_ROUTER.get().defaultBlockState());
                        for (int x = -LENGTH; x <= LENGTH; x++) {
                            Cables.lay(level, ctx.abs(new BlockPos(x, FLOOR, z)), cables.get(row).get());
                        }
                    }
                })
                .thenWaitUntil(() -> {
                    final CableBlockEntity end = clientCable(ctx, lastEnd);
                    return end != null && end.crosses(cables.getLast().get(), Direction.EAST);
                }, 80, "the player's game to know the last row reaches its router")
                .then(0, () -> ctx.player().setXRot(LOOK_DOWN))
                .thenScreenshot(10, label)
                .thenServer(0, level -> clear(ctx, level, cables.size()));
    }

    private static @Nullable CableBlockEntity clientCable(final ClientTestContext ctx, final BlockPos relative) {
        return ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(relative)) instanceof CableBlockEntity
                cable ? cable : null;
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level, final int rows) {
        for (int x = -LENGTH - 1; x <= LENGTH + 1; x++) {
            for (int z = FIRST_ROW; z < FIRST_ROW + rows; z++) {
                level.removeBlock(ctx.abs(new BlockPos(x, FLOOR, z)), false);
            }
        }
    }
}
