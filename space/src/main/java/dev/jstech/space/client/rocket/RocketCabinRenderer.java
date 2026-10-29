/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.rocket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.client.live.LiveScreen;
import dev.jstech.core.client.live.LiveScreens;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.gui.Tube;
import dev.jstech.core.live.LiveRate;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import dev.jstech.space.rocket.CabinLamp;
import dev.jstech.space.rocket.CabinReadings;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws a model's live cabin over it as the flight goes ({@link RocketCabins}): the attitude ball turned and the
 * throttle slid by the readings, each a bone of the model; and on each face of the panel whose block is there, its
 * live instruments, each a picture of its own laid on its region: the strips, the climb's strip, the lamps, the tape
 * and the gauges in the light that falls on the panel, the Flight Computer's screen, the clock's segments and the lit
 * lamps' glass shining of themselves.
 */
public final class RocketCabinRenderer {

    /* How far off its face a picture is laid, and a lit lamp's glass over it, in blocks. */
    private static final float LIFT = 0.0015F;
    private static final float GLOW = 0.0015F;
    /* A model's pixels a block. */
    private static final float PIXELS = 16;
    private static final String STRIP = "strip";
    private static final String UPRIGHT_STRIP = "vstrip";
    private static final String SCREEN = "fcc";
    private static final String LAMP = "lamp";
    private static final String TAPE = "tape";
    private static final String TIMER = "timer";
    private static final String GAUGE = "gauge";
    /* The gauges: the cabin's air on the left, read against twice the air at sea level; the propellant on the right. */
    private static final int AIR_GAUGE = 0;
    private static final float MOST_AIR = 2;

    private RocketCabinRenderer() {
    }

    /** What moves the bones of {@code cabin}'s model by the readings it is drawn with. */
    public static IKitDriver<CabinReadings> driver(final RocketCabins.Cabin cabin) {
        return new Driver(cabin);
    }

