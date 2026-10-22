/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import dev.jstech.core.jstechseries.persistor.PersistorShape;
import dev.jstech.core.jstechseries.persistor.PersistorShapes;
import dev.jstech.core.jstechseries.persistor.SpacePersistor;
import dev.jstech.core.jstechseries.persistor.SpacePersistorBlockEntity;
import dev.jstech.core.jstechseries.persistor.SpacePersistorLayout;
import dev.jstech.core.jstechseries.persistor.client.SpacePersistorScreen;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.region.ChunkLoaders;
import dev.jstech.core.team.Ownership;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The Space Persistor's screen on a player's game, to set beside the approved design: the Persistor owned by the
 * player and powered, keeping its square, with another owner's chunks two to the east; a chunk added with a click on
 * its map; the map taken further out; the circle picked; and its Protection tab, drawn as every machine's is.
 */
public final class SpacePersistorClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final BlockPos PERSISTOR = new BlockPos(0, 2, 1);
    private static final int WAIT = 200;
    private static final int ADDED_DX = 3;
    private static final int OTHERS_DX = 2;

    private SpacePersistorClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void screen_showsTheLandItKeepsAndChangesIt(final ClientTestContext ctx) {
        final BlockPos[] other = new BlockPos[1];
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> other[0] = build(ctx, level))
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(PERSISTOR))))
                .then(2, () -> ctx.rightClick(PERSISTOR, Direction.NORTH))
                .thenWaitUntil(() -> ctx.mc().screen instanceof SpacePersistorScreen && persistor(ctx)
                        .screenReadings().keeping() && !persistor(ctx).screenReadings().others().isEmpty(), WAIT,
                        "the Persistor's screen to open on it keeping its square, another owner's chunks beside it")
                .then(0, () -> ctx.pointAt(1, 1))
                .thenScreenshot(10, "persistor-near")
                .then(0, () -> ctx.pointAt(left(ctx) + SpacePersistorLayout.FACE_X + chunkX(ADDED_DX) + 0.5,
                        top(ctx, SpacePersistorLayout.HEIGHT) + SpacePersistorLayout.FACE_Y + chunkY(0) + 0.5))
                .thenScreenshot(5, "persistor-hover")
                .then(0, () -> face(ctx, chunkX(ADDED_DX), chunkY(0)))
                .thenWaitUntilServer(level -> serverPersistor(ctx, level).area().added()
                        .contains(PersistorShapes.pack(ADDED_DX, 0)), WAIT, "a click on the map to add the chunk",
                        level -> "added " + serverPersistor(ctx, level).area().added())
                .then(0, () -> ctx.pointAt(1, 1))
                .thenScreenshot(25, "persistor-clicked")
                .then(0, () -> face(ctx, SpacePersistorLayout.DISPLAY_X + SpacePersistorLayout.ZOOM_X + 5,
                        SpacePersistorLayout.DISPLAY_Y + SpacePersistorLayout.ZOOM_Y + SpacePersistorLayout.ZOOM_PITCH
                                + 5))
                .then(2, () -> face(ctx, SpacePersistorLayout.DISPLAY_X + SpacePersistorLayout.ZOOM_X + 5,
                        SpacePersistorLayout.DISPLAY_Y + SpacePersistorLayout.ZOOM_Y + SpacePersistorLayout.ZOOM_PITCH
                                + 5))
                .then(0, () -> ctx.pointAt(1, 1))
                .thenScreenshot(25, "persistor-far")
                .then(0, () -> face(ctx, SpacePersistorLayout.COLUMN_X + PersistorShape.round().indexOf(
                        PersistorShape.CIRCLE) * SpacePersistorLayout.KEY_PITCH + 8, SpacePersistorLayout.KEYS_Y + 6))
                .thenWaitUntilServer(level -> serverPersistor(ctx, level).area().shape() == PersistorShape.CIRCLE,
                        WAIT, "the circle's key to pick the circle", level -> "still "
                                + serverPersistor(ctx, level).area().shape())
                .then(0, () -> ctx.pointAt(1, 1))
                .thenScreenshot(25, "persistor-circle")
                .then(0, () -> ctx.click(left(ctx) + MachineScreenLayout.tabX(1) + MachineScreenLayout.TAB_WIDTH
                        / 2.0, top(ctx, SpacePersistorLayout.HEIGHT) + MachineScreenLayout.TAB_HEIGHT / 2.0))
                .thenWaitUntil(() -> persistorScreen(ctx).getMenu().tab() == MachineMenu.PROTECTION, WAIT,
                        "the Protection tab to show")
                .thenScreenshot(5, "persistor-protection")
                .then(0, () -> ctx.mc().setScreen(null))
                .thenServer(0, level -> {
                    ChunkLoaders.releaseAll(level, other[0]);
                    level.setBlockAndUpdate(ctx.abs(PERSISTOR), Blocks.AIR.defaultBlockState());
                });
    }

    /*
     * The Persistor, the player's and powered, and another owner's chunks two to its east; where those are kept from.
     */
    private static BlockPos build(final ClientTestContext ctx, final ServerLevel level) {
        final BlockPos at = ctx.abs(PERSISTOR);
        level.setBlockAndUpdate(at, SpacePersistor.BLOCK.get().defaultBlockState());
        if (!(level.getBlockEntity(at) instanceof SpacePersistorBlockEntity persistor)) {
            throw new IllegalStateException("no Space Persistor where it was placed");
        }
        persistor.own(Ownership.of(ctx.player().getUUID()));
        Objects.requireNonNull(persistor.power().portStore(), "the Persistor has a port")
                .setStored(SpacePersistorBlockEntity.CAPACITY);
        final ChunkPos own = new ChunkPos(at);
        final BlockPos source = at.east(16 * OTHERS_DX).below();
        final UUID stranger = UUID.randomUUID();
        for (int dz = -1; dz <= 1; dz++) {
            ChunkLoaders.load(level, source, stranger, SpacePersistor.REASON, new ChunkPos(own.x + OTHERS_DX,
                    own.z + dz), false);
        }
        return source;
    }

    private static SpacePersistorScreen persistorScreen(final ClientTestContext ctx) {
        return (SpacePersistorScreen) Objects.requireNonNull(ctx.mc().screen, "no screen");
    }

    private static SpacePersistorBlockEntity persistor(final ClientTestContext ctx) {
        return (SpacePersistorBlockEntity) persistorScreen(ctx).getMenu().machine();
    }

    /* The Persistor as the server holds it, read on the server's own thread. */
    private static SpacePersistorBlockEntity serverPersistor(final ClientTestContext ctx, final ServerLevel level) {
        return (SpacePersistorBlockEntity) Objects.requireNonNull(level.getBlockEntity(ctx.abs(PERSISTOR)),
                "no Persistor on the server");
    }

    /* The face's x of the middle of the chunk {@code dx} east of the Persistor's own, closest in. */
    private static int chunkX(final int dx) {
        return SpacePersistorLayout.DISPLAY_X + (SpacePersistorLayout.middle(0) + dx) * SpacePersistorLayout.cell(0)
                + SpacePersistorLayout.cell(0) / 2;
    }

    /* The face's y of the middle of the chunk {@code dz} south of the Persistor's own, closest in. */
    private static int chunkY(final int dz) {
        return SpacePersistorLayout.DISPLAY_Y + (SpacePersistorLayout.middle(0) + dz) * SpacePersistorLayout.cell(0)
                + SpacePersistorLayout.cell(0) / 2;
    }

    /* Clicks the point ({@code x}, {@code y}) of the Persistor's face, on its Main tab. */
    private static void face(final ClientTestContext ctx, final int x, final int y) {
        ctx.click(left(ctx) + SpacePersistorLayout.FACE_X + x + 0.5, top(ctx, SpacePersistorLayout.HEIGHT)
                + SpacePersistorLayout.FACE_Y + y + 0.5);
    }

    private static int left(final ClientTestContext ctx) {
        return (ctx.mc().getWindow().getGuiScaledWidth() - SpacePersistorLayout.WIDTH) / 2;
    }

    private static int top(final ClientTestContext ctx, final int height) {
        return (ctx.mc().getWindow().getGuiScaledHeight() - height) / 2;
    }
}
