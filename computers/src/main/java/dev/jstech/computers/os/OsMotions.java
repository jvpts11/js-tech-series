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
import org.jetbrains.annotations.Nullable;

/**
 * How each desktop of today moves, with the timings of the real system it imitates, each kept in
 * {@code assets/jsc/motions/<system>.json} for a resource pack to slow down, speed up or keep still.
 *
 * <p>The names in each motion's group are the boxes of that system's own settings page that switch it off, so the
 * page and the motion agree on what a box does: Frames XP's "Animate windows when minimizing and maximizing" is
 * {@code minimize}, Cinnamon's "Window effects" is {@code effects}. CDE moves no window; only its busy light and its
 * terminal's cursor blink.
 *
 * <p>Besides the windows and the menus, each says how its waits look (the bar for a wait with no known end, the
 * throbber in a file manager's corner), how its progress bars fill and how its terminal's cursor blinks; and the text
 * consoles each have a profile of their own, since a console blinks to its hardware's beat and not its desktop's.
 */
public final class OsMotions {

    /**
     * Every effect of Frames 11 answers to its one box, "Animation effects", and Frames 10's to "Play animations";
     * on Frames 7 it is Ease of Access's "Turn off all unnecessary animations", which stops them all.
     */
    public static final String ANIMATIONS = "animations";
    /** Frames 7's "Enable transparent glass": off, the glass frames turn opaque, as Frames 7 Basic drew them. */
    public static final String GLASS = "glass";
    /** Frames 10's "Transparency effects": off, its taskbar, Start menu and Action Center are drawn opaque. */
    public static final String TRANSPARENCY = "transparency";
    /** The box over minimizing and restoring a window on the systems that have one. */
    public static final String MINIMIZE = "minimize";
    /** The box over menus and the launcher sliding in. */
    public static final String MENUS = "menus";
    /** Frames XP's "Fade or slide ToolTips into view". */
    public static final String TOOLTIPS = "tooltips";
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

    /** How solid a scaling or sliding thing starts, and how solid it ends. */
    private static final String OPACITY_FROM = "opacity_from";
    private static final String OPACITY_TO = "opacity_to";
    /* A VGA card turns its text cursor over every sixteen frames: about 229 ms at its seventy hertz. */
    private static final int VGA_CURSOR_MS = 229;
    /* The caret blink time Frames has always shipped with, in the console and in every text box alike. */
    private static final int WINDOWS_CARET_MS = 530;

    public static final DeclaredMotion FRAMES_95 = declare("frames_95", MotionProfile.builder()
            // The caption flies to the taskbar and back, as DrawAnimatedRects drew it; everything else is instant.
            .kind(MotionKinds.WINDOW_MINIMIZE, caption(250, "linear", MINIMIZE, true))
            .kind(MotionKinds.WINDOW_RESTORE, caption(250, "linear", MINIMIZE, false))
            .kind(MotionKinds.BUSY, loop(1080, 12))
            .kind(MotionKinds.CARET_BLINK, blink(WINDOWS_CARET_MS))
            // The flying paper of the copy window, sixteen pictures at fifteen a second.
            .kind(MotionKinds.COPY, loop(1056, 16))
            .build());

    public static final DeclaredMotion FRAMES_XP = declare("frames_xp", MotionProfile.builder()
            .kind(MotionKinds.WINDOW_MINIMIZE, caption(250, "linear", MINIMIZE, true))
            .kind(MotionKinds.WINDOW_RESTORE, caption(250, "linear", MINIMIZE, false))
            // Fade, the setting's own default over slide.
            .kind(MotionKinds.MENU_SHOW, appear(200, "linear", MENUS, false))
            .kind(MotionKinds.TOOLTIP_SHOW, appear(200, "linear", TOOLTIPS, false))
            .kind(MotionKinds.NOTICE_SHOW, appear(300, "linear", "", false))
            .kind(MotionKinds.NOTICE_HIDE, appear(300, "linear", "", true))
            // The screen behind Turn Off drains to grey over a second and a half.
            .kind(MotionKinds.DIM, grey(1500, "linear"))
            .kind(MotionKinds.PROGRESS_WAIT, waiting(MotionStyles.BLOCKS, 2000, "linear").with("blocks", 3))
            .kind(MotionKinds.BUSY, loop(960, 12))
            .kind(MotionKinds.CARET_BLINK, blink(WINDOWS_CARET_MS))
            // The hourglass: twelve steps of falling sand and four of turning over, a tenth of a second each.
            .kind(MotionKinds.POINTER_BUSY, loop(1600, 16))
            .kind(MotionKinds.POINTER_WORKING, loop(1600, 16))
            .kind(MotionKinds.COPY, loop(1056, 16))
            .build());

