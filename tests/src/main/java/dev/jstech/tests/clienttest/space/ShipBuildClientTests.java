/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.client.multiblock.PlanHologram;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.text.GameText;
import dev.jstech.space.client.shipyard.ShipHolograms;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A ship half built by hand in a Small Shipyard, on a player's game, as the approved drawing of it shows: its cockpit,
 * airlock and air intakes formed into their models, its hold being raised, a little under half its blocks in place;
 * the rest of the ship in the hologram, each part still to form in its box with its name, its size and how many of
 * its blocks are in place, and the line across the top naming the ship and the part being built. Seen from where the
 * drawing's two cameras stood, from over the front of the bay and from close by the nose.
 */
public final class ShipBuildClientTests {

    /* The dock dug into the land where the test stands, as the shipyard's client test builds it. */
    private static final BlockPos CORNER = new BlockPos(-11, -1, 6);
    /*
     * The drawing's cameras, in the dock's own places: where they looked, and the eyes, brought nearer along the same
     * line so the game's wider view frames the ship as the drawing's narrower ones did.
     */
    private static final Vec3 WIDE_EYES = new Vec3(-7.4, 15.6, -12.4);
    private static final Vec3 WIDE_AT = new Vec3(0, 3.0, -0.5);
    private static final Vec3 CLOSE_EYES = new Vec3(-4.4, 7.2, -10.5);
    private static final Vec3 CLOSE_AT = new Vec3(0, 3.2, -0.5);
    /* The parts formed, by their numbers in the plan: the cockpit, the airlock, and the air intakes. */
    private static final List<Integer> FORMED = List.of(0, 1, 5);
    /* The hold, being built, and how much of it is in place, lowest first. */
    private static final int HOLD = 2;
    private static final double HOLD_PLACED = 0.45;
    /* How the box of the part being built ends its count. */
    private static final String BUILDING = "BUILDING";
    private static final int X0 = -13;
    private static final int X1 = 13;
    private static final int Z0 = 4;
    private static final int Z1 = 34;
    private static final int Y_BOTTOM = -2;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 16;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 20;

    private ShipBuildClientTests() {
    }

    @ClientTest(timeoutTicks = 2000)
    public static void build_halfBuiltShipIsDrawnAsTheApprovedOne(final ClientTestContext ctx) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final ShipDesign ship = new ShipDesign("JS-01", List.of(part("orbiter_cockpit"), part("airlock"),
                part("bay"), part("reactor_flush")), List.of(part("twin_bells"), part("side_intakes"),
                part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));
        ctx.afterTest(() -> ctx.server().execute(() -> {
            if (built[0] != null) {
                clear(ctx, built[0]);
                built[0].players().forEach(player -> player.setGameMode(GameType.CREATIVE));
            }
        }));
        ctx.thenServer(0, level -> {
            built[0] = level;
            terminal[0] = TestShipyards.build(level, ctx.abs(CORNER));
            ctx.serverPlayer().setGameMode(GameType.SPECTATOR);
        }).thenWaitUntilServer(level -> level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity dock
                && dock.formed(), ARRIVE, "the dock to form", level -> "not formed")
                .thenServer(0, level -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal[0]);
                    dock.design(ship);
                    ctx.assertTrue(dock.confirm(), "the ship is sent to the dock");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan.plan().spots()) {
                        if (FORMED.contains(spot.part())) {
                            lay(level, plan, spot);
                        }
                    }
                    // The hold raised from its floor up, a little under half of it in place.
                    final List<PlanSpot> hold = plan.plan().spots().stream().filter(spot -> spot.part() == HOLD)
                            .sorted(Comparator.comparingInt((PlanSpot spot) -> spot.at().getY())
                                    .thenComparingInt(spot -> spot.at().getZ())).toList();
                    hold.subList(0, (int) (hold.size() * HOLD_PLACED)).forEach(spot -> lay(level, plan, spot));
                })
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity
                        dock && dock.formedParts().equals(FORMED), ARRIVE, "the parts formed to reach the player");
        stand(ctx, WIDE_EYES);
        ctx.then(2, () -> ctx.lookAt(place(ctx, WIDE_AT)))
                .thenWaitUntil(() -> PlanHologram.banner().isPresent(), ARRIVE, "the hologram and its line to show")
                .thenScreenshot(SETTLE, "ship-build-wide")
                .then(0, () -> {
                    final String line = GameText.resolve(PlanHologram.banner().orElseThrow());
                    ctx.assertTrue(line.contains(ship.name()), "the line across the top names the ship: " + line);
                    final PlanHologram.Shown shown = ShipHolograms.shown().getFirst();
                    ctx.assertTrue(shown.parts().size() == ship.spine().size() + ship.hung().size() - FORMED.size()
                            && shown.parts().stream().filter(part -> GameText.resolve(part.state()).endsWith(
                            BUILDING)).count() == 1, "each part still to form has its box, one of them being built");
                });
        stand(ctx, CLOSE_EYES);
        ctx.then(2, () -> ctx.lookAt(place(ctx, CLOSE_AT)))
                .thenScreenshot(SETTLE, "ship-build-close");
    }

    /* Floats the player, a spectator, with their eyes at the dock's place {@code eyes}. */
    private static void stand(final ClientTestContext ctx, final Vec3 eyes) {
        ctx.thenServer(0, level -> {
            final Vec3 there = place(ctx, eyes);
            ctx.serverPlayer().teleportTo(level, there.x, there.y - ctx.serverPlayer().getEyeHeight(), there.z, 0,
                    0);
        }).thenWaitUntil(() -> ctx.player().getEyePosition().distanceTo(place(ctx, eyes)) < 0.1, ARRIVE,
                "the player to float where the drawing's camera stood");
    }

    /* The world's point of the dock's place {@code at}: across from the bay's middle, up from the ground, along. */
    private static Vec3 place(final ClientTestContext ctx, final Vec3 at) {
        return Vec3.atLowerCornerOf(ctx.abs(CORNER.offset(11, 3, 13))).add(at);
    }

    private static void lay(final ServerLevel level, final PlannedBuild plan, final PlanSpot spot) {
        level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block()).defaultBlockState());
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
