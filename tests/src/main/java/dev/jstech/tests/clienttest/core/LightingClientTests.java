/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.logging.LogUtils;
import dev.jstech.core.client.light.ColouredLights;
import dev.jstech.core.client.instancing.Instancing;
import dev.jstech.core.client.light.DynamicLightsClient;
import dev.jstech.core.light.LampBlockEntity;
import dev.jstech.core.light.LightBlock;
import dev.jstech.core.light.LightingSettings;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.TestLights;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * How light looks to a player: a green lamp tints the floor it lights green, in the corners of the floor's faces as
 * the world's mesh is built; white light through a wall of red glass lights the floor past it red, and the sun through
 * a roof of green glass the floor under it green; a field of sea lanterns' light spread away from the main thread,
 * which only hands it over; a lamp placed with a click, lit and tinting its floor within a tick of showing, and a
 * soul lantern's blue seen and gone with the lantern; the colours under a deferred shader pack, run with Iris; a torch
 * held lights the way on the player's own game, and the way goes dark once it is put away; a floodlight's shaft
 * through the night air, lighting a wall twenty blocks off; and the halos of bright lamps in the dark, whole over the
 * floor, gone behind stone and there behind glass, and none about glow lichen, which glows in its own drawing.
 */
public final class LightingClientTests {

    private static final BlockPos LAMP = new BlockPos(0, 2, 3);
    private static final BlockPos FLOOR = new BlockPos(1, 1, 3);
    private static final BlockPos STAND = new BlockPos(0, 3, -2);
    private static final BlockPos FLOOD = new BlockPos(-8, 2, 6);
    private static final int WALL_X = 12;
    private static final BlockPos FLOOD_STAND = new BlockPos(2, 3, -4);
    private static final BlockPos PLACE_STAND = new BlockPos(-2, 2, 2);
    private static final BlockPos PLACED = new BlockPos(-2, 2, 4);
    private static final BlockPos BESIDE_PLACED = new BlockPos(-1, 1, 4);
    private static final BlockPos BLUE = new BlockPos(2, 2, 5);
    private static final BlockPos BESIDE_BLUE = new BlockPos(3, 1, 5);
    /* The fragile lamp's level, as its kind declares it. */
    private static final int FRAGILE_LEVEL = 12;
    private static final int SETTLE = 5;
    private static final long NIGHT = 18_000L;
    private static final long DAY = 6_000L;
    private static final long FULL_DAY = 24_000L;
    private static final long DUSK = 13_000L;
    private static final int WAIT = 300;
    /*
     * The halo read off the screen: how far above and below the lamp, in the GUI's pixels, past the lamp itself and
     * within its glow, and how far aside of the crosshair, past its arms; over how many pixels each is read; how much
     * brighter a glow seen makes them at least, and a hidden one at most, in the sum of the three channels; how much
     * of the brightening above shows below too; and the ticks for a change of the glow to be drawn.
     */
    private static final int GLARE_OFF = 36;
    private static final int GLARE_NEAR = 12;
    private static final int GLARE_PIXELS = 9;
    private static final int GLARE_SEEN = 20;
    private static final int GLARE_HIDDEN = 8;
    private static final double GLARE_WHOLE = 0.5;
    private static final int GLARE_SETTLE = 5;
    private static final int CHANNEL = 0xFF;
    private static final int GREEN_SHIFT = 8;
    private static final int BLUE_SHIFT = 16;
    private static final long SEED = 42L;
    /* A corner's colour left as the face's own: white, wholly opaque. */
    private static final int WHITE_CORNER = 0xFFFFFFFF;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int FIELD_SIDE = 10;
    private static final int FIELD_STEP = 4;
    private static final int FIELD_X = 8;
    private static final int SAMPLES = 100;
    /* What the main thread may spend on coloured light a tick, on average, with the field's light to look after. */
    private static final double MOST_MAIN_MILLIS = 0.5;
    private static final String IRIS = "iris";
    private static final String IRIS_CLASS = "net.irisshaders.iris.Iris";
    /* A deferred pack, which the run's shaderpacks folder holds for a run with Iris. */
    private static final String PACK = "ComplementaryReimagined_r5.9.3.zip";
    private static final int PACK_WAIT = 600;

