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
 * Droidix's apps, as the K0.11 apps page drew its network app, its alerts, its Messenger, its gallery and its
 * prospecting: the black of 2009 under a grey title band, its lists in white and its accents orange; the dark 2014
 * under a band in the app's own colour, its sections in pale blue and its switches pills; and the light 2019, its
 * large title on the left, its tabs pills, its parts on white cards and its buttons round.
 */
@PaletteHolder
public final class DroidixKit implements IAppKit {

    private final MobileSystem system;

    private static final float PITCH = 15.0F;
    private static final String[] BACK = {"..X", ".X.", "X..", ".X.", "..X"};
    private static final String[] TICK = {"....X", "...X.", "X.X..", ".X..."};
    /** Its colours, as the K0.11 apps page drew them. */
    private static final Palette<Colours> COLOURS = Palettes.declare(JsComputers.MODID, "mobile/droidix_apps",
            new Colours(0xFFFFFFFF, 0xFF000000, 0xFF20242A, 0xFF7A8090, 0xFFF4F5F9, 0xFF1A73E8, 0xFFE8F0FE,
                    0xFF202326, 0xFF33373C, 0xFF7FD0FF, 0xFFA0A8B0, 0xFF4A4E54, 0xFFA0A4AA, 0xFF101010, 0xFF5A5A5A,
                    0xFF2A2A2A, 0xFFFFB000, 0xFF30A050, 0xFFE04040, 0xFFF0A020, 0xFFE3E7EE, 0xFF5A6070,
                    0xFF9AA0AA, 0xFF2C3036, 0xFFDFE3EA, 0xFF1FA595, 0xFF3A3A3A));

    DroidixKit(final MobileSystem system) {
        this.system = system;
    }

    @Override
    public boolean dark(final MobileApp app) {
        return app == MobileApp.CAMERA || system != MobileSystem.DROIDIX_2019;
    }

    @Override
    public void ground(final MobileCanvas c, final MobileApp app) {
        final Colours k = COLOURS.get();
        final int ground = app == MobileApp.CAMERA ? k.black() : switch (system) {
            case DROIDIX_2009 -> k.ground2009();
            case DROIDIX_2014 -> k.ground2014();
            default -> k.ground2019();
        };
        c.rect(0, 0, c.width(), c.height(), ground);
    }

    @Override
    public int ink(final Tone tone) {
        final Colours k = COLOURS.get();
        final boolean light = system == MobileSystem.DROIDIX_2019;
        return switch (tone) {
            case TEXT -> light ? k.darkInk() : k.white();
            case DIM -> light ? k.softInk() : k.value2014();
            case ACCENT -> system == MobileSystem.DROIDIX_2009 ? k.orange() : light ? k.blue() : k.caps2014();
            case GOOD -> k.good();
            case WARN -> k.warn();
            case BAD -> k.bad();
            case LINE -> light ? k.line2019() : system == MobileSystem.DROIDIX_2014 ? k.line2014() : k.line2009();
            case SURFACE -> light ? k.white() : k.fieldDark();
        };
    }

