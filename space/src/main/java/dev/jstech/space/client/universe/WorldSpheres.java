/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.universe;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import dev.jstech.space.JsSpace;
import dev.jstech.space.universe.OverworldMap;
import dev.jstech.space.universe.SolarBody;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The maps the Solar System's worlds are drawn from in its space, on the graphics card: the Moon's and Mars' colour and
 * height maps from the mod's textures, and the Overworld's drawn from the number the server sends for its seed
 * ({@link OverworldMap}), with its clouds laid once in bands the world-sphere shader slides round it. The maps are
 * drawn away from the render thread; only putting them on the card is done on it, where everything here is called.
 */
final class WorldSpheres {

    private static final Logger LOGGER = LogUtils.getLogger();
    /* The Overworld's maps, as wide as the approved views draw it. */
    private static final int OVERWORLD_WIDTH = 2_048;
    /* How long the clouds take to go once round the Overworld at the pace of a whole turn, in ticks. */
    private static final double CLOUD_TURN_TICKS = 48_000;
    private static final ResourceLocation CLOUDS = ResourceLocation.withDefaultNamespace(
            "textures/environment/clouds.png");
    private static final int CLOUD_TILE = 256;
    private static final int FULL = 255;
    private static final Map<SolarBody, Maps> MAPS = new EnumMap<>(SolarBody.class);

    private static boolean seeded;
    private static long mapSeed;
    @Nullable
    private static CompletableFuture<Overworld> overworldJob;
    @Nullable
    private static Cover clouds;
    /* Set when a map could not be read or drawn, so it is not tried on every frame; cleared as the player leaves. */
    private static boolean failed;

    private WorldSpheres() {
    }

    /** The Overworld's map number has come from the server: its maps are drawn again from it. */
    static void receive(final long seed) {
        if (seeded && seed == mapSeed) {
            return;
        }
        dropOverworld();
        seeded = true;
        mapSeed = seed;
    }

    /** Everything on the card is let go, as the player leaves the world. */
    static void clear() {
        dropOverworld();
        seeded = false;
        failed = false;
        MAPS.values().forEach(Maps::release);
        MAPS.clear();
    }

    /**
     * {@code body}'s maps, once on the card: the Moon's and Mars' are read on the first call, the Overworld's once
     * drawn from its number, which starts on the first call after it came.
     */
    static @Nullable Maps of(final SolarBody body) {
        if (failed) {
            return MAPS.get(body);
        }
        if (body == SolarBody.OVERWORLD) {
            return overworld();
        }
        if (!MAPS.containsKey(body)) {
            read(body);
        }
        return MAPS.get(body);
    }

    /** The Overworld's clouds, laid once with its maps; none until then. */
    static @Nullable Cover clouds() {
        return clouds;
    }

    /**
     * How many turns the Overworld's clouds have gone round it at {@code time}, in ticks, counted modulo
     * {@link OverworldMap#PACE_STEPS} so the shader's single floats keep them exact ({@link OverworldMap#slide}).
     */
    static float cloudTurn(final double time) {
        final double turn = time / CLOUD_TURN_TICKS;
        return (float) (Math.floorMod((long) Math.floor(turn), OverworldMap.PACE_STEPS) + turn - Math.floor(turn));
    }

    private static @Nullable Maps overworld() {
        final Maps ready = MAPS.get(SolarBody.OVERWORLD);
        if (ready != null || !seeded) {
            return ready;
        }
        if (overworldJob == null) {
            final long seed = mapSeed;
            final boolean[] tile = cloudTile();
            overworldJob = CompletableFuture.supplyAsync(() -> {
                final OverworldMap.Maps maps = OverworldMap.maps(OVERWORLD_WIDTH, seed);
                return new Overworld(SphereTexture.chain(opaque(maps.colours()), OVERWORLD_WIDTH),
                        SphereTexture.chain(grey(maps.heights()), OVERWORLD_WIDTH),
                        bands(OverworldMap.cloudBands(OVERWORLD_WIDTH, seed, tile),
                                OverworldMap.cloudPaces(OVERWORLD_WIDTH, seed)),
                        cover(OverworldMap.gathering(OVERWORLD_WIDTH, seed)));
            }, Util.backgroundExecutor());
        }
        final CompletableFuture<Overworld> job = overworldJob;
        if (!job.isDone()) {
            return null;
        }
        overworldJob = null;
        final Overworld drawn = done(job);
        if (drawn == null) {
            return null;
        }
        clouds = new Cover(new SphereTexture(drawn.bands()), new SphereTexture(drawn.gathering()));
        final Maps maps = new Maps(new SphereTexture(drawn.colour()), new SphereTexture(drawn.height()));
        MAPS.put(SolarBody.OVERWORLD, maps);
        return maps;
    }

    /* What {@code job} drew, or none if drawing it failed, which is said once and not tried again. */
    private static <T> @Nullable T done(final CompletableFuture<T> job) {
        try {
            return job.join();
        } catch (final CompletionException broken) {
            LOGGER.error("J's Space could not draw the Overworld's maps", broken.getCause());
            failed = true;
            return null;
        }
    }

