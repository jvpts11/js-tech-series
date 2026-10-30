/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.client.live.ILivePainter;
import dev.jstech.core.client.live.LiveScreens;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.gui.Tube;
import dev.jstech.core.live.LiveRate;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import dev.jstech.space.client.rocket.RocketCabins;
import dev.jstech.space.rocket.CabinLamp;
import dev.jstech.space.rocket.CabinReadings;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws a starship cockpit's live faces over its model, as the flight's readings say, and moves its side stick, its
 * throttle and its navigation globe: the screens follow the flight, the needles and the lamps the ship's state, the
 * master caution lights with the master alarm, Kosmozavod's guards lift while the engines run, the globe turns with
 * the heading, and the stick and the throttle move with the pilot's hand.
 */
public final class ShipCabinRenderer {

    /* How many pixels a block each instrument is drawn in. */
    private static final int DENSITY = 400;
    /* A live picture lies a hair off its face towards the crew. */
    private static final float LIFT = 0.0015F;
    /* A model's bones move in sixteenths of a block. */
    private static final float PIXELS = 16;
    /* How far the stick leans at the most, in degrees, and the attitude that leans it so far. */
    private static final float STICK_MOST = 18;
    private static final float ATTITUDE_MOST = 30;
    private static final String HORIZON = "horizon";
    private static final String MAP = "map";
    private static final String BARS = "bars";
    private static final String CRT = "crt";
    private static final String CAUTION = "caution";
    private static final String LAMP = "lamp";
    private static final String GUARDS = "guards";
    private static final String DIAL = "dial";
    private static final int DIALS = 4;

    private ShipCabinRenderer() {
    }

    /** The driver that moves {@code cabin}'s stick, throttle and globe by the readings. */
    public static IKitDriver<CabinReadings> driver(final RocketCabins.Cabin cabin) {
        return new Driver(cabin);
    }

    /**
     * Draws {@code cabin}'s live faces over {@code instance}'s model, the pose stack standing on the model's origin,
     * as {@code readings} say, {@code distanceSquared} from the viewer; a Kosmozavod cockpit's dials are {@code black}.
     */
    public static void draw(final BlockEntity owner, final String part, final KitInstance instance,
                            final RocketCabins.Cabin cabin, final CabinReadings readings, final boolean black,
                            final PoseStack poses, final MultiBufferSource buffers, final int light,
                            final double distanceSquared) {
        if (!LiveRate.shows(distanceSquared)) {
            return;
        }
        final ModelPose pose = instance.pose();
        final float scale = instance.model().scale();
        for (final RocketCabins.Face face : cabin.faces()) {
            final int cell = pose.index(face.cell());
            if (cell >= 0 && pose.hidden(cell)) {
                continue;
            }
            final List<RocketCabins.Region> regions = cabin.regions().stream()
                    .filter(region -> region.face().equals(face.name())).toList();
            if (regions.isEmpty()) {
                continue;
            }
            poses.pushPose();
            poses.scale(scale, scale, scale);
            final float shade = LiveScreens.shade(poses.last().normal().transform(new Vector3f(face.out()))
                    .normalize());
            poses.mulPose(onFace(face));
            for (final RocketCabins.Region region : regions) {
                drawRegion(owner, part, face, region, readings, black, poses, buffers, light, shade,
                        distanceSquared);
            }
            poses.popPose();
        }
    }

    /* The face's own space: its top left the origin, across it and down it a pixel a unit, off it towards the crew. */
    private static Matrix4f onFace(final RocketCabins.Face face) {
        final Vector3f right = face.right();
        final Vector3f up = face.up();
        final Vector3f out = face.out();
        final float per = 1.0F / DENSITY;
        final Vector3f corner = new Vector3f(face.centre()).sub(new Vector3f(right).mul(face.halfWidth()))
                .add(new Vector3f(up).mul(face.halfHeight())).add(new Vector3f(out).mul(LIFT));
        return new Matrix4f(right.x() * per, right.y() * per, right.z() * per, 0,
                -up.x() * per, -up.y() * per, -up.z() * per, 0,
                -out.x(), -out.y(), -out.z(), 0,
                corner.x(), corner.y(), corner.z(), 1);
    }

