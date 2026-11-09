/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.client.gui.component.Grounds;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * How Quince's faces draw a window and the controls in it (computers/docs/design/quince.md): the Quince System's
 * one bit with its striped title and its one close box, Platinum's bevels and pinstripes with the close, zoom and
 * collapse boxes, and from Quince OS X on the three lights at the left of a title bar. Every title is centred, and
 * unlike the rest of the mod the buttons keep their round corners.
 *
 * <p>The colours are the face's own ({@link QuinceLook}); the skin's are read only for the window's ground.
 */
final class QuinceChrome implements IFormChrome {

    private final QuinceLook look;

    static final QuinceChrome ONE_BIT = new QuinceChrome(QuinceLook.ONE_BIT);
    static final QuinceChrome PLATINUM = new QuinceChrome(QuinceLook.PLATINUM);
    static final QuinceChrome PULP = new QuinceChrome(QuinceLook.PULP);
    static final QuinceChrome FLAT = new QuinceChrome(QuinceLook.FLAT);
    static final QuinceChrome QUINCE_OS = new QuinceChrome(QuinceLook.QUINCE_OS);

    private QuinceChrome(final QuinceLook look) {
        this.look = look;
    }

    /** The face this chrome draws. */
    QuinceLook look() {
        return this.look;
    }

    @Override
    public OsSkin.Form form() {
        return switch (look) {
            case ONE_BIT -> OsSkin.Form.ONE_BIT;
            case PLATINUM -> OsSkin.Form.PLATINUM;
            case PULP -> OsSkin.Form.PULP;
            // The later faces are flat ones, and a program drawing in them draws its flat widgets.
            case FLAT, QUINCE_OS -> OsSkin.Form.FLAT;
        };
    }

    @Override
    public int edge(final OsSkin skin) {
        return look.colours().shade();
    }

    @Override
    public int panelFill(final OsSkin skin) {
        return look.colours().face();
    }

    @Override
    public void windowFrame(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        final QuinceLook.Colours c = look.colours();
        switch (look) {
            case ONE_BIT -> {
                g.fill(x + 1, y + h, x + w + 1, y + h + 1, c.shadow());
                g.fill(x + w, y + 1, x + w + 1, y + h + 1, c.shadow());
                Grounds.fill(g, x, y, x + w, y + h, skin.windowBg());
                ChromeShapes.outline(g, x, y, w, h, c.frame());
            }
            case PLATINUM -> {
                QuinceArt.bevel(g, x, y, x + w, y + h, skin.windowBg(), c.light(), c.shade());
                Grounds.declare(g, x, y, x + w, y + h, skin.windowBg());
                ChromeShapes.outline(g, x, y, w, h, c.frame());
            }
            default -> {
                QuinceArt.roundRect(g, x, y, x + w, y + h, look.round(), c.frame());
                QuinceArt.roundRect(g, x + 1, y + 1, x + w - 1, y + h - 1, Math.max(0, look.round() - 1),
                        skin.windowBg());
                Grounds.declare(g, x + 1, y + 1, x + w - 1, y + h - 1, skin.windowBg());
            }
        }
    }

