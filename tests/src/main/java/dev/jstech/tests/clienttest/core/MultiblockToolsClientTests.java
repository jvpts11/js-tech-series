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
import dev.jstech.core.client.multiblock.MultiblockShows;
import dev.jstech.core.integration.emi.CoreEmiPlugin;
import dev.jstech.core.integration.jei.CoreJeiPlugin;
import dev.jstech.core.jstechseries.builder.BuildStatus;
import dev.jstech.core.jstechseries.builder.MultiblockBuilder;
import dev.jstech.core.jstechseries.builder.client.BuilderClient;
import dev.jstech.core.jstechseries.builder.client.MultiblockBuilderScreen;
import dev.jstech.core.jstechseries.probe.ProbeLayout;
import dev.jstech.core.jstechseries.projector.ProjectorSettings;
import dev.jstech.core.jstechseries.projector.ProjectorView;
import dev.jstech.core.jstechseries.projector.StructureProjector;
import dev.jstech.core.jstechseries.projector.StructureProjectorBlockEntity;
import dev.jstech.core.jstechseries.projector.client.ProjectorClient;
import dev.jstech.core.jstechseries.projector.client.StructureProjectorScreen;
import dev.jstech.core.multiblock.ShapePreview;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestGuide;
import dev.jstech.tests.TestMultiblocks;
import dev.jstech.tests.TestStructures;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * The tools that show and build multiblocks, as a player meets them, to set beside the approved designs: the Structure
 * Projector showing the test furnace's ghosts, a wrong block in red, and its screen; the multiblocks in the recipe
 * viewer; the Multiblock Builder aimed at the test baker's terminal, its pages, and the box it builds; and a
 * multiblock drawn in a manual.
 */
