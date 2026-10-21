/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.tool.ModeRadialScreen;
import dev.jstech.core.client.tool.ToolClient;
import dev.jstech.core.gui.layout.ToolHudLayout;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.tool.ToolHeat;
import dev.jstech.core.tool.Tools;
import dev.jstech.tests.TestTools;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Tools on a player's game: the ring of a drill's modes opens over the world and chooses the mode the mouse points
 * at; the HUD then names the drill with its new mode and its energy; the drill in its area mode outlines the blocks it
 * will take; and a wand shows the ghosts of the blocks it will place.
 */
public final class ToolClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    /* The middle of a wall of stone three by three, across the player's way south. */
    private static final BlockPos WALL = new BlockPos(0, 3, 1);
    /* The middle of a row of three stones on the floor, the wand's plane. */
    private static final BlockPos ROW = new BlockPos(0, 2, 1);
    private static final int WAIT = 200;
    private static final int SETTLE = 5;

    private ToolClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void ring_choosesTheModeTheMousePointsAtAndTheHudNamesIt(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND,
                        charged(new ItemStack(TestTools.DRILL.get()))))
                .thenWaitUntil(() -> ctx.player().getMainHandItem().is(TestTools.DRILL.get()), WAIT,
                        "the drill in hand")
                .then(SETTLE, ToolClient::openRing)
                .thenAwaitScreen(ModeRadialScreen.class, WAIT)
                .then(0, () -> {
                    final int count = ItemStates.of(ctx.player().getMainHandItem()).modes().size();
                    final int cx = ctx.mc().getWindow().getGuiScaledWidth() / 2;
                    final int cy = ctx.mc().getWindow().getGuiScaledHeight() / 2 - ToolHudLayout.RAISED;
                    ctx.pointAt(cx + ToolHudLayout.placeX(2, count), cy + ToolHudLayout.placeY(2, count));
                })
                .thenAssert(SETTLE, () -> ctx.screen(ModeRadialScreen.class).pointed() == 2,
                        "the mouse points at the third mode, the vein")
                .thenScreenshot(0, "tools-ring")
                .then(0, () -> ctx.screen(ModeRadialScreen.class).choose())
                .thenAwaitNoScreen(WAIT)
                .thenWaitUntil(() -> ItemStates.mode(ctx.serverPlayer().getMainHandItem())
                        .filter(TestTools.VEIN::equals).isPresent(), WAIT, "the drill in its vein mode")
                .thenScreenshot(SETTLE, "tools-hud");
    }

    @ClientTest(timeoutTicks = 600)
    public static void drill_outlinesTheBlocksItsAreaWillTake(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    for (int x = -1; x <= 1; x++) {
                        for (int y = 2; y <= 4; y++) {
                            level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, WALL.getZ())),
                                    Blocks.STONE.defaultBlockState());
                        }
                    }
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(-1, 2, WALL.getZ())),
                            Blocks.OBSIDIAN.defaultBlockState());
                    final ItemStack drill = charged(new ItemStack(TestTools.DRILL.get()));
                    ItemStates.setMode(drill, TestTools.AREA);
                    ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND, drill);
                })
                .thenWaitUntil(() -> ctx.player().getMainHandItem().is(TestTools.DRILL.get())
                        && ctx.mc().level.getBlockState(ctx.abs(WALL)).is(Blocks.STONE), WAIT, "the wall and drill")
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(WALL))))
                .thenScreenshot(SETTLE, "tools-outline")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 600)
    public static void wand_showsTheGhostsOfWhatItWillPlace(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    for (int x = -1; x <= 1; x++) {
                        level.setBlockAndUpdate(ctx.abs(new BlockPos(x, ROW.getY(), ROW.getZ())),
                                Blocks.STONE.defaultBlockState());
                    }
                    final ItemStack wand = new ItemStack(TestTools.WAND.get());
                    ItemStates.setMode(wand, TestTools.PLANE);
                    ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND, wand);
                    ctx.give(1, new ItemStack(Items.STONE, 2));
                })
                .thenWaitUntil(() -> ctx.player().getMainHandItem().is(TestTools.WAND.get())
                        && ctx.mc().level.getBlockState(ctx.abs(ROW)).is(Blocks.STONE), WAIT, "the row and wand")
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(ROW)).add(0, 0.5, 0)))
                .thenScreenshot(SETTLE, "tools-ghosts")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 400)
    public static void pick_showsItsHeatUnderTheCrosshair(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    final ItemStack pick = new ItemStack(TestTools.HOT_PICK.get());
                    pick.set(Tools.HEAT.get(), new ToolHeat.Heated(TestTools.PICK_HEAT.most() * 0.8,
                            level.getGameTime(), false));
                    ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND, pick);
                })
                .thenWaitUntil(() -> ctx.player().getMainHandItem().has(Tools.HEAT.get()), WAIT, "the hot pick")
                .thenAssert(0, () -> TestTools.PICK_HEAT.heat(ctx.mc().level, ctx.player().getMainHandItem()) > 0,
                        "the pick is hot on the player's game")
                .thenScreenshot(SETTLE, "tools-heat");
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = -1; x <= 1; x++) {
            for (int y = 2; y <= 4; y++) {
                level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, WALL.getZ())), Blocks.AIR.defaultBlockState());
            }
        }
        ctx.serverPlayer().getInventory().clearContent();
    }

    /* Fills {@code stack} with energy, a transfer at a time; gives it. */
    private static ItemStack charged(final ItemStack stack) {
        final IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        while (energy != null && energy.receiveEnergy(Integer.MAX_VALUE, false) > 0) {
            // Each transfer takes what the item takes at once.
        }
        return stack;
    }
}
