/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui;

import dev.jstech.computers.os.OsMotions;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Each system's page for switching its own visual effects, as the Settings window shows it: the page that system
 * kept them on, under the name it gave it, with the boxes, sliders and choices it had, but only for the effects
 * that exist on these desktops. A box names the effect it switches by the name the system's motion profile gives it,
 * so what the box says and what moves always agree.
 *
 * <p>CDE has no page, and neither do the text consoles: nothing of theirs moves that could be switched off.
 */
@TextHolder
public final class EffectsPages {

    /** The Settings pages a system's effects page is reached from, by the index the window lists them at. */
    public static final int FROM_PERSONALIZE = 0;
    public static final int FROM_SYSTEM = 1;
    public static final int FROM_DISPLAY = 4;

    /** Frames XP's three presets and the custom choice, the radio buttons over its list. */
    public static final int PRESET_CHOOSE = 0;
    public static final int PRESET_APPEARANCE = 1;
    public static final int PRESET_PERFORMANCE = 2;
    public static final int PRESET_CUSTOM = 3;

    // Frames 95, Display Properties.
    static final TextKey EFFECTS = TextKey.of("jsc.settings.effects.effects", "Effects");
    static final TextKey VISUAL_EFFECTS = TextKey.of("jsc.settings.effects.visual_effects", "Visual effects");
    static final TextKey ANIMATE_MIN_RESTORE = TextKey.of("jsc.settings.effects.animate_min_restore",
            "Animate windows when minimizing and restoring");

    // Frames XP, Performance Options.
    static final TextKey PERFORMANCE_OPTIONS = TextKey.of("jsc.settings.effects.performance_options",
            "Performance Options");
    static final TextKey XP_NOTE = TextKey.of("jsc.settings.effects.xp_note",
            "Select the settings you want to use for the appearance and performance of Frames on this computer.");
    static final TextKey LET_CHOOSE = TextKey.of("jsc.settings.effects.let_choose",
            "Let Frames choose what's best for my computer");
    static final TextKey BEST_APPEARANCE = TextKey.of("jsc.settings.effects.best_appearance",
            "Adjust for best appearance");
    static final TextKey BEST_PERFORMANCE = TextKey.of("jsc.settings.effects.best_performance",
            "Adjust for best performance");
    static final TextKey CUSTOM = TextKey.of("jsc.settings.effects.custom", "Custom:");
    static final TextKey ANIMATE_MIN_MAX = TextKey.of("jsc.settings.effects.animate_min_max",
            "Animate windows when minimizing and maximizing");
    static final TextKey FADE_SLIDE_MENUS = TextKey.of("jsc.settings.effects.fade_slide_menus",
            "Fade or slide menus into view");
    static final TextKey ICON_SHADOWS = TextKey.of("jsc.settings.effects.icon_shadows",
            "Use drop shadows for icon labels on the desktop");

    // Frames 11, Personalize > Visual effects.
    static final TextKey VISUAL_EFFECTS_11 = TextKey.of("jsc.settings.effects.visual_effects_11",
            "Personalize > Visual effects");
    static final TextKey ANIMATION_EFFECTS = TextKey.of("jsc.settings.effects.animation_effects",
            "Animation effects");
    static final TextKey ANIMATION_EFFECTS_NOTE = TextKey.of("jsc.settings.effects.animation_effects_note",
            "Windows open, close and minimize with motion, flyouts slide");

    // Plasma, Workspace behavior.
    static final TextKey WORKSPACE_BEHAVIOR = TextKey.of("jsc.settings.effects.workspace_behavior",
            "Workspace behavior");
    static final TextKey ANIMATION_SPEED = TextKey.of("jsc.settings.effects.animation_speed", "Animation speed:");
    static final TextKey SLOW = TextKey.of("jsc.settings.effects.slow", "Slow");
    static final TextKey INSTANT = TextKey.of("jsc.settings.effects.instant", "Instant");
    static final TextKey DESKTOP_EFFECTS = TextKey.of("jsc.settings.effects.desktop_effects", "Desktop effects");
    static final TextKey SCALE = TextKey.of("jsc.settings.effects.scale", "Scale (window open and close)");
    static final TextKey SQUASH = TextKey.of("jsc.settings.effects.squash", "Squash (minimize)");
    static final TextKey SLIDING_POPUPS = TextKey.of("jsc.settings.effects.sliding_popups", "Sliding popups");

