/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.JsCore;
import dev.jstech.core.camera.CameraView;
import dev.jstech.core.camera.CameraViewEvent;
import dev.jstech.core.camera.CameraViews;
import dev.jstech.core.client.camera.CameraFeeds;
import dev.jstech.core.client.camera.CameraViewsClient;
import dev.jstech.core.client.camera.CameraZoomClient;
import dev.jstech.core.client.camera.FreeLook;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.vehicle.VehicleEntity;
import dev.jstech.core.vehicle.Vehicles;
import dev.jstech.tests.TestCameras;
import dev.jstech.tests.TestVehicles;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The camera on a player's game: binoculars magnify the view by their power; a scene written as a resource pack's file
 * plays along its path and tells the server it finished; watching an armour stand looks from it until the player
 * sneaks; free look turns the view and not the player; riding a vehicle that turns turns the rider with it; and a
 * screen shows what a camera above it sees.
 */
public final class CameraClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final BlockPos SCREEN = new BlockPos(0, 3, 2);
    private static final int WAIT = 200;
    private static final int SETTLE = 5;
    private static final float TURN = 30.0F;
    private static final double CLOSE = 1.5;

    private CameraClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void binoculars_magnifyTheViewByTheirPower(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().options.keyUse.setDown(false));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND,
                        new ItemStack(TestCameras.BINOCULARS.get())))
                .thenWaitUntil(() -> ctx.player().getMainHandItem().is(TestCameras.BINOCULARS.get()), WAIT,
                        "the binoculars in hand")
                .then(0, () -> {
                    ctx.mc().options.keyUse.setDown(true);
                    ctx.mc().gameMode.useItem(ctx.player(), InteractionHand.MAIN_HAND);
                })
                .thenWaitUntil(() -> ctx.player().isUsingItem(), WAIT, "looking through the binoculars")
                .thenAssert(SETTLE, () -> CameraZoomClient.power() == TestCameras.POWERS.get(0),
                        "they magnify by their first power")
                .thenScreenshot(0, "camera-zoom")
                .then(0, () -> ctx.mc().options.keyUse.setDown(false))
                .thenWaitUntil(() -> !ctx.player().isUsingItem() && CameraZoomClient.power() == 1.0, WAIT,
                        "the view back to the eye's");
    }

    @ClientTest(timeoutTicks = 600)
    public static void scene_playsAlongItsPathAndTellsTheServerItFinished(final ClientTestContext ctx) {
        final List<CameraViewEvent> heard = new CopyOnWriteArrayList<>();
        final Consumer<CameraViewEvent> listener = heard::add;
        ctx.afterTest(() -> JsCore.events().unsubscribe(CameraViewEvent.class, listener));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    JsCore.events().subscribe(CameraViewEvent.class, listener);
                    CameraViews.play(ctx.serverPlayer(), TestCameras.FILE_SCENE, ctx.serverPlayer().position(),
                            0.0F);
                })
                .thenWaitUntil(() -> CameraViewsClient.showing().filter(CameraView.Scene.class::isInstance)
                        .isPresent() && ctx.mc().getCameraEntity() != ctx.player(), WAIT, "the scene playing")
                .thenScreenshot(20, "camera-scene")
                .thenWaitUntil(() -> CameraViewsClient.showing().isEmpty()
                        && ctx.mc().getCameraEntity() == ctx.player(), WAIT, "the scene over")
                .thenWaitUntil(() -> heard.stream().anyMatch(event -> event.ending()
                        == CameraViewEvent.Ending.FINISHED), WAIT, "the server told it finished");
    }

    @ClientTest(timeoutTicks = 600)
    public static void watch_looksFromTheEntityUntilThePlayerSneaks(final ClientTestContext ctx) {
        final int[] watched = {-1};
        ctx.afterTest(() -> ctx.sneak(false));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    final ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, level);
                    stand.moveTo(Vec3.atBottomCenterOf(ctx.abs(new BlockPos(0, 2, 4))), 180.0F, 0.0F);
                    /* An entity sees the way its head turns: north, at the player. */
                    stand.setYHeadRot(180.0F);
                    level.addFreshEntity(stand);
                    watched[0] = stand.getId();
                    CameraViews.watch(ctx.serverPlayer(), stand);
                })
                .thenWaitUntil(() -> {
                    final Entity camera = ctx.mc().getCameraEntity();
                    return camera != null && camera.getId() == watched[0];
                }, WAIT, "the view from the armour stand")
                .thenScreenshot(SETTLE, "camera-watch")
                .then(0, () -> ctx.sneak(true))
                .thenWaitUntil(() -> ctx.mc().getCameraEntity() == ctx.player(), WAIT, "back to the player")
                .then(0, () -> ctx.sneak(false))
                .thenWaitUntilServer(level -> CameraViews.showing(ctx.serverPlayer()).isEmpty(), WAIT,
                        "the server told the view ended", level -> "the server still shows a view");
    }

    @ClientTest(timeoutTicks = 400)
    public static void freeLook_turnsTheViewAndNotThePlayer(final ClientTestContext ctx) {
        final float[] facing = new float[1];
        ctx.afterTest(() -> KeyActionsClient.mapping(CoreKeys.FREE_LOOK).setDown(false));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .then(SETTLE, () -> {
                    facing[0] = ctx.player().getYRot();
                    KeyActionsClient.mapping(CoreKeys.FREE_LOOK).setDown(true);
                    FreeLook.turn(200.0, 0.0);
                })
                .thenAssert(SETTLE, () -> {
                    final float view = ctx.mc().gameRenderer.getMainCamera().getYRot();
                    return ctx.player().getYRot() == facing[0] && FreeLook.turned()[0] > 0
                            && Math.abs(Mth.wrapDegrees(view - facing[0] - FreeLook.turned()[0])) < CLOSE;
                }, "the view turns by the head's turn, the player not at all")
                .thenScreenshot(0, "camera-free-look")
                .then(0, () -> KeyActionsClient.mapping(CoreKeys.FREE_LOOK).setDown(false))
                .thenWaitUntil(() -> FreeLook.turned()[0] == 0.0F, WAIT, "the head back ahead");
    }

    @ClientTest(timeoutTicks = 600)
    public static void chase_turnsTheRiderWithTheVehicle(final ClientTestContext ctx) {
        final int[] van = {-1};
        final float[] before = new float[1];
        ctx.afterTest(() -> ctx.server().submit(() -> {
            ctx.serverPlayer().stopRiding();
            final Entity made = ctx.serverLevel().getEntity(van[0]);
            if (made != null) {
                made.discard();
            }
        }));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    final VehicleEntity made = Vehicles.make(level, TestVehicles.VAN,
                            Vec3.atBottomCenterOf(ctx.abs(new BlockPos(0, 2, 2))), 0.0F, "", ctx.serverPlayer())
                            .orElseThrow();
                    van[0] = made.getId();
                    ctx.serverPlayer().startRiding(made, true);
                })
                .thenWaitUntil(() -> ctx.player().getRootVehicle() instanceof VehicleEntity, WAIT,
                        "riding the van")
                .then(SETTLE, () -> {
                    /* The driver's game steers the van it drives: turned here, as the wheel would turn it. */
                    before[0] = ctx.player().getYRot();
                    final Entity ridden = ctx.player().getRootVehicle();
                    ridden.setYRot(ridden.getYRot() + TURN);
                })
                .thenWaitUntil(() -> Math.abs(Mth.wrapDegrees(ctx.player().getYRot() - before[0] - TURN)) < CLOSE,
                        WAIT, "the rider turned with the van");
    }

    @ClientTest(timeoutTicks = 600)
    public static void screen_showsWhatItsCameraSees(final ClientTestContext ctx) {
        final int[] drawnBefore = new int[1];
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(SCREEN),
                        TestCameras.SCREEN.get().defaultBlockState()))
                .then(0, () -> drawnBefore[0] = CameraFeeds.drawn())
                .thenWaitUntil(() -> CameraFeeds.drawn() > drawnBefore[0] + 1, WAIT, "the camera's view drawn")
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(SCREEN))))
                .thenScreenshot(SETTLE, "camera-feed")
                .thenTeleport(0, new BlockPos(0, 2, 1), Direction.SOUTH)
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(SCREEN))))
                .thenScreenshot(SETTLE, "camera-feed-near")
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(SCREEN), Blocks.AIR.defaultBlockState()));
    }
}