    private static void drawRegion(final BlockEntity owner, final String part, final RocketCabins.Face face,
                                   final RocketCabins.Region region, final CabinReadings readings,
                                   final boolean black, final PoseStack poses, final MultiBufferSource buffers,
                                   final int light, final float shade, final double distanceSquared) {
        final float wide = face.halfWidth() * 2 * DENSITY;
        final float tall = face.halfHeight() * 2 * DENSITY;
        final float x0 = region.left() * wide;
        final float y0 = region.top() * tall;
        final float x1 = region.right() * wide;
        final float y1 = region.bottom() * tall;
        final int w = Math.max(1, Math.round(x1 - x0));
        final int h = Math.max(1, Math.round(y1 - y0));
        final Key key = new Key(owner, part, face.name(), region.kind(), region.index());
        final boolean running = readings.throttle() > 0;
        final ILivePainter painter = switch (region.kind()) {
            case HORIZON -> (g, gw, gh, tick) -> ShipCabinArt.horizon(g, gw, gh, readings.pitch(), readings.roll());
            case MAP -> (g, gw, gh, tick) -> ShipCabinArt.map(g, gw, gh, readings.heading());
            case BARS -> (g, gw, gh, tick) -> ShipCabinArt.bars(g, gw, gh, readings.throttle());
            case CRT -> (g, gw, gh, tick) -> ShipCabinArt.crt(g, gw, gh, readings.clock());
            case CAUTION -> (g, gw, gh, tick) -> ShipCabinArt.caution(g, gw, gh,
                    readings.lit(CabinLamp.MASTER_ALARM));
            case LAMP -> (g, gw, gh, tick) -> ShipCabinArt.lamp(g, gw, gh, region.index(),
                    Math.floorMod(region.index(), 3) == 1 ? readings.lit(CabinLamp.MASTER_ALARM) : running);
            case GUARDS -> (g, gw, gh, tick) -> ShipCabinArt.guards(g, gw, gh, running);
            case DIAL -> (g, gw, gh, tick) -> ShipCabinArt.dial(g, gw, gh, black, dial(readings, region.index()));
            default -> null;
        };
        if (painter == null) {
            return;
        }
        final boolean glows = HORIZON.equals(region.kind()) || MAP.equals(region.kind())
                || BARS.equals(region.kind()) || CRT.equals(region.kind());
        LiveScreens.draw(poses, buffers, LiveScreens.ask(key, painter, w, h, 1, Tube.COLOUR, distanceSquared),
                x0, y0, x1, y1, glows ? LightTexture.FULL_BRIGHT : light, glows ? 1 : shade);
    }

    /* What the dial {@code index} reads, 0 to 1: the throttle, the climb, the propellant, the air, in turn. */
    private static float dial(final CabinReadings readings, final int index) {
        return switch (index % DIALS) {
            case 0 -> readings.throttle();
            case 1 -> readings.climbShare();
            case 2 -> readings.propellant();
            default -> Math.min(1, readings.air());
        };
    }

    /* A picture of a cockpit, known by what draws it, its part, its face, its kind and which of its kind it is. */
    private record Key(BlockEntity owner, String part, String face, String kind, int index) {
    }

    /*
     * Turns the globe about its pole by the heading, leans the stick forward by the pitch and aside by the roll, as
     * far as its travel allows, and slides the throttle along its slot to its setting.
     */
    private record Driver(RocketCabins.Cabin cabin) implements IKitDriver<CabinReadings> {

        @Override
        public void drive(final CabinReadings readings, final ModelAnimator animator, final double seconds) {
            // Nothing plays on a cockpit; its bones are moved below.
        }

        @Override
        public void move(final CabinReadings readings, final ModelPose pose, final double seconds) {
            this.cabin.ball().ifPresent(globe -> {
                final int bone = pose.index(globe.bone());
                if (bone >= 0) {
                    pose.setTurn(bone, new Quaternionf().rotateAxis((float) Math.toRadians(readings.heading()),
                            globe.up()));
                }
            });
            this.cabin.stick().ifPresent(stick -> {
                final int bone = pose.index(stick.bone());
                if (bone >= 0) {
                    final float forward = lean(readings.pitch());
                    final float aside = lean(readings.roll());
                    pose.setTurn(bone, new Quaternionf().rotateAxis((float) Math.toRadians(-forward), stick.right())
                            .rotateAxis((float) Math.toRadians(-aside), stick.out()));
                }
            });
            this.cabin.throttle().ifPresent(throttle -> {
                final int bone = pose.index(throttle.bone());
                if (bone >= 0) {
                    final float along = (readings.throttle() - throttle.rest()) * PIXELS;
                    pose.addMove(bone, throttle.travel().x() * along, throttle.travel().y() * along,
                            throttle.travel().z() * along);
                }
            });
        }

        @Override
        public boolean movesBones() {
            return true;
        }

        private static float lean(final float degrees) {
            return Math.max(-STICK_MOST, Math.min(STICK_MOST, degrees / ATTITUDE_MOST * STICK_MOST));
        }
    }
}
