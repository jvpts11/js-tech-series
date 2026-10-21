/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.recipe.RecipeChooserClient;
import dev.jstech.core.recipe.RecipeChooserLayout;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Optional;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

/**
 * The chooser of recipes as a player meets it: two barriers in a crafting table's grid fit two of the test mod's
 * recipes, so the chooser stands beside the result, its tooltip naming both while the mouse is on it; a click on it
 * turns the result into the other recipe's.
 */
public final class RecipeChooserClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final BlockPos TABLE = new BlockPos(0, 2, 0);
    private static final int WAIT = 200;
    private static final int RESULT = 0;
    private static final int MIDDLE_X = RecipeChooserLayout.WIDTH / 2;
    private static final int MIDDLE_Y = RecipeChooserLayout.HEIGHT / 2;

    private RecipeChooserClientTests() {
    }

    @ClientTest(timeoutTicks = 800)
    public static void chooser_standsBesideTheResultAndChooses(final ClientTestContext ctx) {
        final Item[] before = new Item[1];
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(TABLE),
                        Blocks.CRAFTING_TABLE.defaultBlockState()))
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.getBlockState(ctx.abs(TABLE))
                        .is(Blocks.CRAFTING_TABLE), WAIT, "the crafting table to reach the player")
                .thenRightClick(2, TABLE)
                .thenAwaitScreen(CraftingScreen.class, WAIT)
                .thenServer(2, level -> {
                    final AbstractContainerMenu menu = ctx.serverPlayer().containerMenu;
                    menu.getSlot(1).set(new ItemStack(Items.BARRIER));
                    menu.getSlot(2).set(new ItemStack(Items.BARRIER));
                })
                .thenWaitUntil(() -> RecipeChooserClient.offer().isPresent() && !result(ctx).isEmpty(), WAIT,
                        "the server to tell that two recipes fit the grid")
                .thenAssert(2, () -> place(ctx).isPresent() && place(ctx).get()[0] == screen(ctx).getGuiLeft()
                        + RecipeChooserLayout.x(RecipeChooserLayout.CRAFTING_RESULT_X) && place(ctx).get()[1]
                        == screen(ctx).getGuiTop() + RecipeChooserLayout.y(RecipeChooserLayout.RESULT_Y),
                        "the chooser stands beside the result, where the mock puts it")
                .then(1, () -> ctx.pointAt(place(ctx).get()[0] + MIDDLE_X + 0.5, place(ctx).get()[1] + MIDDLE_Y
                        + 0.5))
                .thenScreenshot(4, "recipe-chooser")
                .then(1, () -> {
                    before[0] = result(ctx).getItem();
                    ctx.clickGui(RecipeChooserLayout.x(RecipeChooserLayout.CRAFTING_RESULT_X) + MIDDLE_X,
                            RecipeChooserLayout.y(RecipeChooserLayout.RESULT_Y) + MIDDLE_Y);
                })
                .thenWaitUntil(() -> !result(ctx).isEmpty() && !result(ctx).is(before[0]), WAIT,
                        "the result to turn into the other recipe's")
                .thenScreenshot(2, "recipe-chooser-chosen")
                .then(2, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT)
                .thenServer(0, level -> {
                    level.removeBlock(ctx.abs(TABLE), false);
                    final ServerPlayer player = ctx.serverPlayer();
                    player.getInventory().clearOrCountMatchingItems(stack -> stack.is(Items.BARRIER), -1,
                            player.inventoryMenu.getCraftSlots());
                });
    }

    private static CraftingScreen screen(final ClientTestContext ctx) {
        return (CraftingScreen) ctx.mc().screen;
    }

    private static Optional<int[]> place(final ClientTestContext ctx) {
        return ctx.mc().screen instanceof CraftingScreen screen ? RecipeChooserClient.place(screen)
                : Optional.empty();
    }

    private static ItemStack result(final ClientTestContext ctx) {
        return ctx.mc().screen instanceof CraftingScreen screen ? screen.getMenu().getSlot(RESULT).getItem()
                : ItemStack.EMPTY;
    }
}