public final class MultiblockToolsClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    /*
     * The projector as a player south of it looking north places it, turned to face them: the furnace stands north of
     * it, beyond it, between it and the player standing to look.
     */
    private static final BlockPos PROJECTOR = new BlockPos(0, 2, 6);
    private static final BlockPos FURNACE_MIDDLE = new BlockPos(0, 2, 4);
    /* Within reach of the projector, on its far side from the furnace. */
    private static final BlockPos BEHIND_PROJECTOR = new BlockPos(0, 2, 8);
    /* The baker's terminal stands amid the north face of the box it forms, off the floor. */
    private static final BlockPos BAKER = new BlockPos(0, 3, 1);
    /*
     * Far south of the furnace, past the reach a look once had, with the projector and a sugar cane on the line to the
     * middle of its south side, low on it, so the look goes through the projector's body and the cane's leaves.
     */
    private static final BlockPos AFAR = new BlockPos(0, 2, 20);
    private static final BlockPos CANE = new BlockPos(0, 2, 10);
    private static final BlockPos FURNACE_SOUTH = new BlockPos(0, 2, 5);
    private static final Vec3 LOW_ON_IT = new Vec3(0.5, 0.3, 0.5);
    private static final int AFAR_LAST_Z = 21;
    /* Shown in front of a player standing here facing north: a slab's ghost, and a stone's behind it. */
    private static final BlockPos SHAPES_STAND = new BlockPos(0, 2, 7);
    private static final ResourceLocation SHAPES = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "looked_shapes");
    private static final String SLAB = "minecraft:stone_slab";
    private static final String STONE = "minecraft:stone";
    /* Round the crosshair, its own pixels left out, where the outline of a ghost a block across at that range lies. */
    private static final int CROSSHAIR = 4;
    private static final int AROUND = 14;
    /* Inside the dark ground of the words a ghost looked at shows, to the right of the crosshair. */
    private static final int WORDS_LEFT = 24;
    private static final int WORDS_RIGHT = 50;
    private static final int WORDS_BOTTOM = 12;
    private static final int CHANNEL = 0xFF;
    private static final int WHITE_LEAST = 235;
    private static final int DARK_MOST = 40;
    private static final int GREEN_MOST = 16;
    private static final int BLUE_MOST = 48;
    private static final int PURPLE_LEAST = 8;
    private static final int MORE_OUTLINE = 30;
    private static final int MORE_WORDS = 100;
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
                    level.setBlockAndUpdate(ctx.abs(PROJECTOR), StructureProjector.BLOCK.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
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

    /*
     * From fourteen blocks off, past a sugar cane and through the projector itself, the furnace's ghost looked at is
     * found, outlined in white and named beside the crosshair; with the projector switched off, the same view has
     * neither the outline nor the words.
     */
    @ClientTest(timeoutTicks = 1200)
    public static void projector_outlinesAndNamesTheGhostLookedAtFromAfar(final ClientTestContext ctx) {
        final int[] shown = new int[2];
        final int[] hidden = new int[2];
        final ProjectorView.Place[] looked = new ProjectorView.Place[1];
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clearAfar(ctx, level);
                    level.setBlockAndUpdate(ctx.abs(PROJECTOR), StructureProjector.BLOCK.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    if (level.getBlockEntity(ctx.abs(PROJECTOR)) instanceof StructureProjectorBlockEntity projector) {
                        projector.apply(ProjectorSettings.FRESH.choosing(TestMultiblocks.FURNACE_ID));
                    }
                    // Its neighbours are left alone, so the cane stands on whatever the floor is.
                    level.setBlock(ctx.abs(CANE), Blocks.SUGAR_CANE.defaultBlockState(), Block.UPDATE_CLIENTS);
                })
                .thenWaitUntil(() -> showing(ctx, true), WAIT, "the projector to show the furnace")
                .thenTeleport(0, AFAR, Direction.NORTH)
                .then(2, () -> ctx.lookAt(Vec3.atLowerCornerOf(ctx.abs(FURNACE_SOUTH)).add(LOW_ON_IT)))
                .then(SETTLE, () -> {
                    looked[0] = ProjectorClient.lookedAt().orElse(null);
                    count(ctx, shown);
                })
                .thenScreenshot(0, "projector-looked-afar")
                .thenServer(0, level -> {
                    if (level.getBlockEntity(ctx.abs(PROJECTOR)) instanceof StructureProjectorBlockEntity projector) {
                        final ProjectorSettings on = projector.settings();
                        projector.apply(new ProjectorSettings(on.chosen(), on.turn(), on.mirrored(), on.layer(),
                                on.missingOnly(), false));
                    }
                })
                .thenWaitUntil(() -> showing(ctx, false), WAIT, "the projector to be switched off")
                .then(SETTLE, () -> {
                    count(ctx, hidden);
                    ctx.assertTrue(looked[0] != null && looked[0].pos().equals(ctx.abs(FURNACE_SOUTH)),
                            "the look from afar meets the ghost in the middle of the furnace's south side: "
                                    + (looked[0] == null ? "none" : looked[0].pos()));
                    ctx.assertTrue(shown[0] - hidden[0] > MORE_OUTLINE, "the ghost looked at is outlined in white: "
                            + shown[0] + " white pixels round the crosshair, " + hidden[0] + " switched off");
                    ctx.assertTrue(shown[1] - hidden[1] > MORE_WORDS, "beside the crosshair it says what goes there: "
                            + shown[1] + " pixels of the words' ground, " + hidden[1] + " switched off");
                })
                .thenServer(0, level -> clearAfar(ctx, level));
    }

    /*
     * A slab's ghost with a stone's behind it, shown in front of the player: a look over the slab's empty upper half
     * meets the stone, and a look at the slab's half meets the slab.
     */
    @ClientTest(timeoutTicks = 600)
    public static void ghosts_areLookedAtByTheirShapes(final ClientTestContext ctx) {
        final ShapePreview shapes = ShapePreview.of(List.of(
                new ShapePreview.Ghost(0, 0, 0, 0, List.of(SLAB), List.of(), false),
                new ShapePreview.Ghost(0, 0, -1, 0, List.of(STONE), List.of(), false)));
        final String[] looked = new String[2];
        ctx.afterTest(() -> hide(shapes));
        ctx.thenServer(0, level -> clear(ctx, level))
                .thenTeleport(0, SHAPES_STAND, Direction.NORTH)
                .then(2, () -> ctx.assertTrue(MultiblockShows.toggle(SHAPES, shapes),
                        "the shapes are shown in front of the player"))
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(SHAPES_STAND.north(2)))))
                .then(SETTLE, () -> looked[0] = lookedFirst())
                .thenScreenshot(0, "ghost-looked-over")
                .then(0, () -> ctx.lookAt(Vec3.atLowerCornerOf(ctx.abs(SHAPES_STAND.north())).add(0.5, 0.25, 0.5)))
                .then(SETTLE, () -> looked[1] = lookedFirst())
                .then(0, () -> {
                    ctx.assertTrue(STONE.equals(looked[0]),
                            "the look goes over the slab's ghost to the stone's behind it: " + looked[0]);
                    ctx.assertTrue(SLAB.equals(looked[1]), "the slab's ghost is looked at by its half: " + looked[1]);
                })
                .then(SETTLE, () -> hide(shapes));
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

    /* Whether the projector on this player's side shows the furnace and is switched {@code on}, or off. */
    private static boolean showing(final ClientTestContext ctx, final boolean on) {
        return ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(PROJECTOR))
                instanceof StructureProjectorBlockEntity projector && projector.settings().chosen().isPresent()
                && projector.settings().on() == on;
    }

    /* What goes in the ghost looked at now, as its first block, or none. */
    @Nullable
    private static String lookedFirst() {
        return ProjectorClient.lookedAt().map(place -> place.ghost().first()).orElse(null);
    }

    private static void hide(final ShapePreview shapes) {
        if (MultiblockShows.shownInWorld().filter(SHAPES::equals).isPresent()) {
            MultiblockShows.toggle(SHAPES, shapes);
        }
    }

    /*
     * In the frame now: in {@code out[0]} how many pixels round the crosshair, its own left out, are white, as the
     * outline of the ghost looked at is; in {@code out[1]} how many to its right are the purple-black ground of the
     * words a ghost looked at shows.
     */
    private static void count(final ClientTestContext ctx, final int[] out) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        out[0] = 0;
        out[1] = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -AROUND; dx <= AROUND; dx++) {
                for (int dy = -AROUND; dy <= AROUND; dy++) {
                    final int argb = frame.pixel(x + dx, y + dy);
                    if ((Math.abs(dx) > CROSSHAIR || Math.abs(dy) > CROSSHAIR) && (argb >> 16 & CHANNEL) >= WHITE_LEAST
                            && (argb >> 8 & CHANNEL) >= WHITE_LEAST && (argb & CHANNEL) >= WHITE_LEAST) {
                        out[0]++;
                    }
                }
            }
            for (int dx = WORDS_LEFT; dx <= WORDS_RIGHT; dx++) {
                for (int dy = 0; dy <= WORDS_BOTTOM; dy++) {
                    final int argb = frame.pixel(x + dx, y + dy);
                    final int green = argb >> 8 & CHANNEL;
                    final int blue = argb & CHANNEL;
                    if ((argb >> 16 & CHANNEL) <= DARK_MOST && green <= GREEN_MOST && blue <= BLUE_MOST
                            && blue - green >= PURPLE_LEAST) {
                        out[1]++;
                    }
                }
            }
        }
    }

    /* The furnace's places, the projector and the line south of it to where the player stands afar, back to air. */
    private static void clearAfar(final ClientTestContext ctx, final ServerLevel level) {
        clear(ctx, level);
        for (int x = -2; x <= 2; x++) {
            for (int y = 2; y <= 5; y++) {
                for (int z = PROJECTOR.getZ() + 2; z <= AFAR_LAST_Z; z++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState());
                }
            }
        }
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