    /*
     * Frames 7: the window grows out of a smaller one as it fades in and shrinks away as it fades out, minimizing
     * flies to its button, menus and tooltips fade; each answers to its own box and to Ease of Access's switch over
     * all of them. The ring of the busy pointer, eighteen pictures of fifty milliseconds, came with it.
     */
    public static final DeclaredMotion FRAMES_7 = declare("frames_7", MotionProfile.builder()
            .kind(MotionKinds.WINDOW_OPEN, scale(250, "ease-out-cubic", MINIMIZE + " " + ANIMATIONS, 0.9, 1.0)
                    .with(OPACITY_FROM, 0.0))
            .kind(MotionKinds.DIALOG_OPEN, scale(200, "ease-out-cubic", MINIMIZE + " " + ANIMATIONS, 0.9, 1.0)
                    .with(OPACITY_FROM, 0.0))
            .kind(MotionKinds.WINDOW_CLOSE, scale(200, "ease-in-cubic", MINIMIZE + " " + ANIMATIONS, 1.0, 0.9)
                    .with(OPACITY_TO, 0.0))
            .kind(MotionKinds.WINDOW_MINIMIZE, zoomOut(250, "ease-in-cubic", MINIMIZE + " " + ANIMATIONS))
            .kind(MotionKinds.WINDOW_RESTORE, zoomIn(250, "ease-out-cubic", MINIMIZE + " " + ANIMATIONS))
            .kind(MotionKinds.MENU_SHOW, appear(200, "linear", MENUS + " " + ANIMATIONS, false))
            .kind(MotionKinds.TOOLTIP_SHOW, appear(200, "linear", TOOLTIPS + " " + ANIMATIONS, false))
            .kind(MotionKinds.NOTICE_SHOW, appear(300, "linear", ANIMATIONS, false))
            .kind(MotionKinds.NOTICE_HIDE, appear(300, "linear", ANIMATIONS, true))
            .kind(MotionKinds.SCENE_FADE, fade(400, "linear"))
            // The band of light sweeping a bar that has no known end.
            .kind(MotionKinds.PROGRESS_WAIT, waiting(MotionStyles.SEGMENT, 2000, "linear").with("length", 0.3))
            .kind(MotionKinds.PROGRESS_FILL, ease(250, "ease-out-cubic", ANIMATIONS))
            .kind(MotionKinds.CARET_BLINK, blink(WINDOWS_CARET_MS))
            .kind(MotionKinds.POINTER_BUSY, loop(900, 18))
            .kind(MotionKinds.POINTER_WORKING, loop(900, 18))
            .build());

    /*
     * Frames 10: windows scale and fade, the Start menu slides up out of the taskbar as it fades in, tooltips come
     * at once; all of it answers to "Play animations", which also stops the live tiles turning.
     */
    public static final DeclaredMotion FRAMES_10 = declare("frames_10", MotionProfile.builder()
            .kind(MotionKinds.WINDOW_OPEN, scale(200, "ease-out-cubic", ANIMATIONS, 0.92, 1.0)
                    .with(OPACITY_FROM, 0.0))
            .kind(MotionKinds.DIALOG_OPEN, scale(167, "ease-out-cubic", ANIMATIONS, 0.95, 1.0)
                    .with(OPACITY_FROM, 0.0))
            .kind(MotionKinds.WINDOW_CLOSE, scale(167, "ease-in-cubic", ANIMATIONS, 1.0, 0.92)
                    .with(OPACITY_TO, 0.0))
            .kind(MotionKinds.WINDOW_MINIMIZE, zoomOut(250, "ease-in-cubic", ANIMATIONS))
            .kind(MotionKinds.WINDOW_RESTORE, zoomIn(250, "ease-out-cubic", ANIMATIONS))
            .kind(MotionKinds.MENU_SHOW, slide(250, "ease-out-cubic", ANIMATIONS, 0.2).with(OPACITY_FROM, 0.0))
            .kind(MotionKinds.TOOLTIP_SHOW, appear(100, "linear", ANIMATIONS, false))
            .kind(MotionKinds.NOTICE_SHOW, appear(300, "ease-out-cubic", ANIMATIONS, false))
            .kind(MotionKinds.NOTICE_HIDE, appear(300, "ease-in-cubic", ANIMATIONS, true))
            .kind(MotionKinds.SCENE_FADE, fade(333, "linear"))
            // The dots that run along a bar with no known end.
            .kind(MotionKinds.PROGRESS_WAIT, waiting(MotionStyles.SEGMENT, 2000, "linear").with("length", 0.2))
            .kind(MotionKinds.PROGRESS_FILL, ease(250, "ease-out-cubic", ANIMATIONS))
            .kind(MotionKinds.CARET_BLINK, blink(WINDOWS_CARET_MS))
            .kind(MotionKinds.POINTER_BUSY, loop(900, 18))
            .kind(MotionKinds.POINTER_WORKING, loop(900, 18))
            .build());

