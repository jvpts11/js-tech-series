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
 * Frames Pocket's programs, as the K0.11 apps page drew its network: a white window under the system's title bar,
 * a list with the heads of its columns, the chosen row in dark blue, push buttons and check boxes in grey; the left
 * soft key does the page's own work and the right one opens the Menu, where the program's other pages are, as the
 * Pocket PC's programs kept them.
 */
@PaletteHolder
public final class FramesPocketKit implements IAppKit {

    private final boolean legacy;

    private static final float PITCH = 15.0F;
    private static final float KEYS_H = 13.0F;
    private static final float MENU_ROW = 12.5F;
    private static final float MENU_X = 70.0F;
    private static final String[] TICK = {"....X", "...X.", "X.X..", ".X..."};
    /** Its colours, as the K0.11 apps page drew them. */
    private static final Palette<Colours> COLOURS = Palettes.declare(JsComputers.MODID, "mobile/frames_pocket_apps",
            new Colours(0xFFFFFFFF, 0xFF000000, 0xFFE8ECF4, 0xFF303030, 0xFF0A246A, 0xFFFFD040, 0xFF808080,
                    0xFFD4D0C8, 0xFF404040, 0xFF008000, 0xFFC07000, 0xFFC00000, 0xFFE0E0E0));

    FramesPocketKit(final MobileSystem system) {
        this.legacy = system == MobileSystem.FRAMES_POCKET_2002;
    }

    @Override
    public boolean dark(final MobileApp app) {
        return false;
    }

    @Override
    public void ground(final MobileCanvas c, final MobileApp app) {
        c.rect(0, 0, c.width(), c.height(), COLOURS.get().white());
    }

    @Override
    public int ink(final Tone tone) {
        final Colours k = COLOURS.get();
        return switch (tone) {
            case TEXT -> k.black();
            case DIM -> k.grey();
            case ACCENT -> k.chosen();
            case GOOD -> k.good();
            case WARN -> k.warn();
            case BAD -> k.bad();
            case LINE -> k.face();
            case SURFACE -> k.header();
        };
    }

    /* The system's title bar names the program; a page inside it puts its own name and words under it. */
    @Override
    public float bar(final MobileCanvas c, final MobileApp app, final float top, final String title,
                     @Nullable final String right, final boolean back) {
        if (!back) {
            return top;
        }
        final Colours k = COLOURS.get();
        c.text(c.fit(title, c.width() - 40, MobileCanvas.SMALL), 4, top + 3, k.chosen(), MobileCanvas.SMALL,
                Align.LEFT, true);
        if (right != null) {
            c.text(c.fit(right, 34, MobileCanvas.SMALL), c.width() - 4, top + 3, k.grey(), MobileCanvas.SMALL,
                    Align.RIGHT, false);
        }
        c.rect(4, top + 12, c.width() - 8, 1, k.face());
        return top + 15;
    }

    @Override
    public float tabs(final MobileCanvas c, final float y, final List<String> labels, final int on,
                      final List<MobileTap> taps) {
        return y;
    }

    @Override
    public float columns(final MobileCanvas c, final float y, final String left, final String right) {
        final Colours k = COLOURS.get();
        c.rect(0, y, c.width(), 12, k.header());
        c.text(left, 22, y + 2.5F, k.headerInk(), MobileCanvas.SMALL);
        c.text(right, c.width() - 4, y + 2.5F, k.headerInk(), MobileCanvas.SMALL, Align.RIGHT, false);
        return y + 14;
    }

    @Override
    public float section(final MobileCanvas c, final float y, final String label) {
        c.text(c.fit(label, c.width() - 8, MobileCanvas.SMALL), 4, y + 1, COLOURS.get().black(), MobileCanvas.SMALL,
                Align.LEFT, true);
        return y + 11;
    }

