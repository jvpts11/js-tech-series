/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.hazard;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.GameLocale;
import dev.jstech.core.client.effect.ScreenEffects;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.hud.IHudElement;
import dev.jstech.core.gui.HudStack;
import dev.jstech.core.hazard.BodyHeat;
import dev.jstech.core.hazard.BodyTemperature;
import dev.jstech.core.hazard.BodyTemperaturePayload;
import dev.jstech.core.hazard.HazardBand;
import dev.jstech.core.hazard.HazardKind;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The body's temperature on the HUD, only while the core is out of its normal band: a thermometer whose column stands
 * as high as the core is warm, the core in degrees, the band in its colour and which way the core goes how fast, and
 * five marks beside them, two lit for each band reached, from the left in the cold and from the right in the heat.
 * The screen's edge frosts in the cold and hazes in the heat, deeper the worse the band.
 */
@TextHolder
@PaletteHolder
public final class BodyTemperatureHud implements IHudElement {

    public static final TextKey CORE = TextKey.of("jscore.hud.body_temperature.core", "%s°C");
    public static final TextKey FALLING = TextKey.of("jscore.hud.body_temperature.falling", "falling %s°C/min");
    public static final TextKey RISING = TextKey.of("jscore.hud.body_temperature.rising", "rising %s°C/min");
    public static final TextKey STEADY = TextKey.of("jscore.hud.body_temperature.steady", "steady");
    /** The HUD's colours. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "body_temperature_hud",
            new Colours(0x96 << 24, 0xFF << 24 | 0xFFFFFF, 0xFF << 24 | 0x78B4FF, 0xFF << 24 | 0x285096,
                    0xFF << 24 | 0xF09632, 0xFF << 24 | 0xE63C28, 0xFF << 24 | 0xBEBEBE, 0xFF << 24 | 0x3C3C3C,
                    0xFF << 24 | 0xDCDCDC, 0x78 << 24 | 0xD7E8FF, 0x5A << 24 | 0xFFAA50));
    private static final ResourceLocation FROST = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "hypothermia");
    private static final ResourceLocation HAZE = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "overheating");
    private static final int WIDTH = 124;
    private static final int HEIGHT = 34;
    private static final int TEXT_X = 18;
    private static final int TEXT_ROOM = 72;
    private static final int PIPS_X = 92;
    private static final int PIP_PITCH = 6;
    private static final int PIP = 4;
    private static final int PIPS = 5;
    private static final float SMALL = 0.75F;
    /* The thermometer's scale, the body screen's own, from 26 to 44 °C, in kelvin. */
    private static final double SCALE_FROM = 299.15;
    private static final double SCALE_SPAN = 18.0;
    private static final double STILL = 0.05;
    private static final float LEAST_EDGE = 0.3F;

    @Override
    public boolean shown(final Minecraft minecraft) {
        return band(BodyTemperature.HYPOTHERMIA).isPresent() || band(BodyTemperature.OVERHEATING).isPresent();
    }

    @Override
    public HudStack.Size size(final Minecraft minecraft) {
        return new HudStack.Size(WIDTH, HEIGHT);
    }

    @Override
    public void render(final GuiGraphics g, final DeltaTracker delta, final int x, final int y) {
        final Colours c = PALETTE.get();
        final Font font = Minecraft.getInstance().font;
        final BodyTemperaturePayload told = BodyTemperatureClient.now();
        final Optional<HazardBand> cold = band(BodyTemperature.HYPOTHERMIA);
        final Optional<HazardBand> hot = band(BodyTemperature.OVERHEATING);
        final boolean chilled = cold.isPresent();
        final HazardKind kind = chilled ? BodyTemperature.HYPOTHERMIA : BodyTemperature.OVERHEATING;
        final Optional<HazardBand> band = chilled ? cold : hot;
        final int worse = band.map(found -> kind.bands().indexOf(found)).orElse(-1);
        final int colour = chilled ? worse >= kind.bands().size() - 1 ? c.coldDark() : c.cold()
                : worse >= kind.bands().size() - 1 ? c.hot() : c.warm();
        g.fill(x, y, x + WIDTH, y + HEIGHT, c.box());
        thermometer(g, c, x + 5, y + 9, (told.coreKelvin() - SCALE_FROM) / SCALE_SPAN, colour);
        Draw.text(g, font, GameText.resolve(CORE.with(tenths(told.coreKelvin() - BodyHeat.FREEZING))), x + TEXT_X,
                y + 5, c.value());
        band.ifPresent(found -> fitted(g, font, GameText.resolve(found.name()).toUpperCase(Locale.ROOT),
                x + TEXT_X, y + 16, colour));
        fitted(g, font, GameText.resolve(trend(told.perMinute())), x + TEXT_X, y + 24, c.trend());
        pips(g, c, x + PIPS_X, y + 6, chilled, worse);
        edge(c, chilled, worse, kind.bands().size());
    }