    // KDE 2 and 3, Window Behavior.
    static final TextKey WINDOW_BEHAVIOR = TextKey.of("jsc.settings.effects.window_behavior", "Window Behavior");
    static final TextKey ANIMATE_MIN_RESTORE_KDE = TextKey.of("jsc.settings.effects.animate_min_restore_kde",
            "Animate minimize and restore");
    static final TextKey FAST = TextKey.of("jsc.settings.effects.fast", "Fast");
    static final TextKey GUI_EFFECTS = TextKey.of("jsc.settings.effects.gui_effects", "Enable GUI effects");
    static final TextKey MENU_EFFECT = TextKey.of("jsc.settings.effects.menu_effect", "Menu effect:");
    static final TextKey ANIMATE = TextKey.of("jsc.settings.effects.animate", "Animate");
    static final TextKey DISABLE = TextKey.of("jsc.settings.effects.disable", "Disable");

    // GNOME 1, the panel's properties.
    static final TextKey PANEL = TextKey.of("jsc.settings.effects.panel", "Panel");
    static final TextKey WIREFRAME = TextKey.of("jsc.settings.effects.wireframe",
            "Wireframe when minimizing (window manager)");

    // GNOME, Personalize > Effects.
    static final TextKey EFFECTS_GNOME = TextKey.of("jsc.settings.effects.effects_gnome", "Personalize > Effects");
    static final TextKey REDUCE_ANIMATION = TextKey.of("jsc.settings.effects.reduce_animation", "Reduce Animation");
    static final TextKey REDUCE_ANIMATION_NOTE = TextKey.of("jsc.settings.effects.reduce_animation_note",
            "Windows and the overview appear at once");

    // Cinnamon, Effects.
    static final TextKey ENABLE_EFFECTS = TextKey.of("jsc.settings.effects.enable_effects", "Enable effects");
    static final TextKey WINDOW_EFFECTS = TextKey.of("jsc.settings.effects.window_effects", "Window effects");
    static final TextKey DIALOG_EFFECTS = TextKey.of("jsc.settings.effects.dialog_effects",
            "Effects on dialog boxes");
    static final TextKey MENU_EFFECTS = TextKey.of("jsc.settings.effects.menu_effects", "Effects on menus");
    static final TextKey CUSTOMIZE = TextKey.of("jsc.settings.effects.customize", "Customize");
    static final TextKey EFFECTS_SPEED = TextKey.of("jsc.settings.effects.effects_speed", "Window effects speed");
    static final TextKey NORMAL = TextKey.of("jsc.settings.effects.normal", "Normal");
    static final TextKey OPENING = TextKey.of("jsc.settings.effects.opening", "Opening windows");
    static final TextKey CLOSING = TextKey.of("jsc.settings.effects.closing", "Closing windows");
    static final TextKey MINIMIZING = TextKey.of("jsc.settings.effects.minimizing", "Minimizing windows");
    static final TextKey TRADITIONAL = TextKey.of("jsc.settings.effects.traditional", "Traditional");
    static final TextKey NONE = TextKey.of("jsc.settings.effects.none", "None");

    /*
     * The speeds the sliders and the speed choices offer, as percentages of the system's own time. Plasma's slider
     * runs from eight times as long to instant and rests on the system's own pace; KDE 3's from twice as long to a
     * fifth; Cinnamon's choice is its own three paces.
     */
    private static final List<Integer> PLASMA_SPEEDS = List.of(800, 400, 200, 100, 50, 25, 12, 0);
    private static final List<Integer> KDE_SPEEDS = List.of(200, 175, 150, 125, 100, 80, 60, 40, 20);
    private static final List<Integer> CINNAMON_SPEEDS = List.of(140, 100, 60);

