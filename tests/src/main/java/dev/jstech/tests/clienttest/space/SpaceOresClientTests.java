/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.jstechseries.SeriesMinerals;
import dev.jstech.core.material.HostRock;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.OreLook;
import dev.jstech.space.geology.SpaceOres;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The ores of the Moon and of Mars on a player's game, texel by texel: each is its rock's own texture with the specks
 * of the game's ore its mineral is drawn with laid exactly where that ore has them, the rock untouched everywhere
 * else, and the mineral's sheen on the brightest of them, which is what shows a black mineral on a dark rock; and a
 * wall of them in the world, each rock under its ores, for the art to be judged by eye.
 */
public final class SpaceOresClientTests {

    private static final List<Ore> ORES = List.of(new Ore(SeriesMinerals.ILMENITE, SpaceOres.MARE_BASALT),
            new Ore(SeriesMinerals.ILMENITE, SpaceOres.MARE_REGOLITH),
            new Ore(SeriesMinerals.ARMALCOLITE, SpaceOres.MARE_BASALT),
            new Ore(SeriesMinerals.TRANQUILLITYITE, SpaceOres.MARE_BASALT),
            new Ore(SeriesMinerals.MERRILLITE, SpaceOres.MARE_BASALT),
            new Ore(SeriesMinerals.NATIVE_IRON, SpaceOres.HIGHLAND_REGOLITH),
            new Ore(SeriesMinerals.HEMATITE, SpaceOres.MARTIAN_REGOLITH),
            new Ore(SeriesMinerals.HEMATITE, SpaceOres.MUDSTONE),
            new Ore(SeriesMinerals.MAGNETITE, SpaceOres.MARTIAN_BASALT),
            new Ore(SeriesMinerals.OLIVINE, SpaceOres.MARTIAN_BASALT),
            new Ore(SeriesMinerals.JAROSITE, SpaceOres.MUDSTONE),
            new Ore(SeriesMinerals.GYPSUM, SpaceOres.MUDSTONE));
    /* How far a texel of the game's ore is from its rock's, the three channels summed, to be a speck: the Core's. */
    private static final int SPECK = 40;
    /* Most speck texels change the rock under them; a few may by chance land on a texel of the same colour. */
    private static final double MOST_SPECKS = 0.9;
    private static final int SIZE = 16;
    private static final int SETTLE = 4;
    /* The wall's foot, on the ground as the players stand at y 2, and where it is looked at from. */
    private static final BlockPos WALL = new BlockPos(2, 2, 4);
    private static final BlockPos VIEW = new BlockPos(4, 2, 10);
    /* Each rock at the foot of a column, its ores above it, so the loose regoliths stand on the ground. */
    private static final List<HostRock> COLUMNS = List.of(SpaceOres.MARE_BASALT, SpaceOres.MARE_REGOLITH,
            SpaceOres.HIGHLAND_REGOLITH, SpaceOres.MARTIAN_REGOLITH, SpaceOres.MARTIAN_BASALT, SpaceOres.MUDSTONE);

    private SpaceOresClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void ores_standOverTheirRocksInAWall(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    for (int x = 0; x < COLUMNS.size(); x++) {
                        final HostRock rock = COLUMNS.get(x);
                        world.setBlock(WALL.east(x), rock.rock());
                        int y = 1;
                        for (final Ore ore : ORES) {
                            if (ore.rock() == rock) {
                                world.setBlock(WALL.east(x).above(y++),
                                        MaterialUses.oreBlock(ore.mineral(), rock).get());
                            }
                        }
                    }
                })
                .thenTeleport(SETTLE, VIEW, Direction.NORTH)
                .thenScreenshot(SETTLE * 2, "the-moons-and-mars-rocks-each-under-its-ores");
    }

    @ClientTest(timeoutTicks = 100)
    public static void ores_shineOnTheirRocksWhereTheGamesOreHasItsSpecks(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            for (final Ore ore : ORES) {
                final String id = ore.mineral().id().getPath() + " in " + ore.rock();
                final OreLook look = ore.mineral().oreLook();
                ctx.assertTrue(look.sheen().isPresent(), id + ": its mineral has a sheen");
                final BlockState state = MaterialUses.oreBlock(ore.mineral(), ore.rock()).get().defaultBlockState();
                final BakedModel model = ctx.mc().getModelManager().getBlockModelShaper().getBlockModel(state);
                final int[] drawn = texels(model.getQuads(state, Direction.NORTH, RandomSource.create(0L),
                        ModelData.EMPTY, null).getFirst().getSprite().contents().getOriginalImage());
                final int[] rock = texels(ctx, ore.rock().texture());
                final int[] gameOre = texels(ctx, ResourceLocation.withDefaultNamespace(look.pattern().texture(false)));
                final int[] gameRock = texels(ctx, ResourceLocation.withDefaultNamespace(look.pattern().rock(false)));
                final int sheen = look.sheen().getAsInt();
                int specks = 0;
                int changed = 0;
                boolean shines = false;
                for (int i = 0; i < SIZE * SIZE; i++) {
                    if (difference(gameOre[i], gameRock[i]) > SPECK) {
                        specks++;
                        changed += drawn[i] != rock[i] ? 1 : 0;
                        shines |= (drawn[i] & 0xFFFFFF) == sheen;
                    } else {
                        ctx.assertTrue(drawn[i] == rock[i], id + ": the rock is untouched where the game's ore has"
                                + " no speck, at texel " + i);
                    }
                }
                ctx.assertTrue(specks > 0 && changed >= specks * MOST_SPECKS, id + ": its specks lie where "
                        + look.pattern() + "'s do: " + changed + " of " + specks);
                ctx.assertTrue(shines, id + ": the brightest of them catch the mineral's sheen");
            }
        });
    }

    private static int difference(final int a, final int b) {
        return Math.abs((a >> 16 & 0xFF) - (b >> 16 & 0xFF)) + Math.abs((a >> 8 & 0xFF) - (b >> 8 & 0xFF))
                + Math.abs((a & 0xFF) - (b & 0xFF));
    }

    /* The texels of the texture {@code name} as the player's game reads its file, ARGB, the first 16 by 16. */
    private static int[] texels(final ClientTestContext ctx, final ResourceLocation name) {
        final ResourceLocation file = name.withPath(path -> "textures/" + path + ".png");
        final Resource resource = ctx.mc().getResourceManager().getResource(file)
                .orElseThrow(() -> new IllegalStateException("no texture " + file));
        try (InputStream in = resource.open(); NativeImage image = NativeImage.read(NativeImage.Format.RGBA, in)) {
            return texels(image);
        } catch (final IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    /* The first 16 by 16 texels of an image, ARGB, row by row. */
    private static int[] texels(final NativeImage image) {
        final int[] out = new int[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                final int abgr = image.getPixelRGBA(x, y);
                out[y * SIZE + x] = abgr & 0xFF00FF00 | (abgr & 0xFF) << 16 | abgr >> 16 & 0xFF;
            }
        }
        return out;
    }

    /* An ore of the Moon or of Mars: its mineral and its rock. */
    private record Ore(Material mineral, HostRock rock) {
    }
}