    public static final DeclaredMotion FRAMES_11 = declare("frames_11", MotionProfile.builder()
            .kind(MotionKinds.WINDOW_OPEN, scale(250, "fluent-entrance", ANIMATIONS, 0.92, 1.0)
                    .with(OPACITY_FROM, 0.0))
            .kind(MotionKinds.DIALOG_OPEN, scale(167, "fluent-entrance", ANIMATIONS, 0.95, 1.0)
                    .with(OPACITY_FROM, 0.0))
            .kind(MotionKinds.WINDOW_CLOSE, scale(167, "fluent-exit", ANIMATIONS, 1.0, 0.92).with(OPACITY_TO, 0.0))
            .kind(MotionKinds.WINDOW_MINIMIZE, zoomOut(250, "fluent-point", ANIMATIONS))
            .kind(MotionKinds.WINDOW_RESTORE, zoomIn(250, "fluent-point", ANIMATIONS))
            .kind(MotionKinds.MENU_SHOW, slide(250, "fluent-entrance", ANIMATIONS, 0.2).with(OPACITY_FROM, 0.0))
            .kind(MotionKinds.TOOLTIP_SHOW, appear(83, "linear", ANIMATIONS, false))
            .kind(MotionKinds.SCENE_FADE, fade(333, "linear"))
            .kind(MotionKinds.PROGRESS_WAIT, waiting(MotionStyles.GROW, 2000, "linear"))
            .kind(MotionKinds.PROGRESS_FILL, ease(250, "fluent-point", ANIMATIONS))
            .kind(MotionKinds.CARET_BLINK, blink(WINDOWS_CARET_MS))
            // The ring whose light runs round, eighteen pictures of fifty milliseconds.
            .kind(MotionKinds.POINTER_BUSY, loop(900, 18))
            .kind(MotionKinds.POINTER_WORKING, loop(900, 18))
            .build());

    public static final DeclaredMotion KDE_CLASSIC = declare("kde_classic", MotionProfile.builder()
            .kind(MotionKinds.WINDOW_MINIMIZE, outline(250, "linear", MINIMIZE, true, 3))
            .kind(MotionKinds.WINDOW_RESTORE, outline(250, "linear", MINIMIZE, false, 3))
            .kind(MotionKinds.MENU_SHOW, slide(150, "linear", GUI_EFFECTS + " " + MENUS, 1.0).with(STEADY, 1))
            .kind(MotionKinds.PROGRESS_WAIT, waiting(MotionStyles.BOUNCE, 1000, "linear").with("length", 0.25))
            .kind(MotionKinds.BUSY, loop(720, 12))
            // The launch feedback: the program's icon bouncing beside the arrow while it starts.
            .kind(MotionKinds.POINTER_LAUNCH, loop(600, 12))
            .build());

    public static final DeclaredMotion PLASMA = declare("plasma", MotionProfile.builder()
            .kind(MotionKinds.WINDOW_OPEN, scale(200, "ease-out-cubic", SCALE, 0.8, 1.0).with(OPACITY_FROM, 0.0))
            .kind(MotionKinds.DIALOG_OPEN, scale(200, "ease-out-cubic", SCALE, 0.8, 1.0).with(OPACITY_FROM, 0.0))
            .kind(MotionKinds.WINDOW_CLOSE, scale(200, "ease-in-cubic", SCALE, 1.0, 0.8).with(OPACITY_TO, 0.0))
            .kind(MotionKinds.WINDOW_MINIMIZE, zoomOut(250, "ease-in-cubic", SQUASH))
            .kind(MotionKinds.WINDOW_RESTORE, zoomIn(250, "ease-out-cubic", SQUASH))
            .kind(MotionKinds.MENU_SHOW, slide(150, "ease-out-cubic", SLIDING_POPUPS, 1.0).with(OPACITY_FROM, 0.0))
            .kind(MotionKinds.SCENE_FADE, fade(400, "ease-out"))
            .kind(MotionKinds.PROGRESS_WAIT, waiting(MotionStyles.SEGMENT, 2000, "linear").with("length", 0.3))
            .kind(MotionKinds.PROGRESS_FILL, ease(200, "ease-out-cubic", ""))
            .kind(MotionKinds.POINTER_BUSY, loop(990, 18))
            .kind(MotionKinds.POINTER_WORKING, loop(990, 18))
            .kind(MotionKinds.POINTER_LAUNCH, loop(600, 12))
            .build());

