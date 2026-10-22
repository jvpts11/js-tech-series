/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.JsCore;
import dev.jstech.core.camera.CameraShake;
import dev.jstech.core.camera.CameraView;
import dev.jstech.core.camera.CameraViewEvent;
import dev.jstech.core.camera.CameraViews;
import dev.jstech.core.camera.ThirdPersonView;
import dev.jstech.core.client.camera.CameraFeeds;
import dev.jstech.core.client.camera.CameraMotion;
import dev.jstech.core.client.camera.CameraViewsClient;
import dev.jstech.core.client.camera.CameraZoomClient;
import dev.jstech.core.client.camera.FreeLook;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.vehicle.VehicleEntity;
import dev.jstech.core.vehicle.VehicleLean;
import dev.jstech.core.vehicle.Vehicles;
import dev.jstech.tests.TestCameras;
import dev.jstech.tests.TestVehicles;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;

/**
 * The camera on a player's game: binoculars magnify the view by their power; a scene written as a resource pack's file
 * plays along its path and tells the server it finished; watching an armour stand looks from it until the player
 * sneaks; free look turns the view and not the player; riding a vehicle that turns turns the rider with it; a rider
 * of a leaning van leans with it, as another player sees; a shake turns the view and dies away, and a lean banks it and
 * lets it go level; a mod's camera in the third person stands over the shoulder, short of a wall, and round a place of
 * its own, and gives the view back to the game; and a screen shows what a camera above it sees.
 */
