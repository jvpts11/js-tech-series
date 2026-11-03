/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.SmallShipFinishes;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.TestShips;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * The small ship's functional modules on a player's game, furnished as the approved modules were: a ship of the
 * Orbiter's cockpit and airlock, a workshop, a laboratory and quarters, and its reactor; the workshop's crafting table
 * standing against its left wall, drawn by the workshop's model with the game's own crafting table's texture, drawn in
 * as the game loads; the laboratory's bench and the quarters' bunk drawn by their models; and a bay's barrels along
 * its walls, drawn the same way.
 */
public final class ShipModulesClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final double HULL = 2;
    /* Where the workshop begins along the ship, from the stand's north-west corner. */
    private static final int WORKSHOP = 8;
    /* Where the player looks from and at, inside the ship: the crafting table's front, against the left wall. */
    private static final Vec3 WORKSHOP_EYES = new Vec3(0.3, HULL + 1.7, WORKSHOP + 1.6);
    private static final Vec3 CRAFTING_TABLE = new Vec3(-1.2, HULL + 0.9, WORKSHOP + 0.5);
    private static final Vec3 LAB_EYES = new Vec3(0.3, HULL + 1.7, 11);
    private static final Vec3 LAB_BENCH = new Vec3(-1.8, HULL + 1.2, 11);
    private static final Vec3 QUARTERS_EYES = new Vec3(0.5, HULL + 1.7, 13);
    private static final Vec3 BUNK = new Vec3(-1.6, HULL + 0.9, 13);
    /* In the hold of a ship of the Cab's cockpit and a docking module, its bay from 8 to 13: a barrel on the right. */
    private static final Vec3 HOLD_EYES = new Vec3(-0.3, HULL + 1.7, 10.5);
    private static final Vec3 BARREL = new Vec3(1.2, HULL + 0.9, 10.5);
    private static final int PATCH = 1;
    /* How far red outshines blue on the crafting table's planks, which take the cabin's light aslant. */
    private static final int WOODEN = 12;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 10;
    private static final int CHANNEL = 0xFF;

    private ShipModulesClientTests() {
    }

    @ClientTest(timeoutTicks = 1500)
    public static void modules_areFurnishedAsTheApprovedOnesTheWorkshopsTableDrawnAsTheGamesOwn(
            final ClientTestContext ctx) {
        final int[][] seen = new int[2][];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.serverLevel().removeBlock(ctx.abs(STAND), false);
            ctx.serverPlayer().setGameMode(GameType.CREATIVE);
        }));
        final ShipDesign ship = new ShipDesign("MODULES", List.of(part("orbiter_cockpit"), part("airlock"),
                part("workshop"), part("lab"), part("quarters"), part("reactor_flush")), List.of(part("twin_bells"),
                part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"),
                part("wheels")));
        ctx.thenServer(0, level -> {
            level.setBlockAndUpdate(ctx.abs(STAND), TestShips.DISPLAY.get().defaultBlockState());
            ((TestShips.DisplayEntity) level.getBlockEntity(ctx.abs(STAND))).show(ship, SmallShipFinishes.DEFAULT);
            ctx.serverPlayer().setGameMode(GameType.SPECTATOR);
        }).thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(STAND))
                instanceof TestShips.DisplayEntity stand && stand.design().equals(ship), ARRIVE,
                "the stand's ship to reach the player's game");
        stand(ctx, WORKSHOP_EYES);
        ctx.then(2, () -> ctx.lookAt(corner(ctx).add(CRAFTING_TABLE)))
                .then(SETTLE, () -> seen[0] = mean(ctx))
                .thenScreenshot(0, "ship-module-workshop");
        stand(ctx, LAB_EYES);
        ctx.then(2, () -> ctx.lookAt(corner(ctx).add(LAB_BENCH))).thenScreenshot(SETTLE, "ship-module-lab");
        stand(ctx, QUARTERS_EYES);
        ctx.then(2, () -> ctx.lookAt(corner(ctx).add(BUNK))).thenScreenshot(SETTLE, "ship-module-quarters");
        final ShipDesign hold = new ShipDesign("HOLD", List.of(part("cab_cockpit"), part("docking"), part("bay"),
                part("reactor_fins")), List.of(part("cluster"), part("stub_wings"), part("legs")));
        ctx.thenServer(0, level -> ((TestShips.DisplayEntity) level.getBlockEntity(ctx.abs(STAND))).show(hold,
                SmallShipFinishes.DEFAULT)).thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(STAND))
                instanceof TestShips.DisplayEntity stand && stand.design().equals(hold), ARRIVE,
                "the stand's hold to reach the player's game");
        stand(ctx, HOLD_EYES);
        ctx.then(2, () -> ctx.lookAt(corner(ctx).add(BARREL)))
                .then(SETTLE, () -> seen[1] = mean(ctx))
                .thenScreenshot(0, "ship-module-hold")
                .then(0, () -> {
                    ctx.assertTrue(seen[0][0] - seen[0][2] > WOODEN, "the workshop's crafting table stands against"
                            + " its wall, drawn with the game's own planks: " + str(seen[0]));
                    ctx.assertTrue(seen[1][0] - seen[1][2] > WOODEN, "the hold's barrels stand along its walls,"
                            + " drawn with the game's own staves: " + str(seen[1]));
                });
    }

    /* Floats the player, a spectator, with their eyes at {@code eyes} from the stand's corner. */
    private static void stand(final ClientTestContext ctx, final Vec3 eyes) {
        ctx.thenServer(0, level -> {
            final Vec3 there = corner(ctx).add(eyes);
            ctx.serverPlayer().teleportTo(level, there.x, there.y - ctx.serverPlayer().getEyeHeight(), there.z, 0,
                    0);
        }).thenWaitUntil(() -> ctx.player().getEyePosition().distanceTo(corner(ctx).add(eyes)) < 0.1, ARRIVE,
                "the player to float inside the ship");
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }

    private static Vec3 corner(final ClientTestContext ctx) {
        return Vec3.atLowerCornerOf(ctx.abs(STAND));
    }

    /* The mean colour, red, green and blue, of a patch at the middle of the screen, round the crosshair. */
    private static int[] mean(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        final long[] sum = new long[3];
        int count = 0;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -PATCH * 3; dx <= PATCH * 3; dx++) {
                for (int dy = -PATCH * 3; dy <= PATCH * 3; dy++) {
                    if (Math.abs(dx) <= PATCH && Math.abs(dy) <= PATCH) {
                        continue;
                    }
                    final int argb = frame.pixel(x + dx, y + dy);
                    sum[0] += argb >> 16 & CHANNEL;
                    sum[1] += argb >> 8 & CHANNEL;
                    sum[2] += argb & CHANNEL;
                    count++;
                }
            }
        }
        return new int[] {(int) (sum[0] / count), (int) (sum[1] / count), (int) (sum[2] / count)};
    }

    private static String str(final int[] colour) {
        return "(" + colour[0] + ", " + colour[1] + ", " + colour[2] + ")";
    }
}
