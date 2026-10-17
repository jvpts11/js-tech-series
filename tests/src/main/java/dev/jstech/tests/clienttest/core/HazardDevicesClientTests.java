/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.client.hazard.HazardMapsClient;
import dev.jstech.core.client.hazard.HazardsClient;
import dev.jstech.core.client.hud.HudElements;
import dev.jstech.core.gui.HudStack;
import dev.jstech.core.hazard.HazardMaps;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.hazard.Lungs;
import dev.jstech.core.jstechseries.hazard.HazardDevices;
import dev.jstech.core.jstechseries.hazard.client.BodyScreen;
import dev.jstech.core.jstechseries.hazard.client.DeviceArt;
import dev.jstech.core.jstechseries.hazard.client.HazardDevicesClient;
import dev.jstech.core.jstechseries.hazard.client.HazardMapScreen;
import dev.jstech.core.jstechseries.hazard.client.HeldDeviceScreen;
import dev.jstech.tests.TestHazards;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import org.lwjgl.glfw.GLFW;

/**
 * The hazard devices on a player's game, to look at: the Geiger counter, the dosimeter and the hazard map on the HUD,
 * each in its corner, the map with a hazard of the test mod's own read round the player; the map opened whole and a
 * chunk clicked; the Geiger counter held up and its range pressed, kept on the server; the dosimeter held up; and the
 * body's screen on its key, with the lungs harmed and another hazard on the body.
 */
public final class HazardDevicesClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final int WAIT = 200;
    private static final int SETTLE = 10;
    /* Long enough for the map device to draw the land round the player. */
    private static final int LAND = 60;
    private static final int READ_ROUND = 4;

    private HazardDevicesClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void devices_showOnTheHudAndHeldUpWithTheMapAndTheBody(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> {
            final ServerPlayer player = ctx.serverPlayer();
            for (int slot = 0; slot < 3; slot++) {
                ctx.give(slot, ItemStack.EMPTY);
            }
            TestHazards.expose(player, 0);
            Hazards.set(player, TestHazards.KIND, 0);
            Hazards.set(player, Lungs.BLACK_LUNG, 0);
            Hazards.set(player, Lungs.SILICOSIS, 0);
        }));
        ctx.thenServer(0, level -> {
            ctx.give(0, new ItemStack(HazardDevices.GEIGER_COUNTER.get()));
            ctx.give(1, new ItemStack(HazardDevices.POCKET_DOSIMETER.get()));
            ctx.give(2, new ItemStack(HazardDevices.HAZARD_MAP.get()));
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .thenServer(SETTLE, level -> {
                    final ServerPlayer player = ctx.serverPlayer();
                    // Readings round the player rising towards the north east, a few chunks never read.
                    final ChunkPos here = player.chunkPosition();
                    for (int dx = -READ_ROUND; dx <= READ_ROUND; dx++) {
                        for (int dz = -READ_ROUND; dz <= READ_ROUND; dz++) {
                            if (dx + dz != 3) {
                                HazardMaps.record(player, level.dimension(), new ChunkPos(here.x + dx, here.z + dz),
                                        TestHazards.KIND, Math.pow(10, (dx - dz + 2 * READ_ROUND) / 4.0 - 1));
                            }
                        }
                    }
                    Hazards.set(player, Lungs.BLACK_LUNG, 9);
                    Hazards.set(player, Lungs.SILICOSIS, 3);
                    // Kept coming, so it is still on the body when its screen opens.
                    Hazards.set(player, TestHazards.KIND, 7);
                    TestHazards.expose(player, 0.2);
                })
                .then(0, () -> ctx.selectHotbar(2))
                .thenWaitUntil(() -> HazardMapsClient.known(TestHazards.KIND.id()) > 0, WAIT,
                        "the map's readings to reach the player's game")
                .thenWaitUntil(() -> placed(HudStack.Corner.BOTTOM_LEFT, HazardDevicesClient.GEIGER_HUD)
                        && placed(HudStack.Corner.BOTTOM_LEFT, HazardDevicesClient.DOSIMETER_HUD)
                        && placed(HudStack.Corner.BOTTOM_RIGHT, HazardDevicesClient.MAP_HUD), WAIT,
                        "the three devices on the HUD, each in its corner")
                .thenScreenshot(LAND, "hud")
                .then(0, () -> use(ctx))
                .thenAwaitScreen(HazardMapScreen.class, WAIT)
                .then(LAND, () -> {
                    final HazardMapScreen map = ctx.screen(HazardMapScreen.class);
                    final int[] area = map.mapArea();
                    ctx.click(area[0] + area[2] / 2.0, area[1] + area[3] / 2.0);
                })
                .thenAssert(SETTLE, () -> {
                    final HazardMapScreen map = ctx.screen(HazardMapScreen.class);
                    return map.clicked().isPresent() && map.shownKind().map(kind -> kind == TestHazards.KIND)
                            .orElse(false);
                }, "the map shows the test hazard and the chunk clicked")
                .thenScreenshot(0, "map_screen")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT)
                .then(0, () -> ctx.selectHotbar(0))
                .then(2, () -> use(ctx))
                .thenAwaitScreen(HeldDeviceScreen.class, WAIT)
                .then(SETTLE, () -> {
                    final HeldDeviceScreen held = ctx.screen(HeldDeviceScreen.class);
                    ctx.click(held.left() + DeviceArt.RANGE_X * HeldDeviceScreen.UNIT + 4,
                            held.top() + DeviceArt.GEIGER_BUTTON_Y * HeldDeviceScreen.UNIT + 2);
                })
                .thenWaitUntilServer(level -> HazardDevices.state(ctx.serverPlayer().getInventory().getItem(0))
                        .range() == 1, WAIT, "the range pressed to reach the counter on the server",
                        level -> "the counter is set " + HazardDevices.state(ctx.serverPlayer().getInventory()
                                .getItem(0)))
                .thenScreenshot(SETTLE, "geiger_held_up")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT)
                .then(0, () -> ctx.selectHotbar(1))
                .then(2, () -> use(ctx))
                .thenAwaitScreen(HeldDeviceScreen.class, WAIT)
                .thenScreenshot(SETTLE, "dosimeter_held_up")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT)
                .then(0, () -> {
                    KeyMapping.resetMapping();
                    KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_B));
                })
                .thenAwaitScreen(BodyScreen.class, WAIT)
                .thenAssert(SETTLE, () -> HazardsClient.level(TestHazards.KIND.id()) >= 5
                        && HazardsClient.level(Lungs.BLACK_LUNG.id()) > 8, "the body's hazards reach its screen")
                .thenScreenshot(SETTLE, "body_screen")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT);
    }

    private static boolean placed(final HudStack.Corner corner, final ResourceLocation element) {
        final Map<ResourceLocation, HudStack.Point> placed = HudElements.placedIn(corner);
        return placed.containsKey(element);
    }

    private static void use(final ClientTestContext ctx) {
        Objects.requireNonNull(ctx.mc().gameMode, "no game mode").useItem(ctx.player(), InteractionHand.MAIN_HAND);
    }
}
