/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.effect.ClientEffects;
import dev.jstech.core.client.effect.EffectEngine;
import dev.jstech.core.client.instancing.Instancing;
import dev.jstech.core.client.model.kit.KitInstancing;
import dev.jstech.core.mechanical.MechanicalPartBlockEntity;
import dev.jstech.core.model.DrawingSettings;
import dev.jstech.tests.TestMechanical;
import dev.jstech.tests.TestModels;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.BenchReport;
import java.io.IOException;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Alike things drawn many at once, as a player sees them: presses and shafts of one model each drawn together look as
 * they do drawn one by one; turning parts are drawn together as they turn; and a field of a thousand docks of one
 * model, and the particles of thirty-two plumes of steam, are drawn no slower together than one by one, the numbers
 * going to the bench reports; the field looks the same both ways, and the steam fills the view both ways.
 */
public final class InstancingClientTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    /* The presses stand in a row before the player, the shafts on them; the player looks down on them. */
    private static final BlockPos ROW = new BlockPos(0, 2, 3);
    private static final BlockPos STAND = new BlockPos(0, 5, -1);
    /* The parts' line runs along X; they are looked at from the east, face on. */
    private static final BlockPos CRANK = new BlockPos(-3, 2, 2);
    private static final BlockPos SMALL_GEAR = new BlockPos(-1, 2, 2);
    private static final BlockPos LARGE_GEAR = new BlockPos(-1, 3, 2);
    private static final BlockPos PARTS_STAND = new BlockPos(4, 3, 3);
    /* The field of docks, a side of 32 blocks, looked across from the ground before its near edge. */
    private static final int FIELD = 32;
    private static final int FIELD_NEAR = 4;
    private static final BlockPos FIELD_STAND = new BlockPos(0, 2, 0);
    /* The plumes of steam, eight by four. */
    private static final int PLUMES_ACROSS = 8;
    private static final int PLUMES_DEEP = 4;
    private static final int PLUME_GAP = 3;
    private static final long DAY = 6_000L;
    private static final int WAIT = 300;
    private static final int SETTLE = 10;
    /* Before frames are counted, and how many seconds they are counted over. */
    private static final int WARM = 60;
    private static final int SECONDS = 4;
    private static final int TICKS_A_SECOND = 20;
    /* How much slower than one by one together may come, for the noise of one run. */
    private static final double SLACK = 0.9;
    private static final int UNLIMITED = 260;
    /* How bright each channel of a pixel of steam is at least, and how much of the view's middle it fills. */
    private static final int WHITE = 190;
    private static final double STEAMY = 0.2;
    /* How much redder than green a pixel of copper is at least, and how much of the view's middle the gears fill. */
    private static final int REDDER = 30;
    private static final double COPPERY = 0.02;
    private static final String KIT_BENCH = "kit_instancing";
    private static final String PARTICLE_BENCH = "particle_instancing";

    private InstancingClientTests() {
    }

    @ClientTest(timeoutTicks = 900)
    public static void models_lookTheSameDrawnTogetherAsOneByOne(final ClientTestContext ctx) {
        final NativeImage[] together = new NativeImage[1];
        ctx.afterTest(() -> {
            if (together[0] != null) {
                together[0].close();
            }
            together(true);
            ctx.server().execute(() -> clear(ctx, ctx.serverLevel()));
        });
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(DAY);
                    for (int x = -1; x <= 1; x++) {
                        level.setBlockAndUpdate(ctx.abs(ROW.east(x)), TestModels.PRESS_BLOCK.get()
                                .defaultBlockState());
                        part(ctx, level, ROW.east(x).south(), TestMechanical.SHAFT.get());
                    }
                })
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(ROW.south()))))
                .thenWaitUntil(() -> Instancing.on() && KitInstancing.drawnLastFrame() >= 6, WAIT,
                        "the presses and shafts to be drawn together")
                .thenScreenshot(SETTLE, "presses-and-shafts-together")
                .then(0, () -> together[0] = middle(ctx))
                .then(0, () -> together(false))
                .thenWaitUntil(() -> KitInstancing.drawnLastFrame() == 0, WAIT, "them to be drawn one by one")
                .then(SETTLE, () -> {
                    try (NativeImage alone = middle(ctx)) {
                        ctx.assertSamePicture("together-and-one-by-one", alone, together[0]);
                    }
                });
    }

    @ClientTest(timeoutTicks = 900)
    public static void turningParts_areDrawnTogetherAsTheyTurn(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> clear(ctx, ctx.serverLevel())));
        ctx.thenTeleport(0, PARTS_STAND, Direction.WEST)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    part(ctx, level, CRANK, TestMechanical.CRANK.get());
                    part(ctx, level, CRANK.east(), TestMechanical.SHAFT.get());
                    part(ctx, level, SMALL_GEAR, TestMechanical.SMALL_GEAR.get());
                    part(ctx, level, LARGE_GEAR, TestMechanical.LARGE_GEAR.get());
                })
                .thenWaitUntil(() -> client(ctx, LARGE_GEAR) instanceof MechanicalPartBlockEntity gear
                        && Math.abs(gear.rpm()) > 1.0, WAIT, "the crank to turn the gears")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(SMALL_GEAR))))
                .thenWaitUntil(() -> KitInstancing.drawnLastFrame() >= 3, WAIT,
                        "the shaft and the gears to be drawn together as they turn")
                .thenAssert(5, () -> copper(ctx) > COPPERY, "the copper gears are seen in the middle of the view")
                .thenScreenshot(0, "turning-parts-together");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void aThousandDocks_drawNoSlowerTogetherThanOneByOne(final ClientTestContext ctx) {
        final double[] fps = new double[2];
        final int[] drawn = new int[1];
        final int[] calls = new int[1];
        final NativeImage[] field = new NativeImage[1];
        final Uncapped uncapped = new Uncapped(ctx.mc());
        final FrameRate alone = new FrameRate();
        final FrameRate together = new FrameRate();
        ctx.afterTest(() -> {
            if (field[0] != null) {
                field[0].close();
            }
            uncapped.restore();
            together(true);
            ctx.server().execute(() -> clearField(ctx, ctx.serverLevel()));
        });
        ctx.thenTeleport(0, FIELD_STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    level.setDayTime(DAY);
                    for (int x = 0; x < FIELD; x++) {
                        for (int z = 0; z < FIELD; z++) {
                            level.setBlock(ctx.abs(new BlockPos(x - FIELD / 2, 2, FIELD_NEAR + z)),
                                    TestModels.DOCK_BLOCK.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                    }
                })
                .then(2, () -> {
                    ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, 2, FIELD_NEAR + FIELD / 4))));
                    uncapped.apply();
                })
                .thenWaitUntil(() -> KitInstancing.drawnLastFrame() > FIELD * FIELD / 2, WAIT,
                        "most of the field to be drawn together")
                .thenScreenshot(SETTLE, "docks-together")
                .then(0, () -> {
                    drawn[0] = KitInstancing.drawnLastFrame();
                    calls[0] = KitInstancing.callsLastFrame();
                    field[0] = middle(ctx);
                })
                .thenWaitUntil(together, WARM + SECONDS * TICKS_A_SECOND + WAIT, "the frames drawn together counted")
                .then(0, () -> together(false))
                .thenWaitUntil(() -> KitInstancing.drawnLastFrame() == 0, WAIT, "the field drawn one by one")
                .then(SETTLE, () -> {
                    try (NativeImage each = middle(ctx)) {
                        ctx.assertSamePicture("field-together-and-one-by-one", each, field[0]);
                    }
                })
                .thenWaitUntil(alone, WARM + SECONDS * TICKS_A_SECOND + WAIT, "the frames one by one counted")
                .then(0, () -> {
                    fps[0] = alone.mean();
                    fps[1] = together.mean();
                    final BenchReport report = new BenchReport(KIT_BENCH)
                            .put("docks_drawn_together", drawn[0])
                            .put("calls_together", calls[0])
                            .put("fps_one_by_one", fps[0])
                            .put("fps_together", fps[1]);
                    write(KIT_BENCH, report);
                    ctx.assertTrue(fps[1] >= fps[0] * SLACK, "together the field came at " + fps[1]
                            + " frames a second, one by one at " + fps[0]);
                });
    }

    @ClientTest(timeoutTicks = 3600)
    public static void particles_drawNoSlowerTogetherThanOneByOne(final ClientTestContext ctx) {
        final Object[] holders = new Object[PLUMES_ACROSS * PLUMES_DEEP];
        final Uncapped uncapped = new Uncapped(ctx.mc());
        final FrameRate alone = new FrameRate();
        final FrameRate together = new FrameRate();
        final int[] drawn = new int[1];
        ctx.afterTest(() -> {
            for (final Object holder : holders) {
                ClientEffects.release(holder);
            }
            uncapped.restore();
            together(true);
        });
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .then(2, () -> {
                    for (int i = 0; i < holders.length; i++) {
                        holders[i] = new Object();
                        final BlockPos at = ctx.abs(new BlockPos((i % PLUMES_ACROSS - PLUMES_ACROSS / 2) * PLUME_GAP,
                                1, 4 + i / PLUMES_ACROSS * PLUME_GAP));
                        ClientEffects.holdOnBlock(holders[i], ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
                                "steam"), at);
                    }
                    ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, 4, 8))));
                    uncapped.apply();
                })
                .thenWaitUntil(() -> EffectEngine.particles() > holders.length * 100, WAIT,
                        "the plumes to fill out")
                .thenAssert(2, () -> EffectEngine.instancedLastFrame() > 0 && steam(ctx) > STEAMY,
                        "the particles are drawn together, the steam filling the middle of the view")
                .thenScreenshot(0, "steam-together")
                .then(0, () -> drawn[0] = EffectEngine.instancedLastFrame())
                .thenWaitUntil(together, WARM + SECONDS * TICKS_A_SECOND + WAIT, "the frames drawn together counted")
                .then(0, () -> together(false))
                .thenAssert(2, () -> EffectEngine.instancedLastFrame() == 0 && steam(ctx) > STEAMY,
                        "the particles are drawn one by one, the steam filling the middle of the view")
                .thenScreenshot(0, "steam-one-by-one")
                .thenWaitUntil(alone, WARM + SECONDS * TICKS_A_SECOND + WAIT, "the frames one by one counted")
                .then(0, () -> {
                    final BenchReport report = new BenchReport(PARTICLE_BENCH)
                            .put("particles_drawn_together", drawn[0])
                            .put("fps_one_by_one", alone.mean())
                            .put("fps_together", together.mean());
                    write(PARTICLE_BENCH, report);
                    ctx.assertTrue(together.mean() >= alone.mean() * SLACK, "together the plumes came at "
                            + together.mean() + " frames a second, one by one at " + alone.mean());
                });
    }

    /* Turns drawing alike things together on or off, as the player would in the settings. */
    private static void together(final boolean on) {
        DrawingSettings.FILE.set(DrawingSettings.INSTANCING, on);
    }

    /* How much of the middle of the frame is redder than it is green, as copper is and grass is not. */
    private static double copper(final ClientTestContext ctx) {
        try (NativeImage view = middle(ctx)) {
            int copper = 0;
            for (int y = 0; y < view.getHeight(); y++) {
                for (int x = 0; x < view.getWidth(); x++) {
                    final int pixel = view.getPixelRGBA(x, y);
                    if ((pixel & 0xFF) > (pixel >> 8 & 0xFF) + REDDER) {
                        copper++;
                    }
                }
            }
            return (double) copper / (view.getWidth() * view.getHeight());
        }
    }

    /* How much of the middle of the frame is near white, as steam is. */
    private static double steam(final ClientTestContext ctx) {
        try (NativeImage view = middle(ctx)) {
            int white = 0;
            for (int y = 0; y < view.getHeight(); y++) {
                for (int x = 0; x < view.getWidth(); x++) {
                    final int pixel = view.getPixelRGBA(x, y);
                    if ((pixel & 0xFF) > WHITE && (pixel >> 8 & 0xFF) > WHITE && (pixel >> 16 & 0xFF) > WHITE) {
                        white++;
                    }
                }
            }
            return (double) white / (view.getWidth() * view.getHeight());
        }
    }

    /* The middle of the frame, a third of it each way, clear of the hotbar. */
    private static NativeImage middle(final ClientTestContext ctx) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        return ctx.capture(width / 3, height / 3, width / 3, height / 3);
    }

    private static void write(final String name, final BenchReport report) {
        LOGGER.info("[JSC-BENCH] {} {}", name, report.summary());
        try {
            report.write();
        } catch (final IOException failed) {
            LOGGER.warn("[JSC-BENCH] the numbers of {} could not be written", name, failed);
        }
    }

    private static void part(final ClientTestContext ctx, final ServerLevel level, final BlockPos at,
                             final Block block) {
        level.setBlockAndUpdate(ctx.abs(at), block.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
                Direction.Axis.X));
    }

    private static BlockEntity client(final ClientTestContext ctx, final BlockPos at) {
        return ctx.mc().level == null ? null : ctx.mc().level.getBlockEntity(ctx.abs(at));
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = -4; x <= 2; x++) {
            for (int y = 2; y <= 5; y++) {
                for (int z = 0; z <= 6; z++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void clearField(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = 0; x < FIELD; x++) {
            for (int z = 0; z < FIELD; z++) {
                level.setBlock(ctx.abs(new BlockPos(x - FIELD / 2, 2, FIELD_NEAR + z)),
                        Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    /* The frames a second, read once a second while it is waited on, until it has read enough. */
    private static final class FrameRate implements BooleanSupplier {

        private int ticks;
        private int read;
        private double sum;

        @Override
        public boolean getAsBoolean() {
            this.ticks++;
            if (this.ticks > WARM && this.ticks % TICKS_A_SECOND == 0) {
                this.sum += Minecraft.getInstance().getFps();
                this.read++;
            }
            return this.read >= SECONDS;
        }

        private double mean() {
            return this.read == 0 ? 0 : this.sum / this.read;
        }
    }

    /* The game's frames let run as fast as they come, then put back as the player had them. */
    private static final class Uncapped {

        private final Minecraft mc;
        private final int limit;
        private final boolean vsync;

        private Uncapped(final Minecraft mc) {
            this.mc = mc;
            this.limit = mc.options.framerateLimit().get();
            this.vsync = mc.options.enableVsync().get();
        }

        private void apply() {
            this.mc.options.framerateLimit().set(UNLIMITED);
            this.mc.options.enableVsync().set(false);
        }

        private void restore() {
            this.mc.options.framerateLimit().set(this.limit);
            this.mc.options.enableVsync().set(this.vsync);
        }
    }
}
