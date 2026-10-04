/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.motion.DeclaredMotion;
import dev.jstech.core.motion.IEasing;
import dev.jstech.core.motion.MotionKinds;
import dev.jstech.core.motion.MotionProfile;
import dev.jstech.core.motion.MotionProfiles;
import dev.jstech.core.motion.MotionSpec;
import dev.jstech.core.motion.MotionStyles;

/**
 * How each desktop of today moves, with the timings of the real system it imitates, each kept in
 * {@code assets/jsc/motions/<system>.json} for a resource pack to slow down, speed up or keep still.
 *
 * <p>The names in each motion's group are the boxes of that system's own settings page that switch it off, so the
 * page and the motion agree on what a box does: Frames XP's "Animate windows when minimizing and maximizing" is
 * {@code minimize}, Cinnamon's "Window effects" is {@code effects}. A system that never moved a thing, CDE, is still.
 */
public final class OsMotions {

    /** Every effect of Frames 11 answers to its one box, "Animation effects". */
    public static final String ANIMATIONS = "animations";
    /** The box over minimizing and restoring a window on the systems that have one. */
    public static final String MINIMIZE = "minimize";
    /** The box over menus and the launcher sliding in. */
    public static final String MENUS = "menus";
    /** Frames XP's "Use drop shadows for icon labels on the desktop", which is a look rather than a motion. */
    public static final String ICON_SHADOWS = "icon_shadows";
    /** Plasma's Scale effect: windows growing in and shrinking away. */
    public static final String SCALE = "scale";
    /** Plasma's Squash effect, drawn as the window shrinking to its button. */
    public static final String SQUASH = "squash";
    /** Plasma's Sliding popups. */
    public static final String SLIDING_POPUPS = "sliding_popups";
    /** KDE 2 and 3's "Enable GUI effects", over every effect of the toolkit. */
    public static final String GUI_EFFECTS = "gui_effects";
    /** GNOME 1's "Wireframe when minimizing", the window manager's outline. */
    public static final String WIREFRAME = "wireframe";
    /** Cinnamon's "Window effects", over every effect on a window. */
    public static final String EFFECTS = "effects";
    /** Cinnamon's "Effects on dialog boxes". */
    public static final String DIALOGS = "dialogs";
    /** Cinnamon's style for opening windows, switched off by choosing none. */
    public static final String MAP = "map";
    /** Cinnamon's style for closing windows, switched off by choosing none. */
    public static final String CLOSE = "close";

    /** A motion its system's speed setting leaves alone, as KDE's minimize slider leaves the toolkit's menus. */
    public static final String STEADY = "steady";

    public static final DeclaredMotion FRAMES_95 = declare("frames_95", MotionProfile.builder()
            // The caption's flight to the taskbar, drawn here as the window going down to its button.
            .kind(MotionKinds.WINDOW_MINIMIZE, zoomOut(250, "linear", MINIMIZE))
            .kind(MotionKinds.WINDOW_RESTORE, zoomIn(250, "linear", MINIMIZE))
            .build());

    public static final DeclaredMotion FRAMES_XP = declare("frames_xp", MotionProfile.builder()
            .kind(MotionKinds.WINDOW_MINIMIZE, zoomOut(250, "linear", MINIMIZE))
            .kind(MotionKinds.WINDOW_RESTORE, zoomIn(250, "linear", MINIMIZE))
            .kind(MotionKinds.MENU_SHOW, slide(200, "ease-out", MENUS, 1.0))
            .build());

    public static final DeclaredMotion FRAMES_11 = declare("frames_11", MotionProfile.builder()
            .kind(MotionKinds.WINDOW_OPEN, scale(250, "fluent-entrance", ANIMATIONS, 0.92, 1.0))
            .kind(MotionKinds.DIALOG_OPEN, scale(167, "fluent-entrance", ANIMATIONS, 0.95, 1.0))
            .kind(MotionKinds.WINDOW_CLOSE, scale(167, "fluent-exit", ANIMATIONS, 1.0, 0.92))
            .kind(MotionKinds.WINDOW_MINIMIZE, zoomOut(250, "fluent-point", ANIMATIONS))
            .kind(MotionKinds.WINDOW_RESTORE, zoomIn(250, "fluent-point", ANIMATIONS))
            .kind(MotionKinds.MENU_SHOW, slide(250, "fluent-entrance", ANIMATIONS, 0.2))
            .kind(MotionKinds.SCENE_FADE, fade(333, "linear"))
            .build());

    public static final DeclaredMotion KDE_CLASSIC = declare("kde_classic", MotionProfile.builder()
            .kind(MotionKinds.WINDOW_MINIMIZE, outline(250, "linear", MINIMIZE, true, 3))
            .kind(MotionKinds.WINDOW_RESTORE, outline(250, "linear", MINIMIZE, false, 3))
            .kind(MotionKinds.MENU_SHOW, slide(150, "linear", GUI_EFFECTS + " " + MENUS, 1.0).with(STEADY, 1))
            .build());

    public static final DeclaredMotion PLASMA = declare("plasma", MotionProfile.builder()
            .kind(MotionKinds.WINDOW_OPEN, scale(200, "ease-out-cubic", SCALE, 0.8, 1.0))
            .kind(MotionKinds.DIALOG_OPEN, scale(200, "ease-out-cubic", SCALE, 0.8, 1.0))
            .kind(MotionKinds.WINDOW_CLOSE, scale(200, "ease-in-cubic", SCALE, 1.0, 0.8))
            .kind(MotionKinds.WINDOW_MINIMIZE, zoomOut(250, "ease-in-cubic", SQUASH))
            .kind(MotionKinds.WINDOW_RESTORE, zoomIn(250, "ease-out-cubic", SQUASH))
            .kind(MotionKinds.MENU_SHOW, slide(150, "ease-out-cubic", SLIDING_POPUPS, 1.0))
            .kind(MotionKinds.SCENE_FADE, fade(400, "ease-out"))
            .build());

