/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.portable.MobileCanvas.Align;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.MobileSystem;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The qOS's apps, as the K0.11 apps page drew its network app, its camera and its maps: white pages under a bar with
 * the app's name centred (a blue-grey band in 2007, a large title on the left from 2017), the segmented tabs, the
 * rows ruled from their name on, the blue buttons and the green switches; from 2017 the grouped grey with white cards
 * and the home bar at the foot of a device with no home button. The camera and the maps are dark, as their pictures
 * fill the screen.
 */
@PaletteHolder
public final class QosKit implements IAppKit {

    private final MobileSystem system;

    private static final float PITCH = 15.0F;
    private static final float BUTTON_H = 14.0F;
    private static final float SWITCH_W = 16.0F;
    private static final float SWITCH_H = 9.0F;
    private static final String[] BACK = {"..X", ".X.", "X..", ".X.", "..X"};
    /** Its colours, as the K0.11 apps page drew them. */
    private static final Palette<Colours> COLOURS = Palettes.declare(JsComputers.MODID, "mobile/qos_apps",
            new Colours(0xFFFFFFFF, 0xFF000000, 0xFF8E8E93, 0xFF2F7EF0, 0xFF007AFF, 0xFFFF3B30, 0xFFFF9500,
                    0xFF34C759, 0xFFE8E8EC, 0xFFE8E8ED, 0xFFE5E5EA, 0xFFF2F2F7, 0xFF6D6D72, 0xFFB4C2D4, 0xFF6D84A2,
                    0xFF2D3D53, 0xFF4CD964, 0xFFE0E8F4));

    QosKit(final MobileSystem system) {
        this.system = system;
    }

    @Override
    public boolean dark(final MobileApp app) {
        return app == MobileApp.CAMERA || app == MobileApp.MAPS;
    }

    @Override
    public void ground(final MobileCanvas c, final MobileApp app) {
        final Colours k = COLOURS.get();
        final int ground = dark(app) ? k.black() : system == MobileSystem.QOS_2017 ? k.grouped() : k.white();
        c.rect(0, 0, c.width(), c.height(), ground);
    }

    @Override
    public int ink(final Tone tone) {
        final Colours k = COLOURS.get();
        return switch (tone) {
            case TEXT -> k.black();
            case DIM -> k.grey();
            case ACCENT -> k.blue();
            case GOOD -> k.green();
            case WARN -> k.orange();
            case BAD -> k.red();
            case LINE -> k.line();
            case SURFACE -> system == MobileSystem.QOS_2017 ? k.white() : k.field();
        };
    }

