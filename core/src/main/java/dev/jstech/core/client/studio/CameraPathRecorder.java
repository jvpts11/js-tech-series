/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.camera.CameraPath;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.model.Easing;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * The camera path the studio edits, kept while the game runs as the player goes from the studio into the world and
 * back: its keyframes, the place it is played from, facing south, and the keyframe chosen. While it records, the key
 * {@link CoreKeys#KEEP_KEYFRAME} keeps where the player stands and looks as its next keyframe, two seconds after the
 * one before, and a line at the top of the screen says so.
 */
public final class CameraPathRecorder {

    /** How long after the keyframe before a new one is put, in ticks. */
    public static final int STEP_TICKS = 40;

    private static final List<CameraPath.Keyframe> KEYFRAMES = new ArrayList<>();
    private static final double TENTHS = 10.0;
    private static final float SMALL = 0.75F;
    private static final int LINE_X = 4;
    private static final int LINE_Y = 4;
    /* The test recorder's line stands at the top too; this one goes under it then. */
    private static final int UNDER_THE_TESTS = 9;

    private static @Nullable ResourceLocation editing;
    /* The name written so far for a new path. */
    private static String written = "";
    private static Vec3 origin = Vec3.ZERO;
    private static boolean placed;
    private static boolean hideHud = true;
    private static int chosen = -1;
    private static volatile boolean recording;

    private CameraPathRecorder() {
    }

    /** Whether it records now. */
    public static boolean recording() {
        return recording;
    }

    /** The path edited, or none for a new one not yet named. */
    public static Optional<ResourceLocation> editing() {
        return Optional.ofNullable(editing);
    }

    /** Its keyframes, in order of time. */
    public static List<CameraPath.Keyframe> keyframes() {
        return List.copyOf(KEYFRAMES);
    }

    /** The path its keyframes make, or none while they are fewer than two. */
    public static Optional<CameraPath> path() {
        return CameraPath.ordered(KEYFRAMES) ? Optional.of(new CameraPath(KEYFRAMES, hideHud)) : Optional.empty();
    }

    /** The place it is played from, facing south. */
    public static Vec3 origin() {
        return origin;
    }

    /** The keyframe chosen, or -1 for none. */
    public static int chosen() {
        return chosen;
    }

    /** Hands the game's bus and the key what the recorder answers, once, from the studio's client. */
    static void listen() {
        KeyActionsClient.onPress(CoreKeys.KEEP_KEYFRAME, () -> {
            if (recording && Minecraft.getInstance().screen == null) {
                final int kept = keepHere();
                final LocalPlayer player = Minecraft.getInstance().player;
                if (kept >= 0 && player != null) {
                    player.displayClientMessage(GameText.component(CameraPathTexts.KEPT.with(kept + 1)), true);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, event -> {
            final Minecraft game = Minecraft.getInstance();
            if (recording && game.screen == null) {
                Draw.textScaled(event.getGuiGraphics(), game.font, line(), LINE_X, LINE_Y
                        + (TestRecorder.recording() ? UNDER_THE_TESTS : 0), StudioPalette.get().recording(), SMALL);
            }
        });
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> recording = false);
    }

    /** What the line at the top of the screen says while it records, naming the key. */
    static String line() {
        return GameText.resolve(CameraPathTexts.RECORDING.with(KeyActionsClient.mapping(CoreKeys.KEEP_KEYFRAME)
                .getTranslatedKeyMessage().getString()));
    }

    /** Starts recording, and closes the studio so the player can fly where the camera goes. */
    static void start() {
        recording = true;
        Minecraft.getInstance().setScreen(null);
    }

    static void stop() {
        recording = false;
    }

    /** Whether it has a place to be played from yet. */
    static boolean placed() {
        return placed;
    }

    /** Edits {@code path} as {@code id}, or a new path when none, played from {@code at}. */
    static void edit(@Nullable final ResourceLocation id, @Nullable final CameraPath path, final Vec3 at) {
        editing = id;
        written = "";
        origin = at;
        placed = true;
        KEYFRAMES.clear();
        hideHud = path == null || path.hideHud();
        if (path != null) {
            KEYFRAMES.addAll(path.keyframes());
        }
        chosen = KEYFRAMES.isEmpty() ? -1 : 0;
    }

    /** Names the path edited {@code id}, as it is saved. */
    static void named(final ResourceLocation id) {
        editing = id;
        written = "";
    }

    /** The name written so far for a new path, kept while the studio is shut. */
    static String written() {
        return written;
    }

    static void write(final String name) {
        written = name;
    }

    static void choose(final int index) {
        chosen = index >= 0 && index < KEYFRAMES.size() ? index : -1;
    }

    /** Has the keyframe {@code index} be {@code keyframe}. */
    static void set(final int index, final CameraPath.Keyframe keyframe) {
        KEYFRAMES.set(index, keyframe);
    }

    /** Takes the keyframe {@code index} away, choosing the one before it. */
    static void remove(final int index) {
        if (index >= 0 && index < KEYFRAMES.size()) {
            KEYFRAMES.remove(index);
            chosen = Math.min(index, KEYFRAMES.size() - 1);
        }
    }

    /**
     * Keeps where the player stands and looks now, from the path's place, as its next keyframe, two seconds after the
     * last; which it is, or -1 when there is no player.
     */
    static int keepHere() {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return -1;
        }
        final Vec3 eye = player.getEyePosition();
        final int time = KEYFRAMES.isEmpty() ? 0 : KEYFRAMES.get(KEYFRAMES.size() - 1).time() + STEP_TICKS;
        KEYFRAMES.add(new CameraPath.Keyframe(time, tenths(eye.x - origin.x), tenths(eye.y - origin.y),
                tenths(eye.z - origin.z), Math.round(Mth.wrapDegrees(player.getYRot())), Math.round(player.getXRot()),
                0.0F, 0.0, KEYFRAMES.isEmpty() ? Easing.LINEAR : Easing.EASE_IN_OUT_SINE));
        chosen = KEYFRAMES.size() - 1;
        return chosen;
    }

    private static double tenths(final double value) {
        return Math.round(value * TENTHS) / TENTHS;
    }
}