    @Override
    public void titleBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                         final boolean active, final int left, final int right) {
        final QuinceLook.Colours c = look.colours();
        switch (look) {
            case ONE_BIT -> {
                Grounds.fill(g, x + 1, y + 1, x + w - 1, y + h - 1, c.face());
                g.fill(x, y + h - 1, x + w, y + h, c.frame());
                if (active) {
                    for (int yy = y + 2; yy < y + h - 2; yy += 2) {
                        g.fill(x + 2, yy, x + w - 2, yy + 1, c.stripe());
                    }
                }
            }
            case PLATINUM -> {
                Grounds.fill(g, x + 1, y + 1, x + w - 1, y + h - 1, c.face());
                if (active) {
                    for (int yy = y + 3; yy < y + h - 3; yy += 2) {
                        g.fill(x + 4, yy, x + w - 4, yy + 1, c.stripe());
                        g.fill(x + 4, yy + 1, x + w - 4, yy + 2, c.light());
                    }
                }
            }
            default -> {
                if (look.round() == 0) {
                    ChromeShapes.vGradient(g, x + 1, y + 1, x + w - 1, y + h - 1, active ? c.titleTop() : c.face(),
                            active ? c.titleBottom() : c.face());
                } else {
                    Grounds.fill(g, x + look.round(), y + 1, x + w - look.round(), y + h - 1, c.titleTop());
                }
                g.fill(x + 1, y + h - 1, x + w - 1, y + h, c.shade());
            }
        }
    }

    /** Clears the stripes from behind a classic title, as the real ones left a gap round their words. */
    @Override
    public void titleGlow(final GuiGraphics g, final int x, final int y, final int textW) {
        if (look.classic()) {
            Grounds.fill(g, x - 5, y - 1, x + textW + 5, y + 8, look.colours().face());
        }
    }

    @Override
    public void control(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y,
                        final int bw, final int bh, final OsSkin.Control control, final boolean hovered,
                        final boolean pressed) {
        final QuinceLook.Colours c = look.colours();
        switch (look) {
            case ONE_BIT -> {
                // The close box: a square cleared out of the stripes, filled while it is held.
                g.fill(x, y, x + bw - 1, y + bh - 2, c.face());
                ChromeShapes.outline(g, x + 1, y, bw - 2, bh - 2, c.frame());
                if (pressed) {
                    g.fill(x + 2, y + 1, x + bw - 2, y + bh - 3, c.frame());
                }
            }
            case PLATINUM -> {
                final int s = Math.min(bw, bh) - 1;
                QuinceArt.bevel(g, x, y, x + s, y + s, c.face(), c.shade(), c.light());
                QuinceArt.bevel(g, x + 1, y + 1, x + s - 1, y + s - 1, pressed ? c.shade() : c.track(), c.light(),
                        c.shade());
                if (control == OsSkin.Control.MINIMIZE) {
                    g.fill(x + 2, y + s / 2, x + s - 2, y + s / 2 + 1, c.shadow());
                } else if (control != OsSkin.Control.CLOSE) {
                    ChromeShapes.outline(g, x + 2, y + 2, s - 5, s - 5, c.shadow());
                }
            }
            default -> {
                final QuinceArt.Colours art = QuinceArt.colours();
                final int index = switch (control) {
                    case CLOSE -> 0;
                    case MINIMIZE -> 1;
                    case MAXIMIZE, RESTORE -> 2;
                };
                final int face = art.light(index);
                final int cx = x + bw / 2;
                final int cy = y + bh / 2;
                QuinceArt.disc(g, cx, cy, 3, ChromeShapes.blend(face, art.lightRing(), 0.25F), 0);
                QuinceArt.disc(g, cx, cy, 2, pressed ? ChromeShapes.darken(face) : face,
                        look == QuinceLook.PULP ? art.lightGleam(index) : 0);
                // The marks come up on the lights while the pointer is over them.
                if (hovered) {
                    g.fill(cx - 1, cy, cx + 1, cy + 1, ChromeShapes.darken(ChromeShapes.darken(face)));
                }
            }
        }
    }

    @Override
    public void panel(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h) {
        final QuinceLook.Colours c = look.colours();
        if (look == QuinceLook.PLATINUM) {
            QuinceArt.bevel(g, x, y, x + w, y + h, c.face(), c.shade(), c.light());
            Grounds.declare(g, x, y, x + w, y + h, c.face());
            return;
        }
        Grounds.fill(g, x, y, x + w, y + h, look == QuinceLook.ONE_BIT ? c.face() : c.light());
        ChromeShapes.outline(g, x, y, w, h, look == QuinceLook.ONE_BIT ? c.frame() : c.shade());
    }

    @Override
    public void button(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                       final boolean hovered, final boolean pressed, final boolean primary) {
        QuinceArt.button(g, look, x, y, w, h, primary, pressed);
    }

    @Override
    public int buttonText(final OsSkin skin, final boolean primary) {
        return QuinceArt.buttonInk(look, primary);
    }

    @Override
    public void field(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                      final boolean focused) {
        final QuinceLook.Colours c = look.colours();
        if (look == QuinceLook.PLATINUM) {
            g.fill(x, y, x + w, y + 1, c.shade());
            g.fill(x, y, x + 1, y + h, c.shade());
            g.fill(x, y + h - 1, x + w, y + h, c.light());
            g.fill(x + w - 1, y, x + w, y + h, c.light());
            return;
        }
        ChromeShapes.outline(g, x, y, w, h, focused && look.lights() ? c.select() : c.frame());
    }

    @Override
    public void tab(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                    final boolean active) {
        final QuinceLook.Colours c = look.colours();
        if (look.classic()) {
            QuinceArt.rounded(g, x, y, x + w, y + h + 2, c.frame(), 2, 1, active ? c.face() : c.track());
            return;
        }
        // The later faces' segmented control: the chosen segment in the accent.
        QuinceArt.roundRect(g, x, y, x + w, y + h, 3, c.frame());
        QuinceArt.roundRect(g, x + 1, y + 1, x + w - 1, y + h - 1, 2, active ? c.select() : c.button());
    }

    @Override
    public int tabText(final OsSkin skin, final boolean active) {
        final QuinceLook.Colours c = look.colours();
        return active && look.lights() ? c.selectInk() : c.ink();
    }

    @Override
    public void scrollThumb(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        final QuinceLook.Colours c = look.colours();
        switch (look) {
            case ONE_BIT -> {
                g.fill(x, y, x + w, y + h, c.face());
                ChromeShapes.outline(g, x, y, w, h, c.frame());
            }
            case PLATINUM -> QuinceArt.bevel(g, x, y, x + w, y + h, c.thumb(), c.light(), c.shade());
            default -> QuinceArt.roundRect(g, x + 1, y, x + w - 1, y + h, Math.max(1, (w - 2) / 2), c.thumb());
        }
    }

    @Override
    public void statusBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                          final int h) {
        final QuinceLook.Colours c = look.colours();
        Grounds.fill(g, x, y, x + w, y + h, look.classic() ? c.face() : c.titleBottom());
        g.fill(x, y, x + w, y + 1, look == QuinceLook.ONE_BIT ? c.frame() : c.shade());
    }

    @Override
    public boolean titleCentered() {
        return true;
    }

    @Override
    public OsSkin.Controls controls() {
        return switch (look) {
            case ONE_BIT -> OsSkin.Controls.CLOSE_LEFT;
            case PLATINUM -> OsSkin.Controls.CLASSIC;
            default -> OsSkin.Controls.LIGHTS;
        };
    }

    @Override
    public boolean focusOutline() {
        return false;
    }

    @Override
    public int shadowSpread() {
        return look.classic() ? 0 : 4;
    }
}
