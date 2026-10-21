/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.JsCore;
import dev.jstech.core.camera.CameraPath;
import dev.jstech.core.camera.CameraPathCodecs;
import dev.jstech.core.camera.CameraView;
import dev.jstech.core.camera.CameraViewEvent;
import dev.jstech.core.camera.CameraViews;
import dev.jstech.core.model.Easing;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCameras;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * The Core's views from elsewhere on the server: a player watches an entity near, never one past their view
 * distance; a scene plays and the Core's events say how it ended, skipped by the player or stopped by the server or by
 * another view starting.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CameraViewGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jscore_camera";
    /* Far past any view distance the server allows. */
    private static final double FAR = 10_000.0;

    private CameraViewGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void watch_seesAnEntityNearAndNotOneFarAway(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, "camera-watch");
        final ArmorStand near = helper.spawn(EntityType.ARMOR_STAND, 2, 2, 2);
        helper.assertTrue(CameraViews.watch(player, near), "an entity near is watched");
        helper.assertTrue(CameraViews.showing(player).filter(view -> view instanceof CameraView.Watch watch
                && watch.entity() == near.getId()).isPresent(), "and the view is of it");
        final ArmorStand far = new ArmorStand(EntityType.ARMOR_STAND, helper.getLevel());
        far.setPos(player.position().add(FAR, 0, 0));
        helper.assertTrue(!CameraViews.watch(player, far), "one past the view distance is not");
        helper.assertTrue(!CameraViews.lookFrom(player, player.position().add(0, 0, FAR), 0, 0, true),
                "nor a point there");
        CameraViews.end(player);
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void scene_playsAndTheCoreSaysHowItEnded(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, "camera-scene");
        final List<CameraViewEvent> heard = new ArrayList<>();
        final Consumer<CameraViewEvent> listener = event -> {
            if (event.player() == player) {
                heard.add(event);
            }
        };
        JsCore.events().subscribe(CameraViewEvent.class, listener);
        try {
            CameraViews.play(player, TestCameras.SCENE, player.position(), 90.0F);
            helper.assertTrue(CameraViews.showing(player).filter(CameraView.Scene.class::isInstance).isPresent(),
                    "the scene shows");
            CameraViews.ended(player, CameraViewEvent.Ending.SKIPPED);
            helper.assertTrue(heard.size() == 1 && heard.get(0).ending() == CameraViewEvent.Ending.SKIPPED
                    && CameraViews.showing(player).isEmpty(), "skipped, the Core says so and the view is gone");
            CameraViews.play(player, TestCameras.SCENE, player.position(), 0.0F);
            CameraViews.lookFrom(player, player.position().add(0, 3, 0), 0, 30, true);
            helper.assertTrue(heard.size() == 2 && heard.get(1).ending() == CameraViewEvent.Ending.STOPPED
                            && heard.get(1).view() instanceof CameraView.Scene,
                    "another view starting stops the scene");
            CameraViews.end(player);
            helper.assertTrue(heard.size() == 3 && heard.get(2).view() instanceof CameraView.Point
                    && heard.get(2).ending() == CameraViewEvent.Ending.STOPPED, "and the server ends the point's");
            CameraViews.ended(player, CameraViewEvent.Ending.FINISHED);
            helper.assertTrue(heard.size() == 3, "an end told for no view says nothing");
        } finally {
            JsCore.events().unsubscribe(CameraViewEvent.class, listener);
        }
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void paths_readAsAResourcePackWritesThem(final GameTestHelper helper) {
        final String json = "{\"keyframes\": [{\"time\": 0, \"position\": [1, 2, 3]},"
                + "{\"time\": 20, \"position\": [4, 5, 6], \"yaw\": 90, \"easing\": \"easeInOutSine\"}]}";
        final Optional<CameraPath> path = CameraPathCodecs.PATH.parse(JsonOps.INSTANCE,
                JsonParser.parseString(json)).result();
        helper.assertTrue(path.isPresent() && path.get().keyframes().size() == 2
                        && path.get().keyframes().get(1).easing() == Easing.EASE_IN_OUT_SINE && path.get().hideHud(),
                "a path reads with its keyframes, its easing by Blockbench's name, and the HUD hidden unless said");
        final String backwards = "{\"keyframes\": [{\"time\": 20, \"position\": [0, 0, 0]},"
                + "{\"time\": 10, \"position\": [1, 1, 1]}]}";
        helper.assertTrue(CameraPathCodecs.PATH.parse(JsonOps.INSTANCE, JsonParser.parseString(backwards)).error()
                .isPresent(), "keyframes out of order read as an error");
        helper.succeed();
    }

    /* A player of a game connected to the server, in survival, in the test's arena. */
    private static ServerPlayer player(final GameTestHelper helper, final String name) {
        final ServerLevel level = helper.getLevel();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),
                name), false);
        final ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(),
                cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        for (int id = 0; id <= 8; id++) {
            player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(id));
        }
        player.setGameMode(GameType.SURVIVAL);
        final Vec3 there = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        player.moveTo(there.x, there.y, there.z, 0, 0);
        /* A mock player is never ticked: the server is told where it stands, as its tick tells it, for its chunks. */
        level.getChunkSource().move(player);
        return player;
    }

    private static void leave(final GameTestHelper helper, final ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }
}
