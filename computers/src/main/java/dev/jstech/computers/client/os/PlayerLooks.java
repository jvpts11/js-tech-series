/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.player.PlayerDesktop;
import dev.jstech.computers.sigma.DesktopFacts;
import dev.jstech.core.effect.EffectCurves;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * How a desktop a player wrote looks (computers/docs/design/player-systems.md): the six colours it declares, and the
 * few shades worked out of them for what it never names (its menu, the row lit in it, the quiet button of a window),
 * made into the look the mod's desktops are drawn with, so its windows move and take the focus as theirs do.
 *
 * <p>Its frames are the style it names; the band of a window's title and the rim around it are in its own colours,
 * and its flat frames are plain squares for buttons, as it was drawn when it was approved.
 */
@PaletteHolder
final class PlayerLooks {

    /* The one colour a player's desktop does not declare: what closes, and what shuts the machine down. */
    private static final Palette<Fixed> FIXED = Palettes.declare(JsComputers.MODID, "desktop/player",
            new Fixed(0xFFF08A3C));

    /* How much lighter than its panel the menu is, and the row lit in it. */
    private static final double MENU_SHADE = 1.6;
    private static final double LIT_SHADE = 2.5;
    /* A panel darker than this in every channel has no colour to lighten, so its shades lean to its text instead. */
    private static final int TOO_DARK = 24;
    /* How far a window's quiet button leans from the lit row to the text, and its dimmed words to its ground. */
    private static final double QUIET = 0.3;
    private static final double DIM = 0.45;
    private static final double HOVER = 0.25;
    /* A colour brighter than this reads as light, and is written on in the panel's colour. */
    private static final int LIGHT = 140;
    private static final int OPAQUE = 0xFF << 24;

    /* Each desktop's look as last made, and the declaration it was made from, so a rebuild is seen. */
    private static final Map<ResourceLocation, Made> MADE = new HashMap<>();

    private PlayerLooks() {
    }

    /** The look of that desktop, made again only when what it declared has changed. */
    static DesktopLook of(final PlayerDesktop desktop) {
        final Made made = MADE.get(desktop.id());
        if (made != null && made.facts().equals(desktop.facts())) {
            return made.look();
        }
        final Colours colours = colours(desktop.facts());
        final IFormChrome base = chromeOf(desktop.facts().frames(), colours.window());
        final OsSkin skin = OsSkin.player(new PlayerChrome(base, colours), () -> skinColours(colours));
        final DesktopColours theme = desktopColours(colours);
        // Its wall is its own picture, or its desktop colour until that is read: the built-in style is never hung.
        final DesktopLook look = new DesktopLook(skin, null, new DesktopTheme(() -> theme, false),
                WallpaperStyle.TEAL);
        MADE.put(desktop.id(), new Made(desktop.facts(), look));
        return look;
    }

    /** The colours of that desktop, as it declared them and as they are worked out from what it declared. */
    static Colours colours(final DesktopFacts facts) {
        final int desktop = declared(facts, "DesktopColour");
        final int panel = declared(facts, "PanelColour");
        final int text = declared(facts, "TextColour");
        final int accent = declared(facts, "AccentColour");
        final int window = declared(facts, "WindowColour");
        final int title = declared(facts, "TitleColour");
        final int lit = shade(panel, text, LIT_SHADE);
        final int ink = light(window) ? panel : text;
        return new Colours(desktop, panel, text, accent, window, title, shade(panel, text, MENU_SHADE), lit,
                EffectCurves.blend(lit, text, QUIET), FIXED.get().close(), ink);
    }

    /*
     * The style of frame it names, the mod's own: flat is the light or dark one by its windows' ground, and a name the
     * build never lets through falls back to flat.
     */
    private static IFormChrome chromeOf(final String frames, final int window) {
        return switch (frames.toLowerCase(Locale.ROOT)) {
            case "bevel" -> BevelChrome.INSTANCE;
            case "luna" -> LunaChrome.INSTANCE;
            case "aero" -> AeroChrome.INSTANCE;
            case "metro" -> light(window) ? MetroChrome.LIGHT : MetroChrome.DARK;
            case "kde2" -> Kde2Chrome.INSTANCE;
            case "gnome1" -> Gnome1Chrome.INSTANCE;
            case "oxygen" -> OxygenChrome.INSTANCE;
            case "clearlooks" -> ClearlooksChrome.INSTANCE;
            case "platinum" -> QuinceChrome.PLATINUM;
            default -> light(window) ? FlatChrome.LIGHT : FlatChrome.DARK;
        };
    }

    /* What the windows' insides are drawn in: the desktop's window ground written on in its panel's colour or text. */
    private static SkinColours skinColours(final Colours c) {
        final int onAccent = light(c.accent()) ? c.panel() : c.text();
        return new SkinColours(c.title(), c.window(), c.accent(), c.accent(), c.ink(),
                EffectCurves.blend(c.ink(), c.window(), DIM), c.window(), c.accent(), onAccent,
                EffectCurves.blend(c.window(), c.accent(), HOVER));
    }

    /* What the panel and the menu are drawn in, as the mod's desktops name theirs. */
    private static DesktopColours desktopColours(final Colours c) {
        return new DesktopColours(c.panel(), c.accent(), c.accent(), c.text(), c.menu(), c.text(), c.menu(), c.text(),
                c.lit());
    }

    /* A colour the desktop declared, opaque; the build lets none through that is not one. */
    private static int declared(final DesktopFacts facts, final String role) {
        final String written = facts.colours().getOrDefault(role, "#000000");
        return OPAQUE | Integer.parseInt(written.substring(1), 16);
    }

    /* A lighter shade of the panel's own colour, or of its text on a panel too dark to have a colour. */
    private static int shade(final int panel, final int text, final double factor) {
        final int r = panel >> 16 & 0xFF;
        final int g = panel >> 8 & 0xFF;
        final int b = panel & 0xFF;
        if (Math.max(r, Math.max(g, b)) < TOO_DARK) {
            return EffectCurves.blend(panel, text, (factor - 1) / 10);
        }
        return OPAQUE | scaled(r, factor) << 16 | scaled(g, factor) << 8 | scaled(b, factor);
    }

    private static int scaled(final int channel, final double factor) {
        return (int) Math.min(0xFF, Math.round(channel * factor));
    }

    /* Whether a colour reads as light, by how bright it looks rather than by its numbers. */
    private static boolean light(final int colour) {
        final int r = colour >> 16 & 0xFF;
        final int g = colour >> 8 & 0xFF;
        final int b = colour & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000 > LIGHT;
    }

    /**
     * A player's desktop's colours.
     *
     * @param desktop the wall behind everything, until its picture is read
     * @param panel   its panel, and the band of a window's title
     * @param text    what its panel and its menu are written in
     * @param accent  its rims, its rules, the name on its panel and the marks in its menu
     * @param window  the ground of its windows
     * @param title   what a window's title is written in
     * @param menu    the ground of its menu, a shade of the panel
     * @param lit     the row lit in its menu, a deeper shade of the panel
     * @param quiet   a window's least button, between the lit row and the text
     * @param close   what closes a window and shuts the machine down
     * @param ink     what is written on its windows' ground
     */
    record Colours(int desktop, int panel, int text, int accent, int window, int title, int menu, int lit, int quiet,
                   int close, int ink) {
    }

    /**
     * The colour a player's desktop never declares.
     *
     * @param close what closes a window, and the mark beside Shut down
     */
    public record Fixed(int close) {
    }

    /* A look as made, with the declaration it was made from. */
    private record Made(DesktopFacts facts, DesktopLook look) {
    }
}
