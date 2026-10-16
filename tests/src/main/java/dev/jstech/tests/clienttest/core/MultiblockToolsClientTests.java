/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.client.guide.ManualScreen;
import dev.jstech.core.integration.emi.CoreEmiPlugin;
import dev.jstech.core.integration.jei.CoreJeiPlugin;
import dev.jstech.core.jstechseries.builder.BuildStatus;
import dev.jstech.core.jstechseries.builder.MultiblockBuilder;
import dev.jstech.core.jstechseries.builder.client.BuilderClient;
import dev.jstech.core.jstechseries.builder.client.MultiblockBuilderScreen;
import dev.jstech.core.jstechseries.probe.ProbeLayout;
import dev.jstech.core.jstechseries.projector.ProjectorSettings;
import dev.jstech.core.jstechseries.projector.StructureProjector;
import dev.jstech.core.jstechseries.projector.StructureProjectorBlockEntity;
import dev.jstech.core.jstechseries.projector.client.StructureProjectorScreen;
import dev.jstech.tests.TestGuide;
import dev.jstech.tests.TestMultiblocks;
import dev.jstech.tests.TestStructures;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

/**
 * The tools that show and build multiblocks, as a player meets them, to set beside the approved designs: the Structure
 * Projector showing the test furnace's ghosts, a wrong block in red, and its screen; the multiblocks in the recipe
 * viewer; the Multiblock Builder aimed at the test baker's controller, its pages, and the box it builds; and a
 * multiblock drawn in a manual.
 */
