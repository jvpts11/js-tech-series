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
import dev.jstech.core.tool.AreaMining;
import dev.jstech.core.tool.BuildingTools;
import dev.jstech.core.tool.ToolHeat;
import dev.jstech.core.tool.Tools;
import dev.jstech.tests.TestTools;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
    /* The wall's lower corner, far harder than the stone round it. */
    private static final BlockPos OBSIDIAN = new BlockPos(-1, 2, 1);
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
                .thenWaitUntil(() -> ItemStates.mode(ctx.player().getMainHandItem())
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
                    level.setBlockAndUpdate(ctx.abs(OBSIDIAN), Blocks.OBSIDIAN.defaultBlockState());
                    final ItemStack drill = charged(new ItemStack(TestTools.DRILL.get()));
                    ItemStates.setMode(drill, TestTools.AREA);
                    ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND, drill);
                })
                .thenWaitUntil(() -> ctx.player().getMainHandItem().is(TestTools.DRILL.get())
                        && ctx.mc().level.getBlockState(ctx.abs(WALL)).is(Blocks.STONE), WAIT, "the wall and drill")
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(WALL))))
                .thenAssert(SETTLE, () -> {
                    final List<BlockPos> outlined = outlined(ctx);
                    return outlined.size() == 7 && !outlined.contains(ctx.abs(OBSIDIAN))
                            && outlined.stream().allMatch(pos -> pos.getZ() == ctx.abs(WALL).getZ());
                }, "the charged drill in its area mode outlines the seven stones round the one aimed at, and not "
                        + "the obsidian, far harder")
                .thenScreenshot(0, "tools-outline")
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
                .thenAssert(SETTLE, () -> {
                    final List<BlockPos> placed = placed(ctx);
                    final int above = ctx.abs(ROW).getY() + 1;
                    return placed.size() == 3 && placed.stream().allMatch(pos -> pos.getY() == above);
                }, "the wand in its plane mode lays its ghosts out over each of the three stones")
                .thenScreenshot(0, "tools-ghosts")
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

    /*
     * The blocks the drill in hand takes round the block the player aims at, worked out on the player's game as its
     * outline works them out: nothing unless the drill works there, charged, and mines more than one block.
     */
    private static List<BlockPos> outlined(final ClientTestContext ctx) {
        final ItemStack drill = ctx.player().getMainHandItem();
        if (!(ctx.mc().hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !Tools.works(ctx.mc().level, drill)) {
            return List.of();
        }
        return Tools.mines(ctx.mc().level, drill).map(shape -> AreaMining.around(ctx.mc().level, ctx.player(), drill,
                hit.getBlockPos(), hit.getDirection(), shape)).orElse(List.of());
    }

    /* Where the wand in hand would place blocks from the face the player aims at, as its ghosts are laid out. */
    private static List<BlockPos> placed(final ClientTestContext ctx) {
        if (!(ctx.mc().hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return List.of();
        }
        return Tools.builds(ctx.player().getMainHandItem()).map(mode -> BuildingTools.targets(ctx.mc().level,
                ctx.player(), hit.getBlockPos(), hit.getDirection(), mode)).orElse(List.of());
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
