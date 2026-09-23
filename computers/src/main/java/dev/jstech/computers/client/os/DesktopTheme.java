/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.CdeScheme;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

/**
 * The chrome of a desktop OS around its programs: the panel, the launcher and the captions of the desktop icons.
 * Each desktop names its theme in its {@link DesktopLook}, so the shared {@link DesktopScreen} engine renders a
 * distinct look per desktop (Frames 95 grey, Frames XP blue Luna, Frames 11 light).
 *
 * <p>The colours are a declared palette, {@code jsc:desktop/<desktop>}, which a resource pack can recolour, and are
 * asked for each time the chrome is painted. Whether the desktop's captions cast a shadow is its design.
 */
@PaletteHolder
public final class DesktopTheme {

    private final Supplier<DesktopColours> colours;
    private final boolean textShadow;

    static final DesktopTheme WIN95 = new DesktopTheme(Palettes.declare(JsComputers.MODID, "desktop/frames_95",
            new DesktopColours(
                    0xFFC0C0C0, 0xFFFFFFFF, 0xFFC0C0C0, 0xFF000000, 0xFFC0C0C0,
                    0xFFFFFFFF, 0xFFC0C0C0, 0xFF000000, 0xFF000080)), true);

    static final DesktopTheme XP = new DesktopTheme(Palettes.declare(JsComputers.MODID, "desktop/frames_xp",
            new DesktopColours(
                    0xFF295FBE, 0xFF6E9BE0, 0xFF3FA13F, 0xFFFFFFFF, 0xFF4F7FCB,
                    0xFFFFFFFF, 0xFFECECF6, 0xFF101030, 0xFF295FBE)), true);

    static final DesktopTheme WIN11 = new DesktopTheme(Palettes.declare(JsComputers.MODID, "desktop/frames_11",
            new DesktopColours(
                    0xFFF1F2F6, 0xFFD8DAE2, 0xFFF1F2F6, 0xFF202434, 0xFFE3E5EE,
                    0xFFFFFFFF, 0xFFFAFAFE, 0xFF202434, 0xFF2A3656)), false);

    // KDE Plasma (Breeze): dark panel, sky-blue accents.
    static final DesktopTheme KDE = new DesktopTheme(Palettes.declare(JsComputers.MODID, "desktop/kde_plasma",
            new DesktopColours(
                    0xFF1B1E24, 0xFF2A2F38, 0xFF3DAEE9, 0xFFEFF0F1, 0xFF2A2F38,
                    0xFFFFFFFF, 0xFF31363B, 0xFFEFF0F1, 0xFF3DAEE9)), false);

    // GNOME (Adwaita): black top bar, GNOME blue accents.
    static final DesktopTheme GNOME = new DesktopTheme(Palettes.declare(JsComputers.MODID, "desktop/gnome",
            new DesktopColours(
                    0xFF0F0F12, 0xFF1F1F24, 0xFF0F0F12, 0xFFFFFFFF, 0xFF2A2A30,
                    0xFFFFFFFF, 0xFFF6F5F4, 0xFF2E3436, 0xFF3584E4)), false);

    // Cinnamon (Mint-Y): dark grey panel, Mint green accents.
    static final DesktopTheme CINNAMON = new DesktopTheme(Palettes.declare(JsComputers.MODID, "desktop/cinnamon",
            new DesktopColours(
                    0xFF2B2B2B, 0xFF3A3A3A, 0xFF69B03B, 0xFFE3E3E3, 0xFF3A3A3A,
                    0xFFFFFFFF, 0xFF2F2F2F, 0xFFE8E8E8, 0xFF69B03B)), false);

    // CDE in the scheme it ships with.
    static final DesktopTheme CDE_DEFAULT = cde(CdeScheme.DEFAULT);

    /**
     * A theme in the colours {@code colours} gives, which it asks for each time it paints: a declared palette, or
     * colours worked out from one with {@link Palettes#derive}.
     */
    public DesktopTheme(final Supplier<DesktopColours> colours, final boolean textShadow) {
        this.colours = colours;
        this.textShadow = textShadow;
    }

    /** The theme of the desktop under that id, as its look names it. */
    public static DesktopTheme forDesktop(final ResourceLocation desktopId) {
        return DesktopLook.of(desktopId).theme();
    }

    /**
     * CDE in that scheme. It has no taskbar and no Start button, so the colours that name those are the Front
     * Panel's grey and its active colour, which is what draws in their place.
     */
    public static DesktopTheme cde(final CdeScheme scheme) {
        return new DesktopTheme(Palettes.derive(() -> fromCde(scheme.colours())), false);
    }

    public int taskbar() {
        return this.colours.get().taskbar();
    }

    public int taskbarEdge() {
        return this.colours.get().taskbarEdge();
    }

    public int startButton() {
        return this.colours.get().startButton();
    }

    public int startText() {
        return this.colours.get().startText();
    }

    public int taskButton() {
        return this.colours.get().taskButton();
    }

    public int iconText() {
        return this.colours.get().iconText();
    }

    public int menuBg() {
        return this.colours.get().menuBg();
    }

    public int menuText() {
        return this.colours.get().menuText();
    }

    public int titleActive() {
        return this.colours.get().titleActive();
    }

    /** Whether the desktop's captions cast a shadow. */
    public boolean textShadow() {
        return this.textShadow;
    }

    private static DesktopColours fromCde(final CdePalette p) {
        return new DesktopColours(p.window(), p.light(), p.active(), p.ink(), p.window(),
                0xFFFFFFFF, p.window(), p.ink(), p.active());
    }
}
