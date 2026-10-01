/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.client.model.CableBakedModel;
import dev.jstech.core.client.model.ConnectedBakedModel;
import dev.jstech.core.connect.ConnectedQuadrants;
import dev.jstech.tests.JsTestsClient;
import dev.jstech.tests.TestBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * Models put together as the game runs, drawn into the world's mesh: a data cable drawing the bus on its face from its
 * block entity's layout, and a wall of panels whose faces run into each other, each quarter from its own tile.
 */
public final class MultipartModelClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final BlockPos CABLE = new BlockPos(0, 3, 3);
    /** The top corner of the wall at its east end, seen from the north on the left. */
    private static final BlockPos WALL_CORNER = new BlockPos(1, 4, 3);
    private static final BlockPos WALL_MIDDLE = new BlockPos(0, 3, 3);

    private MultipartModelClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void cable_drawsItsBusInTheWorldMesh(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> Cables.lay(level, ctx.abs(CABLE), ComputingModule.ETHERNET_CABLE.get()))
                .thenServer(2, level -> ((CableBlockEntity) level.getBlockEntity(ctx.abs(CABLE)))
                        .addPart(Direction.NORTH, ComputingParts.IMPORT.get().create()))
                .thenWaitUntil(() -> {
                    final CableBlockEntity cable = clientCable(ctx);
                    return cable != null && cable.partType(Direction.NORTH) == ComputingParts.IMPORT.get();
                }, 60, "the player's game to know the bus on the cable")
                .thenAssert(2, () -> {
                    final CableBlockEntity cable = clientCable(ctx);
                    final BlockState state = cable.getBlockState();
                    final BakedModel model = ctx.mc().getBlockRenderer().getBlockModel(state);
                    final int bare = model.getQuads(state, null, RandomSource.create(0L), ModelData.EMPTY,
                            RenderType.cutout()).size();
                    final int drawn = model.getQuads(state, null, RandomSource.create(0L), cable.getModelData(),
                            RenderType.cutout()).size();
                    return model instanceof CableBakedModel && drawn > bare;
                }, "the bus's quads drawn with the cable's own")
                .thenScreenshot(5, "cable-with-bus")
                .thenServer(0, level -> level.removeBlock(ctx.abs(CABLE), false));
    }

    @ClientTest(timeoutTicks = 400)
    public static void connectedPanels_drawEachQuarterFromItsTile(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    for (int x = -1; x <= 1; x++) {
                        for (int y = 2; y <= 4; y++) {
                            level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, 3)),
                                    TestBlocks.CONNECTED_PANEL.get().defaultBlockState());
                        }
                    }
                })
                .thenWaitUntil(() -> ctx.mc().level != null
                        && ctx.mc().level.getBlockState(ctx.abs(WALL_CORNER)).is(TestBlocks.CONNECTED_PANEL.get())
                        && ctx.mc().level.getBlockState(ctx.abs(new BlockPos(-1, 2, 3)))
                        .is(TestBlocks.CONNECTED_PANEL.get()), 60, "the wall on the player's game")
                .thenAssert(2, () -> tilesOf(ctx, WALL_MIDDLE).equals(sprites(ctx, ConnectedQuadrants.WHOLE,
                        ConnectedQuadrants.WHOLE, ConnectedQuadrants.WHOLE, ConnectedQuadrants.WHOLE)),
                        "the middle of the wall drawn whole")
                // Joined to its right, below and between, as the north face sees it.
                .thenAssert(0, () -> tilesOf(ctx, WALL_CORNER).equals(sprites(ctx, ConnectedQuadrants.ALONE,
                        ConnectedQuadrants.ACROSS, ConnectedQuadrants.WHOLE, ConnectedQuadrants.UPRIGHT)),
                        "the corner drawn a quarter at a time")
                .thenScreenshot(5, "connected-wall")
                .thenServer(0, level -> {
                    for (int x = -1; x <= 1; x++) {
                        for (int y = 2; y <= 4; y++) {
                            level.removeBlock(ctx.abs(new BlockPos(x, y, 3)), false);
                        }
                    }
                });
    }

    private static @Nullable CableBlockEntity clientCable(final ClientTestContext ctx) {
        return ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(CABLE)) instanceof CableBlockEntity
                cable ? cable : null;
    }

    /* The sprites the north face of the panel at {@code relative} is drawn with, quarter by quarter. */
    private static List<TextureAtlasSprite> tilesOf(final ClientTestContext ctx, final BlockPos relative) {
        final BlockPos at = ctx.abs(relative);
        final BlockState state = ctx.mc().level.getBlockState(at);
        final BakedModel model = ctx.mc().getBlockRenderer().getBlockModel(state);
        if (!(model instanceof ConnectedBakedModel)) {
            return List.of();
        }
        final ModelData data = model.getModelData(ctx.mc().level, at, state, ModelData.EMPTY);
        return model.getQuads(state, Direction.NORTH, RandomSource.create(0L), data, null).stream()
                .map(BakedQuad::getSprite).toList();
    }

    private static List<TextureAtlasSprite> sprites(final ClientTestContext ctx, final int... tiles) {
        final List<TextureAtlasSprite> out = new ArrayList<>();
        for (final int tile : tiles) {
            out.add(ctx.mc().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS)
                    .getSprite(JsTestsClient.PANEL_TILES.get(tile)));
        }
        return out;
    }
}