    /* The band the player stands in of {@code kind}, as the server last said. */
    private static Optional<HazardBand> band(final HazardKind kind) {
        return kind.band(HazardsClient.level(kind.id()));
    }

    /* Which way the core goes, and how fast. */
    private static Text trend(final double perMinute) {
        return Math.abs(perMinute) < STILL ? STEADY.text()
                : (perMinute < 0 ? FALLING : RISING).with(tenths(Math.abs(perMinute)));
    }

    /* {@code value} to a tenth, as the player's language writes it. */
    private static String tenths(final double value) {
        return GameLocale.numbers().decimal(Math.round(value * 10) / 10.0);
    }

    /* A thermometer eight by sixteen: the glass, the bulb, the column as high as {@code share} of its scale. */
    private static void thermometer(final GuiGraphics g, final Colours c, final int x, final int y,
                                    final double share, final int colour) {
        g.fill(x + 3, y, x + 6, y + 12, c.glass());
        g.fill(x + 1, y + 11, x + 8, y + 16, c.glass());
        g.fill(x + 2, y + 12, x + 7, y + 15, colour);
        final int top = y + 11 - (int) Math.round(10 * Math.clamp(share, 0, 1));
        g.fill(x + 4, top, x + 5, y + 12, colour);
    }

    /*
     * The five marks: two lit for each band reached, from the left in the cold and from the right in the heat, the
     * outermost in the worst band's colour.
     */
    private static void pips(final GuiGraphics g, final Colours c, final int x, final int y, final boolean chilled,
                             final int worse) {
        final int reached = Math.min(PIPS, 2 * (worse + 1));
        for (int k = 0; k < PIPS; k++) {
            final boolean lit = chilled ? k < reached : k >= PIPS - reached;
            final int colour = !lit ? c.off() : chilled ? k == 0 ? c.coldDark() : c.cold() : k == PIPS - 1 ? c.hot()
                    : c.warm();
            g.fill(x + k * PIP_PITCH, y, x + k * PIP_PITCH + PIP, y + PIP, colour);
        }
    }

    /* The screen's edge frosted in the cold or hazed in the heat, deeper the worse the band of {@code bands}. */
    private static void edge(final Colours c, final boolean chilled, final int worse, final int bands) {
        final float deep = LEAST_EDGE + (1 - LEAST_EDGE) * Math.max(0, worse) / Math.max(1, bands - 1);
        ScreenEffects.vignette(chilled ? FROST : HAZE, chilled ? c.frost() : c.haze(), deep);
    }

    private static void fitted(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                               final int colour) {
        final float fit = Math.min(SMALL, TEXT_ROOM * 4 / 3F / Math.max(1, font.width(text)) * SMALL);
        Draw.textScaled(g, font, text, x, y, colour, fit);
    }

    /**
     * The HUD's colours.
     *
     * @param box      the ground under it
     * @param value    the core's degrees
     * @param cold     the cold bands, their marks and the thermometer in the cold
     * @param coldDark the worst cold band
     * @param warm     the first hot band
     * @param hot      the worst hot band
     * @param trend    which way the core goes
     * @param off      a mark not lit
     * @param glass    the thermometer's glass
     * @param frost    the screen's edge in the cold
     * @param haze     the screen's edge in the heat
     */
    public record Colours(int box, int value, int cold, int coldDark, int warm, int hot, int trend, int off,
                          int glass, int frost, int haze) {
    }
}