    public static final Page FRAMES_95 = new Page(FROM_DISPLAY, EFFECTS, EFFECTS, Footer.OK_CANCEL_APPLY,
            List.of(new Heading(VISUAL_EFFECTS), new Check(ANIMATE_MIN_RESTORE, OsMotions.MINIMIZE)));

    public static final Page FRAMES_XP = new Page(FROM_SYSTEM, PERFORMANCE_OPTIONS, PERFORMANCE_OPTIONS,
            Footer.OK_CANCEL_APPLY, List.of(new Heading(VISUAL_EFFECTS), new Note(XP_NOTE),
            new Presets(List.of(LET_CHOOSE, BEST_APPEARANCE, BEST_PERFORMANCE, CUSTOM),
                    List.of(OsMotions.MINIMIZE, OsMotions.MENUS, OsMotions.ICON_SHADOWS)),
            new Check(ANIMATE_MIN_MAX, OsMotions.MINIMIZE), new Check(FADE_SLIDE_MENUS, OsMotions.MENUS),
            new Check(ICON_SHADOWS, OsMotions.ICON_SHADOWS)));

    public static final Page FRAMES_11 = new Page(FROM_PERSONALIZE, VISUAL_EFFECTS, VISUAL_EFFECTS_11, Footer.NONE,
            List.of(new Toggle(ANIMATION_EFFECTS, ANIMATION_EFFECTS_NOTE, OsMotions.ANIMATIONS, false)));

    public static final Page PLASMA = new Page(FROM_PERSONALIZE, WORKSPACE_BEHAVIOR, WORKSPACE_BEHAVIOR,
            Footer.NONE, List.of(new Slider(ANIMATION_SPEED, SLOW, INSTANT, PLASMA_SPEEDS),
            new Heading(DESKTOP_EFFECTS), new Check(SCALE, OsMotions.SCALE), new Check(SQUASH, OsMotions.SQUASH),
            new Check(SLIDING_POPUPS, OsMotions.SLIDING_POPUPS)));

    public static final Page KDE_CLASSIC = new Page(FROM_PERSONALIZE, WINDOW_BEHAVIOR, WINDOW_BEHAVIOR, Footer.NONE,
            List.of(new Check(ANIMATE_MIN_RESTORE_KDE, OsMotions.MINIMIZE),
                    new Slider(ANIMATION_SPEED, SLOW, FAST, KDE_SPEEDS), new Heading(EFFECTS),
                    new Check(GUI_EFFECTS, OsMotions.GUI_EFFECTS),
                    new Choice(MENU_EFFECT, OsMotions.MENUS, ANIMATE, DISABLE)));

    public static final Page GNOME_CLASSIC = new Page(FROM_PERSONALIZE, PANEL, PANEL, Footer.OK_APPLY_CLOSE,
            List.of(new Check(WIREFRAME, OsMotions.WIREFRAME)));

    public static final Page GNOME = new Page(FROM_PERSONALIZE, EFFECTS, EFFECTS_GNOME, Footer.NONE,
            List.of(new Toggle(REDUCE_ANIMATION, REDUCE_ANIMATION_NOTE, OsMotions.ANIMATIONS, true)));

    public static final Page CINNAMON = new Page(FROM_PERSONALIZE, EFFECTS, EFFECTS, Footer.NONE,
            List.of(new Heading(ENABLE_EFFECTS), new Toggle(WINDOW_EFFECTS, null, OsMotions.EFFECTS, false),
                    new Toggle(DIALOG_EFFECTS, null, OsMotions.DIALOGS, false),
                    new Toggle(MENU_EFFECTS, null, OsMotions.MENUS, false), new Heading(CUSTOMIZE),
                    new SpeedChoice(EFFECTS_SPEED, List.of(SLOW, NORMAL, FAST), CINNAMON_SPEEDS),
                    new Choice(OPENING, OsMotions.MAP, TRADITIONAL, NONE),
                    new Choice(CLOSING, OsMotions.CLOSE, TRADITIONAL, NONE),
                    new Choice(MINIMIZING, OsMotions.MINIMIZE, TRADITIONAL, NONE)));

