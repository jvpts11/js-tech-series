/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.audio.AudioDebugTexts;
import dev.jstech.core.audio.acoustics.AcousticSpace;
import dev.jstech.core.audio.acoustics.Reverb;
import dev.jstech.core.audio.acoustics.SoundMedium;
import dev.jstech.core.client.GameLocale;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Sound made visible, for whoever builds with it: on the debug screen, the place's echo, its walls and its air, and
 * the ringing ears; in the world, while the player turns it on with its key, the rays of the place's last round (a
 * wall met, or the open air reached) and a line to every sound playing, coloured by whether walls muffle it.
 */
final class AcousticsDebug {

    /* Half the side of the box drawn where a sound plays, and of the cross where a ray ends. */
    private static final double MARK = 0.15;
    /* How far under the eyes the lines start, and how far ahead of them, in blocks. */
    private static final double UNDER_EYES = 0.3;
    private static final double AHEAD_OF_EYES = 0.6;

    private static volatile boolean shown;

    private AcousticsDebug() {
    }

    /** Turns the overlay on or off. */
    static void toggle() {
        shown = !shown;
    }

    /** Whether the overlay is on. */
    static boolean shown() {
        return shown;
    }

    /** The lines on the debug screen, under the Sound Mixer's: the echo, the place, the air, the ears. */
    static List<String> lines() {
        final List<String> out = new ArrayList<>();
        final Reverb echo = SoundShaper.echo();
        out.add(GameText.resolve(AudioDebugTexts.ECHO.with(format(echo.decaySeconds()),
                Math.round(echo.wet() * 100))));
        final AcousticSpace place = AcousticProbe.space();
        out.add(GameText.resolve(AudioDebugTexts.PLACE.with(Math.round(place.enclosure() * 100),
                format(place.meanFreePath()), Math.round(place.absorption() * 100))));
        if (ListenerAir.inRoom()) {
            out.add(GameText.resolve(AudioDebugTexts.ROOM.with(GameLocale.count(
                    Math.round(ListenerAir.now().roomVolume())), GameLocale.count(
                    Math.round(ListenerAir.now().roomSurface())))));
        }
        final SoundMedium medium = ListenerAir.medium();
        out.add(GameText.resolve(switch (medium.kind()) {
            case VACUUM -> AudioDebugTexts.MEDIUM_VACUUM.text();
            case WATER -> AudioDebugTexts.MEDIUM_WATER.with(Math.round(medium.speed()));
            case AIR -> AudioDebugTexts.MEDIUM_AIR.with(format(medium.pressure()), Math.round(medium.speed()),
                    Math.round(medium.gain(0) * 100));
        }));
        if (RingingEars.ringing()) {
            out.add(GameText.resolve(AudioDebugTexts.RINGING.with(Math.round((1 - RingingEars.heard()) * 100))));
        }
        return out;
    }

    /** Draws the overlay, once the world's see-through blocks are drawn, while it is on. */
    static void render(final RenderLevelStageEvent event) {
        if (!shown || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        final AcousticsDebugPalette.Colours colours = AcousticsDebugPalette.get();
        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack pose = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        final Matrix4f matrix = pose.last().pose();
        // Drawn from a little ahead and under the eyes: a line out of the eye itself is seen end on, as a point, and
        // one from beside the eye starts behind what the camera sees, which the game's lines do not draw.
        final Vec3 under = camera.add(new Vec3(event.getCamera().getLookVector()).scale(AHEAD_OF_EYES))
                .add(0, -UNDER_EYES, 0);
        for (final AcousticProbe.Ray ray : AcousticProbe.rays()) {
            final int colour = ray.hit() ? colours.wall() : colours.open();
            line(lines, pose, matrix, under, ray.to(), colour);
            cross(lines, pose, matrix, ray.to(), colour);
        }
        for (final Map.Entry<SoundInstance, SoundShaper.Playing> entry : SoundShaper.playing().entrySet()) {
            final SoundInstance sound = entry.getKey();
            if (sound.isRelative() || sound.getAttenuation() == SoundInstance.Attenuation.NONE) {
                continue;
            }
            final Vec3 at = new Vec3(sound.getX(), sound.getY(), sound.getZ());
            final int colour = Walls.through(level, camera, at) < 1F ? colours.muffled() : colours.clear();
            line(lines, pose, matrix, under, at, colour);
            LevelRenderer.renderLineBox(pose, lines, new AABB(at, at).inflate(MARK), red(colour), green(colour),
                    blue(colour), 1F);
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static void line(final VertexConsumer lines, final PoseStack pose, final Matrix4f matrix,
                             final Vec3 from, final Vec3 to, final int colour) {
        final Vec3 along = to.subtract(from).normalize();
        lines.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z).setColor(colour)
                .setNormal(pose.last(), (float) along.x, (float) along.y, (float) along.z);
        lines.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z).setColor(colour)
                .setNormal(pose.last(), (float) along.x, (float) along.y, (float) along.z);
    }

    /* A small cross of three lines, one along each axis, where a ray ended. */
    private static void cross(final VertexConsumer lines, final PoseStack pose, final Matrix4f matrix, final Vec3 at,
                              final int colour) {
        line(lines, pose, matrix, at.add(-MARK, 0, 0), at.add(MARK, 0, 0), colour);
        line(lines, pose, matrix, at.add(0, -MARK, 0), at.add(0, MARK, 0), colour);
        line(lines, pose, matrix, at.add(0, 0, -MARK), at.add(0, 0, MARK), colour);
    }

    private static String format(final double value) {
        return String.format(GameLocale.locale(), "%.1f", value);
    }

    private static float red(final int colour) {
        return (colour >> 16 & 0xFF) / 255F;
    }

    private static float green(final int colour) {
        return (colour >> 8 & 0xFF) / 255F;
    }

    private static float blue(final int colour) {
        return (colour & 0xFF) / 255F;
    }
}
