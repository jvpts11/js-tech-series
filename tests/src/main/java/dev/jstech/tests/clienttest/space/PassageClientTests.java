/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.client.dimension.TravelScreens;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.space.worldgen.MarsWorld;
import dev.jstech.space.worldgen.MoonWorld;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Passages between the Solar System's worlds made by a command, on a player's game: neither the game's loading screen
 * nor a frame of a world with nothing in it yet is ever drawn. Every frame from before the command to the settled view
 * after it shows the last picture drawn before the passage, held, or the ground where the player arrived.
 */
public final class PassageClientTests {

    /*
     * Where the player stands in the test's area of the Overworld, on its floor, and arrives on each world; which way
     * they face, how far down they look, how long the land takes to be drawn.
     */
    private static final BlockPos START = new BlockPos(3, 2, 8);
    private static final int AT_X = 24;
    private static final int AT_Z = -40;
    private static final float NORTH = 180F;
    private static final float LOOKING_DOWN = 60F;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 20;
    /*
     * The part of each frame read, in hundredths of its width and height: under the sky, which looking down this far
     * is out of the picture, clear of the hand and of the bar, every eighth pixel of it.
     */
    private static final int LEFT = 15;
    private static final int RIGHT = 60;
    private static final int TOP = 20;
    private static final int BOTTOM = 70;
    private static final int HUNDRED = 100;
    private static final int STEP = 8;
    /*
     * How far apart two frames may be, as the mean difference of a channel over the part read, and still be one
     * picture; and how far apart the two worlds' pictures must be for the test to tell them.
     */
    private static final double LIKE = 10;
    private static final double APART = 25;
    private static final int CHANNEL = 0xFF;
    private static final int CHANNELS = 3;

    private PassageClientTests() {
    }