    public static final DeclaredMotion GNOME_CLASSIC = declare("gnome_classic", MotionProfile.builder()
            // Sawfish drew the outline in sixteen steps of twenty milliseconds.
            .kind(MotionKinds.WINDOW_MINIMIZE, outline(320, "steps(16)", WIREFRAME, true, 0))
            .kind(MotionKinds.WINDOW_RESTORE, outline(320, "steps(16)", WIREFRAME, false, 0))
            .kind(MotionKinds.PROGRESS_WAIT, waiting(MotionStyles.BOUNCE, 1000, "linear").with("length", 0.25))
            .kind(MotionKinds.BUSY, loop(800, 8))
            .build());

    public static final DeclaredMotion GNOME = declare("gnome", MotionProfile.builder()
            // A window comes up out of a point at the middle of its foot, as the shell's window manager maps one.
            .kind(MotionKinds.WINDOW_OPEN, scale(150, "ease-out-expo", ANIMATIONS, 0.01, 1.0)
                    .with("from_y", 0.05).with("pivot_y", 1.0).with(OPACITY_FROM, 0.0))
            // An attached dialog unfolds downwards from its middle.
            .kind(MotionKinds.DIALOG_OPEN, scale(100, "ease-out-quad", ANIMATIONS, 1.0, 1.0).with("from_y", 0.0))
            .kind(MotionKinds.WINDOW_CLOSE, scale(150, "ease-out-quad", ANIMATIONS, 1.0, 0.8)
                    .with(OPACITY_TO, 0.0))
            .kind(MotionKinds.WINDOW_MINIMIZE, zoomOut(400, "ease-out-expo", ANIMATIONS))
            .kind(MotionKinds.WINDOW_RESTORE, zoomIn(400, "ease-out-expo", ANIMATIONS))
            .kind(MotionKinds.MENU_SHOW, scale(250, "ease-out-quad", ANIMATIONS, 0.97, 1.0).with(OPACITY_FROM, 0.0))
            .kind(MotionKinds.SCENE_FADE, fade(250, "ease-out-quad"))
            .kind(MotionKinds.PROGRESS_WAIT, waiting(MotionStyles.BOUNCE, 1000, "ease-in-out").with("length", 0.2))
            .kind(MotionKinds.PROGRESS_FILL, ease(250, "ease-out-quad", ANIMATIONS))
            .kind(MotionKinds.POINTER_BUSY, loop(960, 16))
            .kind(MotionKinds.POINTER_WORKING, loop(960, 16))
            .build());

    public static final DeclaredMotion CINNAMON = declare("cinnamon", MotionProfile.builder()
            .kind(MotionKinds.WINDOW_OPEN, scale(120, "ease-out-quad", EFFECTS + " " + MAP, 0.5, 1.0)
                    .with(OPACITY_FROM, 0.0))
            .kind(MotionKinds.DIALOG_OPEN, scale(120, "ease-out-quad", EFFECTS + " " + DIALOGS, 0.5, 1.0)
                    .with(OPACITY_FROM, 0.0))
            .kind(MotionKinds.WINDOW_CLOSE, scale(120, "ease-out-quad", EFFECTS + " " + CLOSE, 1.0, 0.5)
                    .with(OPACITY_TO, 0.0))
            .kind(MotionKinds.WINDOW_MINIMIZE, zoomOut(120, "ease-in-quad", EFFECTS + " " + MINIMIZE))
            .kind(MotionKinds.WINDOW_RESTORE, zoomIn(120, "ease-out-quad", EFFECTS + " " + MINIMIZE))
            .kind(MotionKinds.MENU_SHOW, slide(150, "ease-out-quad", MENUS, 0.1).with(OPACITY_FROM, 0.0))
            .kind(MotionKinds.SCENE_FADE, fade(400, "ease-out-quad"))
            .kind(MotionKinds.PROGRESS_WAIT, waiting(MotionStyles.SEGMENT, 2000, "linear").with("length", 0.27))
            .kind(MotionKinds.PROGRESS_FILL, ease(120, "ease-out-quad", EFFECTS))
            .kind(MotionKinds.POINTER_BUSY, loop(800, 8))
            .kind(MotionKinds.POINTER_WORKING, loop(800, 8))
            .build());

