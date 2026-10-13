/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.multipart.ModelLayout;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.multipart.PlacedModel;
import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The buses as a player sees them: each era's Import, Export and External Storage Bus in a row on their cables, and
 * their items in the hotbar; and a bus drawn with its lamps blinking while it moves, dark again after.
 */
public final class BusModelClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final List<HardwareEra> ERAS = List.of(HardwareEra.VINTAGE, HardwareEra.LEGACY,
            HardwareEra.TRANSITION, HardwareEra.STANDARD, HardwareEra.ADVANCED);
    /* Each row's height: the Import Buses below, the Export Buses above them, the External Storage Buses on top. */
    private static final int IMPORT_Y = 2;
    private static final int EXPORT_Y = 3;
    private static final int EXTERNAL_Y = 4;
    private static final int ROW_Z = 4;
    private static final BlockPos LONE = new BlockPos(0, 3, 3);

    private BusModelClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void buses_wearTheirErasModels(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenGive(0, new ItemStack(ComputingModule.VINTAGE_IMPORT_BUS_ITEM.get()),
                        new ItemStack(ComputingModule.LEGACY_EXPORT_BUS_ITEM.get()),
                        new ItemStack(ComputingModule.TRANSITION_EXTERNAL_STORAGE_BUS_ITEM.get()),
                        new ItemStack(ComputingModule.IMPORT_BUS_ITEM.get()),
                        new ItemStack(ComputingModule.EXPORT_BUS_ITEM.get()),
                        new ItemStack(ComputingModule.EXTERNAL_STORAGE_BUS_ITEM.get()),
                        new ItemStack(ComputingModule.ADVANCED_IMPORT_BUS_ITEM.get()),
                        new ItemStack(ComputingModule.CRAFTING_ROUTER_ITEM.get()),
                        new ItemStack(ComputingModule.RECEIVING_BUS_ITEM.get()))
                .thenServer(0, level -> {
                    for (int i = 0; i < ERAS.size(); i++) {
                        lay(ctx, level, row(i, IMPORT_Y));
                        lay(ctx, level, row(i, EXPORT_Y));
                        lay(ctx, level, row(i, EXTERNAL_Y));
                    }
                })
                .thenServer(2, level -> {
                    for (int i = 0; i < ERAS.size(); i++) {
                        final HardwareEra era = ERAS.get(i);
                        mount(ctx, level, row(i, IMPORT_Y), ComputingParts.importBus(era));
                        mount(ctx, level, row(i, EXPORT_Y), ComputingParts.exportBus(era));
                        mount(ctx, level, row(i, EXTERNAL_Y), ComputingParts.externalBus(era));
                    }
                })
                .thenWaitUntil(() -> ComputingParts.externalBus(HardwareEra.ADVANCED).model(false)
                        .equals(drawn(ctx, row(ERAS.size() - 1, EXTERNAL_Y), Direction.NORTH)), 80,
                        "the player's game to draw the last bus")
                .thenAssert(2, () -> {
                    for (int i = 0; i < ERAS.size(); i++) {
                        final HardwareEra era = ERAS.get(i);
                        if (!model(ctx, row(i, IMPORT_Y), ComputingParts.importBus(era))
                                || !model(ctx, row(i, EXPORT_Y), ComputingParts.exportBus(era))
                                || !model(ctx, row(i, EXTERNAL_Y), ComputingParts.externalBus(era))) {
                            return false;
                        }
                    }
                    return true;
                }, "each bus drawn with its own era's idle model")
                .thenScreenshot(10, "buses-by-era")
                .thenServer(0, level -> {
                    for (int i = 0; i < ERAS.size(); i++) {
                        level.removeBlock(ctx.abs(row(i, IMPORT_Y)), false);
                        level.removeBlock(ctx.abs(row(i, EXPORT_Y)), false);
                        level.removeBlock(ctx.abs(row(i, EXTERNAL_Y)), false);
                    }
                });
    }

    @ClientTest(timeoutTicks = 600)
    public static void bus_blinksWhileItMovesAndGoesDarkAfter(final ClientTestContext ctx) {
        final PartType<?> type = ComputingParts.IMPORT.get();
        // Mounted on the east face, so the player looking south sees its side and the lamp on it.
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> lay(ctx, level, LONE))
                .thenServer(2, level -> cable(level, ctx, LONE).addPart(Direction.EAST, type.create()))
                .thenWaitUntil(() -> type.model(false).equals(drawn(ctx, LONE, Direction.EAST)), 60,
                        "the player's game to draw the bus idle")
                .thenScreenshot(5, "bus-idle")
                .thenServer(0, level -> {
                    if (cable(level, ctx, LONE).getPart(Direction.EAST) instanceof AbstractBusPart bus) {
                        bus.worked(level.getGameTime());
                    }
                })
                .thenWaitUntil(() -> type.model(true).equals(drawn(ctx, LONE, Direction.EAST)), 60,
                        "the bus drawn with its lamps blinking once it moved")
                .thenScreenshot(5, "bus-busy")
                .thenScreenshot(3, "bus-busy-later")
                .thenScreenshot(3, "bus-busy-last")
                .thenWaitUntil(() -> type.model(false).equals(drawn(ctx, LONE, Direction.EAST)), 200,
                        "the bus drawn dark again once it has been idle a while")
                .thenServer(0, level -> level.removeBlock(ctx.abs(LONE), false));
    }

    /* The {@code i}th place of a row at height {@code y}, from the player's left. */
    private static BlockPos row(final int i, final int y) {
        return new BlockPos(2 - i, y, ROW_Z);
    }

    private static void lay(final ClientTestContext ctx, final ServerLevel level, final BlockPos relative) {
        Cables.lay(level, ctx.abs(relative), ComputingModule.ETHERNET_CABLE.get());
    }

    /* Mounts a bus of {@code type} on the face of the cable at {@code relative} that looks at the player. */
    private static void mount(final ClientTestContext ctx, final ServerLevel level, final BlockPos relative,
                              final PartType<?> type) {
        cable(level, ctx, relative).addPart(Direction.NORTH, type.create());
    }

    private static CableBlockEntity cable(final ServerLevel level, final ClientTestContext ctx,
                                          final BlockPos relative) {
        return (CableBlockEntity) level.getBlockEntity(ctx.abs(relative));
    }

    private static boolean model(final ClientTestContext ctx, final BlockPos relative, final PartType<?> type) {
        return type.model(false).equals(drawn(ctx, relative, Direction.NORTH));
    }

    /* The bus model the player's game draws on {@code face} of the cable at {@code relative}, or null. */
    private static @Nullable ResourceLocation drawn(final ClientTestContext ctx, final BlockPos relative,
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
            if (placed.facing() == face && placed.model().getPath().startsWith(ComputingParts.MODELS)) {
                return placed.model();
            }
        }
        return null;
    }
}
