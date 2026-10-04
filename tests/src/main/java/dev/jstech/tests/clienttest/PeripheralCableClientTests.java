/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.multipart.ModelLayout;
import dev.jstech.core.multipart.PlacedModel;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.jetbrains.annotations.Nullable;

/**
 * The peripheral cables as a player sees them: each era's cable from a screen to a drive, ending at the screen in its
 * era's video plug and at the drive in its era's device plug.
 */
public final class PeripheralCableClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final int ROW_Z = 4;
    /* Each row, from the floor up: its cable, the screen at its west end, the drive at its east end, both plugs. */
    private static final List<Row> ROWS = List.of(
            new Row(ComputingModule.VINTAGE_PERIPHERAL_CABLE, ComputingModule.VINTAGE_MONITOR,
                    ComputingModule.FLOPPY_DRIVE, "de9", "db25"),
            new Row(ComputingModule.LEGACY_PERIPHERAL_CABLE, ComputingModule.LEGACY_MONITOR,
                    ComputingModule.CD_DRIVE, "vga", "usb"),
            new Row(ComputingModule.TRANSITION_PERIPHERAL_CABLE, ComputingModule.TRANSITION_MONITOR,
                    ComputingModule.DVD_DRIVE, "dvi", "usb_white"),
            new Row(ComputingModule.PERIPHERAL_CABLE, ComputingModule.MONITOR,
                    ComputingModule.DVD_DRIVE, "hdmi", "usb3"));

    private PeripheralCableClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void cables_endInThePlugOfThePortTheyEnter(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    for (int i = 0; i < ROWS.size(); i++) {
                        final Row row = ROWS.get(i);
                        final int y = 2 + i;
                        // A screen faces out of its back, its port toward the cable east of it; the drive faces east.
                        level.setBlockAndUpdate(ctx.abs(new BlockPos(-2, y, ROW_Z)), row.screen().get()
                                .defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                        for (int x = -1; x <= 1; x++) {
                            Cables.lay(level, ctx.abs(new BlockPos(x, y, ROW_Z)), row.cable().get());
                        }
                        level.setBlockAndUpdate(ctx.abs(new BlockPos(2, y, ROW_Z)), row.drive().get()
                                .defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
                    }
                })
                .thenWaitUntil(() -> {
                    for (int i = 0; i < ROWS.size(); i++) {
                        final Row row = ROWS.get(i);
                        final int y = 2 + i;
                        if (!plug(row.video()).equals(drawnPlug(ctx, new BlockPos(-1, y, ROW_Z), Direction.WEST))
                                || !plug(row.device()).equals(drawnPlug(ctx, new BlockPos(1, y, ROW_Z),
                                Direction.EAST))) {
                            return false;
                        }
                    }
                    return true;
                }, 100, "each cable drawn with its era's video plug at the screen and device plug at the drive")
                .thenScreenshot(10, "peripheral-cables-by-era")
                .thenServer(0, level -> {
                    for (int i = 0; i < ROWS.size(); i++) {
                        for (int x = -2; x <= 2; x++) {
                            level.removeBlock(ctx.abs(new BlockPos(x, 2 + i, ROW_Z)), false);
                        }
                    }
                });
    }

    /* The plug the player's game draws where the cable at {@code relative} meets {@code face}, or null. */
    private static @Nullable ResourceLocation drawnPlug(final ClientTestContext ctx, final BlockPos relative,
                                                        final Direction face) {
        if (ctx.mc().level == null
                || !(ctx.mc().level.getBlockEntity(ctx.abs(relative)) instanceof CableBlockEntity cable)) {
            return null;
        }
        final ModelLayout layout = cable.getModelData().get(ModelLayout.PROPERTY);
        if (layout == null) {
            return null;
        }
        for (final PlacedModel placed : layout.models()) {
            if (placed.facing() == face) {
                return placed.model();
            }
        }
        return null;
    }

    private static ResourceLocation plug(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "block/cable/plug/" + name);
    }

    /* A row of the scene: its cable, the screen and the drive at its ends, and the plug it ends in at each. */
    private record Row(Supplier<CableType> cable, Supplier<? extends Block> screen, Supplier<? extends Block> drive,
                       String video, String device) {
    }
}
