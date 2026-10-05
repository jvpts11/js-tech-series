/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.client.gui.component.Grounds;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Frames 7: a frame of tinted glass round every window, the title on the glass with a white glow behind it, glossy
 * caption buttons with a red close, and the top corners rounded. The glass is see-through, unblurred: whatever is
 * behind a window shows through its frame and title, tinted. Its colours are {@code jsc:chrome/aero}.
 *
 * <p>The window's own client area stays opaque, so a program draws on a ground it can read; only the frame and the
 * title are glass, which is what told this desktop from every other at a glance.
 */
@PaletteHolder
final class AeroChrome implements IFormChrome {

    private final Palette<Colours> palette;

    /** How far the white glow behind a title reaches past its words. */
    private static final int GLOW = 3;

    static final AeroChrome INSTANCE = new AeroChrome(Palettes.declare(JsComputers.MODID, "chrome/aero",
            new Colours(0xFF9FB3CF, 0xFFF4F7FB,
                    0x80BAD2EE, 0x6B96B4DC, 0x66C8D2DE, 0x52A8B4C4, 0xCC283C5A, 0x59FFFFFF, 0xFFA8C2E2,
                    0x8CFFFFFF, 0x73FFFFFF, 0x26FFFFFF, 0xB3283C5A, 0xFFE9A28E, 0xFFC7422A, 0xFF8E2010, 0xFF1A2A40,
                    0xFFFFFFFF,
                    0xFFF2F2F2, 0xFFDDDDDD, 0xFFEAF6FD, 0xFFA7D9F5, 0xFFDAEEF9, 0xFFC2E4F6, 0xFF707070,
                    0xFF3C7FB1, 0xFFABADB3, 0xFF3D7BAD, 0xFFFFFFFF, 0xFFE3EDF8, 0xFFCDDDEE, 0xFF8CA5C2,
                    0xFFF0F0F0, 0xFFDDE3EA, 0xFFB5B5B5, 0x40FFFFFF)));

    private AeroChrome(final Palette<Colours> palette) {
        this.palette = palette;
    }

    @Override
    public OsSkin.Form form() {
        return OsSkin.Form.AERO;
    }

    @Override
    public int edge(final OsSkin skin) {
        return this.palette.get().edge();
    }

    @Override
    public int panelFill(final OsSkin skin) {
        return this.palette.get().panel();
    }

    /**
     * The window with no title of its own to leave room for (a popup, a card): its body opaque, a glass rim round
     * it, the top corners rounded.
     */
    @Override
    public void windowFrame(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        final Colours c = this.palette.get();
        final int r = skin.topRadius();
        ChromeShapes.roundedRect(g, x - 1, y - 1, w + 2, h + 2, c.rim(), r, 0);
        ChromeShapes.roundedRect(g, x, y, w, h, skin.windowBg(), r, 0);
    }

    /**
     * A window with a title bar: glass over the whole of it but the client area, which is opaque, so the frame and
     * the title show what is behind. The title's own glass is laid on by {@link #titleBar}; here it is the sides,
     * the foot and the rim.
     */
    @Override
    public void windowFrame(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h, final int titleH, final int inset) {
        final Colours c = this.palette.get();
        final int r = skin.topRadius();
        ChromeShapes.roundedOutline(g, x - 1, y - 1, w + 2, h + 2, c.rim(), r);
        // The glass of the sides and the foot, the title band left to the title bar.
        g.fillGradient(x, y + titleH, x + inset, y + h, c.glassTop(), c.glassBottom());
        g.fillGradient(x + w - inset, y + titleH, x + w, y + h, c.glassTop(), c.glassBottom());
        g.fill(x + inset, y + h - inset, x + w - inset, y + h, c.glassBottom());
        // The client area, opaque, with the hairline where it meets the glass.
        g.fill(x + inset, y + titleH + inset, x + w - inset, y + h - inset, skin.windowBg());
        Grounds.declare(g, x + inset, y + titleH + inset, x + w - inset, y + h - inset, skin.windowBg());
        ChromeShapes.outline(g, x + inset - 1, y + titleH + inset - 1, w - 2 * inset + 2, h - titleH - 2 * inset + 2,
                c.clientRim());
    }