    /*
     * CDE moved no window. Its front panel's busy light blinks from the click until the window maps, and dtterm's
     * cursor blinks every quarter of a second.
     */
    public static final DeclaredMotion CDE = declare("cde", MotionProfile.builder()
            .kind(MotionKinds.BUSY, blink(250))
            .kind(MotionKinds.CARET_BLINK, blink(250))
            .build());

    /** A text console on a VGA card, MC-DOS's and UNIX's: the hardware cursor turns over every sixteen frames. */
    public static final DeclaredMotion CONSOLE_VGA = declare("console_vga", MotionProfile.builder()
            .kind(MotionKinds.CARET_BLINK, blink(VGA_CURSOR_MS))
            .build());

    /** Linux's framebuffer console, whose cursor timer turns over every fifth of a second. */
    public static final DeclaredMotion CONSOLE_FBCON = declare("console_fbcon", MotionProfile.builder()
            .kind(MotionKinds.CARET_BLINK, blink(200))
            .build());

    /** FreeBSD's syscons, which shows the VGA card's own cursor in text mode. */
    public static final DeclaredMotion CONSOLE_SYSCONS = declare("console_syscons", MotionProfile.builder()
            .kind(MotionKinds.CARET_BLINK, blink(VGA_CURSOR_MS))
            .build());

    /** The Frames console, at the caret's blink time Frames has always set. */
    public static final DeclaredMotion CONSOLE_FRAMES = declare("console_frames", MotionProfile.builder()
            .kind(MotionKinds.CARET_BLINK, blink(WINDOWS_CARET_MS))
            .build());

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
            case FRAMES_7 -> FRAMES_7;
            case FRAMES_10 -> FRAMES_10;
            case FRAMES_11 -> FRAMES_11;
            case KDE -> period ? KDE_CLASSIC : PLASMA;
            case GNOME -> period ? GNOME_CLASSIC : GNOME;
            case CINNAMON -> CINNAMON;
            case CDE -> CDE;
        };
    }

    /**
     * The profile the text console of a system of that family moves by, which is only its cursor's blink: the VGA
     * card's own for MC-DOS, MC-NET and UNIX, the framebuffer console's for Linux, syscons for FreeBSD and the Frames
     * console for Frames. A console with no system yet (an installer's) blinks as a VGA card does.
     */
    public static DeclaredMotion console(@Nullable final Platform platform) {
        if (platform == null) {
            return CONSOLE_VGA;
        }
        return switch (platform) {
            case MC_DOS, MC_NET, UNIX -> CONSOLE_VGA;
            case LINUX -> CONSOLE_FBCON;
            case FREEBSD -> CONSOLE_SYSCONS;
            case FRAMES -> CONSOLE_FRAMES;
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

    private static MotionSpec caption(final int ms, final String easing, final String group, final boolean out) {
        final MotionSpec spec = MotionSpec.of(MotionStyles.CAPTION, ms, IEasing.named(easing), group);
        return out ? spec.with("out", 1) : spec;
    }

    private static MotionSpec appear(final int ms, final String easing, final String group, final boolean out) {
        final MotionSpec spec = MotionSpec.of(MotionStyles.APPEAR, ms, IEasing.named(easing), group);
        return out ? spec.with("out", 1) : spec;
    }

    /* On and off at that beat; no speed setting changes how fast a cursor or a light blinks. */
    private static MotionSpec blink(final int ms) {
        return MotionSpec.of(MotionStyles.BLINK, ms, IEasing.LINEAR, "").with(STEADY, 1);
    }

    /* A turning picture of that many frames, a pass in that time. */
    private static MotionSpec loop(final int ms, final int frames) {
        return MotionSpec.of(MotionStyles.LOOP, ms, IEasing.LINEAR, "").with("frames", frames).with(STEADY, 1);
    }

    /* A bar for a wait with no known end, a pass in that time; its pace is its own, whatever the speed setting. */
    private static MotionSpec waiting(final String style, final int ms, final String easing) {
        return MotionSpec.of(style, ms, IEasing.named(easing), "").with(STEADY, 1);
    }

    private static MotionSpec ease(final int ms, final String easing, final String group) {
        return MotionSpec.of(MotionStyles.EASE, ms, IEasing.named(easing), group);
    }

    private static MotionSpec grey(final int ms, final String easing) {
        return MotionSpec.of(MotionStyles.GREY, ms, IEasing.named(easing), "").with("amount", 1);
    }

    private static MotionSpec outline(final int ms, final String easing, final String group, final boolean out,
                                      final int trail) {
        final MotionSpec spec = MotionSpec.of(MotionStyles.OUTLINE, ms, IEasing.named(easing), group)
                .with("trail", trail);
        return out ? spec.with("out", 1) : spec;
    }
}