    @Override
    public float bar(final MobileCanvas c, final MobileApp app, final float top, final String title,
                     @Nullable final String right, final boolean back) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        if (system == MobileSystem.QOS_2007) {
            c.gradient(0, top, w, 16, k.navTop(), k.navBottom());
            c.rect(0, top + 16, w, 1, k.navLine());
            c.text(c.fit(title, w - 50, MobileCanvas.NORMAL), w / 2, top + 4, k.white(), MobileCanvas.NORMAL,
                    Align.CENTRE, true);
            if (back) {
                c.glyph(BACK, 5, top + 5.5F, k.white(), 1);
                c.press(0, top, 24, 16, MobileTap.BACK);
            }
            if (right != null) {
                c.text(c.fit(right, 30, MobileCanvas.SMALL), w - 4, top + 5.5F, k.white(), MobileCanvas.SMALL,
                        Align.RIGHT, false);
            }
            return top + 20;
        }
        final boolean large = system == MobileSystem.QOS_2017;
        if (back) {
            c.glyph(BACK, 5, top + 4.5F, k.blue(), 1);
            c.press(0, top, 24, 14, MobileTap.BACK);
        }
        if (large) {
            c.text(c.fit(title, w - 50, MobileCanvas.NORMAL), back ? 13 : 8, top + 4, k.black(),
                    MobileCanvas.NORMAL, Align.LEFT, true);
        } else {
            c.text(c.fit(title, w - 60, MobileCanvas.NORMAL), w / 2, top + 2, k.black(), MobileCanvas.NORMAL,
                    Align.CENTRE, true);
        }
        if (right != null) {
            c.text(c.fit(right, 34, MobileCanvas.SMALL), w - 4, top + (large ? 5 : 3.5F), k.grey(),
                    MobileCanvas.SMALL, Align.RIGHT, false);
        }
        return top + (large ? 17 : 13);
    }

    @Override
    public float tabs(final MobileCanvas c, final float y, final List<String> labels, final int on,
                     final List<MobileTap> taps) {
        final Colours k = COLOURS.get();
        final float x = 6;
        final float w = c.width() - 12;
        c.rounded(x, y, w, 11, 3, null, k.blue());
        final float cell = w / labels.size();
        for (int n = 0; n < labels.size(); n++) {
            if (n == on) {
                c.rect(x + n * cell, y, cell, 11, k.blue());
            } else if (n > 0) {
                c.rect(x + n * cell, y, 1, 11, k.blue());
            }
            c.text(c.fit(labels.get(n), cell - 2, MobileCanvas.SMALL), x + n * cell + cell / 2, y + 2.5F,
                    n == on ? k.white() : k.blue(), MobileCanvas.SMALL, Align.CENTRE, false);
            c.press(x + n * cell, y, cell, 11, taps.get(n));
        }
        return y + 15;
    }

    @Override
    public float columns(final MobileCanvas c, final float y, final String left, final String right) {
        return y;
    }

    @Override
    public float section(final MobileCanvas c, final float y, final String label) {
        c.text(c.fit(label, c.width() - 12, MobileCanvas.SMALL), 6, y + 1, COLOURS.get().caps(),
                MobileCanvas.SMALL);
        return y + 10;
    }

    @Override
    public float row(final MobileCanvas c, final float y, @Nullable final ItemStack icon, final String name,
                     final String value, final Tone tone, @Nullable final MobileTap tap, final boolean chosen) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        if (chosen) {
            c.rect(0, y - 1, w, PITCH, k.chosen());
        }
        final float valueW = c.width(value, MobileCanvas.SMALL);
        final float nameX = icon == null ? 6 : 20;
        if (icon != null) {
            c.item(icon, 4, y, 12);
        }
        c.text(c.fit(name, w - nameX - valueW - 8, MobileCanvas.SMALL), nameX, y + 3, k.black(), MobileCanvas.SMALL);
        c.text(value, w - 4, y + 3, tone == Tone.TEXT ? k.grey() : ink(tone), MobileCanvas.SMALL, Align.RIGHT,
                false);
        c.rect(nameX, y + PITCH - 2, w - nameX, 1, k.line());
        if (tap != null) {
            c.press(0, y - 1, w, PITCH, tap);
        }
        return y + PITCH;
    }

    @Override
    public float button(final MobileCanvas c, final float x, final float y, final float w, final String label,
                        final boolean primary, final MobileTap tap) {
        final Colours k = COLOURS.get();
        final float r = system == MobileSystem.QOS_2017 ? 7 : 4;
        c.rounded(x, y, w, BUTTON_H, r, primary ? k.buttonBlue() : k.buttonGrey(), null);
        c.text(c.fit(label, w - 4, MobileCanvas.SMALL), x + w / 2, y + 3.5F, primary ? k.white() : k.buttonBlue(),
                MobileCanvas.SMALL, Align.CENTRE, false);
        c.press(x, y, w, BUTTON_H, tap);
        return y + BUTTON_H;
    }

    @Override
    public float toggle(final MobileCanvas c, final float y, final String label, final boolean on,
                        final MobileTap tap) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        c.text(c.fit(label, w - 34, MobileCanvas.SMALL), 6, y + 3, k.black(), MobileCanvas.SMALL);
        final float sx = w - SWITCH_W - 5;
        c.rounded(sx, y + 1, SWITCH_W, SWITCH_H, SWITCH_H / 2, on ? k.switchOn() : k.buttonGrey(), null);
        c.disc(on ? sx + SWITCH_W - SWITCH_H / 2 : sx + SWITCH_H / 2, y + 1 + SWITCH_H / 2, SWITCH_H / 2 - 1,
                k.white());
        c.rect(6, y + PITCH - 2, w - 6, 1, k.line());
        c.press(0, y - 1, w, PITCH, tap);
        return y + PITCH;
    }

    @Override
    public float field(final MobileCanvas c, final float x, final float y, final float w, final String typed,
                       final String placeholder, final boolean focused, final MobileTap tap) {
        final Colours k = COLOURS.get();
        c.rounded(x, y, w, 11, 3, k.field(), null);
        final String shown = typed.isEmpty() ? placeholder : c.fit(typed, w - 10, MobileCanvas.SMALL);
        final float tw = c.text(shown, x + 5, y + 2.5F, typed.isEmpty() ? k.grey() : k.black(), MobileCanvas.SMALL);
        if (focused) {
            c.rect(x + 5 + (typed.isEmpty() ? 0 : tw) + 0.5F, y + 2, 0.5F, 7, k.blue());
        }
        c.press(x, y, w, 11, tap);
        return y + 11;
    }

    @Override
    public void meter(final MobileCanvas c, final float x, final float y, final float w, final float share,
                      final Tone tone) {
        final Colours k = COLOURS.get();
        c.rect(x, y, w, 3, k.buttonGrey());
        c.rect(x, y, w * Math.max(0.0F, Math.min(1.0F, share)), 3, tone == Tone.TEXT ? k.blue() : ink(tone));
    }

    @Override
    public void card(final MobileCanvas c, final float x, final float y, final float w, final float h) {
        if (system == MobileSystem.QOS_2017) {
            c.rounded(x, y, w, h, 8, COLOURS.get().white(), null);
        }
    }

    @Override
    public void note(final MobileCanvas c, final float y, final String words, final Tone tone) {
        c.text(c.fit(words, c.width() - 8, MobileCanvas.SMALL), c.width() / 2.0F, y, ink(tone), MobileCanvas.SMALL,
                Align.CENTRE, false);
    }

    @Override
    public float bottom(final MobileCanvas c) {
        return system == MobileSystem.QOS_2017 ? c.height() - 7 : c.height();
    }

    @Override
    public void keys(final MobileCanvas c, @Nullable final Entry left, final List<Entry> menu, final int chosen,
                     final boolean menuOpen, final MobileTap toggleMenu) {
        if (system == MobileSystem.QOS_2017) {
            // The home bar of a device with no home button, a swipe up from it going home.
            c.rounded(c.width() / 2.0F - 18, c.height() - 4, 36, 2, 1, COLOURS.get().black(), null);
        }
    }

    @Override
    public float rowPitch() {
        return PITCH;
    }

    @Override
    public boolean picksBeforeOpening() {
        return false;
    }

    /** The qOS apps' colours, as the palette above names them. */
    private record Colours(int white, int black, int grey, int blue, int buttonBlue, int red, int orange, int green,
                           int line, int field, int buttonGrey, int grouped, int caps, int navTop, int navBottom,
                           int navLine, int switchOn, int chosen) {
    }
}
