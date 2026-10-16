/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.effect.ClientEffects;
import dev.jstech.core.client.effect.EffectEditorScreen;
import dev.jstech.core.client.effect.EffectEngine;
import dev.jstech.core.client.effect.EffectFileWriter;
import dev.jstech.core.client.effect.EffectTools;
import dev.jstech.core.client.effect.NewEffectScreen;
import dev.jstech.core.client.effect.ScreenEffects;
import dev.jstech.core.effect.EffectDefinition;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Effects as a player sees them: steam rising from a block and drifting with the world's wind; sparks, an arc flash
 * and a laser at night; bubbles that live only in water; scorch on the ground; the screen's flash, shake, vignette and
 * the ring of a shockwave; the effects editor open on the real block, an emitter turned off and the file saved; and a
 * new effect made from a starting point and opened in the editor.
 */
public final class EffectsClientTests {

    private static final BlockPos TOWER = new BlockPos(0, 2, 4);
    private static final BlockPos STAND = new BlockPos(0, 3, -2);
    private static final BlockPos POOL = new BlockPos(3, 1, 4);
    private static final long NIGHT = 18_000L;
    private static final long DAY = 6_000L;
    private static final int WAIT = 300;
    private static final Object TOWER_HOLDER = new Object();

    private EffectsClientTests() {
    }

