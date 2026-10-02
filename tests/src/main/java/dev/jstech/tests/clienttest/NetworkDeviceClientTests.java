/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

/**
 * The devices of the data network, one era to a column from the Vintage at the west, to set beside their approved
 * pages: the routers in the front row, the optical routers a step up behind them, and the repeaters a step higher at
 * the back.
 */
public final class NetworkDeviceClientTests {

    private static final BlockPos STAND = new BlockPos(0, 6, -6);
    private static final int FLOOR = 2;
    private static final int FIRST_COLUMN = -2;
    private static final float LOOK_DOWN = 16.0F;

    private NetworkDeviceClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void devices_eachEraInItsFace(final ClientTestContext ctx) {
        final List<List<Supplier<? extends Block>>> rows = List.of(
                List.of(ComputingModule.VINTAGE_ROUTER, ComputingModule.PERSONAL_ROUTER,
                        ComputingModule.TRANSITION_ROUTER, ComputingModule.STANDARD_ROUTER,
                        ComputingModule.ADVANCED_ROUTER),
                List.of(ComputingModule.OPTICAL_ROUTER, ComputingModule.ADVANCED_OPTICAL_ROUTER),
                List.of(ComputingModule.VINTAGE_REPEATER, ComputingModule.LEGACY_REPEATER,
                        ComputingModule.TRANSITION_REPEATER, ComputingModule.STANDARD_REPEATER,
                        ComputingModule.ADVANCED_REPEATER));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    for (int row = 0; row < rows.size(); row++) {
                        final List<Supplier<? extends Block>> devices = rows.get(row);
                        final int offset = row == 1 ? 3 : 0;
                        for (int i = 0; i < devices.size(); i++) {
                            level.setBlockAndUpdate(ctx.abs(new BlockPos(FIRST_COLUMN + offset + i, FLOOR + row,
                                    row)), devices.get(i).get().defaultBlockState());
                        }
                    }
                })
                .then(0, () -> ctx.player().setXRot(LOOK_DOWN))
                .thenScreenshot(20, "network-devices")
                .thenServer(0, level -> {
                    for (int x = FIRST_COLUMN; x < FIRST_COLUMN + 5; x++) {
                        for (int row = 0; row < rows.size(); row++) {
                            level.removeBlock(ctx.abs(new BlockPos(x, FLOOR + row, row)), false);
                        }
                    }
                });
    }
}