    @Override
    public float bar(final MobileCanvas c, final MobileApp app, final float top, final String title,
                     @Nullable final String right, final boolean back) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        if (system == MobileSystem.DROIDIX_2019) {
            if (back) {
                c.glyph(BACK, 6, top + 5.5F, k.darkInk(), 1);
                c.press(0, top, 20, 16, MobileTap.BACK);
            }
            c.text(c.fit(title, w - 50, MobileCanvas.NORMAL), back ? 15 : 8, top + 4, k.darkInk(),
                    MobileCanvas.NORMAL, Align.LEFT, true);
            if (right != null) {
                c.text(c.fit(right, 40, MobileCanvas.SMALL), w - 6, top + 5.5F, k.softInk(), MobileCanvas.SMALL,
                        Align.RIGHT, false);
            }
            return top + 18;
        }
        final boolean old = system == MobileSystem.DROIDIX_2009;
        if (old) {
            c.gradient(0, top, w, 15, k.bar2009Top(), k.bar2009Bottom());
        } else {
            // The alerts' band is the green-blue the K0.11 page drew it in; the others wear their icon's colour.
            c.rect(0, top, w, 16, app == MobileApp.ALERTS ? k.teal() : MobileArt.colour(app));
        }
        if (back) {
            c.glyph(BACK, 4, top + 5, k.white(), 1);
            c.press(0, top, 16, 16, MobileTap.BACK);
        }
        c.text(c.fit(title, w - 50, MobileCanvas.NORMAL), back ? 11 : 6, top + 4, k.white(), MobileCanvas.NORMAL,
                Align.LEFT, !old);
        if (right != null) {
            c.text(c.fit(right, 44, MobileCanvas.SMALL), w - 5, top + 5.5F, k.white(), MobileCanvas.SMALL,
                    Align.RIGHT, false);
        }
        return top + 21;
    }

    @Override
    public float tabs(final MobileCanvas c, final float y, final List<String> labels, final int on,
                      final List<MobileTap> taps) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        if (system == MobileSystem.DROIDIX_2019) {
            float x = 6;
            for (int n = 0; n < labels.size(); n++) {
                final float pill = Math.max(32, c.width(labels.get(n), MobileCanvas.SMALL) + 10);
                if (n == on) {
                    c.rounded(x, y, pill, 11, 5.5F, k.blue(), null);
                }
                c.text(labels.get(n), x + pill / 2, y + 2.5F, n == on ? k.white() : k.pillText(), MobileCanvas.SMALL,
                        Align.CENTRE, false);
                c.press(x, y, pill, 11, taps.get(n));
                x += pill + 2;
            }
            return y + 16;
        }
        final float cell = w / labels.size();
        for (int n = 0; n < labels.size(); n++) {
            c.text(c.fit(labels.get(n), cell - 2, MobileCanvas.SMALL), n * cell + cell / 2, y + 2,
                    n == on ? k.white() : k.value2014(), MobileCanvas.SMALL, Align.CENTRE, n == on);
            if (n == on) {
                c.rect(n * cell + 2, y + 10, cell - 4, 1.5F, ink(Tone.ACCENT));
            }
            c.press(n * cell, y, cell, 12, taps.get(n));
        }
        c.rect(0, y + 11.5F, w, 0.5F, ink(Tone.LINE));
        return y + 16;
    }

    @Override
    public float columns(final MobileCanvas c, final float y, final String left, final String right) {
        return y;
    }

    @Override
    public float section(final MobileCanvas c, final float y, final String label) {
        c.text(c.fit(label, c.width() - 10, MobileCanvas.SMALL), 5, y + 1,
                system == MobileSystem.DROIDIX_2019 ? COLOURS.get().softInk() : ink(Tone.ACCENT), MobileCanvas.SMALL);
        return y + 10;
    }

    @Override
    public float row(final MobileCanvas c, final float y, @Nullable final ItemStack icon, final String name,
                     final String value, final Tone tone, @Nullable final MobileTap tap, final boolean chosen) {
        final float w = c.width();
        if (chosen) {
            c.rect(0, y - 1, w, PITCH, system == MobileSystem.DROIDIX_2019 ? COLOURS.get().paleBlue()
                    : COLOURS.get().chosenDark());
        }
        final float nameX = icon == null ? 5 : 21;
        if (icon != null) {
            c.item(icon, 5, y, 12);
        }
        final float valueW = c.width(value, MobileCanvas.SMALL);
        c.text(c.fit(name, w - nameX - valueW - 8, MobileCanvas.SMALL), nameX, y + 3, ink(Tone.TEXT),
                MobileCanvas.SMALL);
        c.text(value, w - 5, y + 3, tone == Tone.TEXT ? ink(Tone.DIM) : ink(tone), MobileCanvas.SMALL, Align.RIGHT,
                false);
        c.rect(5, y + PITCH - 2, w - 10, 1, ink(Tone.LINE));
        if (tap != null) {
            c.press(0, y - 1, w, PITCH, tap);
        }
        return y + PITCH;
    }

    @Override
    public float button(final MobileCanvas c, final float x, final float y, final float w, final String label,
                        final boolean primary, final MobileTap tap) {
        final Colours k = COLOURS.get();
        final float h;
        switch (system) {
            case DROIDIX_2019 -> {
                h = 16;
                c.rounded(x, y, w, h, 8, primary ? k.blue() : k.paleBlue(), null);
                c.text(c.fit(label, w - 6, MobileCanvas.SMALL), x + w / 2, y + 4.5F, primary ? k.white() : k.blue(),
                        MobileCanvas.SMALL, Align.CENTRE, false);
            }
            case DROIDIX_2014 -> {
                h = 14;
                c.rounded(x, y, w, h, 2, primary ? k.teal() : k.fieldDark(), null);
                c.text(c.fit(label, w - 6, MobileCanvas.SMALL), x + w / 2, y + 3.5F, k.white(), MobileCanvas.SMALL,
                        Align.CENTRE, primary);
            }
            default -> {
                h = 14;
                c.gradient(x, y, w, h, k.bar2009Top(), k.bar2009Bottom());
                c.rounded(x, y, w, h, 2, null, primary ? k.orange() : k.button2009Edge());
                c.text(c.fit(label, w - 6, MobileCanvas.SMALL), x + w / 2, y + 3.5F, k.white(), MobileCanvas.SMALL,
                        Align.CENTRE, false);
            }
        }
        c.press(x, y, w, h, tap);
        return y + h;
    }

    @Override
    public float toggle(final MobileCanvas c, final float y, final String label, final boolean on,
                        final MobileTap tap) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        c.text(c.fit(label, w - 30, MobileCanvas.SMALL), 5, y + 2, ink(Tone.TEXT), MobileCanvas.SMALL);
        if (system == MobileSystem.DROIDIX_2009) {
            // The check box of the first Droidix, green when on.
            c.rect(w - 14, y, 9, 9, k.white());
            if (on) {
                c.glyph(TICK, w - 12, y + 2.5F, k.good(), 1);
            }
        } else {
            final boolean light = system == MobileSystem.DROIDIX_2019;
            c.rounded(w - 19, y + 1, 14, 7, 3.5F, on ? (light ? k.blue() : k.teal())
                    : (light ? k.muted() : k.offTrack2014()), null);
            c.disc(on ? w - 8.5F : w - 15.5F, y + 4.5F, 3.5F, on ? k.white() : k.offKnob2014());
        }
        c.rect(5, y + 11, w - 10, 1, ink(Tone.LINE));
        c.press(0, y - 1, w, 13, tap);
        return y + 13;
    }

    @Override
    public float field(final MobileCanvas c, final float x, final float y, final float w, final String typed,
                       final String placeholder, final boolean focused, final MobileTap tap) {
        final Colours k = COLOURS.get();
        final boolean light = system == MobileSystem.DROIDIX_2019;
        if (light) {
            c.rounded(x, y, w, 13, 6, k.white(), null);
        } else {
            c.rect(x, y, w, 13, k.fieldDark());
            c.rect(x, y + 12, w, 1, focused ? ink(Tone.ACCENT) : k.value2014());
        }
        final String shown = typed.isEmpty() ? placeholder : c.fit(typed, w - 12, MobileCanvas.SMALL);
        final float tw = c.text(shown, x + 6, y + 3.5F, typed.isEmpty() ? k.hint() : ink(Tone.TEXT),
                MobileCanvas.SMALL);
        if (focused) {
            c.rect(x + 6 + (typed.isEmpty() ? 0 : tw) + 0.5F, y + 3, 0.5F, 7, ink(Tone.ACCENT));
        }
        c.press(x, y, w, 13, tap);
        return y + 13;
    }

    @Override
    public void meter(final MobileCanvas c, final float x, final float y, final float w, final float share,
                      final Tone tone) {
        final Colours k = COLOURS.get();
        final boolean light = system == MobileSystem.DROIDIX_2019;
        c.rect(x, y, w, 3, light ? k.line2019() : k.offTrack2014());
        c.rect(x, y, w * Math.max(0.0F, Math.min(1.0F, share)), 3, tone == Tone.TEXT
                ? (light ? k.blue() : ink(Tone.ACCENT)) : ink(tone));
    }

    @Override
    public void card(final MobileCanvas c, final float x, final float y, final float w, final float h) {
        if (system == MobileSystem.DROIDIX_2019) {
            c.rounded(x, y, w, h, 8, COLOURS.get().white(), null);
        }
    }

    @Override
    public void note(final MobileCanvas c, final float y, final String words, final Tone tone) {
        c.text(c.fit(words, c.width() - 8, MobileCanvas.SMALL), c.width() / 2.0F, y,
                tone == Tone.DIM && system == MobileSystem.DROIDIX_2019 ? COLOURS.get().hint() : ink(tone),
                MobileCanvas.SMALL, Align.CENTRE, false);
    }

    @Override
    public float bottom(final MobileCanvas c) {
        return c.height();
    }

    @Override
    public void keys(final MobileCanvas c, @Nullable final Entry left, final List<Entry> menu, final int chosen,
                     final boolean menuOpen, final MobileTap toggleMenu) {
        // Droidix's own keys are the system's, under every app alike.
    }

    @Override
    public float rowPitch() {
        return PITCH;
    }

    @Override
    public boolean picksBeforeOpening() {
        return false;
    }

    /** Droidix apps' colours, as the palette above names them. */
    private record Colours(int white, int black, int darkInk, int softInk, int ground2019, int blue, int paleBlue,
                           int ground2014, int line2014, int caps2014, int value2014, int offTrack2014,
                           int offKnob2014, int ground2009, int bar2009Top, int bar2009Bottom, int orange, int good,
                           int bad, int warn, int line2019, int pillText, int hint, int fieldDark, int muted, int teal,
                           int button2009Edge) {

        /* The rule between rows on the first Droidix. */
        int line2009() {
            return button2009Edge;
        }

        /* A chosen row on the dark Droidixes. */
        int chosenDark() {
            return fieldDark;
        }
    }
}