public final class CameraClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final BlockPos SCREEN = new BlockPos(0, 3, 2);
    private static final int WAIT = 200;
    private static final int SETTLE = 5;
    private static final float TURN = 30.0F;
    private static final double CLOSE = 1.5;
    private static final float BANK = 20.0F;
    private static final double SHOULDER_BACK = 6.0;
    private static final double SHOULDER_RIGHT = 1.5;
    private static final double SHOULDER_UP = 0.5;
    private static final double PIVOT_BACK = 5.0;
    /* How near where it is wanted the camera stands, in blocks. */
    private static final double NEAR = 0.05;

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
    public static void riders_leanWithTheVehicleAsOthersSeeThem(final ClientTestContext ctx) {
        final int[] made = {-1, -1};
        final BlockPos at = new BlockPos(4, 2, 4);
        ctx.afterTest(() -> ctx.server().submit(() -> {
            for (final int id : made) {
                final Entity entity = ctx.serverLevel().getEntity(id);
                if (entity != null) {
                    entity.discard();
                }
            }
            for (int x = 3; x <= 5; x++) {
                ctx.serverLevel().setBlockAndUpdate(ctx.abs(new BlockPos(x, 1, 5)), Blocks.GRASS_BLOCK
                        .defaultBlockState());
            }
        }));
        ctx.thenTeleport(0, new BlockPos(-1, 2, 4), Direction.EAST)
                .thenServer(0, level -> {
                    // The ground under its front wheels taken away, so the van stands nose down.
                    for (int x = 3; x <= 5; x++) {
                        level.setBlockAndUpdate(ctx.abs(new BlockPos(x, 1, 5)), Blocks.AIR.defaultBlockState());
                    }
                    final VehicleEntity van = Vehicles.make(level, TestVehicles.VAN, Vec3.atBottomCenterOf(ctx.abs(at)),
                            0.0F, "", ctx.serverPlayer()).orElseThrow();
                    final Pig rider = EntityType.PIG.create(level);
                    rider.moveTo(Vec3.atBottomCenterOf(ctx.abs(at)));
                    rider.setNoAi(true);
                    level.addFreshEntity(rider);
                    rider.startRiding(van, true);
                    made[0] = van.getId();
                    made[1] = rider.getId();
                })
                .thenWaitUntil(() -> leaning(ctx, made[0]) < -5, WAIT, "the van to lean nose down on the player's game")
                .thenWaitUntil(() -> seated(ctx, made[0], made[1]), WAIT, "its rider to sit where the leaning seat "
                        + "is, on the player's game as on the server")
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(at))))
                .thenScreenshot(10, "rider-leans-with-the-van");
    }

    @ClientTest(timeoutTicks = 800)
    public static void motion_shakesAndLeansTheView(final ClientTestContext ctx) {
        final Object owner = new Object();
        ctx.afterTest(() -> CameraMotion.release(owner));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                // The tests run with less movement asked for, which stills every shake.
                .then(0, () -> CameraMotion.shake(CameraShake.of(1.0, 0.5)))
                .thenAssert(SETTLE, () -> CameraMotion.turn().yaw() == 0.0F && CameraMotion.turn().pitch() == 0.0F,
                        "with less movement asked for, a shake turns nothing")
                .thenWaitUntil(() -> !CameraMotion.active(), WAIT, "that shake to be over")
                .then(0, () -> {
                    MotionClock.setReduced(false);
                    CameraMotion.shake(CameraShake.of(1.0, 2.0));
                })
                .thenWaitUntil(() -> Math.abs(Mth.wrapDegrees(ctx.mc().gameRenderer.getMainCamera().getYRot()
                        - ctx.player().getYRot())) > 0.1, WAIT, "the shake to turn the view, not the player")
                .thenWaitUntil(() -> !CameraMotion.active(), WAIT, "the shake to die away")
                .then(0, () -> CameraMotion.lean(owner, 0.0F, 0.0F, BANK, 0.3))
                .thenWaitUntil(() -> Math.abs(CameraMotion.turn().roll() - BANK) < CLOSE / 3, WAIT,
                        "the view to bank as its owner leans it")
                .thenScreenshot(2, "view-banked-by-a-lean")
                .then(0, () -> CameraMotion.release(owner))
                .thenWaitUntil(() -> !CameraMotion.active() && Math.abs(CameraMotion.turn().roll()) < CLOSE / 15,
                        WAIT, "and to come back level once let go");
    }

    @ClientTest(timeoutTicks = 600)
    public static void thirdPerson_standsWhereAModsCameraSays(final ClientTestContext ctx) {
        final Vec3[] pivot = new Vec3[1];
        ctx.afterTest(() -> {
            TestThirdPerson.SHOWN.set(null);
            ctx.mc().options.setCameraType(CameraType.FIRST_PERSON);
            ctx.server().submit(() -> wall(ctx, Blocks.AIR));
        });
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .then(0, () -> {
                    ctx.mc().options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    TestThirdPerson.SHOWN.set(ThirdPersonView.behind(SHOULDER_BACK).shoulder(SHOULDER_RIGHT,
                            SHOULDER_UP));
                })
                .thenWaitUntil(() -> standsAt(ctx, ctx.player().getEyePosition(), SHOULDER_BACK, SHOULDER_RIGHT,
                        SHOULDER_UP), WAIT, "the camera behind and over the right shoulder")
                .thenScreenshot(0, "third-person-over-the-shoulder")
                .thenServer(0, level -> wall(ctx, Blocks.STONE))
                .thenWaitUntil(() -> {
                    final Vec3 at = ctx.mc().gameRenderer.getMainCamera().getPosition();
                    final Vec3 right = rightOfTheView(ctx);
                    final double moved = at.subtract(ctx.player().getEyePosition()).dot(right);
                    return moved > 0 && moved < 1.0 - 0.1 && ctx.mc().level.getBlockState(BlockPos.containing(at))
                            .isAir();
                }, WAIT, "a wall at the right shoulder bringing the camera in short of it")
                .thenServer(0, level -> wall(ctx, Blocks.AIR))
                .then(SETTLE, () -> {
                    pivot[0] = ctx.player().getEyePosition().add(0, 2, 0);
                    TestThirdPerson.SHOWN.set(ThirdPersonView.behind(PIVOT_BACK).around(new Vector3d(pivot[0].x,
                            pivot[0].y, pivot[0].z)));
                })
                .thenWaitUntil(() -> standsAt(ctx, pivot[0], PIVOT_BACK, 0, 0), WAIT,
                        "the camera turning round a place of its own")
                .then(0, () -> TestThirdPerson.SHOWN.set(null))
                .thenWaitUntil(() -> standsAt(ctx, ctx.player().getEyePosition(), ThirdPersonView.GAME_DISTANCE, 0,
                        0), WAIT, "the game's own camera back once the mod's leaves the view");
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

    /*
     * Whether the player's camera stands {@code back} blocks behind {@code from}, {@code right} to the right of the
     * view and {@code up} up it.
     */
    private static boolean standsAt(final ClientTestContext ctx, final Vec3 from, final double back,
                                    final double right, final double up) {
        final Camera camera = ctx.mc().gameRenderer.getMainCamera();
        final Vector3f look = camera.getLookVector();
        final Vector3f raised = camera.getUpVector();
        final Vec3 wanted = from.add(rightOfTheView(ctx).scale(right))
                .add(raised.x * up - look.x * back, raised.y * up - look.y * back, raised.z * up - look.z * back);
        return camera.getPosition().distanceTo(wanted) < NEAR;
    }

    /* Which way the right of the player's view is. */
    private static Vec3 rightOfTheView(final ClientTestContext ctx) {
        final Vector3f left = ctx.mc().gameRenderer.getMainCamera().getLeftVector();
        return new Vec3(-left.x, -left.y, -left.z);
    }

    /* Builds the column a block to the right of the stand, as high as the player and a block more, of {@code block}. */
    private static void wall(final ClientTestContext ctx, final Block block) {
        for (int y = STAND.getY(); y <= STAND.getY() + 2; y++) {
            ctx.serverLevel().setBlockAndUpdate(ctx.abs(new BlockPos(STAND.getX() - 1, y, STAND.getZ())),
                    block.defaultBlockState());
        }
    }

    /* How far the van {@code id} leans its nose up on the player's game; none for no van seen. */
    private static float leaning(final ClientTestContext ctx, final int id) {
        return ctx.mc().level != null && ctx.mc().level.getEntity(id) instanceof VehicleEntity van ? van.leanPitch()
                : 0.0F;
    }

    /* Whether the rider {@code rider} sits on the player's game where the van's leaning seat is. */
    private static boolean seated(final ClientTestContext ctx, final int van, final int rider) {
        if (ctx.mc().level == null || !(ctx.mc().level.getEntity(van) instanceof VehicleEntity vehicle)
                || ctx.mc().level.getEntity(rider) == null) {
            return false;
        }
        final Entity sitting = ctx.mc().level.getEntity(rider);
        final Vec3 at = vehicle.def().seats().get(vehicle.seatOf(sitting)).at();
        final Vector3d seat = VehicleLean.seat(at.x, at.y, at.z, vehicle.getYRot(), vehicle.leanPitch(),
                vehicle.leanRoll());
        return sitting.position().distanceTo(vehicle.position().add(seat.x, seat.y, seat.z)) < CLOSE / 10;
    }
}
