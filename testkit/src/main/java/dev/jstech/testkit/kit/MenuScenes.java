/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.kit;

import dev.jstech.testkit.gametest.Expect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A scene for menus: a block opened by a player as a right click opens it, its menu checked to be the one expected
 * and still valid; and clicks on it as the player's game sends them, on a slot or on a button of its screen.
 *
 * <pre>{@code
 * final ServerPlayer player = GameTestPlayers.connected(helper, "tester");
 * try {
 *     final KilnMenu menu = MenuScenes.open(helper, player, KILN, KilnMenu.class);
 *     MenuScenes.press(player, KilnMenu.SWITCH);
 *     MenuScenes.quickMove(player, KilnMenu.OUTPUT);
 * } finally {
 *     GameTestPlayers.leave(player);
 * }
 * }</pre>
 *
 * <p>Opening a menu sends its screen to the player's game, so the player is one that logged in over a connection:
 * {@code GameTestPlayers.connected}.
 */
public final class MenuScenes {

    private MenuScenes() {
    }

    /**
     * Has {@code player} open the block at the arena's place {@code at} with an empty hand, as a right click on its
     * top would, and gives back the menu it opened; a failure when it opened none, or another kind.
     */
    public static <M extends AbstractContainerMenu> M open(final GameTestHelper helper, final ServerPlayer player,
                                                           final BlockPos at, final Class<M> type) {
        final BlockPos pos = helper.absolutePos(at);
        final BlockState state = helper.getLevel().getBlockState(pos);
        final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        final InteractionResult result = state.useWithoutItem(helper.getLevel(), player, hit);
        final AbstractContainerMenu menu = player.containerMenu;
        if (menu == player.inventoryMenu) {
            throw Expect.failure(helper, at, "a menu opened: the block answered " + result + " and opened none");
        }
        final M opened = Expect.instance(helper, at, "the menu opened", type, menu);
        Expect.that(helper, at, opened.stillValid(player), "the menu opened stays open for the player who opened it",
                "it does not");
        return opened;
    }

    /**
     * Has {@code player} press the button {@code id} of the menu open, as its screen sends a press.
     *
     * @return whether the menu took the press
     */
    public static boolean press(final ServerPlayer player, final int id) {
        final AbstractContainerMenu menu = player.containerMenu;
        final boolean pressed = menu.clickMenuButton(player, id);
        menu.broadcastChanges();
        return pressed;
    }

    /** Has {@code player} click the slot {@code slot} of the menu open with the left button. */
    public static void click(final ServerPlayer player, final int slot) {
        click(player, slot, 0, ClickType.PICKUP);
    }

    /** Has {@code player} shift-click the slot {@code slot}, moving its stack across. */
    public static void quickMove(final ServerPlayer player, final int slot) {
        click(player, slot, 0, ClickType.QUICK_MOVE);
    }

    /** Has {@code player} click the slot {@code slot} with {@code button} and {@code type}, as the game sends it. */
    public static void click(final ServerPlayer player, final int slot, final int button, final ClickType type) {
        final AbstractContainerMenu menu = player.containerMenu;
        menu.clicked(slot, button, type, player);
        menu.broadcastChanges();
    }

    /** Closes the menu {@code player} has open, as closing its screen does. */
    public static void close(final ServerPlayer player) {
        player.closeContainer();
    }
}
