/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.physics.LightLawsClient;
import dev.jstech.core.client.physics.PortalViews;
import dev.jstech.core.physics.Laws;
import dev.jstech.core.physics.Physics;
import dev.jstech.core.physics.PhysicsArea;
import dev.jstech.core.physics.Pull;
import dev.jstech.core.region.Box;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The physics kit on a player's game, to look at: the light of a place shifted towards red, then shimmering in the
 * dark; a mass bending the view of a white wall round its black horizon; a portal with no edge showing a gold wall
 * far beyond it in place of the white one behind; and the player floating under laws of their own, falling again once
 * they carry none.
 */
public final class PhysicsClientTests {

    private static final int FLOOR = 2;
    private static final BlockPos STAND = new BlockPos(0, FLOOR, -4);
    /* A white wall behind everything the player looks at, and a gold one far beyond it, seen through the portal. */
    private static final int WALL_Z = 12;
    private static final int GOLD_Z = 34;
    private static final int WALL_HALF = 6;
    private static final int WALL_HIGH = 8;
    private static final Vec3 MASS = new Vec3(0.5, 3.6, 4.5);
    private static final double MASS_RADIUS = 1.5;
    /* The portal, three blocks square and one deep, and how far beyond it its far side lies. */
    private static final BlockPos PORTAL_LOW = new BlockPos(-1, FLOOR, 4);
    private static final BlockPos PORTAL_HIGH = new BlockPos(1, FLOOR + 2, 4);
    private static final int LEADS = 26;
    private static final int RED_TINT = 0xFF8040;
    private static final String LIGHT = "client_light";
    private static final String MASS_ID = "client_mass";
    private static final String PORTAL = "client_portal";
    private static final int ARRIVE = 600;
    private static final int WAIT = 100;
    private static final int SETTLE = 10;
    private static final int FLOAT = 30;
    private static final int DARK = 60;
    /* How far above the bottom of the screen the ground at the player's feet is read, in the GUI's pixels. */
    private static final int GROUND_ROW = 8;