    /** Every page, for whatever checks them all. */
    public static final List<Page> ALL = List.of(FRAMES_95, FRAMES_XP, FRAMES_11, PLASMA, KDE_CLASSIC, GNOME_CLASSIC,
            GNOME, CINNAMON);

    private EffectsPages() {
    }

    /** The effects page of a desktop of that panel, its period look or its modern one; null for CDE. */
    @Nullable
    public static Page of(final PanelStyle style, final boolean period) {
        return switch (style) {
            case FRAMES_95 -> FRAMES_95;
            case FRAMES_XP -> FRAMES_XP;
            case FRAMES_11 -> FRAMES_11;
            case KDE -> period ? KDE_CLASSIC : PLASMA;
            case GNOME -> period ? GNOME_CLASSIC : GNOME;
            case CINNAMON -> CINNAMON;
            case CDE -> null;
        };
    }

    /** The buttons along a page's foot, as its system's dialog had them, or none where changes took at once. */
    public enum Footer {
        NONE, OK_CANCEL_APPLY, OK_APPLY_CLOSE
    }

    /**
     * One system's effects page.
     *
     * @param parent the Settings page it is reached from, which stays lit in the list while it is open
     * @param entry  the button on that page that opens it, named as the system named the place
     * @param title  the page's own title
     * @param footer the buttons along its foot
     * @param rows   what it holds, top to bottom
     */
    public record Page(int parent, TextKey entry, TextKey title, Footer footer, List<IRow> rows) {

        public Page {
            rows = List.copyOf(rows);
        }
    }

    /** One thing on an effects page. */
    public sealed interface IRow permits Heading, Note, Check, Toggle, Slider, Choice, SpeedChoice, Presets {
    }

    /** A group's name over the rows under it. */
    public record Heading(TextKey text) implements IRow {
    }

    /** A sentence of explanation, wrapped to the page. */
    public record Note(TextKey text) implements IRow {
    }

    /** A box that switches one effect, lit while the effect is on. */
    public record Check(TextKey label, String effect) implements IRow {
    }

    /**
     * A switch, with a line under it where the system wrote one (null where it did not), as the newer settings pages
     * drew one; {@code inverted} for a switch that is on while the effect is off, which is what GNOME's "Reduce
     * Animation" is.
     */
    public record Toggle(TextKey label, @Nullable TextKey description, String effect, boolean inverted)
            implements IRow {
    }

    /** A slider over the effects' speed, from its slowest step to its fastest, each step a percentage. */
    public record Slider(TextKey label, TextKey low, TextKey high, List<Integer> speeds) implements IRow {

        public Slider {
            speeds = List.copyOf(speeds);
        }
    }

    /** A choice between an effect on, under its name, and off, under its own. */
    public record Choice(TextKey label, String effect, TextKey on, TextKey off) implements IRow {
    }

    /** A choice among named speeds, each a percentage. */
    public record SpeedChoice(TextKey label, List<TextKey> names, List<Integer> speeds) implements IRow {

        public SpeedChoice {
            names = List.copyOf(names);
            speeds = List.copyOf(speeds);
        }
    }

    /**
     * Frames XP's presets over its list, named in the order of {@link #PRESET_CHOOSE} to {@link #PRESET_CUSTOM}:
     * letting the system choose and the best appearance switch every effect on, the best performance switches every
     * one off, and the custom choice is what any other mix is.
     */
    public record Presets(List<TextKey> names, List<String> effects) implements IRow {

        public Presets {
            names = List.copyOf(names);
            effects = List.copyOf(effects);
        }
    }
}