    @Override
    public void titleBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                         final boolean active, final int left, final int right) {
        final Colours c = this.palette.get();
        final int r = skin.topRadius();
        final int top = active ? c.glassTop() : c.idleTop();
        final int bottom = active ? c.glassBottom() : c.idleBottom();
        // Glass rounded at the top, the light along its upper edge that every Aero frame caught.
        for (int i = 0; i < r; i++) {
            final int in = r - i;
            g.fill(x + in, y + i, x + w - in, y + i + 1, top);
        }
        g.fillGradient(x, y + r, x + w, y + h, top, bottom);
        g.fill(x + r, y, x + w - r, y + 1, c.gloss());
        Grounds.declare(g, x, y, x + w, y + h, c.glassGround());
    }

    /** The soft white glow the title's words sit on, so they read on any glass whatever is behind it. */
    @Override
    public void titleGlow(final GuiGraphics g, final int x, final int y, final int textW) {
        final Colours c = this.palette.get();
        for (int i = GLOW; i >= 1; i--) {
            g.fill(x - i - 1, y - i + 1, x + textW + i + 1, y + 8 + i - 1, c.glow());
        }
    }

    @Override
    public void control(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y,
                        final int bw, final int bh, final OsSkin.Control control, final boolean hovered,
                        final boolean pressed) {
        final Colours c = this.palette.get();
        if (control == OsSkin.Control.CLOSE) {
            final int top = hovered ? ChromeShapes.lighten(c.closeTop()) : c.closeTop();
            if (pressed) {
                g.fillGradient(x, y, x + bw, y + bh, c.closeBottom(), top);
            } else {
                g.fillGradient(x, y, x + bw, y + bh / 2, top, c.closeBottom());
                g.fill(x, y + bh / 2, x + bw, y + bh, c.closeBottom());
            }
            ChromeShapes.outline(g, x, y, bw, bh, c.closeRim());
            ChromeShapes.glyph(g, font, control, x + (pressed ? 1 : 0), y + (pressed ? 1 : 0), bw, bh, c.closeGlyph());
            return;
        }
        final int lit = hovered ? c.buttonGloss() : c.buttonTop();
        g.fillGradient(x, y, x + bw, y + bh, pressed ? c.buttonBottom() : lit, pressed ? lit : c.buttonBottom());
        ChromeShapes.outline(g, x, y, bw, bh, c.buttonRim());
        ChromeShapes.glyph(g, font, control, x + (pressed ? 1 : 0), y + (pressed ? 1 : 0), bw, bh, c.glyph());
    }

    @Override
    public void panel(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h) {
        final Colours c = this.palette.get();
        Grounds.fill(g, x, y, x + w, y + h, c.panel());
        ChromeShapes.outline(g, x, y, w, h, c.edge());
    }

    /** The grey glossy button of the period: lighter above the middle, a rim, and a blue one when it is the default. */
    @Override
    public void button(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                       final boolean hovered, final boolean pressed, final boolean primary) {
        final Colours c = this.palette.get();
        final int upper = pressed ? c.pressedTop() : hovered ? c.hoverTop() : c.faceTop();
        final int lower = pressed ? c.pressedBottom() : hovered ? c.hoverBottom() : c.faceBottom();
        ChromeShapes.roundedRect(g, x, y, w, h / 2, upper, 1, 0);
        ChromeShapes.roundedRect(g, x, y + h / 2, w, h - h / 2, lower, 0, 1);
        ChromeShapes.roundedOutline(g, x, y, w, h, primary || hovered ? c.focusRim() : c.faceRim(), 1);
        Grounds.declare(g, x, y, x + w, y + h, upper);
    }

    @Override
    public void field(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                      final boolean focused) {
        final Colours c = this.palette.get();
        ChromeShapes.outline(g, x, y, w, h, focused ? c.fieldFocus() : c.fieldRim());
    }

    @Override
    public void tab(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                    final boolean active) {
        final Colours c = this.palette.get();
        if (active) {
            g.fill(x, y, x + w, y + h, c.tabFront());
            ChromeShapes.outline(g, x, y, w, h + 1, c.edge());
            g.fill(x + 1, y + h, x + w - 1, y + h + 1, c.tabFront());
        } else {
            ChromeShapes.vGradient(g, x, y + 1, x + w, y + h, c.tabTop(), c.tabBottom());
            ChromeShapes.outline(g, x, y + 1, w, h - 1, c.tabRim());
        }
    }

    @Override
    public void scrollThumb(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        final Colours c = this.palette.get();
        g.fillGradient(x, y, x + w, y + h, c.faceTop(), c.faceBottom());
        ChromeShapes.outline(g, x, y, w, h, c.faceRim());
    }

    @Override
    public void statusBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                          final int h) {
        final Colours c = this.palette.get();
        ChromeShapes.vGradient(g, x, y, x + w, y + h, c.statusTop(), c.statusBottom());
        g.fill(x, y, x + w, y + 1, c.edge());
    }

    @Override
    public boolean selectionBar() {
        return false;
    }

    @Override
    public int shadowSpread() {
        return 6;
    }

    @Override
    public int shadowStrength() {
        return 0x16;
    }

    /**
     * The colours of Frames 7's chrome.
     *
     * @param edge          separators, and the rim of a panel
     * @param panel         a content panel
     * @param glassTop      the top of the glass of a window in front, see-through
     * @param glassBottom   its foot
     * @param idleTop       the top of the glass of a window behind, paler
     * @param idleBottom    its foot
     * @param rim           the dark line round a window
     * @param gloss         the light along the top edge of the glass
     * @param glassGround   what the title's words are written on, said as one colour: the glass over a middling
     *                      desktop
     * @param buttonGloss   the top of a hovered caption button
     * @param buttonTop     the top of a caption button at rest
     * @param buttonBottom  the foot of a caption button
     * @param buttonRim     a caption button's rim
     * @param closeTop      the top of the close button's red
     * @param closeBottom   its foot
     * @param closeRim      its rim
     * @param glyph         the marks on the caption buttons
     * @param closeGlyph    the cross on the close button
     * @param faceTop       the upper half of a push button
     * @param faceBottom    its lower half
     * @param hoverTop      the upper half of a hovered push button, lit blue
     * @param hoverBottom   its lower half
     * @param pressedTop    the upper half of a pressed push button
     * @param pressedBottom its lower half
     * @param faceRim       a push button's rim
     * @param focusRim      the rim of the default button, or of one under the cursor
     * @param fieldRim      a text field's rim
     * @param fieldFocus    the rim of the field that has the keyboard
     * @param tabFront      the tab in front
     * @param tabTop        the top of a tab behind
     * @param tabBottom     its foot
     * @param tabRim        its rim
     * @param statusTop     the top of a status bar
     * @param statusBottom  its foot
     * @param clientRim     the hairline where the client area meets the glass
     * @param glow          one layer of the white glow behind a title
     */
    public record Colours(int edge, int panel,
                          int glassTop, int glassBottom, int idleTop, int idleBottom, int rim, int gloss,
                          int glassGround,
                          int buttonGloss, int buttonTop, int buttonBottom, int buttonRim, int closeTop,
                          int closeBottom, int closeRim, int glyph, int closeGlyph,
                          int faceTop, int faceBottom, int hoverTop, int hoverBottom, int pressedTop,
                          int pressedBottom, int faceRim,
                          int focusRim, int fieldRim, int fieldFocus, int tabFront, int tabTop, int tabBottom,
                          int tabRim, int statusTop, int statusBottom, int clientRim, int glow) {
    }
}
