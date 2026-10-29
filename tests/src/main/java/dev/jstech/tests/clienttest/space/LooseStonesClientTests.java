/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.space.geology.LooseStones;
import dev.jstech.space.geology.LooseStonesBlock;
import dev.jstech.space.geology.StoneArrangements;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The loose stones on a player's game: every way each kind lies is drawn with its model, never the missing one, its
 * stones' sides and tops in the very pictures of its rock's textures, texel by texel; and a field of them on the
 * ground, every kind in every arrangement and facing, for the art to be judged by eye.
 */
public final class LooseStonesClientTests {

    private static final List<Kind> KINDS = List.of(new Kind(LooseStones.MARE_BASALT_STONES, LooseStones.MARE_BASALT),
            new Kind(LooseStones.ANORTHOSITE_STONES, LooseStones.ANORTHOSITE),
            new Kind(LooseStones.MARTIAN_BASALT_STONES, LooseStones.MARTIAN_BASALT));
    private static final int SETTLE = 4;
    /* The field's corner, on the ground as the players stand at y 2, and where it is looked at from. */
    private static final BlockPos FIELD = new BlockPos(1, 2, 2);
    private static final BlockPos VIEW = new BlockPos(7, 2, 9);

    private LooseStonesClientTests() {
    }

    @ClientTest(timeoutTicks = 100)
    public static void stones_areDrawnInTheirRocksPicturesEveryWayTheyLie(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            final BakedModel missing = ctx.mc().getModelManager().getMissingModel();
            for (final Kind kind : KINDS) {
                final int[] side = texels(ctx, kind.textures().side());
                final int[] top = texels(ctx, kind.textures().top());
                for (final BlockState state : kind.block().get().getStateDefinition().getPossibleStates()) {
                    final BakedModel model = ctx.mc().getModelManager().getBlockModelShaper().getBlockModel(state);
                    ctx.assertTrue(model != missing, state + " has the model made for it");
                    final List<BakedQuad> quads = model.getQuads(state, null, RandomSource.create(0L),
                            ModelData.EMPTY, null);
                    final int boxes = StoneArrangements.ALL.get(state.getValue(LooseStonesBlock.ARRANGEMENT)).size();
                    ctx.assertTrue(quads.size() == boxes * Direction.values().length, state + " is drawn as "
                            + boxes + " stones of six faces; got " + quads.size() + " faces");
                    for (final BakedQuad quad : quads) {
                        final int[] drawn = texels(quad.getSprite().contents().getOriginalImage());
                        ctx.assertTrue(Arrays.equals(drawn, quad.getDirection() == Direction.UP ? top : side),
                                state + "'s " + quad.getDirection() + " faces wear its rock's picture");
                    }
                }
                ctx.assertTrue(ctx.mc().getItemRenderer().getItemModelShaper().getItemModel(kind.block().get()
                        .asItem()) != missing, kind.block().getId() + "'s item has its model");
            }
        });
    }

    @ClientTest(timeoutTicks = 400)
    public static void stones_lieInAFieldOnTheGround(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    int x = 0;
                    for (final Kind kind : KINDS) {
                        for (int arrangement = 0; arrangement < StoneArrangements.ALL.size(); arrangement++) {
                            int z = 0;
                            for (final Direction facing : Direction.Plane.HORIZONTAL) {
                                final BlockPos at = FIELD.offset(x, 0, z++);
                                world.setBlock(at.below(), Blocks.STONE);
                                world.setBlock(at, kind.block().get().defaultBlockState()
                                        .setValue(LooseStonesBlock.ARRANGEMENT, arrangement)
                                        .setValue(LooseStonesBlock.FACING, facing));
                            }
                            x++;
                        }
                    }
                })
                .thenTeleport(SETTLE, VIEW, Direction.NORTH)
                .thenScreenshot(SETTLE * 2, "every-kind-of-stones-in-every-arrangement-and-facing");
    }

    /* The texels of the texture {@code name} as the player's game reads its file. */
    private static int[] texels(final ClientTestContext ctx, final String name) {
        final ResourceLocation file = ResourceLocation.fromNamespaceAndPath("jsspace", "textures/" + name + ".png");
        final Resource resource = ctx.mc().getResourceManager().getResource(file)
                .orElseThrow(() -> new IllegalStateException("no texture " + file));
        try (InputStream in = resource.open(); NativeImage image = NativeImage.read(NativeImage.Format.RGBA, in)) {
            return texels(image);
        } catch (final IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    private static int[] texels(final NativeImage image) {
        final int[] out = new int[image.getWidth() * image.getHeight()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                out[y * image.getWidth() + x] = image.getPixelRGBA(x, y);
            }
        }
        return out;
    }

    /* A kind of stones: its block, and the textures it is drawn in. */
    private record Kind(BlockEntry<LooseStonesBlock> block, LooseStones.Kind textures) {
    }
}
