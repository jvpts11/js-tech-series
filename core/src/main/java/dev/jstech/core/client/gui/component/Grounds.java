/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import dev.jstech.core.JsCore;
import dev.jstech.core.gui.GroundMap;
import dev.jstech.core.gui.TextShadow;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.joml.Matrix4f;

/**
 * The grounds of the frame being drawn. Whoever paints a background says here which it is, and text drawn over it
 * reads from here the colour of its shadow, by the rule {@link TextShadow} keeps: never the letter's colour, never the
 * ground's, never foreign to the letter.
 *
 * <pre>{@code
 * Grounds.fill(g, x, y, x + w, y + h, palette.panel());        // paints the panel and says what it is
 * Grounds.declare(g, x, y, x + w, y + h, middleOfTheGradient); // a ground painted some other way
 * Draw.text(g, font, "Ready", x + 4, y + 4, palette.text());    // shadowed by the panel under it
 * }</pre>
 *
 * <p>Grounds are kept in the screen's own units, whatever pose they were painted under, so a label drawn in a
 * translated or scaled pose still finds the panel it sits on. Where nothing was declared, the text is taken to be on
 * the ground it reads best on ({@link TextShadow#assumedGround}). Every frame starts with none.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class Grounds {

    private static final GroundMap PAINTED = new GroundMap();
    /** What the map answers where nothing was declared: a clear colour, which no declared ground ever is. */
    private static final int NOTHING = 0;

    private Grounds() {
    }

    /** Paints a ground and says what it is. */
    public static void fill(final GuiGraphics g, final int x1, final int y1, final int x2, final int y2,
                            final int argb) {
        g.fill(x1, y1, x2, y2, argb);
        declare(g, x1, y1, x2, y2, argb);
    }

    /** Says what a ground painted some other way is: a texture by its main tone, a gradient by its middle one. */
    public static void declare(final GuiGraphics g, final int x1, final int y1, final int x2, final int y2,
                               final int argb) {
        final Matrix4f m = g.pose().last().pose();
        PAINTED.declare(screenX(m, x1, y1), screenY(m, x1, y1), screenX(m, x2, y2), screenY(m, x2, y2), argb);
    }

    /** What the point is written on: the ground declared there last, or the one a letter of {@code text} reads on. */
    public static int under(final GuiGraphics g, final float x, final float y, final int text) {
        final Matrix4f m = g.pose().last().pose();
        final int declared = PAINTED.at(screenX(m, x, y), screenY(m, x, y), NOTHING);
        return declared != NOTHING ? declared : TextShadow.assumedGround(text);
    }

    /** The shadow of text in {@code text} colour starting at (x, y), from the ground under its first letter. */
    public static int shadow(final GuiGraphics g, final float x, final float y, final int text) {
        return TextShadow.of(text, under(g, x + 1, y + 4, text));
    }

    /**
     * Forgets every ground. The Core calls it as each frame begins; a drawer that paints somewhere other than the
     * screen (a monitor's picture in the world) calls it once it is done, so its grounds are not read as the screen's.
     */
    public static void clear() {
        PAINTED.clear();
    }

    @SubscribeEvent
    public static void onFrameStart(final RenderFrameEvent.Pre event) {
        clear();
    }

    private static float screenX(final Matrix4f m, final float x, final float y) {
        return m.m00() * x + m.m10() * y + m.m30();
    }

    private static float screenY(final Matrix4f m, final float x, final float y) {
        return m.m01() * x + m.m11() * y + m.m31();
    }
}
