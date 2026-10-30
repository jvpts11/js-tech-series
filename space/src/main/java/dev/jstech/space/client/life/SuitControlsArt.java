/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.life;

import dev.jstech.core.client.GameLocale;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.life.SuitControls;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The face of the suit's controls, drawn live over its case as the hazard devices' faces are: the needle of the suit's
 * pressure, the figures, the time of air left, the state, the bars, the lamps and the buttons' words, in the player's
 * language. Drawn in units of {@code s}, 1 on the HUD and 2 held up, over the case's picture of that size; every place
 * below is the case's at a unit of 1.
 */
@PaletteHolder
public final class SuitControlsArt {

    /** The case at each size, and how big it is at a unit of 1, its hose stubs on top. */
    public static final ResourceLocation CASE = texture("suit_module");
    public static final ResourceLocation CASE_BIG = texture("suit_module_big");
    public static final int WIDTH = 134;
    public static final int HEIGHT = 78;
    /** The light's button, its top left and its width, and a button's height. */
    public static final int LIGHT_X = 87;
    public static final int LIGHT_Y = 67;
    public static final int BUTTON_WIDTH = 10;
    public static final int BUTTON_HEIGHT = 4;
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "suit_controls", new Colours(
            0xFFFFFFFF, 0xFF969696, 0xFFBEBEBE, 0xFF6EC83C, 0xFFF0BE3C, 0xFFE63C32, 0xFF323232, 0xFF282C28,
            0xFF6E8C6E, 0xFF141414, 0xFFF5F5EC, 0xFFC8C8C8, 0xFF3C3C40, 0x3C60D070));

    /* The face, and the pressure's dial: its centre and radius, and the most it reads, in pounds an inch. */
    private static final int FACE_X0 = 5;
    private static final int FACE_Y0 = 9;
    private static final int FACE_Y1 = 62;
    private static final int FACE_X1 = 127;
    private static final int DIAL_X = 29;
    private static final int DIAL_Y = 54;
    private static final int DIAL_RADIUS = 20;
    private static final double MOST_PSI = 5;
    /* Where the readings start right of the dial, the bars' segments, and the lamps. */
    private static final int READINGS_X = 55;
    private static final int SEGMENTS = 10;
    private static final int LAMP_WIDTH = 16;
    private static final int LAMP_STEP = 18;
    private static final int BUTTONS_Y = 67;
    /* The carbon dioxide's segments lit past the fourth turn amber. */
    private static final int CO2_AMBER_FROM = 4;
    /* The small words, the game's font at three quarters of its size; the buttons' words, moulded on the case, keep
     * that size held up too, where the face's words are drawn at the font's own. */
    private static final float SMALL = 0.75F;

    private SuitControlsArt() {
    }

    /**
     * The controls reading {@code reading} with the case's top left at {@code (x, y)}, in units of {@code s}.
     */
    public static void draw(final GuiGraphics g, final Font font, final int x, final int y, final int s,
                            final SuitControlsHud.Reading reading) {
        final Colours c = PALETTE.get();
        g.blit(s == 1 ? CASE : CASE_BIG, x, y, 0, 0, WIDTH * s, HEIGHT * s, WIDTH * s, HEIGHT * s);
        if (reading.light()) {
            g.fill(x + FACE_X0 * s, y + FACE_Y0 * s, x + FACE_X1 * s, y + FACE_Y1 * s, c.backlight());
        }
        dial(g, font, x, y, s, reading.psi(), c);
        final int tx = x + READINGS_X * s;
        final int ty = y + (FACE_Y0 + 3) * s;
        final String left = clock(reading.secondsLeft());
        Draw.textScaled(g, font, left, tx, ty, c.white(), s);
        Draw.textScaled(g, font, GameText.resolve(SuitControls.LEFT), tx + font.width(left) * s + 3 * s, ty + 2 * s,
                c.unit(), words(s));
        Draw.textScaled(g, font, GameText.resolve(stateWord(reading.state())), tx, y + (FACE_Y0 + 14) * s,
                stateColour(reading.state(), c), words(s));
        final int o2 = reading.state() == SuitControlsHud.State.LOW ? c.amber() : c.green();
        bar(g, font, tx, y + (FACE_Y0 + 22) * s, s, SuitControls.OXYGEN, reading.oxygen(), o2, SEGMENTS, c);
        bar(g, font, tx, y + (FACE_Y0 + 30) * s, s, SuitControls.CARBON_DIOXIDE, reading.carbonDioxide(), c.green(),
                CO2_AMBER_FROM, c);
        lamp(g, font, tx, y, s, 0, SuitControls.OXYGEN, reading.state() == SuitControlsHud.State.LOW
                || reading.state() == SuitControlsHud.State.HOLED ? c.amber() : 0, c);
        lamp(g, font, tx, y, s, 1, SuitControls.PSI, reading.state() == SuitControlsHud.State.HOLED ? c.red() : 0, c);
        lamp(g, font, tx, y, s, 2, SuitControls.CARBON_DIOXIDE, 0, c);
        lamp(g, font, tx, y, s, 3, SuitControls.FAN, 0, c);
        button(g, font, x, y, s, FACE_X0 + 16, SuitControls.PURGE, c);
        button(g, font, x, y, s, FACE_X0 + 48 + BUTTON_WIDTH + 2, SuitControls.FAN, c);
        button(g, font, x, y, s, LIGHT_X + BUTTON_WIDTH + 2, SuitControls.LIGHT, c);
    }

    /** {@code seconds} as minutes and seconds, as the controls count the air left: 29:59. */
    public static String clock(final int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    /* The pressure's needle over the printed dial, its figure under the pivot and the ends of its scale. */
    private static void dial(final GuiGraphics g, final Font font, final int x, final int y, final int s,
                             final double psi, final Colours c) {
        final int cx = x + DIAL_X * s;
        final int cy = y + DIAL_Y * s;
        final int r = DIAL_RADIUS * s;
        final double a = Math.PI * (1 + Math.clamp(psi / MOST_PSI, 0, 1));
        for (int q = 0; q < r - 3 * s; q++) {
            final int px = cx + (int) Math.round(q * Math.cos(a));
            final int py = cy + (int) Math.round(q * Math.sin(a));
            g.fill(px, py, px + s, py + 1, c.needle());
        }
        g.fill(cx - s, cy - s, cx + 2 * s, cy + 2 * s, c.pivot());
        Draw.textScaled(g, font, String.format(GameLocale.locale(), "%.1f", psi), cx - 5 * s, cy + 2 * s, c.white(),
                words(s));
        Draw.textScaled(g, font, "0", cx - r - s, cy + 2 * s, c.dim(), words(s));
        Draw.textScaled(g, font, "5", cx + r - 2 * s, cy + 2 * s, c.dim(), words(s));
        Draw.textScaled(g, font, GameText.resolve(SuitControls.SUIT_PSI), x + (FACE_X0 + 3) * s,
                y + (FACE_Y0 + 3) * s, c.dim(), words(s));
    }

    /* A reading on ten segments, its name before them, those lit from {@code amberFrom} on in amber. */
    private static void bar(final GuiGraphics g, final Font font, final int x, final int y, final int s,
                            final TextKey name, final double share, final int lit, final int amberFrom,
                            final Colours c) {
        Draw.textScaled(g, font, GameText.resolve(name), x, y, c.dim(), words(s));
        // Any of it at all lights a segment, so the last of the air still shows.
        final double held = Math.clamp(share, 0, 1);
        final int on = held > 0 ? Math.max(1, (int) Math.round(held * SEGMENTS)) : 0;
        for (int k = 0; k < SEGMENTS; k++) {
            final int sx = x + (18 + k * 5) * s;
            g.fill(sx, y + s, sx + 3 * s, y + 5 * s, k >= on ? c.segmentOff() : k < amberFrom ? lit : c.amber());
        }
    }

    /* A warning lamp, lit in {@code colour}, or dark where it is 0. */
    private static void lamp(final GuiGraphics g, final Font font, final int x, final int y, final int s,
                             final int index, final TextKey name, final int colour, final Colours c) {
        final int lx = x + index * LAMP_STEP * s;
        final int ly = y + (FACE_Y1 - 9) * s;
        final boolean lit = colour != 0;
        g.fill(lx, ly, lx + LAMP_WIDTH * s, ly + 7 * s, lit ? colour : c.lampOff());
        final String word = GameText.resolve(name);
        // A longer word in another language shrinks to stay on its lamp.
        final float size = Math.min(words(s), (float) LAMP_WIDTH * s / Math.max(1, font.width(word)));
        final int w = (int) Math.ceil(font.width(word) * size);
        Draw.textScaled(g, font, word, lx + (LAMP_WIDTH * s - w) / 2, ly + s, lit ? c.lampLit() : c.lampLabel(),
                size);
    }

    /* A button's word, moulded on the case after the button. */
    private static void button(final GuiGraphics g, final Font font, final int x, final int y, final int s,
                               final int after, final TextKey word, final Colours c) {
        Draw.textScaled(g, font, GameText.resolve(word), x + after * s, y + BUTTONS_Y * s - (s == 1 ? 1 : 0),
                c.mould(), SMALL);
    }

    /* The face's words in units of {@code s}: small on the HUD, the game's font held up. */
    private static float words(final int s) {
        return s == 1 ? SMALL : s / 2F;
    }

    private static TextKey stateWord(final SuitControlsHud.State state) {
        return switch (state) {
            case SEALED -> SuitControls.SEALED;
            case LOW -> SuitControls.OXYGEN_LOW;
            case HOLED -> SuitControls.LEAK;
            case OPEN -> SuitControls.OPEN;
        };
    }

    private static int stateColour(final SuitControlsHud.State state, final Colours c) {
        return switch (state) {
            case SEALED -> c.green();
            case LOW -> c.amber();
            case HOLED -> c.red();
            case OPEN -> c.dim();
        };
    }

    private static ResourceLocation texture(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "textures/gui/life/" + name + ".png");
    }

    /**
     * The colours of the controls' face.
     *
     * @param white      the figures
     * @param dim        the faint labels and the dial's ends
     * @param unit       the word after the time
     * @param green      a reading that is well, and the sealed state
     * @param amber      a reading that warns
     * @param red        a reading in danger
     * @param segmentOff a bar's segment unlit
     * @param lampOff    a lamp unlit
     * @param lampLabel  an unlit lamp's word
     * @param lampLit    a lit lamp's word
     * @param needle     the needle
     * @param pivot      the needle's pivot
     * @param mould      the words moulded on the case beside its buttons
     * @param backlight  the face lit, over its black
     */
    public record Colours(int white, int dim, int unit, int green, int amber, int red, int segmentOff, int lampOff,
                          int lampLabel, int lampLit, int needle, int pivot, int mould, int backlight) {
    }
}