    @ClientTest(timeoutTicks = 900)
    public static void steam_risesFromABlockAndDriftsWithTheWind(final ClientTestContext ctx) {
        ctx.afterTest(() -> ClientEffects.release(TOWER_HOLDER));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    for (int y = 2; y <= 4; y++) {
                        level.setBlockAndUpdate(ctx.abs(TOWER.atY(y)), Blocks.STONE_BRICKS.defaultBlockState());
                    }
                })
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.getBlockState(ctx.abs(TOWER.atY(4)))
                        .is(Blocks.STONE_BRICKS), WAIT, "the tower on the player's game")
                .then(0, () -> ClientEffects.holdOnBlock(TOWER_HOLDER, rl("steam"), ctx.abs(TOWER.atY(4))))
                .thenWaitUntil(() -> EffectEngine.particles() > 60, WAIT, "the plume to fill out")
                .then(40, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(TOWER.atY(6)))))
                .thenScreenshot(10, "steam-plume")
                .then(0, () -> ClientEffects.release(TOWER_HOLDER))
                .thenWaitUntil(() -> EffectEngine.particles() == 0, WAIT, "the plume to thin out once let go")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 900)
    public static void sparksArcAndLaser_atNight(final ClientTestContext ctx) {
        final Vec3 arc = Vec3.atCenterOf(ctx.abs(TOWER.atY(3)));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(NIGHT);
                    level.setBlockAndUpdate(ctx.abs(TOWER.atY(2)), Blocks.IRON_BLOCK.defaultBlockState());
                })
                .thenWaitUntil(() -> night(ctx), WAIT, "night on the player's game")
                .then(2, () -> {
                    ClientEffects.play(rl("arc_flash"), arc, new Vec3(0, 1, 0));
                    ClientEffects.play(rl("sparks"), arc.add(-2, 0, 0), new Vec3(0, 1, 0));
                    ClientEffects.play(rl("laser"), arc.add(2, 1.5, -3), new Vec3(0, 0, 1), arc.add(2, 0, 4));
                    ctx.lookAt(arc.add(0, 1, 0));
                })
                .thenScreenshot(4, "sparks-arc-laser-at-night")
                .thenAssert(0, () -> EffectEngine.particles() > 20, "sparks fly")
                .thenServer(0, level -> {
                    level.setDayTime(DAY);
                    clear(ctx, level);
                });
    }

    @ClientTest(timeoutTicks = 900)
    public static void bubbles_liveOnlyInWater(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    // A pool sunk into the ground, the grass round it holding it in.
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            level.setBlockAndUpdate(ctx.abs(POOL.offset(x, 0, z)), Blocks.WATER.defaultBlockState());
                        }
                    }
                })
                .thenWaitUntil(() -> ctx.mc().level != null && !ctx.mc().level.getFluidState(ctx.abs(POOL))
                        .isEmpty(), WAIT, "the pool on the player's game")
                .then(0, () -> ClientEffects.hold(TOWER_HOLDER, rl("bubbles"), () -> Vec3.atCenterOf(ctx.abs(POOL))
                        .add(0, -0.3, 0), () -> new Vec3(0, 1, 0)))
                .thenWaitUntil(() -> EffectEngine.particles() > 5, WAIT, "bubbles to rise")
                .then(30, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(POOL))))
                .thenScreenshot(4, "bubbles-in-water")
                .then(0, () -> ClientEffects.release(TOWER_HOLDER))
                .thenServer(0, level -> {
                    clear(ctx, level);
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            level.setBlockAndUpdate(ctx.abs(POOL.offset(x, 0, z)), Blocks.GRASS_BLOCK
                                    .defaultBlockState());
                        }
                    }
                });
    }

    @ClientTest(timeoutTicks = 900)
    public static void screen_flashesShakesAndBendsAtAShockwave(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setBlockAndUpdate(ctx.abs(TOWER.atY(2)), Blocks.IRON_BLOCK.defaultBlockState());
                })
                .then(10, () -> {
                    ctx.lookAt(Vec3.atCenterOf(ctx.abs(TOWER.atY(2))));
                    ClientEffects.playOnBlock(rl("scorch"), ctx.abs(TOWER.atY(2)));
                    ScreenEffects.fire(Vec3.atCenterOf(ctx.abs(TOWER.atY(3))), 0.3F, 0.6F, 1.0F, 1.5F);
                    ScreenEffects.vignette(rl("test"), 0xFF << 24 | 0x30FF60, 0.8F);
                })
                .thenAssert(2, ScreenEffects::active, "the screen's effects are on")
                .thenScreenshot(6, "screen-shockwave-vignette-scorch")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 900)
    public static void editor_playsOnTheBlockTurnsAnEmitterOffAndSaves(final ClientTestContext ctx) {
        final Path root = temporary();
        ctx.afterTest(() -> EffectFileWriter.writeInto(null));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    for (int y = 2; y <= 4; y++) {
                        level.setBlockAndUpdate(ctx.abs(TOWER.atY(y)), Blocks.STONE_BRICKS.defaultBlockState());
                    }
                })
                .then(10, () -> {
                    EffectFileWriter.writeInto(root);
                    ctx.lookAt(Vec3.atCenterOf(ctx.abs(TOWER.atY(4))));
                    EffectTools.edit(ctx.mc(), ctx.mc().level, ctx.mc().player, rl("sparks"),
                            ctx.abs(TOWER.atY(4)));
                })
                .thenWaitUntil(() -> ctx.mc().screen instanceof EffectEditorScreen, WAIT, "the editor to open")
                .thenWaitUntil(() -> EffectEngine.particles() > 10, WAIT, "the effect to play on the block")
                .thenScreenshot(20, "effects-editor")
                .then(0, () -> ((EffectEditorScreen) ctx.mc().screen).enable(0, false))
                .thenAssert(2, () -> !((EffectEditorScreen) ctx.mc().screen).definition().emitters().getFirst()
                        .enabled(), "the sparks turned off")
                .thenAssert(0, () -> {
                    final Path saved = ((EffectEditorScreen) ctx.mc().screen).save();
                    return saved != null && saved.startsWith(root) && !read(saved).emitters().getFirst().enabled();
                }, "the file written, the sparks off in it")
                .then(0, () -> ctx.mc().setScreen(null))
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 900)
    public static void newEffect_startsFromSmokeAndOpensInTheEditor(final ClientTestContext ctx) {
        final Path root = temporary();
        ctx.afterTest(() -> EffectFileWriter.writeInto(null));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    for (int y = 2; y <= 4; y++) {
                        level.setBlockAndUpdate(ctx.abs(TOWER.atY(y)), Blocks.STONE_BRICKS.defaultBlockState());
                    }
                })
                .then(10, () -> {
                    EffectFileWriter.writeInto(root);
                    ctx.lookAt(Vec3.atCenterOf(ctx.abs(TOWER.atY(4))));
                    ctx.mc().setScreen(new NewEffectScreen(ctx.abs(TOWER.atY(4)), ResourceLocation
                            .fromNamespaceAndPath("jstests", "test_tower"), List.of()));
                })
                .then(4, () -> ((NewEffectScreen) ctx.mc().screen).startFrom(0))
                .thenWaitUntil(() -> EffectEngine.particles() > 10, WAIT, "the smoke to play as it starts")
                .thenScreenshot(20, "new-effect")
                .thenAssert(0, () -> ((NewEffectScreen) ctx.mc().screen).create(), "the file made")
                .thenAssert(2, () -> ctx.mc().screen instanceof EffectEditorScreen, "opened in the editor")
                .thenAssert(0, () -> Files.exists(root.resolve("assets/jstests/effects/test_tower_smoke.json")),
                        "under the block's mod")
                .then(0, () -> ctx.mc().setScreen(null))
                .thenServer(0, level -> clear(ctx, level));
    }

    private static ResourceLocation rl(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }

    private static boolean night(final ClientTestContext ctx) {
        return ctx.mc().level != null && ctx.mc().level.getDayTime() % 24_000L > 13_000L;
    }

    private static Path temporary() {
        try {
            return Files.createTempDirectory("jscore-effects");
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static EffectDefinition read(final Path file) {
        try {
            return EffectDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(file)))
                    .getOrThrow();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /* Everything the tests place, back to air. */
    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = -3; x <= 4; x++) {
            for (int y = 2; y <= 6; y++) {
                for (int z = 1; z <= 7; z++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }
}