    public static final DeclaredMotion GNOME_CLASSIC = declare("gnome_classic", MotionProfile.builder()
            // Sawfish drew the outline in sixteen steps of twenty milliseconds.
            .kind(MotionKinds.WINDOW_MINIMIZE, outline(320, "steps(16)", WIREFRAME, true, 0))
            .kind(MotionKinds.WINDOW_RESTORE, outline(320, "steps(16)", WIREFRAME, false, 0))
            .build());

    public static final DeclaredMotion GNOME = declare("gnome", MotionProfile.builder()
            // A window comes up out of a point at the middle of its foot, as the shell's window manager maps one.
            .kind(MotionKinds.WINDOW_OPEN, scale(150, "ease-out-expo", ANIMATIONS, 0.01, 1.0)
                    .with("from_y", 0.05).with("pivot_y", 1.0))
            // An attached dialog unfolds downwards from its middle.
            .kind(MotionKinds.DIALOG_OPEN, scale(100, "ease-out-quad", ANIMATIONS, 1.0, 1.0).with("from_y", 0.0))
            .kind(MotionKinds.WINDOW_CLOSE, scale(150, "ease-out-quad", ANIMATIONS, 1.0, 0.8))
            .kind(MotionKinds.WINDOW_MINIMIZE, zoomOut(400, "ease-out-expo", ANIMATIONS))
            .kind(MotionKinds.WINDOW_RESTORE, zoomIn(400, "ease-out-expo", ANIMATIONS))
            .kind(MotionKinds.MENU_SHOW, scale(250, "ease-out-quad", ANIMATIONS, 0.97, 1.0))
            .kind(MotionKinds.SCENE_FADE, fade(250, "ease-out-quad"))
            .build());

    public static final DeclaredMotion CINNAMON = declare("cinnamon", MotionProfile.builder()
            .kind(MotionKinds.WINDOW_OPEN, scale(120, "ease-out-quad", EFFECTS + " " + MAP, 0.5, 1.0))
            .kind(MotionKinds.DIALOG_OPEN, scale(120, "ease-out-quad", EFFECTS + " " + DIALOGS, 0.5, 1.0))
            .kind(MotionKinds.WINDOW_CLOSE, scale(120, "ease-out-quad", EFFECTS + " " + CLOSE, 1.0, 0.5))
            .kind(MotionKinds.WINDOW_MINIMIZE, zoomOut(120, "ease-in-quad", EFFECTS + " " + MINIMIZE))
            .kind(MotionKinds.WINDOW_RESTORE, zoomIn(120, "ease-out-quad", EFFECTS + " " + MINIMIZE))
            .kind(MotionKinds.MENU_SHOW, slide(150, "ease-out-quad", MENUS, 0.1))
            .kind(MotionKinds.SCENE_FADE, fade(400, "ease-out-quad"))
            .build());

    public static final DeclaredMotion CDE = declare("cde", MotionProfile.STILL);

    private OsMotions() {
    }

    /** Loads the profiles, so they are declared before the data generation writes them and the packs replace them. */
    public static void declare() {
        // Reading the class is what declares them.
    }

    /** The profile a desktop of that panel moves by; a period Unix desktop moves as its own generation did. */
    public static DeclaredMotion of(final PanelStyle style, final boolean period) {
        return switch (style) {
            case FRAMES_95 -> FRAMES_95;
            case FRAMES_XP -> FRAMES_XP;
            case FRAMES_11 -> FRAMES_11;
            case KDE -> period ? KDE_CLASSIC : PLASMA;
            case GNOME -> period ? GNOME_CLASSIC : GNOME;
            case CINNAMON -> CINNAMON;
            case CDE -> CDE;
        };
    }

    private static DeclaredMotion declare(final String system, final MotionProfile profile) {
        return MotionProfiles.declare(JsComputers.MODID, system, profile);
    }

    private static MotionSpec scale(final int ms, final String easing, final String group, final double from,
                                    final double to) {
        return MotionSpec.of(MotionStyles.SCALE, ms, IEasing.named(easing), group).with("from", from).with("to", to);
    }

    private static MotionSpec slide(final int ms, final String easing, final String group, final double distance) {
        return MotionSpec.of(MotionStyles.SLIDE, ms, IEasing.named(easing), group).with("distance", distance);
    }

    private static MotionSpec zoomOut(final int ms, final String easing, final String group) {
        return MotionSpec.of(MotionStyles.ZOOM, ms, IEasing.named(easing), group).with("out", 1);
    }

    private static MotionSpec zoomIn(final int ms, final String easing, final String group) {
        return MotionSpec.of(MotionStyles.ZOOM, ms, IEasing.named(easing), group);
    }

    /** The boot picture's colour giving way to the desktop; no box switches it off, the speed alone does. */
    private static MotionSpec fade(final int ms, final String easing) {
        return MotionSpec.of(MotionStyles.FADE, ms, IEasing.named(easing), "");
    }

    private static MotionSpec outline(final int ms, final String easing, final String group, final boolean out,
                                      final int trail) {
        final MotionSpec spec = MotionSpec.of(MotionStyles.OUTLINE, ms, IEasing.named(easing), group)
                .with("trail", trail);
        return out ? spec.with("out", 1) : spec;
    }
}
