/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.PortableItems;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.PortableAlert;
import dev.jstech.computers.portable.PortableBattery;
import dev.jstech.computers.portable.PortableModel;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.Text;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * What every family's screens draw alike: the battery's and the Wi-Fi's glyphs on a status bar, the signal's bars, the
 * badge on an app's icon, and the words of a count, a link and a battery's time left.
 */
@PaletteHolder
final class MobileParts {

    /*
     * What every family draws alike: white and black, the red of an icon's badge, and the battery a device shows on its
     * charger, its outline and its green.
     */
    private static final Palette<Colours> COLOURS = Palettes.declare(JsComputers.MODID, "mobile/shared",
            new Colours(0xFFFFFFFF, 0xFF000000, 0xFFFF3B30, 0xFFEBEBEB, 0xFF4CD964));
    /* How dim a glyph without a link is: its colour at this alpha. */
    private static final int DIM_ALPHA = 0x66;
    /* A game hour, in the ticks a battery's time left is counted in, and the hours of a day. */
    private static final int TICKS_AN_HOUR = 1_000;
    private static final int HOURS_A_DAY = 24;
    private static final int TICKS_A_SECOND = 20;

    private MobileParts() {
    }

    /** The shared colours. */
    static Colours colours() {
        return COLOURS.get();
    }

    /** A battery eleven pixels by six, its level filled in {@code fill} (or its own colour), its tip on the right. */
    static void battery(final MobileCanvas c, final float x, final float y, final int argb, final int percent,
                        @Nullable final Integer fill) {
        c.rect(x, y, 11, 1, argb);
        c.rect(x, y + 5, 11, 1, argb);
        c.rect(x, y, 1, 6, argb);
        c.rect(x + 10, y, 1, 6, argb);
        c.rect(x + 2, y + 2, Math.max(1, Math.round(7 * percent / 100.0F)), 2, fill == null ? argb : fill);
        c.rect(x + 11, y + 2, 1, 2, argb);
    }

    /** The Wi-Fi's arcs, while the device has a link; nothing without one. */
    static void wifi(final MobileCanvas c, final MobileFrame f, final float x, final float y, final int argb) {
        if (f.link() != null) {
            c.glyph(MobileArt.WIFI, x, y, argb, 1);
        }
    }

    /** The signal's four bars, all lit while the device has a link, all dim without one. */
    static void bars(final MobileCanvas c, final MobileFrame f, final float x, final float y, final int argb) {
        final int dim = (argb & 0xFFFFFF) | (DIM_ALPHA << 24);
        for (int k = 0; k < 4; k++) {
            final int tall = 2 + k;
            c.rect(x + k * 2, y + 6 - tall, 1, tall, f.link() != null ? argb : dim);
        }
    }

    /** The badge counting {@code count} on an icon whose top right is at {@code (x, y)}; none for none. */
    static void badge(final MobileCanvas c, final float x, final float y, final int count) {
        if (count <= 0) {
            return;
        }
        c.disc(x, y, 4.5F, COLOURS.get().badge());
        c.text(Integer.toString(count), x, y - 3, COLOURS.get().white(), MobileCanvas.SMALL,
                MobileCanvas.Align.CENTRE, false);
    }

    /** How many alerts, in words: none, one, or the count. */
    static String count(final int alerts) {
        return switch (alerts) {
            case 0 -> MobileTexts.say(MobileTexts.NO_ALERTS);
            case 1 -> MobileTexts.say(MobileTexts.ONE_ALERT);
            default -> MobileTexts.say(MobileTexts.ALERT_COUNT.with(alerts));
        };
    }

    /** The access point the device is linked through, or the words for none. */
    static String accessPoint(final MobileFrame f) {
        return f.link() == null ? MobileTexts.say(MobileTexts.NOT_CONNECTED) : f.link().accessPoint();
    }

    /** The network the device is on, by its short id, or the words for none. */
    static String network(final MobileFrame f) {
        return f.link() == null ? MobileTexts.say(MobileTexts.NO_NETWORK) : f.link().network();
    }

    /** How long the battery keeps the device on, in game days and hours. */
    static String timeLeft(final MobileFrame f) {
        final int millijoules = Math.round(f.battery() / 100.0F * PortableBattery.capacity(f.model()));
        final int hours = PortableBattery.secondsLeft(millijoules) * TICKS_A_SECOND / TICKS_AN_HOUR;
        return hours >= HOURS_A_DAY ? MobileTexts.say(MobileTexts.DAYS_HOURS.with(hours / HOURS_A_DAY,
                hours % HOURS_A_DAY)) : MobileTexts.say(MobileTexts.HOURS.with(hours));
    }

    /** The device's storage as its settings put it: what its system takes of all of it. */
    static String storage(final PortableModel model) {
        return MobileTexts.say(MobileTexts.OF.with(PortableModel.size(model.system().footprintMb()),
                PortableModel.size(model.totalStorageMb())));
    }

    /** What {@code alert} says, in the player's language. */
    static String title(final PortableAlert alert) {
        return MobileTexts.say(alert.title());
    }

    /** The line under what {@code alert} says. */
    static String detail(final PortableAlert alert) {
        return MobileTexts.say(alert.detail());
    }

    /** The device's own name, its item's. */
    static String deviceName(final PortableModel model) {
        return PortableItems.item(model).getDescription().getString();
    }

    /** {@code app}'s name. */
    static String name(final MobileApp app) {
        return MobileTexts.say(app.word());
    }

    /** {@code app}'s name under its icon, where there is little room. */
    static String label(final MobileApp app) {
        return MobileTexts.say(app.label());
    }

    /** The first {@code most} of {@code alerts}. */
    static List<PortableAlert> first(final List<PortableAlert> alerts, final int most) {
        return alerts.subList(0, Math.min(most, alerts.size()));
    }

    /** What {@code text} says. */
    static String say(final Text text) {
        return MobileTexts.say(text);
    }

    /** A count as the apps write it, its thousands grouped. */
    static String number(final long count) {
        return String.format(Locale.ROOT, "%,d", count);
    }

    /** The shared colours, as the palette above names them. */
    record Colours(int white, int black, int badge, int batteryLine, int batteryGreen) {
    }
}