public final class MultiblockToolsClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    /* The projector faces north as placed: the furnace stands north of it, between it and the player. */
    private static final BlockPos PROJECTOR = new BlockPos(0, 2, 6);
    private static final BlockPos FURNACE_MIDDLE = new BlockPos(0, 2, 4);
    /* Within reach of the projector, on its far side from the furnace. */
    private static final BlockPos BEHIND_PROJECTOR = new BlockPos(0, 2, 8);
    /* The baker's controller stands amid the north face of the box it forms, off the floor. */
    private static final BlockPos BAKER = new BlockPos(0, 3, 1);
    private static final int WAIT = 200;
    private static final int BUILD_WAIT = 800;
    private static final int SETTLE = 4;
    private static final int VIEWER_WAIT = 400;

    private MultiblockToolsClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void projector_showsTheFurnaceInTheWorldAndOnItsScreen(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setBlockAndUpdate(ctx.abs(PROJECTOR), StructureProjector.BLOCK.get().defaultBlockState());
                    if (level.getBlockEntity(ctx.abs(PROJECTOR)) instanceof StructureProjectorBlockEntity projector) {
                        projector.apply(ProjectorSettings.FRESH.choosing(TestMultiblocks.FURNACE_ID));
                    }
                    level.setBlockAndUpdate(ctx.abs(FURNACE_MIDDLE.north()), TestMultiblocks.PART.get()
                            .defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(FURNACE_MIDDLE.east()), Blocks.DIRT.defaultBlockState());
                })
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(PROJECTOR))
                        instanceof StructureProjectorBlockEntity projector
                        && projector.settings().chosen().isPresent(), WAIT, "the projector to show the furnace")
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(FURNACE_MIDDLE))))
                .thenScreenshot(10, "projector-world")
                .thenTeleport(0, BEHIND_PROJECTOR, Direction.NORTH)
                .then(2, () -> ctx.rightClick(PROJECTOR))
                .thenAwaitScreen(StructureProjectorScreen.class, WAIT)
                .thenScreenshot(10, "projector-screen")
                .then(SETTLE, () -> ctx.mc().setScreen(null))
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 600)
    public static void recipeViewer_showsTheMultiblocks(final ClientTestContext ctx) {
        if (!ModList.get().isLoaded("jei") && !emi()) {
            // Without a viewer there is nothing to show: the test passes empty, as the viewer steps elsewhere do.
            ctx.then(0, () -> { });
            return;
        }
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenWaitUntil(() -> shown() > 0, VIEWER_WAIT, "the viewer running with the multiblocks")
                .then(0, () -> ctx.assertTrue(show(), "the viewer opens on the multiblocks"))
                .thenScreenshot(10, "multiblocks-viewer")
                .then(SETTLE, () -> ctx.mc().setScreen(null));
    }

    @ClientTest(timeoutTicks = 1600)
    public static void builder_buildsTheBakersBoxFromTheInventory(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setBlockAndUpdate(ctx.abs(BAKER), TestStructures.BAKER.get().defaultBlockState());
                })
                .thenGive(0, new ItemStack(MultiblockBuilder.ITEM.get()), new ItemStack(Items.IRON_BLOCK, 64))
                .then(2, () -> {
                    ctx.selectHotbar(0);
                    ctx.lookAt(Vec3.atCenterOf(ctx.abs(BAKER)));
                })
                .then(2, () -> ctx.rightClick(BAKER, Direction.NORTH))
                .thenAwaitScreen(MultiblockBuilderScreen.class, WAIT)
                .thenScreenshot(10, "builder-build")
                .then(0, () -> press(ctx, ProbeLayout.softX(1) + 4, ProbeLayout.SOFT_Y + 4))
                .thenScreenshot(5, "builder-choices")
                .then(0, () -> press(ctx, ProbeLayout.softX(2) + 4, ProbeLayout.SOFT_Y + 4))
                .thenScreenshot(5, "builder-source")
                .then(0, () -> press(ctx, ProbeLayout.softX(0) + 4, ProbeLayout.SOFT_Y + 4))
                .then(2, () -> press(ctx, ProbeLayout.PAD_X, ProbeLayout.PAD_Y))
                .thenWaitUntil(() -> BuilderClient.status().phase() == BuildStatus.Phase.BUILDING, WAIT,
                        "OK to start building")
                .thenScreenshot(10, "builder-building")
                .thenWaitUntil(() -> BuilderClient.status().phase() == BuildStatus.Phase.BUILT, BUILD_WAIT,
                        "the box to be built")
                .thenScreenshot(5, "builder-built")
                .then(SETTLE, () -> ctx.mc().setScreen(null))
                .thenScreenshot(10, "builder-box")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 600)
    public static void manual_drawsTheFurnaceAsAScene(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.then(0, () -> ctx.assertTrue(GuideClient.open(TestGuide.MANUAL, TestGuide.MULTIBLOCK),
                        "the test manual opens at the multiblock's page"))
                .thenAwaitScreen(ManualScreen.class, WAIT)
                .thenScreenshot(20, "manual-multiblock")
                .then(SETTLE, () -> ctx.mc().setScreen(null));
    }

    /* Presses the instrument held up at ({@code x}, {@code y}) of its picture, drawn in the middle of the screen. */
    private static void press(final ClientTestContext ctx, final int x, final int y) {
        final int left = (ctx.mc().getWindow().getGuiScaledWidth() - ProbeLayout.WIDTH) / 2;
        final int top = (ctx.mc().getWindow().getGuiScaledHeight() - ProbeLayout.HEIGHT) / 2;
        ctx.click(left + x + 0.5, top + y + 0.5);
    }

    private static boolean emi() {
        return ModList.get().isLoaded("emi");
    }

    private static int shown() {
        return (emi() ? CoreEmiPlugin.multiblocksShown() : CoreJeiPlugin.multiblocksShown()).orElse(0);
    }

    private static boolean show() {
        return emi() ? CoreEmiPlugin.showMultiblocks() : CoreJeiPlugin.showMultiblocks();
    }

    /* Everything the tests place, back to air: the projector and the furnace's places, the baker's box. */
    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = -2; x <= 2; x++) {
            for (int y = 2; y <= 5; y++) {
                for (int z = 0; z <= 7; z++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }
}
