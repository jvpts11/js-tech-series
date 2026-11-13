/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.ComputerTerminalScreen;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The Ops heading of a network's machine lists the operations under way first and the log after them: a click picks
 * the operation drawn on the row clicked, and the pick stays with that operation when the list moves.
 */
public final class OperationsLogClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 200;
    private static final int BOOT_WAIT = 1_200;
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final ResourceLocation MC_NET = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_net");
    private static final StorageKey COBBLE = StorageKey.of(Items.COBBLESTONE);
    /* The row clicked: the second of the log, under the one operation still under way. */
    private static final int ROW = 2;

    private OperationsLogClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void ops_aClickPicksTheOperationDrawnOnItsRowAndKeepsIt(final ClientTestContext ctx) {
        final MainframeBlockEntity[] mainframe = new MainframeBlockEntity[1];
        final UUID[] picked = new UUID[1];
        ctx.thenBuild(0, world -> {
                    final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
                    mainframe[0] = net.mainframe();
                    final CraftingComputerBlockEntity computer = net.cc();
                    computer.togglePower();
                    computer.formatDisk(0);
                    computer.installOs(MC_NET);
                    computer.togglePower();
                    net.rack().getServerStorage(0).insert(Items.COBBLESTONE, 100);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                // Three SELECTs that finish make the log; a fourth, its type held, stays under way, as do later ones.
                .thenServer(SETTLE * 5, level -> {
                    for (int i = 0; i < 3; i++) {
                        mainframe[0].submitNetworkSelect(Items.COBBLESTONE, 1,
                                new ExternalDataPort(new ItemStackHandler(9), null), "log " + i);
                    }
                })
                .thenServer(SETTLE * 5, level -> {
                    mainframe[0].lockType(COBBLE, Long.MAX_VALUE, null);
                    mainframe[0].submitNetworkSelect(Items.COBBLESTONE, 1,
                            new ExternalDataPort(new ItemStackHandler(9), null), "under way");
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(ComputerTerminalScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> space(ctx).headingPoint("Ops") != null, SCREEN_WAIT, "the Ops heading")
                .then(2, () -> ctx.clickGui(space(ctx).headingPoint("Ops")[0], space(ctx).headingPoint("Ops")[1]))
                .thenWaitUntil(() -> space(ctx).activeHeading() == ComputerTerminalMenu.TAB_OPS
                        && space(ctx).listedOperations().size() >= ROW + 2, SCREEN_WAIT,
                        "the Ops heading to list the operation under way and the log")
                .then(SETTLE, () -> {
                    picked[0] = space(ctx).listedOperations().get(ROW);
                    final int[] row = space(ctx).operationRowPoint(ROW);
                    ctx.clickGui(row[0], row[1]);
                })
                .thenWaitUntil(() -> picked[0].equals(space(ctx).selectedOperation()), SCREEN_WAIT,
                        "the click to pick the operation drawn on its row")
                // Another operation starts, listed above the rest, and the list moves under the pick.
                .thenServer(0, level -> mainframe[0].submitNetworkSelect(Items.COBBLESTONE, 1,
                        new ExternalDataPort(new ItemStackHandler(9), null), "started after"))
                .thenWaitUntil(() -> moved(space(ctx).listedOperations(), picked[0]), SCREEN_WAIT,
                        "the list to move once another operation starts")
                .thenAssert(SETTLE, () -> picked[0].equals(space(ctx).selectedOperation()),
                        "the pick stays with its operation as the list moves")
                .thenServer(0, level -> mainframe[0].unlockType(COBBLE));
    }

    /* Whether {@code picked} stands somewhere other than the row it was picked on. */
    private static boolean moved(final List<UUID> listed, final UUID picked) {
        final int at = listed.indexOf(picked);
        return at >= 0 && at != ROW;
    }

    private static ComputerTerminalScreen space(final ClientTestContext ctx) {
        return ctx.screen(ComputerTerminalScreen.class);
    }
}
