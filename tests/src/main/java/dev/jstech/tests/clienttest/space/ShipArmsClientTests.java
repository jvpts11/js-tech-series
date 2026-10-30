/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.multiblock.plan.ArmMotion;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * A Small Shipyard's robot arms at work, on a player's game, as the approved storyboard shows them: the hold being
 * raised, its lowest blocks set by hand, the arms taking the rest from their feeders, swinging each over to its place,
 * setting it down and welding it with sparks, and the hold glowing round its box as it forms. Seen from where the
 * storyboard's camera stood, east of the bay looking at the west side's arm by its feeder.
 */
public final class ShipArmsClientTests {

    /* The dock dug into the land where the test stands, as the shipyard's client test builds it. */
    private static final BlockPos CORNER = new BlockPos(-11, -1, 6);
    /* The storyboard's camera, in the dock's own places, brought nearer for the game's wider view. */
    private static final Vec3 EYES = new Vec3(3.3, 8.5, -3.8);
    private static final Vec3 AT = new Vec3(-4.8, 4.6, 1.2);
    /* The parts formed by hand, by their numbers in the plan: the cockpit, the airlock, the air intakes. */
    private static final List<Integer> FORMED = List.of(0, 1, 5);
    /* The hold, being built, and how much of it is set by hand, lowest first. */
    private static final int HOLD = 2;
    private static final double BY_HAND = 0.6;
    private static final int ARMS = 4;
    private static final int X0 = -13;
    private static final int X1 = 13;
    private static final int Z0 = 4;
    private static final int Z1 = 34;
    private static final int Y_BOTTOM = -2;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 16;
    private static final int ARRIVE = 600;
    private static final int WORK = 1200;

    private ShipArmsClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void arms_swingSetAndWeldTheHoldsBlocksAsTheStoryboardHasThem(final ClientTestContext ctx) {
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
                    final List<PlanSpot> hold = plan.plan().spots().stream().filter(spot -> spot.part() == HOLD)
                            .sorted(Comparator.comparingInt((PlanSpot spot) -> spot.at().getY())
                                    .thenComparingInt(spot -> spot.at().getZ())).toList();
                    final int byHand = (int) (hold.size() * BY_HAND);
                    hold.subList(0, byHand).forEach(spot -> lay(level, plan, spot));
                    // The rest of the hold's blocks go in for the arms.
                    for (final PlanSpot spot : hold.subList(byHand, hold.size())) {
                        ItemHandlerHelper.insertItem(dock.assembler().inbox(), new ItemStack(
                                BuiltInRegistries.BLOCK.get(spot.block())), false);
                    }
                })
                .thenWaitUntil(() -> dock(ctx, terminal[0]).arms().arms().size() == ARMS, ARRIVE,
                        "the dock's four arms to reach the player");
        stand(ctx, EYES);
        ctx.then(2, () -> ctx.lookAt(place(ctx, AT)))
                .thenWaitUntil(() -> any(ctx, terminal[0], ticks -> ticks > ArmMotion.FETCH + ArmMotion.GRIP + 4
                        && ticks < ArmMotion.SET - 4), WORK, "an arm to swing a block over")
                .thenScreenshot(0, "arms-swing")
                .thenWaitUntil(() -> any(ctx, terminal[0], ticks -> ticks > ArmMotion.SET + 2
                        && ticks < ArmMotion.END - 2), WORK, "an arm to weld a block it set down")
                .thenScreenshot(0, "arms-weld")
                .thenWaitUntil(() -> dock(ctx, terminal[0]).formedParts().contains(HOLD), WORK,
                        "the hold to form as the arms set its last block")
                .thenScreenshot(2, "arms-formed");
    }

    /* Whether, on the player's game, an arm of the dock is {@code ticks} into its block where {@code when} says. */
    private static boolean any(final ClientTestContext ctx, final BlockPos at, final IntPredicate when) {
        final ShipyardTerminalBlockEntity dock = dock(ctx, at);
        final long now = ctx.mc().level.getGameTime();
        for (int arm = 0; arm < dock.arms().arms().size(); arm++) {
            final int into = dock.arms().job(arm).map(job -> (int) (now - job.start())).orElse(-1);
            if (into >= 0 && when.test(into)) {
                return true;
            }
        }
        return false;
    }

    private static ShipyardTerminalBlockEntity dock(final ClientTestContext ctx, final BlockPos at) {
        return (ShipyardTerminalBlockEntity) ctx.mc().level.getBlockEntity(at);
    }

    /* Floats the player, a spectator, with their eyes at the dock's place {@code eyes}. */
    private static void stand(final ClientTestContext ctx, final Vec3 eyes) {
        ctx.thenServer(0, level -> {
            final Vec3 there = place(ctx, eyes);
            ctx.serverPlayer().teleportTo(level, there.x, there.y - ctx.serverPlayer().getEyeHeight(), there.z, 0,
                    0);
        }).thenWaitUntil(() -> ctx.player().getEyePosition().distanceTo(place(ctx, eyes)) < 0.1, ARRIVE,
                "the player to float where the storyboard's camera stood");
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
