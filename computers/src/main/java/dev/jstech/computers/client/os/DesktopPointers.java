/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import com.mojang.blaze3d.platform.Window;
import dev.jstech.computers.JsComputers;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.motion.MotionKinds;
import dev.jstech.core.motion.MotionSpec;
import dev.jstech.core.motion.Rhythm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The pointer a desktop draws over the monitor's glass, in its own system's cursors: Frames 95's arrow and hourglass,
 * Frames XP's with the sand falling, the Aero ring of Frames 11, the X core cursors of CDE, GNOME 1 and KDE 2 and 3,
 * Plasma's Breeze, GNOME's Adwaita and Cinnamon's DMZ-White. Each comes out the size of the computer's own pointer:
 * one texel to one pixel of the game's window at the display's scale, however large the desktop is drawn.
 *
 * <p>The pointer is busy while the program in front waits on the machine; it works, with clicks still going through,
 * while a program starts or a copy runs, and is busy instead on a system that had no such pointer; on KDE a program
 * starting bounces its icon beside the arrow. How fast each turns is in the system's motion profile, and with motion
 * reduced each stands in its first picture.
 */
final class DesktopPointers {

    private final DesktopState desktop;

    private static final Set FRAMES_95 = new Set(art("frames_95_arrow", 12, 19, 1, 0, 0),
            art("frames_95_busy", 13, 20, 1, 6, 10), art("frames_95_working", 20, 22, 1, 0, 0), null);
    private static final Set FRAMES_XP = new Set(art("frames_xp_arrow", 15, 22, 1, 0, 0),
            art("frames_xp_busy", 25, 25, 16, 12, 12), art("frames_xp_working", 22, 24, 16, 0, 0), null);
    private static final Set AERO = new Set(art("frames_aero_arrow", 15, 22, 1, 0, 0),
            art("frames_aero_busy", 16, 16, 18, 8, 8), art("frames_aero_working", 22, 24, 18, 0, 0), null);
    private static final Set CDE = new Set(art("cde_arrow", 10, 16, 1, 0, 0), art("cde_busy", 16, 16, 1, 8, 8),
            null, null);
    private static final Set GNOME1 = new Set(art("gnome1_arrow", 10, 16, 1, 0, 0),
            art("gnome1_busy", 16, 16, 1, 8, 8), null, null);
    private static final Set KDE2 = new Set(art("kde2_arrow", 10, 16, 1, 0, 0), art("kde2_busy", 16, 16, 1, 8, 8),
            null, art("kde2_launch", 24, 26, 12, 0, 0));
    private static final Set PLASMA = new Set(art("plasma_arrow", 15, 22, 1, 0, 0),
            art("plasma_busy", 14, 14, 18, 7, 7), art("plasma_working", 22, 24, 18, 0, 0),
            art("plasma_launch", 24, 26, 12, 0, 0));
    private static final Set GNOME = new Set(art("gnome_arrow", 15, 22, 1, 0, 0),
            art("gnome_busy", 14, 14, 16, 7, 7), art("gnome_working", 22, 24, 16, 0, 0), null);
    private static final Set CINNAMON = new Set(art("cinnamon_arrow", 15, 22, 1, 0, 0),
            art("cinnamon_busy", 14, 14, 8, 7, 7), art("cinnamon_working", 22, 24, 8, 0, 0), null);

    DesktopPointers(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** What the pointer shows this frame. */
    State state() {
        if (desktop.frontWaiting()) {
            return State.BUSY;
        }
        final Set set = set();
        if (desktop.starting() && set.launch() != null) {
            return State.LAUNCH;
        }
        if (desktop.starting() || desktop.copying()) {
            return set.working() != null ? State.WORKING : State.BUSY;
        }
        return State.ARROW;
    }

    /** Draws the pointer with its hot spot at the desktop point ({@code x}, {@code y}). */
    void draw(final GuiGraphics g, final int x, final int y) {
        final State state = state();
        final Set set = set();
        final Art art = switch (state) {
            case BUSY -> set.busy();
            case WORKING -> set.working() != null ? set.working() : set.busy();
            case LAUNCH -> set.launch() != null ? set.launch() : set.busy();
            case ARROW -> set.arrow();
        };
        final int frame = art.frames() > 1 ? frame(state, art) : 0;
        // The size of the computer's own pointer: one texel to one pixel of the window, at the display's own scale.
        final float texel = screenTexel();
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(texel, texel, 1);
        g.blit(art.texture(), -art.hotX(), -art.hotY(), frame * art.w(), 0, art.w(), art.h(),
                art.frames() * art.w(), art.h());
        g.pose().popPose();
    }

    /*
     * How big a texel is drawn in the desktop's own units for it to come out as one pixel of the game's window, times
     * the display's scale (125 or 150 percent on Windows), which is the size the system's own pointer is drawn at.
     */
    private float screenTexel() {
        final Window window = Minecraft.getInstance().getWindow();
        final float[] scaleX = new float[1];
        final float[] scaleY = new float[1];
        GLFW.glfwGetWindowContentScale(window.getWindow(), scaleX, scaleY);
        final float display = scaleX[0] > 0.0F ? scaleX[0] : 1.0F;
        return (float) (display / (window.getGuiScale() * desktop.view().scale()));
    }

    /* The picture a turning pointer stands at, by the system's own pace. */
    private int frame(final State state, final Art art) {
        final String kind = switch (state) {
            case WORKING -> MotionKinds.POINTER_WORKING;
            case LAUNCH -> MotionKinds.POINTER_LAUNCH;
            case BUSY, ARROW -> MotionKinds.POINTER_BUSY;
        };
        final MotionSpec spec = desktop.motion().ongoing(kind);
        return Math.min(art.frames() - 1, Rhythm.frame(spec, MotionClock.loopMs()));
    }

    /* This desktop's cursors, by the look of its system and its generation. */
    private Set set() {
        final boolean period = desktop.periodPanel();
        return switch (desktop.panelStyle()) {
            case FRAMES_95 -> FRAMES_95;
            case FRAMES_XP -> FRAMES_XP;
            case FRAMES_11 -> AERO;
            case KDE -> period ? KDE2 : PLASMA;
            case GNOME -> period ? GNOME1 : GNOME;
            case CINNAMON -> CINNAMON;
            case CDE -> CDE;
        };
    }

    private static Art art(final String name, final int w, final int h, final int frames, final int hotX,
                           final int hotY) {
        return new Art(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "textures/gui/cursor/" + name + ".png"),
                w, h, frames, hotX, hotY);
    }

    /** What the pointer shows. */
    enum State {
        ARROW, BUSY, WORKING, LAUNCH
    }

    /**
     * One pointer picture: its strip, how big each picture is, how many it has side by side, and its hot spot, the
     * arrow's tip or a lone ring's middle.
     */
    record Art(ResourceLocation texture, int w, int h, int frames, int hotX, int hotY) {
    }

    /** A system's cursors: the arrow, the busy one, and the working and launching ones it had, or null. */
    record Set(Art arrow, Art busy, @Nullable Art working, @Nullable Art launch) {
    }
}