    private PhysicsClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void laws_bendLightAndSpaceAndLetThePlayerFloat(final ClientTestContext ctx) {
        final int[] plain = new int[1];
        ctx.afterTest(() -> {
            ctx.mc().options.hideGui = false;
            ctx.server().execute(() -> {
                final ServerLevel level = ctx.serverLevel();
                Physics.remove(level, LIGHT);
                Physics.remove(level, MASS_ID);
                Physics.remove(level, PORTAL);
                Physics.own(ctx.serverPlayer(), Laws.NONE);
                walls(ctx, level, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState());
            });
        });
        ctx.thenServer(0, level -> walls(ctx, level, Blocks.WHITE_CONCRETE.defaultBlockState(),
                        Blocks.GOLD_BLOCK.defaultBlockState()))
                .thenTeleport(0, STAND, Direction.SOUTH)
                .thenAwaitLandDrawn(ARRIVE)
                .then(SETTLE, () -> {
                    // The crosshair is hidden, so the middle of the screen shows only the world; the laws still draw.
                    ctx.mc().options.hideGui = true;
                    ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, FLOOR + 2, WALL_Z))));
                })
                .then(SETTLE, () -> plain[0] = middle(ctx))
                .thenServer(0, level -> Physics.add(level, around(ctx, LIGHT, new Laws.Light(RED_TINT, 0.6, 0, 0, 0,
                        0))))
                .thenWaitUntil(() -> hasArea(ctx, LIGHT), WAIT, "the law of light to reach the player's game")
                .then(SETTLE, () -> {
                    final int shifted = middle(ctx);
                    ctx.assertTrue(blue(shifted) < blue(plain[0]) * 0.4 && red(shifted) > 2 * blue(shifted),
                            "the white wall is seen shifted towards red: " + Integer.toHexString(plain[0]) + " became "
                                    + Integer.toHexString(shifted));
                })
                .thenScreenshot(0, "red_shifted_light")
                .thenServer(0, level -> Physics.add(level, around(ctx, LIGHT, new Laws.Light(0xFFFFFF, 0, 0, 1.0, 0.5,
                        0))))
                .thenWaitUntil(() -> Physics.lawsAt(ctx.mc().level, ctx.player().getEyePosition()).light()
                        .map(light -> light.dark() > 0).orElse(false), WAIT, "the dark to reach the player's game")
                .thenScreenshot(SETTLE, "mirage_in_the_dark")
                .thenServer(0, level -> {
                    Physics.remove(level, LIGHT);
                    final Vec3 centre = Vec3.atLowerCornerOf(ctx.origin()).add(MASS);
                    // A mass that bends the view and pulls nothing, so the player stays where they stand.
                    Physics.mass(level, MASS_ID, centre, 0.0, MASS_RADIUS);
                })
                .then(0, () -> ctx.lookAt(Vec3.atLowerCornerOf(ctx.origin()).add(MASS)))
                .thenWaitUntil(() -> LightLawsClient.lens() != null, WAIT, "the mass to bend the player's view")
                .then(SETTLE, () -> {
                    final int horizon = middle(ctx);
                    ctx.assertTrue(red(horizon) + green(horizon) + blue(horizon) < DARK,
                            "the middle of the mass is a black horizon; got " + Integer.toHexString(horizon));
                })
                .thenScreenshot(0, "mass_bends_the_view")
                .thenServer(0, level -> {
                    Physics.remove(level, MASS_ID);
                    final BlockPos low = ctx.abs(PORTAL_LOW);
                    final BlockPos high = ctx.abs(PORTAL_HIGH);
                    Physics.add(level, new PhysicsArea(PORTAL, Box.between(low.getX(), low.getY(), low.getZ(),
                            high.getX(), high.getY(), high.getZ()), new Laws(Optional.empty(), Optional.empty(),
                            Optional.empty(), Optional.of(new Laws.Space(Laws.Crossing.ENTER, Optional.empty(),
                            Optional.of(Box.between(low.getX(), low.getY(), low.getZ() + LEADS, high.getX(),
                                    high.getY(), high.getZ() + LEADS)), true)), Optional.empty()), 0));
                })
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, FLOOR + 1, PORTAL_LOW.getZ())))))
                .thenWaitUntil(() -> PortalViews.shown() != null, WAIT, "the portal to show its far side")
                .then(SETTLE, () -> {
                    final int through = middle(ctx);
                    ctx.assertTrue(red(through) > 120 && blue(through) < red(through) * 0.6,
                            "through the portal the gold wall far beyond shows, not the white one behind it; got "
                                    + Integer.toHexString(through));
                    final int ground = ctx.pixel(ctx.mc().getWindow().getGuiScaledWidth() / 2,
                            ctx.mc().getWindow().getGuiScaledHeight() - GROUND_ROW);
                    ctx.assertTrue(green(ground) > blue(ground),
                            "and the grass at the player's feet is drawn as ever; got " + Integer.toHexString(ground));
                })
                .thenScreenshot(0, "seamless_portal")
                .thenServer(0, level -> {
                    Physics.remove(level, PORTAL);
                    final ServerPlayer player = ctx.serverPlayer();
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                    final BlockPos up = ctx.abs(STAND.above(4));
                    player.teleportTo(level, up.getX() + 0.5, up.getY(), up.getZ() + 0.5, 0F, 0F);
                    Physics.own(player, new Laws(Optional.of(new Pull.Scaled(0)), Optional.empty(), Optional.empty(),
                            Optional.empty(), Optional.empty()));
                })
                .thenWaitUntil(() -> ctx.player().getY() > ctx.abs(STAND).getY() + 3.5, WAIT,
                        "the player's game to put them up in the air")
                .then(FLOAT, () -> ctx.assertTrue(ctx.player().getY() > ctx.abs(STAND).getY() + 3.5
                                && !ctx.player().onGround(),
                        "carrying their own weightlessness, the player floats; at " + ctx.player().getY()))
                .thenServer(0, level -> Physics.own(ctx.serverPlayer(), Laws.NONE))
                .thenWaitUntil(() -> ctx.player().onGround(), ARRIVE, "the player to fall once they carry no laws");
    }

    /* The wall behind the portal of {@code near}, and the one beyond its far side of {@code far}. */
    private static void walls(final ClientTestContext ctx, final ServerLevel level, final BlockState near,
                              final BlockState far) {
        for (int x = -WALL_HALF; x <= WALL_HALF; x++) {
            for (int y = FLOOR; y < FLOOR + WALL_HIGH; y++) {
                level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, WALL_Z)), near);
                level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, GOLD_Z)), far);
            }
        }
    }

    /* An area round the place the player stands, under {@code light}. */
    private static PhysicsArea around(final ClientTestContext ctx, final String id, final Laws.Light light) {
        final BlockPos low = ctx.abs(STAND.offset(-3, 0, -3));
        final BlockPos high = ctx.abs(STAND.offset(3, 5, 3));
        return new PhysicsArea(id, Box.between(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(),
                high.getZ()), new Laws(Optional.empty(), Optional.empty(), Optional.of(light), Optional.empty(),
                Optional.empty()), 0);
    }

    private static boolean hasArea(final ClientTestContext ctx, final String id) {
        return Physics.areas(ctx.mc().level).stream().anyMatch(area -> area.id().equals(id));
    }

    /* The colour in the middle of the screen. */
    private static int middle(final ClientTestContext ctx) {
        return ctx.pixel(ctx.mc().getWindow().getGuiScaledWidth() / 2, ctx.mc().getWindow().getGuiScaledHeight() / 2);
    }

    private static int red(final int argb) {
        return argb >> 16 & 0xFF;
    }

    private static int green(final int argb) {
        return argb >> 8 & 0xFF;
    }

    private static int blue(final int argb) {
        return argb & 0xFF;
    }
}