    private LightingClientTests() {
    }

    @ClientTest(timeoutTicks = 900)
    public static void colouredLight_tintsTheFloorAGreenLampLights(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.serverPlayer().serverLevel().getServer().execute(() -> floor(ctx,
                ctx.serverPlayer().serverLevel(), Blocks.GRASS_BLOCK.defaultBlockState())));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(NIGHT);
                    // A white floor shows the colours; a red alarm and a blue soul lantern beside the green lamp.
                    floor(ctx, level, Blocks.WHITE_CONCRETE.defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(LAMP), TestLights.FRAGILE_LAMP.get().defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(LAMP.west(5)), TestLights.ALARM_LAMP.get().defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(LAMP.west(5).above()), Blocks.REDSTONE_BLOCK
                            .defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(LAMP.east(5)), Blocks.SOUL_LANTERN.defaultBlockState());
                })
                .thenWaitUntil(() -> lit(ctx, LAMP), WAIT, "the lamp to be on on the player's game")
                .thenWaitUntil(() -> greener(ctx), WAIT, "the floor beside it to be tinted green")
                .thenWaitUntil(() -> night(ctx), WAIT, "night on the player's game")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(FLOOR))))
                .thenScreenshot(20, "coloured-lamps-tint-a-white-floor")
                .thenServer(0, level -> {
                    level.setDayTime(DAY);
                    clear(ctx, level);
                    floor(ctx, level, Blocks.GRASS_BLOCK.defaultBlockState());
                });
    }

    @ClientTest(timeoutTicks = 900)
    public static void colouredLight_takesTheColourOfStainedGlassItPasses(final ClientTestContext ctx) {
        final BlockPos glowstone = new BlockPos(-7, 2, 3);
        final BlockPos past = new BlockPos(-3, 1, 3);
        final BlockPos before = new BlockPos(-8, 1, 3);
        ctx.afterTest(() -> ctx.serverPlayer().serverLevel().getServer().execute(() -> floor(ctx,
                ctx.serverPlayer().serverLevel(), Blocks.GRASS_BLOCK.defaultBlockState())));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(NIGHT);
                    floor(ctx, level, Blocks.WHITE_CONCRETE.defaultBlockState());
                    // White light on one side of a wall of red glass.
                    level.setBlockAndUpdate(ctx.abs(glowstone), Blocks.GLOWSTONE.defaultBlockState());
                    for (int y = 2; y <= 5; y++) {
                        for (int z = 0; z <= 7; z++) {
                            level.setBlockAndUpdate(ctx.abs(new BlockPos(-5, y, z)), Blocks.RED_STAINED_GLASS
                                    .defaultBlockState());
                        }
                    }
                })
                .thenWaitUntil(() -> night(ctx), WAIT, "night on the player's game")
                .thenWaitUntil(() -> redder(top(ctx, past)), WAIT,
                        "the floor past the glass to be lit red by the white light through it")
                .thenAssert(0, () -> top(ctx, before) == WHITE_CORNER, "and white on the glowstone's side: "
                        + Integer.toHexString(top(ctx, before)))
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(-5, 2, 3)))))
                .thenScreenshot(20, "white-light-through-red-glass")
                .thenServer(0, level -> {
                    level.setDayTime(DAY);
                    clear(ctx, level);
                    floor(ctx, level, Blocks.GRASS_BLOCK.defaultBlockState());
                });
    }

    @ClientTest(timeoutTicks = 900)
    public static void daylight_takesTheColourOfAGlassRoof(final ClientTestContext ctx) {
        final BlockPos under = new BlockPos(-2, 1, 3);
        final BlockPos outside = new BlockPos(3, 1, 3);
        ctx.afterTest(() -> ctx.serverPlayer().serverLevel().getServer().execute(() -> floor(ctx,
                ctx.serverPlayer().serverLevel(), Blocks.GRASS_BLOCK.defaultBlockState())));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(DAY);
                    floor(ctx, level, Blocks.WHITE_CONCRETE.defaultBlockState());
                    for (int x = -5; x <= 0; x++) {
                        for (int z = 1; z <= 5; z++) {
                            level.setBlockAndUpdate(ctx.abs(new BlockPos(x, 5, z)), Blocks.LIME_STAINED_GLASS
                                    .defaultBlockState());
                        }
                    }
                })
                .thenWaitUntil(() -> greener(top(ctx, under)), WAIT,
                        "the floor under the roof to be lit green by the sun through it")
                .thenAssert(0, () -> top(ctx, outside) == WHITE_CORNER, "and white in the open: "
                        + Integer.toHexString(top(ctx, outside)))
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(under))))
                .thenScreenshot(20, "sun-through-a-green-glass-roof")
                .thenServer(0, level -> {
                    clear(ctx, level);
                    floor(ctx, level, Blocks.GRASS_BLOCK.defaultBlockState());
                });
    }

    @ClientTest(timeoutTicks = 1200)
    public static void colouredLight_isWorkedOutAwayFromTheMainThread(final ClientTestContext ctx) {
        final double[] taken = new double[2];
        final int[] ticks = new int[1];
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    level.setDayTime(NIGHT);
                    field(ctx, level, Blocks.SEA_LANTERN.defaultBlockState());
                })
                .thenWaitUntil(() -> ColouredLights.spread() >= FIELD_SIDE * FIELD_SIDE, WAIT,
                        "every sea lantern's light to be spread through the blocks")
                .thenWaitUntil(() -> {
                    taken[0] += ColouredLights.lastTickMillis();
                    taken[1] = Math.max(taken[1], ColouredLights.lastTickMillis());
                    return ++ticks[0] >= SAMPLES;
                }, SAMPLES + 20, "the main thread's share to be timed over " + SAMPLES + " ticks")
                .then(0, () -> LOGGER.info("[JSC-BENCH] coloured light {} sources: main thread {} ms a tick on "
                        + "average, {} ms at most", ColouredLights.spread(), String.format(Locale.ROOT, "%.3f",
                        taken[0] / ticks[0]), String.format(Locale.ROOT, "%.3f", taken[1])))
                .thenAssert(0, () -> taken[0] / ticks[0] < MOST_MAIN_MILLIS, "the main thread only hands the "
                        + "batches over and takes them back: " + taken[0] / ticks[0] + " ms a tick")
                .thenServer(0, level -> {
                    field(ctx, level, Blocks.AIR.defaultBlockState());
                    level.setDayTime(DAY);
                });
    }

    /*
     * Run with -PwithIris and a deferred pack in the run's shaderpacks folder; without Iris, the test says only that no
     * pack draws.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void colouredLight_showsUnderADeferredShaderPack(final ClientTestContext ctx) {
        if (!ModList.get().isLoaded(IRIS)) {
            ctx.thenAssert(0, () -> !Instancing.shaderPack(), "without Iris no shader pack draws the world");
            return;
        }
        ctx.afterTest(() -> shaders(null));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(NIGHT);
                    floor(ctx, level, Blocks.WHITE_CONCRETE.defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(LAMP), TestLights.FRAGILE_LAMP.get().defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(LAMP.west(5)), TestLights.ALARM_LAMP.get().defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(LAMP.west(5).above()), Blocks.REDSTONE_BLOCK
                            .defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(LAMP.east(5)), Blocks.SOUL_LANTERN.defaultBlockState());
                })
                .thenWaitUntil(() -> lit(ctx, LAMP), WAIT, "the lamp to be on on the player's game")
                .thenWaitUntil(() -> night(ctx), WAIT, "night on the player's game")
                .thenWaitUntil(() -> greener(ctx), WAIT, "the floor to be tinted green without a pack")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(FLOOR))))
                .thenScreenshot(10, "coloured-light-without-a-pack")
                .then(0, () -> shaders(PACK))
                .thenWaitUntil(Instancing::shaderPack, PACK_WAIT, "the pack " + PACK + " to draw the world")
                .thenAssert(40, () -> greener(ctx), "the floor's faces still carry the green the pack draws them in")
                .thenScreenshot(20, "coloured-light-under-a-deferred-pack")
                .then(0, () -> shaders(null))
                .thenWaitUntil(() -> !Instancing.shaderPack(), PACK_WAIT, "the pack to be put away")
                .thenServer(0, level -> {
                    level.setDayTime(DAY);
                    clear(ctx, level);
                    floor(ctx, level, Blocks.GRASS_BLOCK.defaultBlockState());
                });
    }

    @ClientTest(timeoutTicks = 900)
    public static void lamp_lightsTheTickItIsPlaced(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
        ctx.thenTeleport(0, PLACE_STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(NIGHT);
                    floor(ctx, level, Blocks.WHITE_CONCRETE.defaultBlockState());
                })
                .thenWaitUntil(() -> night(ctx), WAIT, "night on the player's game")
                .thenGive(0, new ItemStack(TestLights.FRAGILE_LAMP.item()))
                .then(SETTLE, () -> ctx.selectHotbar(0))
                // A click as the player's: the lamp is lit, lights its place and tints the floor beside it green within
                // a tick of showing on their game, not when its section's turn comes.
                .thenPlace(1, PLACED)
                .thenWaitUntil(() -> clientState(ctx, PLACED).getBlock() == TestLights.FRAGILE_LAMP.get(), WAIT,
                        "the lamp placed on the player's game")
                .thenWaitUntil(() -> lit(ctx, PLACED) && lightAt(ctx, PLACED) >= FRAGILE_LEVEL, 1,
                        "the lamp lit and its light laid within a tick of it showing", () -> "lit=" + lit(ctx, PLACED)
                                + " light=" + lightAt(ctx, PLACED))
                .thenWaitUntil(() -> greener(top(ctx, BESIDE_PLACED)), 1, "the floor beside it green within a tick "
                        + "of it lighting", () -> "corner " + Integer.toHexString(top(ctx, BESIDE_PLACED)))
                .thenServer(0, level -> {
                    level.setDayTime(DAY);
                    clear(ctx, level);
                    floor(ctx, level, Blocks.GRASS_BLOCK.defaultBlockState());
                });
    }

    @ClientTest(timeoutTicks = 900)
    public static void colouredLight_showsTheTickItsBlockDoes(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(NIGHT);
                    floor(ctx, level, Blocks.WHITE_CONCRETE.defaultBlockState());
                })
                .thenWaitUntil(() -> night(ctx), WAIT, "night on the player's game")
                .thenWaitUntil(() -> top(ctx, BESIDE_BLUE) == WHITE_CORNER, WAIT, "the floor white to begin with")
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(BLUE), Blocks.SOUL_LANTERN
                        .defaultBlockState()))
                .thenWaitUntil(() -> clientState(ctx, BLUE).is(Blocks.SOUL_LANTERN), WAIT,
                        "the soul lantern on the player's game")
                .thenWaitUntil(() -> bluer(top(ctx, BESIDE_BLUE)), 1, "the floor beside it blue within a tick of it "
                        + "showing", () -> "corner " + Integer.toHexString(top(ctx, BESIDE_BLUE)))
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(BLUE), Blocks.AIR.defaultBlockState()))
                .thenWaitUntil(() -> clientState(ctx, BLUE).isAir(), WAIT, "the lantern gone on the player's game")
                .thenWaitUntil(() -> top(ctx, BESIDE_BLUE) == WHITE_CORNER, 1, "the blue gone within a tick of it",
                        () -> "corner " + Integer.toHexString(top(ctx, BESIDE_BLUE)))
                .thenServer(0, level -> {
                    level.setDayTime(DAY);
                    clear(ctx, level);
                    floor(ctx, level, Blocks.GRASS_BLOCK.defaultBlockState());
                });
    }

    @ClientTest(timeoutTicks = 900)
    public static void dynamicLight_aTorchHeldLightsTheWay(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(NIGHT);
                    ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
                })
                .thenWaitUntil(() -> !DynamicLightsClient.laid().isEmpty() && aroundPlayer(ctx) >= 12, WAIT,
                        "the torch's light laid about the player on their own game")
                .thenWaitUntil(() -> night(ctx), WAIT, "night on the player's game")
                .thenWaitUntilServer(level -> !(level.getBlockState(eye(ctx)).getBlock() instanceof LightBlock), 2,
                        "none laid in the server's world", level -> "the server holds "
                                + level.getBlockState(eye(ctx)) + " at the player's eyes")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(FLOOR))))
                .thenScreenshot(10, "torch-held-lights-the-way")
                .thenServer(0, level -> ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND,
                        ItemStack.EMPTY))
                .thenWaitUntil(() -> DynamicLightsClient.laid().isEmpty() && aroundPlayer(ctx) == 0, WAIT,
                        "the way to go dark once the torch is put away")
                .thenServer(0, level -> {
                    level.setDayTime(DAY);
                    clear(ctx, level);
                });
    }

    @ClientTest(timeoutTicks = 900)
    public static void floodlight_shaftLightsAWallFarOff(final ClientTestContext ctx) {
        final BlockPos lit = new BlockPos(WALL_X - 1, FLOOD.getY(), FLOOD.getZ());
        ctx.thenTeleport(0, FLOOD_STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(NIGHT);
                    for (int y = 1; y <= 6; y++) {
                        for (int z = FLOOD.getZ() - 4; z <= FLOOD.getZ() + 4; z++) {
                            level.setBlockAndUpdate(ctx.abs(new BlockPos(WALL_X, y, z)),
                                    Blocks.STONE.defaultBlockState());
                        }
                    }
                    level.setBlockAndUpdate(ctx.abs(FLOOD), TestLights.FLOODLIGHT.get().defaultBlockState()
                            .setValue(DirectionalBlock.FACING, Direction.EAST));
                })
                .thenWaitUntil(() -> clientState(ctx, lit).getBlock() instanceof LightBlock, WAIT,
                        "the wall's light, twenty blocks off, laid by the server and seen on the player's game")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(2, 3, FLOOD.getZ())))))
                .thenScreenshot(20, "floodlight-shaft-at-night")
                .thenServer(0, level -> {
                    level.setDayTime(DAY);
                    clear(ctx, level);
                    for (int y = 1; y <= 6; y++) {
                        for (int z = FLOOD.getZ() - 4; z <= FLOOD.getZ() + 4; z++) {
                            level.setBlockAndUpdate(ctx.abs(new BlockPos(WALL_X, y, z)),
                                    Blocks.AIR.defaultBlockState());
                        }
                    }
                });
    }

    @ClientTest(timeoutTicks = 900)
    public static void halos_glowAboutBrightLampsInTheDark(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(NIGHT);
                    level.setBlockAndUpdate(ctx.abs(LAMP), TestLights.FRAGILE_LAMP.get().defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(LAMP.west(3)), Blocks.SOUL_LANTERN.defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(LAMP.east(3)), Blocks.SEA_LANTERN.defaultBlockState());
                })
                .thenWaitUntil(() -> lit(ctx, LAMP), WAIT, "the lamp to be on on the player's game")
                .then(30, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(LAMP))))
                .thenScreenshot(10, "halos-at-night")
                .thenServer(0, level -> {
                    level.setDayTime(DAY);
                    clear(ctx, level);
                });
    }

    /*
     * A lamp on the floor seen from above at night, its halo read off the screen against the same view with no glow:
     * as bright under the lamp, over the floor, as above it, never cut by the floor; gone behind a stone wall, which
     * hides the lamp; and there behind a glass one, which does not.
     */
    @ClientTest(timeoutTicks = 1200)
    public static void halos_areWholeOverTheFloorAndHiddenOnlyByWhatHidesTheLamp(final ClientTestContext ctx) {
        final int[][] glowing = new int[3][];
        final int[][] dark = new int[3][];
        ctx.afterTest(() -> LightingSettings.FILE.set(LightingSettings.BLOOM, 1.0));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(NIGHT);
                    level.setBlockAndUpdate(ctx.abs(LAMP), TestLights.FRAGILE_LAMP.get().defaultBlockState());
                })
                .thenWaitUntil(() -> lit(ctx, LAMP), WAIT, "the lamp to be on on the player's game")
                .then(30, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(LAMP))));
        for (int seen = 0; seen < glowing.length; seen++) {
            final int phase = seen;
            if (phase > 0) {
                ctx.thenServer(0, level -> {
                    for (int x = -2; x <= 2; x++) {
                        for (int y = 2; y <= 5; y++) {
                            level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, 0)), phase == 1
                                    ? Blocks.STONE.defaultBlockState() : Blocks.GLASS.defaultBlockState());
                        }
                    }
                });
            }
            ctx.then(GLARE_SETTLE, () -> glowing[phase] = glare(ctx))
                    .thenScreenshot(0, "halo-" + new String[] {"on-the-floor", "behind-stone", "behind-glass"}[phase])
                    .then(0, () -> LightingSettings.FILE.set(LightingSettings.BLOOM, 0.0))
                    .then(GLARE_SETTLE, () -> dark[phase] = glare(ctx))
                    .then(0, () -> LightingSettings.FILE.set(LightingSettings.BLOOM, 1.0));
        }
        ctx.then(0, () -> {
            final int above = glowing[0][0] - dark[0][0];
            final int below = glowing[0][1] - dark[0][1];
            ctx.assertTrue(above >= GLARE_SEEN && below >= above * GLARE_WHOLE, "the halo is whole, as bright over"
                    + " the floor under the lamp as above it: " + above + " above, " + below + " below");
            final int stone = Math.max(glowing[1][0] - dark[1][0], glowing[1][1] - dark[1][1]);
            ctx.assertTrue(stone <= GLARE_HIDDEN, "behind a stone wall the lamp's halo is gone: " + stone);
            final int glass = Math.min(glowing[2][0] - dark[2][0], glowing[2][1] - dark[2][1]);
            ctx.assertTrue(glass >= GLARE_SEEN, "behind glass the lamp's halo is there: " + glass);
        }).thenServer(0, level -> {
            level.setDayTime(DAY);
            clear(ctx, level);
        });
    }

    /*
     * Glow lichen on the floor at night, read off the screen against the same view with no glow: it glows in its own
     * drawing and has no halo about it, at its middle or round it.
     */
    @ClientTest(timeoutTicks = 900)
    public static void halos_leaveGlowLichenToGlowInItsOwnDrawing(final ClientTestContext ctx) {
        final int[][] read = new int[2][];
        ctx.afterTest(() -> LightingSettings.FILE.set(LightingSettings.BLOOM, 1.0));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(NIGHT);
                    level.setBlockAndUpdate(ctx.abs(LAMP), Blocks.GLOW_LICHEN.defaultBlockState()
                            .setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true));
                })
                .thenWaitUntil(() -> ctx.mc().level.getBlockState(ctx.abs(LAMP)).is(Blocks.GLOW_LICHEN), WAIT,
                        "the lichen on the player's game")
                .then(30, () -> ctx.lookAt(Vec3.atBottomCenterOf(ctx.abs(LAMP))))
                .then(GLARE_SETTLE, () -> read[0] = glare(ctx))
                .thenScreenshot(0, "glow-lichen-at-night")
                .then(0, () -> LightingSettings.FILE.set(LightingSettings.BLOOM, 0.0))
                .then(GLARE_SETTLE, () -> read[1] = glare(ctx))
                .then(0, () -> LightingSettings.FILE.set(LightingSettings.BLOOM, 1.0))
                .then(0, () -> {
                    int most = 0;
                    for (int side = 0; side < read[0].length; side++) {
                        most = Math.max(most, read[0][side] - read[1][side]);
                    }
                    ctx.assertTrue(most <= GLARE_HIDDEN, "glow lichen has no halo about it: " + most);
                })
                .thenServer(0, level -> {
                    level.setDayTime(DAY);
                    clear(ctx, level);
                });
    }

    /*
     * How bright the screen is a little above and a little below its middle, where the light looked at stands, and
     * just left and right of the crosshair, which inverts what is under it, each the sum of its red, green and blue
     * over a few pixels.
     */
    private static int[] glare(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        final int[][] points = {{x, y - GLARE_OFF}, {x, y + GLARE_OFF}, {x - GLARE_NEAR, y}, {x + GLARE_NEAR, y}};
        final int[] bright = new int[points.length];
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int side = 0; side < bright.length; side++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        final int argb = frame.pixel(points[side][0] + dx, points[side][1] + dy);
                        bright[side] += (argb >> BLUE_SHIFT & CHANNEL) + (argb >> GREEN_SHIFT & CHANNEL)
                                + (argb & CHANNEL);
                    }
                }
                bright[side] /= GLARE_PIXELS;
            }
        }
        return bright;
    }

    /* Whether it is night on the player's game, which hears the time a second at a time. */
    private static boolean night(final ClientTestContext ctx) {
        final ClientLevel level = ctx.mc().level;
        return level != null && level.getDayTime() % FULL_DAY > DUSK;
    }

    /* The floor under the tests' blocks, laid of {@code state}. */
    private static void floor(final ClientTestContext ctx, final ServerLevel level, final BlockState state) {
        for (int x = -9; x <= 4; x++) {
            for (int z = 0; z <= 7; z++) {
                level.setBlockAndUpdate(ctx.abs(new BlockPos(x, 1, z)), state);
            }
        }
    }

    /* Whether the lamp at {@code at} is on on the player's game. */
    private static boolean lit(final ClientTestContext ctx, final BlockPos at) {
        final ClientLevel level = ctx.mc().level;
        return level != null && level.getBlockEntity(ctx.abs(at)) instanceof LampBlockEntity lamp && lamp.lit();
    }

    /* Whether the top of the floor beside the lamp is drawn greener than it is red and blue. */
    private static boolean greener(final ClientTestContext ctx) {
        final ClientLevel level = ctx.mc().level;
        if (level == null) {
            return false;
        }
        final BlockPos at = ctx.abs(FLOOR);
        final BlockState state = level.getBlockState(at);
        final BakedModel model = ctx.mc().getBlockRenderer().getBlockModel(state);
        final ModelData data = model.getModelData(level, at, state, ModelData.EMPTY);
        final List<BakedQuad> top = model.getQuads(state, Direction.UP, RandomSource.create(SEED), data, null);
        if (top.isEmpty()) {
            return false;
        }
        // A corner keeps its colour as red, green, blue and alpha from the lowest byte up.
        final int colour = top.getFirst().getVertices()[3];
        final int red = colour & CHANNEL;
        final int green = colour >> GREEN_SHIFT & CHANNEL;
        final int blue = colour >> BLUE_SHIFT & CHANNEL;
        return green == CHANNEL && red < CHANNEL && blue < CHANNEL;
    }

    /* Has Iris draw the world with the shader pack {@code pack}, or with none; through its own settings. */
    private static void shaders(@Nullable final String pack) {
        try {
            final Class<?> iris = Class.forName(IRIS_CLASS);
            final Object settings = iris.getMethod("getIrisConfig").invoke(null);
            if (pack != null) {
                settings.getClass().getMethod("setShaderPackName", String.class).invoke(settings, pack);
            }
            settings.getClass().getMethod("setShadersEnabled", boolean.class).invoke(settings, pack != null);
            settings.getClass().getMethod("save").invoke(settings);
            iris.getMethod("reload").invoke(null);
        } catch (final ReflectiveOperationException failed) {
            throw new IllegalStateException("Iris could not be told to draw with " + pack, failed);
        }
    }

    /* A field of {@code state}, ten by ten, four blocks apart, east of where the other tests stand. */
    private static void field(final ClientTestContext ctx, final ServerLevel level, final BlockState state) {
        for (int i = 0; i < FIELD_SIDE; i++) {
            for (int j = 0; j < FIELD_SIDE; j++) {
                level.setBlockAndUpdate(ctx.abs(new BlockPos(FIELD_X + i * FIELD_STEP, 2, j * FIELD_STEP)), state);
            }
        }
    }

    /* The colour of the first corner of the top of the block at {@code at}, as the world's mesh would draw it. */
    private static int top(final ClientTestContext ctx, final BlockPos at) {
        final ClientLevel level = ctx.mc().level;
        if (level == null) {
            return 0;
        }
        final BlockPos pos = ctx.abs(at);
        final BlockState state = level.getBlockState(pos);
        final BakedModel model = ctx.mc().getBlockRenderer().getBlockModel(state);
        final ModelData data = model.getModelData(level, pos, state, ModelData.EMPTY);
        final List<BakedQuad> faces = model.getQuads(state, Direction.UP, RandomSource.create(SEED), data, null);
        return faces.isEmpty() ? 0 : faces.getFirst().getVertices()[3];
    }

    /* Whether a corner's colour, red, green, blue and alpha from the lowest byte up, is red over green and blue. */
    private static boolean redder(final int corner) {
        final int red = corner & CHANNEL;
        return red == CHANNEL && (corner >> GREEN_SHIFT & CHANNEL) < CHANNEL && (corner >> BLUE_SHIFT & CHANNEL)
                < CHANNEL;
    }

    /* Whether a corner's colour is blue over red and green. */
    private static boolean bluer(final int corner) {
        return (corner >> BLUE_SHIFT & CHANNEL) == CHANNEL && (corner & CHANNEL) < CHANNEL
                && (corner >> GREEN_SHIFT & CHANNEL) < CHANNEL;
    }

    /* The block light at {@code at} on the player's game. */
    private static int lightAt(final ClientTestContext ctx, final BlockPos at) {
        final ClientLevel level = ctx.mc().level;
        return level == null ? 0 : level.getBrightness(LightLayer.BLOCK, ctx.abs(at));
    }

    /* Whether a corner's colour is green over red and blue. */
    private static boolean greener(final int corner) {
        return (corner >> GREEN_SHIFT & CHANNEL) == CHANNEL && (corner & CHANNEL) < CHANNEL
                && (corner >> BLUE_SHIFT & CHANNEL) < CHANNEL;
    }

    /* The light laid where the player's eyes are, on their own game. */
    private static int aroundPlayer(final ClientTestContext ctx) {
        final ClientLevel level = ctx.mc().level;
        return level == null || ctx.mc().player == null ? 0 : level.getBrightness(LightLayer.BLOCK,
                BlockPos.containing(ctx.mc().player.getEyePosition()));
    }

    private static BlockPos eye(final ClientTestContext ctx) {
        return BlockPos.containing(ctx.serverPlayer().getEyePosition());
    }

    private static BlockState clientState(final ClientTestContext ctx, final BlockPos at) {
        final ClientLevel level = ctx.mc().level;
        return level == null ? Blocks.AIR.defaultBlockState() : level.getBlockState(ctx.abs(at));
    }

    /* Everything the tests place, back to air. */
    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = -9; x <= 4; x++) {
            for (int y = 2; y <= 5; y++) {
                for (int z = 0; z <= 7; z++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }
}