    /* The Overworld's maps and its clouds let go; any being drawn is thrown away when done. */
    private static void dropOverworld() {
        final Maps maps = MAPS.remove(SolarBody.OVERWORLD);
        if (maps != null) {
            maps.release();
        }
        if (clouds != null) {
            clouds.release();
            clouds = null;
        }
        final CompletableFuture<Overworld> job = overworldJob;
        if (job != null) {
            job.thenAccept(drawn -> {
                SphereTexture.close(drawn.colour());
                SphereTexture.close(drawn.height());
                SphereTexture.close(drawn.bands());
                SphereTexture.close(drawn.gathering());
            });
        }
        overworldJob = null;
    }

    /* The Moon's or Mars' maps, read from the mod's textures. */
    private static void read(final SolarBody body) {
        final String name = body.name().toLowerCase(Locale.ROOT);
        try {
            MAPS.put(body, new Maps(new SphereTexture(SphereTexture.chain(image(name))),
                    new SphereTexture(SphereTexture.chain(image(name + "_height")))));
        } catch (final IOException missing) {
            // Without its maps a world is not drawn, rather than drawn wrong.
            LOGGER.error("J's Space could not read the maps of {}", name, missing);
            failed = true;
        }
    }

    private static NativeImage image(final String name) throws IOException {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
                "textures/environment/world/" + name + ".png");
        final Resource resource = Minecraft.getInstance().getResourceManager().getResource(id)
                .orElseThrow(() -> new IOException("no texture " + id));
        try (InputStream in = resource.open()) {
            return NativeImage.read(NativeImage.Format.RGBA, in);
        }
    }

    /* The game's cloud map, true where it has cloud; all clear if it cannot be read. */
    private static boolean[] cloudTile() {
        final boolean[] tile = new boolean[CLOUD_TILE * CLOUD_TILE];
        final Resource resource = Minecraft.getInstance().getResourceManager().getResource(CLOUDS).orElse(null);
        if (resource == null) {
            return tile;
        }
        try (InputStream in = resource.open(); NativeImage image = NativeImage.read(NativeImage.Format.RGBA, in)) {
            for (int y = 0; y < CLOUD_TILE; y++) {
                for (int x = 0; x < CLOUD_TILE; x++) {
                    final int pixel = image.getPixelRGBA(x % image.getWidth(), y % image.getHeight());
                    tile[y * CLOUD_TILE + x] = (pixel >>> 24) > 127;
                }
            }
        } catch (final IOException unreadable) {
            LOGGER.error("J's Space could not read the game's clouds", unreadable);
        }
        return tile;
    }

    private static int[] opaque(final int[] rgb) {
        final int[] out = new int[rgb.length];
        for (int i = 0; i < rgb.length; i++) {
            out[i] = FastColor.ARGB32.opaque(rgb[i]);
        }
        return out;
    }

    /* Heights as grey, 0 black and 1 or more white. */
    private static int[] grey(final double[] heights) {
        final int[] out = new int[heights.length];
        for (int i = 0; i < heights.length; i++) {
            final int level = (int) Math.round(Math.max(0, Math.min(1, heights[i])) * FULL);
            out[i] = FastColor.ARGB32.color(FULL, level, level, level);
        }
        return out;
    }

    /* A cover as white, see-through where it is false. */
    private static NativeImage[] cover(final boolean[] cloud) {
        final int[] out = new int[cloud.length];
        for (int i = 0; i < cloud.length; i++) {
            out[i] = FastColor.ARGB32.color(cloud[i] ? FULL : 0, FULL, FULL, FULL);
        }
        return SphereTexture.chain(out, OVERWORLD_WIDTH);
    }

    /* The clouds' bands, see-through where there is no cloud, each texel's red its row's pace. */
    private static NativeImage[] bands(final boolean[] cloud, final int[] paces) {
        final int[] out = new int[cloud.length];
        for (int i = 0; i < cloud.length; i++) {
            out[i] = FastColor.ARGB32.color(cloud[i] ? FULL : 0, paces[i / OVERWORLD_WIDTH], 0, 0);
        }
        return SphereTexture.chain(out, OVERWORLD_WIDTH);
    }

    /**
     * A world's maps on the card.
     *
     * @param colour its colours
     * @param height its heights, the lowest black
     */
    record Maps(SphereTexture colour, SphereTexture height) {

        void release() {
            this.colour.releaseId();
            this.height.releaseId();
        }
    }

    /**
     * The Overworld's clouds on the card.
     *
     * @param bands its bands of cloud as they stand at the start, each texel's red the pace of its row
     * @param gathering where the weather lets clouds be, at the start
     */
    record Cover(SphereTexture bands, SphereTexture gathering) {

        void release() {
            this.bands.releaseId();
            this.gathering.releaseId();
        }
    }

    /* The Overworld's maps and clouds, drawn away from the render thread. */
    private record Overworld(NativeImage[] colour, NativeImage[] height, NativeImage[] bands,
                             NativeImage[] gathering) {
    }
}