    /**
     * Draws {@code cabin}'s live faces over {@code instance}'s model, the pose stack standing on the model's origin,
     * as {@code readings} say, {@code distanceSquared} from the viewer.
     *
     * @param owner what draws the model, and its part's name, which the pictures are known by
     */
    public static void draw(final BlockEntity owner, final String part, final KitInstance instance,
                            final RocketCabins.Cabin cabin, final CabinReadings readings, final PoseStack poses,
                            final MultiBufferSource buffers, final int light, final double distanceSquared) {
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
            drawFace(owner, part, face, regions, readings, poses, buffers, light, shade, distanceSquared);
            poses.popPose();
        }
    }

    /*
     * The face's own space: its top left the origin, across it to the right and down it, a pixel of the instruments'
     * a unit, and off it towards the crew less deep, in blocks.
     */
    private static Matrix4f onFace(final RocketCabins.Face face) {
        final Vector3f right = face.right();
        final Vector3f up = face.up();
        final Vector3f out = face.out();
        final float per = 1.0F / CabinArt.DENSITY;
        final Vector3f corner = new Vector3f(face.centre()).sub(new Vector3f(right).mul(face.halfWidth()))
                .add(new Vector3f(up).mul(face.halfHeight())).add(new Vector3f(out).mul(LIFT));
        return new Matrix4f(right.x() * per, right.y() * per, right.z() * per, 0,
                -up.x() * per, -up.y() * per, -up.z() * per, 0,
                -out.x(), -out.y(), -out.z(), 0,
                corner.x(), corner.y(), corner.z(), 1);
    }

    private static void drawFace(final BlockEntity owner, final String part, final RocketCabins.Face face,
                                 final List<RocketCabins.Region> regions, final CabinReadings readings,
                                 final PoseStack poses, final MultiBufferSource buffers, final int light,
                                 final float shade, final double distanceSquared) {
        final float wide = face.halfWidth() * 2 * CabinArt.DENSITY;
        final float tall = face.halfHeight() * 2 * CabinArt.DENSITY;
        final List<float[]> lamps = new ArrayList<>();
        for (final RocketCabins.Region region : regions) {
            final float x0 = region.left() * wide;
            final float y0 = region.top() * tall;
            final float x1 = region.right() * wide;
            final float y1 = region.bottom() * tall;
            final int w = Math.max(1, Math.round(x1 - x0));
            final int h = Math.max(1, Math.round(y1 - y0));
            final Key key = new Key(owner, part, face.name(), region.kind(), region.index());
            switch (region.kind()) {
                case STRIP -> {
                    final float level = region.index() == 0 ? readings.fuel() : readings.oxidiser();
                    show(poses, buffers, LiveScreens.ask(key, (g, gw, gh, tick) -> CabinArt.strip(g, gw, gh, level),
                            w, h, 1, Tube.COLOUR, distanceSquared), x0, y0, x1, y1, light, shade);
                }
                case UPRIGHT_STRIP -> {
                    final float level = readings.climbShare();
                    show(poses, buffers, LiveScreens.ask(key, (g, gw, gh, tick) -> CabinArt.uprightStrip(g, gw, gh,
                            level), w, h, 1, Tube.COLOUR, distanceSquared), x0, y0, x1, y1, light, shade);
                }
                case SCREEN -> show(poses, buffers, LiveScreens.ask(key, (g, gw, gh, tick) ->
                                FlightComputerFace.paint(g, gw, gh, readings), FlightComputerFace.WIDTH,
                        FlightComputerFace.HEIGHT, 1, Tube.COLOUR, distanceSquared), x0, y0, x1, y1,
                        LightTexture.FULL_BRIGHT, 1);
                case TAPE -> {
                    final float heading = readings.heading();
                    show(poses, buffers, LiveScreens.ask(key, (g, gw, gh, tick) -> CabinArt.tape(g, gw, gh, heading),
                            w, h, 1, Tube.COLOUR, distanceSquared), x0, y0, x1, y1, light, shade);
                }
                case TIMER -> {
                    final String shown = readings.timer();
                    show(poses, buffers, LiveScreens.ask(key, (g, gw, gh, tick) -> CabinArt.timer(g, gw, gh, shown),
                            w, h, 1, Tube.COLOUR, distanceSquared), x0, y0, x1, y1, LightTexture.FULL_BRIGHT, 1);
                }
                case GAUGE -> {
                    final boolean air = region.index() == AIR_GAUGE;
                    final float level = air ? readings.air() / MOST_AIR : readings.propellant();
                    show(poses, buffers, LiveScreens.ask(key, (g, gw, gh, tick) -> CabinArt.gauge(g, gw, level, !air),
                            w, w, 1, Tube.COLOUR, distanceSquared), x0, y0, x1, y1, light, shade);
                }
                case LAMP -> lamps.add(new float[] {x0, y0, x1, y1});
                default -> {
                    // A kind of instrument this panel does not draw.
                }
            }
        }
        if (!lamps.isEmpty()) {
            drawLamps(owner, part, face, lamps, readings, poses, buffers, light, shade, distanceSquared);
        }
    }

    /* The bank of lamps as one picture over the box they share, then the lit lamps' glass shining over it. */
    private static void drawLamps(final BlockEntity owner, final String part, final RocketCabins.Face face,
                                  final List<float[]> lamps, final CabinReadings readings, final PoseStack poses,
                                  final MultiBufferSource buffers, final int light, final float shade,
                                  final double distanceSquared) {
        float x0 = Float.MAX_VALUE;
        float y0 = Float.MAX_VALUE;
        float x1 = -Float.MAX_VALUE;
        float y1 = -Float.MAX_VALUE;
        for (final float[] box : lamps) {
            x0 = Math.min(x0, box[0]);
            y0 = Math.min(y0, box[1]);
            x1 = Math.max(x1, box[2]);
            y1 = Math.max(y1, box[3]);
        }
        final float left = x0;
        final float top = y0;
        final Key key = new Key(owner, part, face.name(), LAMP, 0);
        show(poses, buffers, LiveScreens.ask(key, (g, gw, gh, tick) -> CabinArt.lamps(g, gw, gh, left, top, lamps,
                        readings), Math.max(1, Math.round(x1 - x0)), Math.max(1, Math.round(y1 - y0)), 1, Tube.COLOUR,
                distanceSquared), x0, y0, x1, y1, light, shade);
        final VertexConsumer glow = buffers.getBuffer(RenderType.textBackground());
        final Matrix4f at = poses.last().pose();
        for (int k = 0; k < lamps.size() && k < CabinLamp.BANK.size(); k++) {
            final CabinLamp lamp = CabinLamp.BANK.get(k);
            if (!readings.lit(lamp)) {
                continue;
            }
            final float[] box = lamps.get(k);
            final float[] glass = CabinArt.glass(box);
            quad(glow, at, glass[0], glass[1], glass[2], glass[3], -GLOW, CabinArt.colour(lamp));
            final float[] marks = CabinArt.marks(box, k);
            for (int m = 0; m + 3 < marks.length; m += 4) {
                quad(glow, at, marks[m], marks[m + 1], marks[m + 2], marks[m + 3], -2 * GLOW, CabinArt.litLabel());
            }
        }
    }

    private static void show(final PoseStack poses, final MultiBufferSource buffers, final LiveScreen screen,
                             final float x0, final float y0, final float x1, final float y1, final int light,
                             final float shade) {
        LiveScreens.draw(poses, buffers, screen, x0, y0, x1, y1, light, shade);
    }

    /* A flat quad in one colour, shining of itself, {@code z} off the face towards the crew. */
    private static void quad(final VertexConsumer to, final Matrix4f at, final float x0, final float y0,
                             final float x1, final float y1, final float z, final int argb) {
        to.addVertex(at, x0, y0, z).setColor(argb).setLight(LightTexture.FULL_BRIGHT);
        to.addVertex(at, x0, y1, z).setColor(argb).setLight(LightTexture.FULL_BRIGHT);
        to.addVertex(at, x1, y1, z).setColor(argb).setLight(LightTexture.FULL_BRIGHT);
        to.addVertex(at, x1, y0, z).setColor(argb).setLight(LightTexture.FULL_BRIGHT);
    }

    /* A picture of a cabin, known by what draws it, its part, its face, its kind and which of its kind it is. */
    private record Key(BlockEntity owner, String part, String face, String kind, int index) {
    }

    /*
     * Turns the attitude ball by the readings, about the panel's right by the pitch, about the way the panel faces by
     * the roll and about its own pole by the heading; slides the throttle along its slot to its setting.
     */
    private record Driver(RocketCabins.Cabin cabin) implements IKitDriver<CabinReadings> {

        @Override
        public void drive(final CabinReadings readings, final ModelAnimator animator, final double seconds) {
            // Nothing plays on a cabin; its bones are moved below.
        }

        @Override
        public void move(final CabinReadings readings, final ModelPose pose, final double seconds) {
            this.cabin.ball().ifPresent(ball -> {
                final int bone = pose.index(ball.bone());
                if (bone >= 0) {
                    pose.setTurn(bone, new Quaternionf()
                            .rotateAxis((float) Math.toRadians(readings.pitch()), ball.right())
                            .rotateAxis((float) -Math.toRadians(readings.roll()), ball.out())
                            .rotateAxis((float) Math.toRadians(readings.heading()), ball.up()));
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
    }
}
