/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.client.PersonalComputerScreen;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.lwjgl.glfw.GLFW;

/**
 * The Network Anchor Card's tooltip in a computer's window (computers/docs/design/hardware.md): its era, the chunks it
 * keeps loaded round the computer, the player's own quota and what it draws, shot for the side-by-side with its mock.
 */
public final class AnchorCardClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 120;
    private static final BlockPos COMPUTER = new BlockPos(3, 2, 3);
    /** Low on a tower's front, in sixty-fourths from its top left: plain on every case, clear of what a click works. */
    private static final double PLAIN_X = 32;
    private static final double PLAIN_Y = 56;

    private AnchorCardClientTests() {
    }

    @ClientTest(timeoutTicks = 900)
    public static void anchorCard_saysWhatItKeepsLoadedAndThePlayersQuota(final ClientTestContext ctx) {
        // An empty hand opens the tower; the card goes into the first slot of the hotbar once it is open.
        ctx.thenGive(0).thenBuild(0, world -> world.setBlock(COMPUTER, ComputingModule.PERSONAL_COMPUTER.get()
                        .defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH)))
                .thenTeleport(SETTLE, COMPUTER.south(2), Direction.NORTH)
                .thenRightClickFront(SETTLE, COMPUTER, PLAIN_X, PLAIN_Y)
                .thenAwaitScreen(PersonalComputerScreen.class, SCREEN_WAIT)
                .thenServer(SETTLE, level -> {
                    ctx.serverPlayer().getInventory().setItem(0,
                            new ItemStack(ComputingModule.NETWORK_ANCHOR_CARD_TRANSITION.get()));
                    ctx.serverPlayer().containerMenu.broadcastChanges();
                })
                .thenWaitUntil(() -> !ctx.player().getInventory().getItem(0).isEmpty(), SCREEN_WAIT,
                        "the card to reach the hotbar")
                .then(SETTLE, () -> {
                    final PersonalComputerScreen screen = ctx.screen(PersonalComputerScreen.class);
                    final Slot slot = hotbar(screen);
                    ctx.pointAt(screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
                })
                .thenScreenshot(SETTLE, "anchor-card-tooltip")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /* The window's slot that shows the first slot of the player's hotbar. */
    private static Slot hotbar(final PersonalComputerScreen screen) {
        for (final Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof Inventory && slot.getContainerSlot() == 0) {
                return slot;
            }
        }
        throw new IllegalStateException("the window shows no hotbar");
    }
}