    @ClientTest(timeoutTicks = 4800)
    public static void command_toTheMoonAndOnToMarsNeverShowsAnEmptyWorld(final ClientTestContext ctx) {
        final Frames toTheMoon = new Frames();
        final Frames toMars = new Frames();
        final int still = TravelScreens.stillPassages();
        ctx.afterTest(() -> {
            toTheMoon.stop();
            toMars.stop();
            ctx.server().execute(() -> {
                final BlockPos home = ctx.origin().above(2);
                ctx.serverPlayer().teleportTo(ctx.server().overworld(), home.getX() + 0.5, home.getY(),
                        home.getZ() + 0.5, 0F, 0F);
            });
        });
        ctx.thenServer(0, level -> {
                    final BlockPos start = ctx.abs(START);
                    ctx.serverPlayer().teleportTo(ctx.server().overworld(), start.getX() + 0.5, start.getY(),
                            start.getZ() + 0.5, NORTH, LOOKING_DOWN);
                })
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.dimension().equals(Level.OVERWORLD),
                        ARRIVE, "the player's game to be in the Overworld")
                .thenAwaitLandDrawn(ARRIVE)
                .then(SETTLE, toTheMoon::start)
                .thenServer(0, level -> sendByCommand(ctx, moon(ctx)))
                .thenWaitUntil(() -> arrived(ctx, MoonWorld.world().dimension()), ARRIVE,
                        "the player's game to reach the Moon and let its picture go")
                .thenAwaitLandDrawn(ARRIVE)
                .then(SETTLE, () -> {
                    toTheMoon.stop();
                    toTheMoon.assertNeverEmpty(ctx, "to the Moon");
                    toMars.start();
                })
                .thenScreenshot(0, "on_the_moon")
                .thenServer(0, level -> sendByCommand(ctx, mars(ctx)))
                .thenWaitUntil(() -> arrived(ctx, MarsWorld.world().dimension()), ARRIVE,
                        "the player's game to reach Mars and let its picture go")
                .thenAwaitLandDrawn(ARRIVE)
                .then(SETTLE, () -> {
                    toMars.stop();
                    toMars.assertNeverEmpty(ctx, "from the Moon to Mars");
                    ctx.assertTrue(TravelScreens.stillPassages() - still == 2, "both passages held their last"
                            + " picture, as passages between two worlds of one system: "
                            + (TravelScreens.stillPassages() - still));
                })
                .thenScreenshot(0, "on_mars");
    }

    /* Sends the player onto the ground of {@code level} by the game's own command, as a player types it. */
    private static void sendByCommand(final ClientTestContext ctx, final ServerLevel level) {
        final ServerPlayer player = ctx.serverPlayer();
        final String command = String.format(Locale.ROOT, "execute in %s run tp %s %.1f %d %.1f",
                level.dimension().location(), player.getGameProfile().getName(), AT_X + 0.5, groundOf(level),
                AT_Z + 0.5);
        ctx.server().getCommands().performPrefixedCommand(ctx.server().createCommandSourceStack(), command);
    }

    /* The height the player stands at on {@code level}'s ground, its land there made first. */
    private static int groundOf(final ServerLevel level) {
        level.getChunk(new BlockPos(AT_X, 0, AT_Z));
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, AT_X, AT_Z);
    }

    /* Whether the player's game is in {@code dimension} and shows its land, the picture let go. */
    private static boolean arrived(final ClientTestContext ctx, final ResourceKey<Level> dimension) {
        return ctx.mc().level != null && ctx.mc().level.dimension().equals(dimension)
                && !TravelScreens.holdingStill();
    }

    /* The Moon, its dimension's own when the world has it, else made from its dimension's file under its own name. */
    private static ServerLevel moon(final ClientTestContext ctx) {
        final ResourceKey<Level> key = MoonWorld.world().dimension();
        return RuntimeDimensions.getOrCreate(ctx.server(), key, key.location());
    }

    /* Mars, as the Moon is had. */
    private static ServerLevel mars(final ClientTestContext ctx) {
        final ResourceKey<Level> key = MarsWorld.world().dimension();
        return RuntimeDimensions.getOrCreate(ctx.server(), key, key.location());
    }

    /*
     * The part read of every frame the player's game draws while it watches, as it is shown, after J's Core has held
     * its picture over it; whether that frame held the picture, and whether the game's own loading screen was up.
     */
    private static final class Frames {

        private final List<int[]> seen = new ArrayList<>();
        private final List<Boolean> held = new ArrayList<>();
        private final Consumer<RenderFrameEvent.Post> listener = event -> frame();
        private int loadingScreens;
        private int width;
        private int height;
        private boolean watching;

        /** Starts reading every frame. */
        void start() {
            watching = true;
            NeoForge.EVENT_BUS.addListener(listener);
        }

        /** Stops reading. */
        void stop() {
            if (watching) {
                watching = false;
                NeoForge.EVENT_BUS.unregister(listener);
            }
        }

        /**
         * Fails unless every frame read showed the first frame, the last picture before the passage, or the last, the
         * ground after it, and no frame the game's loading screen: a world drawn with nothing in it yet is neither.
         */
        void assertNeverEmpty(final ClientTestContext ctx, final String passage) {
            ctx.assertTrue(seen.size() > 2, passage + ": only " + seen.size() + " frames were read");
            ctx.assertTrue(loadingScreens == 0, passage + ": the game's loading screen was up in " + loadingScreens
                    + " frames");
            final int[] before = seen.getFirst();
            final int[] after = seen.getLast();
            ctx.assertTrue(apart(before, after) > APART, passage + ": the ground before and after look alike, "
                    + apart(before, after) + " apart, so no frame between them could be told");
            int strays = 0;
            int worst = -1;
            double worstApart = 0;
            int holding = 0;
            for (int i = 0; i < seen.size(); i++) {
                holding += held.get(i) ? 1 : 0;
                final double off = Math.min(apart(seen.get(i), before), apart(seen.get(i), after));
                if (off > LIKE) {
                    strays++;
                }
                if (off > worstApart) {
                    worstApart = off;
                    worst = i;
                }
            }
            ctx.assertTrue(holding > 0, passage + ": no frame of " + seen.size() + " held the last picture");
            if (strays > 0) {
                leave(ctx, passage, worst);
            }
            ctx.assertTrue(strays == 0, passage + ": " + strays + " of " + seen.size() + " frames showed neither the"
                    + " last picture before the passage nor the ground after it (" + holding + " held the picture);"
                    + " the furthest, frame " + worst + (worst >= 0 && held.get(worst) ? " (held)" : "") + ", "
                    + worstApart + " from both");
        }

        private void frame() {
            final Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof ReceivingLevelScreen) {
                loadingScreens++;
            }
            held.add(TravelScreens.holdingStill());
            try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                seen.add(read(image));
            }
        }

        /* Every STEP-th pixel of the part read, as the game keeps them, red in the low byte. */
        private int[] read(final NativeImage image) {
            final int x0 = image.getWidth() * LEFT / HUNDRED;
            final int y0 = image.getHeight() * TOP / HUNDRED;
            width = (image.getWidth() * RIGHT / HUNDRED - x0) / STEP;
            height = (image.getHeight() * BOTTOM / HUNDRED - y0) / STEP;
            final int[] out = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    out[y * width + x] = image.getPixelRGBA(x0 + x * STEP, y0 + y * STEP);
                }
            }
            return out;
        }

        /* The furthest frame, with the first and the last, as small pictures in the screenshots folder. */
        private void leave(final ClientTestContext ctx, final String passage, final int worst) {
            final File folder = new File(ctx.mc().gameDirectory, Screenshot.SCREENSHOT_DIR);
            final String name = "ct-passage-" + passage.replace(' ', '_');
            final int[][] frames = {seen.getFirst(), seen.get(Math.max(0, worst)), seen.getLast()};
            try (NativeImage out = new NativeImage(width * frames.length, height, false)) {
                for (int f = 0; f < frames.length; f++) {
                    for (int i = 0; i < frames[f].length; i++) {
                        out.setPixelRGBA(f * width + i % width, i / width, frames[f][i]);
                    }
                }
                out.writeToFile(new File(folder, name + ".png"));
            } catch (final IOException failed) {
                ctx.fail(passage + ": the furthest frame could not be left: " + failed);
            }
        }

        /* How far apart two frames read are: the mean difference of a channel over all the pixels read. */
        private static double apart(final int[] a, final int[] b) {
            if (a.length != b.length) {
                return Double.MAX_VALUE;
            }
            long sum = 0;
            for (int i = 0; i < a.length; i++) {
                for (int shift = 0; shift < CHANNELS * Byte.SIZE; shift += Byte.SIZE) {
                    sum += Math.abs((a[i] >> shift & CHANNEL) - (b[i] >> shift & CHANNEL));
                }
            }
            return (double) sum / (a.length * CHANNELS);
        }
    }
}