    @Override
    public float row(final MobileCanvas c, final float y, @Nullable final ItemStack icon, final String name,
                     final String value, final Tone tone, @Nullable final MobileTap tap, final boolean chosen) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        if (chosen) {
            c.rect(0, y - 1, w, PITCH, k.chosen());
        }
        final float nameX = icon == null ? 4 : 22;
        if (icon != null) {
            c.item(icon, 3, y, 12);
        }
        final float valueW = c.width(value, MobileCanvas.SMALL);
        c.text(c.fit(name, w - nameX - valueW - 8, MobileCanvas.SMALL), nameX, y + 2, chosen ? k.white() : k.black(),
                MobileCanvas.SMALL);
        c.text(value, w - 4, y + 2, chosen ? k.chosenValue() : tone == Tone.TEXT ? k.black() : ink(tone),
                MobileCanvas.SMALL, Align.RIGHT, false);
        if (tap != null) {
            c.press(0, y - 1, w, PITCH, tap);
        }
        return y + PITCH;
    }

    @Override
    public float button(final MobileCanvas c, final float x, final float y, final float w, final String label,
                        final boolean primary, final MobileTap tap) {
        final Colours k = COLOURS.get();
        final float h = 13;
        // A push button of its years: a grey face lit at its top and left, shaded at its foot and right.
        c.rect(x, y, w, h, k.edge());
        c.rect(x, y, w - 1, h - 1, k.white());
        c.rect(x + 1, y + 1, w - 2, h - 2, k.face());
        c.text(c.fit(label, w - 4, MobileCanvas.SMALL), x + w / 2, y + 3, k.black(), MobileCanvas.SMALL,
                Align.CENTRE, primary);
        c.press(x, y, w, h, tap);
        return y + h;
    }

    @Override
    public float toggle(final MobileCanvas c, final float y, final String label, final boolean on,
                        final MobileTap tap) {
        final Colours k = COLOURS.get();
        c.rect(4, y + 1, 9, 9, k.edge());
        c.rect(5, y + 2, 7, 7, k.white());
        if (on) {
            c.glyph(TICK, 6, y + 3.5F, k.black(), 1);
        }
        c.text(c.fit(label, c.width() - 20, MobileCanvas.SMALL), 16, y + 2.5F, k.black(), MobileCanvas.SMALL);
        c.press(0, y - 1, c.width(), 13, tap);
        return y + 13;
    }

    @Override
    public float field(final MobileCanvas c, final float x, final float y, final float w, final String typed,
                       final String placeholder, final boolean focused, final MobileTap tap) {
        final Colours k = COLOURS.get();
        c.rect(x, y, w, 12, k.edge());
        c.rect(x + 1, y + 1, w - 2, 10, k.white());
        final String shown = typed.isEmpty() ? placeholder : c.fit(typed, w - 8, MobileCanvas.SMALL);
        final float tw = c.text(shown, x + 3, y + 2.5F, typed.isEmpty() ? k.grey() : k.black(), MobileCanvas.SMALL);
        if (focused) {
            c.rect(x + 3 + (typed.isEmpty() ? 0 : tw) + 0.5F, y + 2, 0.5F, 8, k.black());
        }
        c.press(x, y, w, 12, tap);
        return y + 12;
    }

    @Override
    public void meter(final MobileCanvas c, final float x, final float y, final float w, final float share,
                      final Tone tone) {
        final Colours k = COLOURS.get();
        c.rect(x, y, w, 4, k.edge());
        c.rect(x + 0.5F, y + 0.5F, w - 1, 3, k.track());
        c.rect(x + 0.5F, y + 0.5F, (w - 1) * Math.max(0.0F, Math.min(1.0F, share)), 3,
                tone == Tone.TEXT ? k.chosen() : ink(tone));
    }

    @Override
    public void card(final MobileCanvas c, final float x, final float y, final float w, final float h) {
        // The Pocket PC drew its windows flat: a card is the window itself.
    }

    @Override
    public void note(final MobileCanvas c, final float y, final String words, final Tone tone) {
        c.text(c.fit(words, c.width() - 8, MobileCanvas.SMALL), c.width() / 2.0F, y, ink(tone), MobileCanvas.SMALL,
                Align.CENTRE, false);
    }

    @Override
    public float bottom(final MobileCanvas c) {
        return c.height() - KEYS_H;
    }

    @Override
    public void keys(final MobileCanvas c, @Nullable final Entry left, final List<Entry> menu, final int chosen,
                     final boolean menuOpen, final MobileTap toggleMenu) {
        final Colours k = COLOURS.get();
        final float foot = bottom(c);
        if (menuOpen && !menu.isEmpty()) {
            // The Menu rises over the page from the right soft key, as a Pocket PC's did.
            final float h = menu.size() * MENU_ROW + 2;
            final float top = foot - h - 1;
            final float w = c.width() - MENU_X - 2;
            c.rect(MENU_X, top, w, h, k.edge());
            c.rect(MENU_X + 1, top + 1, w - 2, h - 2, k.white());
            for (int n = 0; n < menu.size(); n++) {
                final float y = top + 1 + n * MENU_ROW;
                if (n == chosen) {
                    c.rect(MENU_X + 1, y, w - 2, MENU_ROW, k.chosen());
                }
                c.text(c.fit(menu.get(n).label(), w - 8, MobileCanvas.SMALL), MENU_X + 5, y + 2.5F,
                        n == chosen ? k.white() : k.black(), MobileCanvas.SMALL);
                c.press(MENU_X, y, w, MENU_ROW, menu.get(n).tap());
            }
        }
        FramesPocketLook.softKeys(c, legacy, left == null ? MobileTexts.say(MobileTexts.DONE) : left.label(),
                menu.isEmpty() ? null : MobileTexts.say(AppTexts.MENU), left == null ? MobileTap.BACK : left.tap(),
                menu.isEmpty() ? null : toggleMenu);
    }

    @Override
    public float rowPitch() {
        return PITCH;
    }

    @Override
    public boolean picksBeforeOpening() {
        return true;
    }

    /** Frames Pocket programs' colours, as the palette above names them. */
    private record Colours(int white, int black, int header, int headerInk, int chosen, int chosenValue, int grey,
                           int face, int edge, int good, int warn, int bad, int track) {
    }
}
