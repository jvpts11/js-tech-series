/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.mojang.logging.LogUtils;
import dev.jstech.core.client.effect.EffectEngine;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipReadings;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * A small ship's engines running on a player's game, as the approved mock draws them, its console told on the
 * player's game alone while the ship stands in its dock: in thick air its bells' mouths glow faintly, cold, and its
 * air engines blow their exhaust back out of their nozzles; where the bells push, in thin air, their mouths glow
 * bright and a jet grows out of each with its heat trail, longer boosting; stood on the land, all of it dies away.
 */
public final class ShipEnginesClientTests {

    /* The dock dug into the land where the test stands, as the shipyard's client test builds it. */
    private static final BlockPos CORNER = new BlockPos(-11, -1, 6);
    /* The camera, in the dock's own places: behind the ship's tail and to its left, looking down at its engines. */
    private static final Vec3 EYES = new Vec3(-6, 4, 18);
    private static final Vec3 AT = new Vec3(0, 1.5, 8);
    private static final int X0 = -13;
    private static final int X1 = 13;
    private static final int Z0 = 4;
    private static final int Z1 = 40;
    private static final int Y_BOTTOM = -2;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 16;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 20;
    /* Long enough for what was let go to die away, its heat trail the longest lived, in ticks. */
    private static final int DIED_AWAY = 60;
    /* The bells' mouths of the ship, its twin bells', and its air engines' nozzles, two under each side intake. */
    private static final int MOUTHS = 2;
    private static final int NOZZLES = 4;
    /* Its throttle cruising and full, and the bells' share of the push in thin air, in hundredths. */
    private static final int CRUISING = 60;
    private static final int FULL = 100;
    private static final int THIN = 60;
    /* How far two pixels may differ and be the same, in each channel. */
    private static final int SAME = 6;
    private static final int CHANNEL = 0xFF;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ShipDesign SHIP = new ShipDesign("JS-08", List.of(part("orbiter_cockpit"), part("airlock"),
            part("quarters"), part("workshop"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));

    private ShipEnginesClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void engines_glowAndBlowAsTheyRunAndDieAwayStood(final ClientTestContext ctx) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final int[] before = new int[1];
        final int[][] frames = new int[4][];
        final int[] playing = new int[2];
        ctx.afterTest(() -> {
            ctx.mc().options.hideGui = false;
            ctx.server().execute(() -> {
                if (ship[0] != null) {
                    MovingStructures.takeAway(ship[0]);
                }
                if (built[0] != null) {
                    clear(ctx, built[0]);
                    built[0].players().forEach(player -> player.setGameMode(GameType.CREATIVE));
                }
            });
        });
        ctx.thenServer(0, level -> {
            built[0] = level;
            clear(ctx, level);
            terminal[0] = TestShipyards.build(level, ctx.abs(CORNER));
            ctx.serverPlayer().setGameMode(GameType.SPECTATOR);
        }).thenWaitUntilServer(level -> level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity dock
                && dock.formed(), ARRIVE, "the dock to form", level -> "not formed")
                .thenServer(0, level -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal[0]);
                    dock.design(SHIP);
                    ctx.assertTrue(dock.confirm(), "the ship is sent to the dock");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan.plan().spots()) {
                        level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                })
                .thenServer(SETTLE, level -> ship[0] = ((ShipyardTerminalBlockEntity) level.getBlockEntity(
                        terminal[0])).release(null).orElseThrow())
                .thenWaitUntilServer(level -> ship[0].ready(), ARRIVE, "its blocks to come into the structure",
                        level -> "arriving")
                .thenWaitUntil(() -> console(ctx, ship[0]) != null, ARRIVE, "its console on the player's game");
        stand(ctx, EYES);
        ctx.then(2, () -> {
            ctx.lookAt(place(ctx, AT));
            ctx.mc().options.hideGui = true;
        })
                .then(SETTLE, () -> {
                    before[0] = EffectEngine.playing();
                    frames[0] = frame(ctx);
                })
                .thenScreenshot(0, "ship-engines-stood")
                // In thick air, cruising: the bells cold, the air engines pushing.
                .then(0, () -> tell(ctx, ship[0], false, CRUISING, 0, true))
                .then(SETTLE, () -> {
                    playing[0] = EffectEngine.playing() - before[0];
                    frames[1] = frame(ctx);
                })
                .thenScreenshot(0, "ship-engines-thick-air")
                // In thin air at full throttle: the bells giving most of the push.
                .then(0, () -> tell(ctx, ship[0], false, FULL, THIN, true))
                .then(SETTLE, () -> {
                    playing[1] = EffectEngine.playing() - before[0];
                    frames[2] = frame(ctx);
                })
                .thenScreenshot(0, "ship-engines-thin-air")
                .then(0, () -> tell(ctx, ship[0], true, FULL, THIN, true))
                .then(SETTLE, () -> frames[3] = frame(ctx))
                .thenScreenshot(0, "ship-engines-boosting")
                .then(0, () -> tell(ctx, ship[0], false, 0, 0, false))
                .thenWaitUntil(() -> EffectEngine.playing() == before[0], DIED_AWAY * 2,
                        "stood on the land, all its engines blew to die away")
                .then(0, () -> {
                    final int thick = changed(frames[0], frames[1]);
                    final int thin = changed(frames[0], frames[2]);
                    final int boosting = changed(frames[0], frames[3]);
                    LOGGER.info("[CT] engines: {} and {} effects playing; {}, {} and {} pixels lit thick, thin and "
                            + "boosting", playing[0], playing[1], thick, thin, boosting);
                    ctx.assertTrue(playing[0] == MOUTHS + NOZZLES, "in thick air, a faint glow at each mouth and an "
                            + "exhaust out of each nozzle: " + playing[0]);
                    ctx.assertTrue(playing[1] == MOUTHS * 2 + NOZZLES, "where the bells push, a bright glow and a "
                            + "jet at each mouth, and the exhaust: " + playing[1]);
                    ctx.assertTrue(thick > 0 && thin > thick && boosting > thin, "seen: the bells' jets add to the "
                            + "exhaust, and boosting they grow; the log says how much");
                });
    }

    /*
     * Tells the console of {@code ship}, on the player's game alone, it flies with its throttle {@code throttle},
     * boosting or not, its bells giving {@code bells} hundredths of the push and its air engines breathing or not; at
     * a shut throttle, not breathing, it stands on the land.
     */
    private static void tell(final ClientTestContext ctx, final MovingStructure ship, final boolean boosting,
                             final int throttle, final int bells, final boolean breathing) {
        final AirReserveEntity console = console(ctx, ship);
        ctx.assertTrue(console != null, "its console on the player's game");
        final boolean grounded = throttle == 0 && !breathing;
        console.flew(new ShipReadings(grounded ? 0 : 40, 0, throttle, 0, boosting, 0, false, false, true, false,
                grounded ? 0 : 40, grounded, ShipReadings.Pilot.NONE, ShipReadings.Pulse.NONE, bells, breathing));
    }

    /* The console of {@code ship} on the player's game, if it has arrived. */
    private static AirReserveEntity console(final ClientTestContext ctx, final MovingStructure ship) {
        final BlockPos at = ((ShipFlight) ship.motion()).consoleIn(ship).orElseThrow();
        return ctx.mc().level.getBlockEntity(at) instanceof AirReserveEntity keeper ? keeper : null;
    }

    /* The whole frame, its pixels row by row. */
    private static int[] frame(final ClientTestContext ctx) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        final int[] out = new int[width * height];
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    out[y * width + x] = frame.pixel(x, y);
                }
            }
        }
        return out;
    }

    /* How many pixels differ between two frames by more than {@link #SAME} in a channel. */
    private static int changed(final int[] a, final int[] b) {
        int found = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            for (int shift = 0; shift <= 16; shift += 8) {
                if (Math.abs((a[i] >> shift & CHANNEL) - (b[i] >> shift & CHANNEL)) > SAME) {
                    found++;
                    break;
                }
            }
        }
        return found;
    }

    /* Floats the player, a spectator, with their eyes at the dock's place {@code eyes}. */
    private static void stand(final ClientTestContext ctx, final Vec3 eyes) {
        ctx.thenServer(0, level -> {
            final Vec3 there = place(ctx, eyes);
            ctx.serverPlayer().teleportTo(level, there.x, there.y - ctx.serverPlayer().getEyeHeight(), there.z, 0,
                    0);
        }).thenWaitUntil(() -> ctx.player().getEyePosition().distanceTo(place(ctx, eyes)) < 0.1, ARRIVE,
                "the player to float by the dock");
    }

    /* The world's point of the dock's place {@code at}: across from the bay's middle, up from the ground, along. */
    private static Vec3 place(final ClientTestContext ctx, final Vec3 at) {
        return Vec3.atLowerCornerOf(ctx.abs(CORNER.offset(11, 3, 13))).add(at);
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                for (int y = Y_BOTTOM; y <= Y_TOP; y++) {
                    final Block block = y == Y_BOTTOM ? Blocks.BEDROCK : y < Y_GRASS ? Blocks.DIRT
                            : y == Y_GRASS ? Blocks.GRASS_BLOCK : Blocks.AIR;
                    level.setBlock(ctx.abs(new BlockPos(x, y, z)), block.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
