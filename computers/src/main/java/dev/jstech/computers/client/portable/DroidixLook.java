/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.portable.MobileArt.IconStyle;
import dev.jstech.computers.client.portable.MobileCanvas.Align;
import dev.jstech.computers.portable.AlertKind;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.MobileFamily;
import dev.jstech.computers.portable.MobileSystem;
import dev.jstech.computers.portable.PortableAlert;
import dev.jstech.computers.portable.PortableModel;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * Droidix, open, on the Linux kernel, on the other makers' phones and tablets, after Android
 * (computers/docs/design/wireless.md): the dark 2009 with its green slider and the drawer's handle, the 2014 with its
 * translucent bars and round icons, and the light, pastel 2019. Its alerts wait on the lock and in the shade pulled
 * down from the top. A tablet wears its phone's look of the same year.
 */
@PaletteHolder
public final class DroidixLook implements IMobileLook {

    private final MobileSystem system;
    private final IconStyle style;

    private static final int ROW_PITCH = 34;
    /**
     * Its colours, as the K0.11 pages drew them: the inks of the light look, the shade's, the store's blue, the droid's
     * green, the 2009 lock's slider and foot, the cards, the search bars, the drawer and the dock.
     */
    private static final Palette<Colours> COLOURS = Palettes.declare(JsComputers.MODID, "mobile/droidix",
            new Colours(0xFFFFFFFF, 0xFF000000, 0xFF20242A, 0xFF7A8090, 0xFF7FD0FF, 0xFFA0A8B0, 0xFF1A73E8,
                    0xFF3DDC84, 0xFF06100A, 0xFF1F5A38, 0x28FFFFFF, 0xC8000000, 0xFF1A1A1A, 0xFF4A4A4A, 0xFF5A5A5A,
                    0xFF8A8A8A, 0xFF3A9A3A, 0xFFC0C0C0, 0xD7FFFFFF, 0xFF5A6070, 0xDCFFFFFF, 0xFF6A6A6A, 0xAA000000,
                    0xFF2A2C30, 0xFF6A6E76, 0x3CFFFFFF, 0xE6FFFFFF, 0xFF606060, 0xC8FFFFFF, 0xFF6A7080, 0xFF181A1E,
                    0xFF2A3A44, 0xFF2C3036, 0xFFF4F5F9, 0xFFEEF0F4, 0xFFE8F0FE, 0xFF101010, 0x5A000000, 0xFF7FD04A));

    DroidixLook(final MobileSystem system) {
        this.system = system;
        this.style = system == MobileSystem.DROIDIX_2009 ? IconStyle.DROIDIX_2009
                : system == MobileSystem.DROIDIX_2014 ? IconStyle.DROIDIX_2014 : IconStyle.DROIDIX_2019;
    }

    @Override
    public void paint(final MobileCanvas c, final MobileFrame f) {
        switch (f.page()) {
            case LOCK -> lock(c, f);
            case ALERTS -> shade(c, f);
            case SETTINGS -> settings(c, f);
            case STORE -> store(c, f);
            case APP -> app(c, f);
            case PROGRAMS -> drawer(c, f);
            case HOME, START -> home(c, f);
        }
    }

    /** Its start: the droid over the word, the light running across the word's letters. */
    @Override
    public void boot(final MobileCanvas c, final MobileFrame f, final float progress) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        final float h = c.height();
        c.gradient(0, 0, w, h, k.black(), k.bootBottom());
        c.picture(MobileArt.DROID, MobileArt.DROID_WIDTH, MobileArt.DROID_HEIGHT, w / 2 - 12, h * 0.36F,
                MobileArt.DROID_WIDTH * 2, MobileArt.DROID_HEIGHT * 2);
        final String word = MobileParts.say(MobileFamily.DROIDIX.word().text()).toLowerCase(Locale.ROOT);
        float x = w / 2 - c.width(word, MobileCanvas.BIG) / 2;
        // The light runs over two letters at a time, from the first to the last.
        final int lit = Math.min(word.length() - 2, Math.round(progress * (word.length() - 2)));
        for (int n = 0; n < word.length(); n++) {
            x += c.text(word.substring(n, n + 1), x, h * 0.36F + 26, n == lit || n == lit + 1 ? k.green()
                    : k.wordDark(), MobileCanvas.BIG);
        }
    }

    /** The slider of 2009, the swipe up from the foot after it. */
    @Override
    public Unlock unlock() {
        return system == MobileSystem.DROIDIX_2009 ? Unlock.SLIDE : Unlock.SWIPE_UP;
    }

    @Override
    public boolean pullsAlerts() {
        return true;
    }

    /* The lock: the clock and the day, the alerts waiting, and how it lets go. */
    private void lock(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        wallpaper(c, f);
        final boolean dark = system != MobileSystem.DROIDIX_2019;
        final float top = status(c, f, dark);
        final int ink = dark ? k.white() : k.darkInk();
        final float w = c.width();
        final float h = c.height();
        final List<PortableAlert> unseen = f.unseen();
        if (system == MobileSystem.DROIDIX_2009) {
            c.text(f.clock(), w / 2, top + 22, k.white(), MobileCanvas.HUGE, Align.CENTRE, false);
            c.text(day(f), w / 2, top + 49, k.dayGrey(), MobileCanvas.SMALL, Align.CENTRE, false);
            if (!unseen.isEmpty()) {
                c.rounded(6, top + 68, w - 12, 22, 3, k.alertsBox(), null);
                icon(c, "network", 9, top + 72, 12);
                c.text(MobileParts.say(MobileTexts.NEW_ALERTS.with(unseen.size())), 25, top + 75, k.white(),
                        MobileCanvas.SMALL);
            }
            c.rect(0, h - 32, w, 32, k.dimGround());
            c.rounded(4, h - 24, w - 8, 16, 2, k.track(), k.trackEdge());
            final float tab = 5 + f.slide() * (w - 40);
            c.rounded(w - 35, h - 23, 30, 14, 2, k.endTab(), null);
            c.text(MobileTexts.say(MobileTexts.UNLOCK_WORD), w / 2, h - 19.5F, k.unlockWord(), MobileCanvas.SMALL,
                    Align.CENTRE, false);
            c.rounded(tab, h - 23, 30, 14, 2, k.unlockTab(), null);
            c.glyph(MobileArt.UNLOCK_TAB, tab + 11, h - 20, k.white(), 1);
            return;
        }
        c.text(f.clock(), w / 2, top + 30, ink, MobileCanvas.HUGE, Align.CENTRE, false);
        c.text(day(f), w / 2, top + 57, ink, MobileCanvas.SMALL, Align.CENTRE, false);
        float y = top + 74;
        for (final PortableAlert alert : MobileParts.first(unseen, 2)) {
            c.rounded(6, y, w - 12, 30, 8, k.card(), null);
            icon(c, "network", 10, y + 4, 10);
            c.text(MobileParts.name(MobileApp.NETWORK), 23, y + 5, k.cardWords(), MobileCanvas.SMALL);
            c.text(c.fit(MobileParts.title(alert), w - 22, MobileCanvas.SMALL), 10, y + 17, k.darkInk(),
                    MobileCanvas.SMALL, Align.LEFT, true);
            y += 34;
        }
        c.text(MobileTexts.say(MobileTexts.SWIPE_TO_UNLOCK), w / 2, h - 16 - f.slide() * h / 3, ink,
                MobileCanvas.SMALL, Align.CENTRE, false);
    }

    /* The home: the clock, the apps on a grid, and the year's foot: the drawer, the dock or the search bar. */
    private void home(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        wallpaper(c, f);
        final boolean dark = system != MobileSystem.DROIDIX_2019;
        final float top = status(c, f, dark);
        final int ink = dark ? k.white() : k.darkInk();
        final float w = c.width();
        final float h = c.height();
        final int unseen = f.unseen().size();
        float y = top + 6;
        switch (system) {
            case DROIDIX_2009 -> {
                c.rounded(4, y, w - 8, 13, 2, k.searchBar(), null);
                c.text(MobileTexts.say(MobileTexts.SEARCH), 9, y + 3, k.searchWords(), MobileCanvas.SMALL);
                c.text(f.clock(), w / 2, y + 20, k.white(), MobileCanvas.BIG, Align.CENTRE, false);
                y += 46;
            }
            case DROIDIX_2014 -> {
                c.text(f.clock(), w / 2, y + 2, k.white(), MobileCanvas.BIG, Align.CENTRE, false);
                c.text(MobileParts.say(MobileTexts.DAY_AND_ALERTS.with(f.day(), unseen)), w / 2, y + 20, k.white(),
                        MobileCanvas.SMALL, Align.CENTRE, false);
                y += 34;
            }
            default -> {
                c.text(f.clock(), 8, y + 4, ink, MobileCanvas.BIG);
                c.text(day(f), 8, y + 22, ink, MobileCanvas.SMALL);
                y += 38;
            }
        }
        grid(c, homeTiles(f), y, ink, unseen);
        switch (system) {
            case DROIDIX_2009 -> {
                c.rect(0, h - 22, w, 22, k.drawerFoot());
                c.rounded(w / 2 - 12, h - 19, 24, 16, 4, k.drawerButton(), k.drawerEdge());
                c.glyph(MobileArt.DRAWER, w / 2 - 3, h - 14, k.white(), 1);
                c.press(w / 2 - 12, h - 19, 24, 16, MobileTap.PROGRAMS);
            }
            case DROIDIX_2014 -> {
                c.rect(0, h - 34, w, 1, k.dockLine());
                final String[] dock = {"network", "camera", null, "gallery", "settings"};
                for (int n = 0; n < dock.length; n++) {
                    final float cx = w / 5.0F * n + w / 10.0F;
                    final MobileApp app = dock[n] == null ? null : appOf(dock[n]);
                    if (dock[n] == null) {
                        c.disc(cx, h - 17, 9, k.drawerDisc());
                        c.glyph(MobileArt.DRAWER, cx - 2.5F, h - 19.5F, k.drawerDots(), 1);
                        c.press(cx - 10, h - 27, 20, 20, MobileTap.PROGRAMS);
                    } else if (app == null || f.installed().contains(app)) {
                        icon(c, dock[n], cx - 10, h - 27, style.size());
                        c.press(cx - 10, h - 27, 20, 20, app == null ? MobileTap.SETTINGS : MobileTap.open(app));
                    }
                }
            }
            default -> {
                c.rounded(6, h - 17, w - 12, 11, 5, k.lightBar(), null);
                c.text(MobileTexts.say(MobileTexts.SEARCH), 12, h - 14.5F, k.lightBarWords(), MobileCanvas.SMALL);
            }
        }
    }

    /* The app drawer: every app on the device, with the store and the settings. */
    private void drawer(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        wallpaper(c, f);
        final boolean dark = system != MobileSystem.DROIDIX_2019;
        final float top = status(c, f, dark);
        c.rect(0, top, c.width(), c.height() - top, dark ? k.dimGround() : k.lightBar());
        final List<Tile> tiles = new ArrayList<>();
        for (final MobileApp app : f.apps()) {
            tiles.add(new Tile(MobileArt.glyph(app), MobileParts.label(app), MobileTap.open(app),
                    app == MobileApp.ALERTS));
        }
        tiles.add(new Tile("store", MobileParts.say(MobileFamily.DROIDIX.store().text()), MobileTap.STORE, false));
        tiles.add(new Tile("settings", MobileTexts.say(MobileTexts.SETTINGS), MobileTap.SETTINGS, false));
        grid(c, tiles, top + 8, dark ? k.white() : k.darkInk(), f.unseen().size());
    }

    /* The shade pulled over the home: the time, the quick toggles, the alerts, and CLEAR ALL. */
    private void shade(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        c.rect(0, 0, w, c.height(), k.shadeGround());
        c.text(f.clock(), 5, 5, k.white(), MobileCanvas.BIG);
        c.text(day(f), 5, 22, k.shadeGrey(), MobileCanvas.SMALL);
        final Quick[] quick = {new Quick(MobileArt.NETWORK_GLYPH, MobileTap.open(MobileApp.NETWORK)),
            new Quick(MobileArt.SETTINGS_GLYPH, MobileTap.SETTINGS),
            new Quick(MobileArt.ALERTS_GLYPH, MobileTap.open(MobileApp.ALERTS))};
        // Where a camera's hole is in the glass beside the clock, the toggles go down beside the day, clear of it.
        final boolean below = PortableFace.of(f.model()).cutout() > 0;
        final float ty = below ? 28 : 12;
        for (int n = 0; n < quick.length; n++) {
            final float cx = w - 12 - n * 16;
            c.disc(cx, ty, 6, k.toggle());
            c.glyph(quick[n].glyph(), cx - 6, ty - 6, k.shadeBlue(), 1);
            c.press(cx - 7, ty - 7, 14, 14, quick[n].tap());
        }
        float y = below ? 40 : 34;
        final List<PortableAlert> alerts = f.alerts();
        final int rows = (int) ((c.height() - 50) / 30);
        for (int n = f.scroll(); n < alerts.size() && n < f.scroll() + rows; n++) {
            final PortableAlert alert = alerts.get(n);
            c.rect(0, y, w, 28, k.shadeRow());
            c.disc(11, y + 11, 6, MobileArt.colour(MobileApp.NETWORK));
            c.glyph(alert.kind() == AlertKind.ITEMS ? MobileArt.NETWORK_GLYPH : MobileArt.ALERTS_GLYPH, 5, y + 5,
                    k.white(), 1);
            c.text(c.fit(MobileParts.title(alert), w - 26, MobileCanvas.SMALL), 22, y + 5, k.white(),
                    MobileCanvas.SMALL, Align.LEFT, true);
            c.text(c.fit(MobileParts.detail(alert), w - 26, MobileCanvas.SMALL), 22, y + 15, k.shadeGrey(),
                    MobileCanvas.SMALL);
            c.press(0, y, w, 28, MobileTap.open(MobileApp.ALERTS));
            y += 30;
        }
        if (alerts.isEmpty()) {
            c.text(MobileTexts.say(MobileTexts.NO_ALERTS), w / 2, y + 4, k.shadeGrey(), MobileCanvas.SMALL,
                    Align.CENTRE, false);
        } else {
            final String clear = MobileTexts.say(MobileTexts.CLEAR_ALL);
            final float wide = c.width(clear, MobileCanvas.SMALL);
            c.text(clear, w - 4, y + 4, k.shadeBlue(), MobileCanvas.SMALL, Align.RIGHT, false);
            c.press(w - 8 - wide, y, wide + 8, 12, MobileTap.CLEAR_ALERTS);
        }
    }

    /* The settings, in white cards on the pale ground: the connections, the alerts, the battery, the store, about. */
    private void settings(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        final PortableModel model = f.model();
        c.rect(0, 0, w, c.height(), k.settingsGround());
        status(c, f, false);
        c.text(MobileTexts.say(MobileTexts.SETTINGS), 8, 34, k.darkInk(), MobileCanvas.BIG);
        float y = 60;
        y = card(c, y, new SettingRow[] {
            new SettingRow("network", MobileTexts.say(MobileTexts.CONNECTIONS), f.link() == null
                    ? MobileTexts.say(MobileTexts.NOT_CONNECTED)
                    : MobileParts.say(MobileTexts.WIFI_THROUGH.with(f.link().accessPoint())), null),
            new SettingRow("alerts", MobileTexts.say(MobileTexts.NOTIFICATIONS),
                    MobileParts.say(MobileTexts.KINDS_ON.with(AlertKind.values().length)),
                    MobileTap.open(MobileApp.ALERTS))});
        y = card(c, y, new SettingRow[] {
            new SettingRow("settings", MobileTexts.say(MobileTexts.BATTERY_WORD),
                    MobileParts.say(MobileTexts.BATTERY_LEFT.with(f.battery(), MobileParts.timeLeft(f))), null),
            new SettingRow("gallery", MobileTexts.say(MobileTexts.STORAGE), MobileParts.storage(model), null)});
        card(c, y, new SettingRow[] {
            new SettingRow("store", MobileParts.say(MobileFamily.DROIDIX.store().text()),
                    MobileTexts.say(MobileTexts.FROM_THE_MIRROR), MobileTap.STORE),
            new SettingRow("clock", MobileTexts.say(MobileTexts.ABOUT_DEVICE), MobileParts.deviceName(model),
                    null)});
    }

    /* The Droidix Store: what the Mirror serves for Droidix, each free, to install or to open. */
    private void store(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        c.rect(0, 0, w, c.height(), k.white());
        status(c, f, false);
        c.rounded(5, 15, w - 10, 13, 6, k.storeSearch(), null);
        c.text(MobileParts.say(MobileFamily.DROIDIX.store().text()), 12, 18, k.cardWords(), MobileCanvas.SMALL);
        c.text(MobileTexts.say(f.mirror() ? MobileTexts.FROM_THE_MIRROR : MobileTexts.MIRROR_OUT_OF_REACH), 6, 34,
                f.mirror() ? k.darkInk() : k.softInk(), MobileCanvas.NORMAL, Align.LEFT, f.mirror());
        final List<MobileApp> apps = f.storeApps();
        final int rows = (int) ((c.height() - 48) / 29);
        float y = 48;
        for (int n = f.scroll(); n < apps.size() && n < f.scroll() + rows; n++) {
            final MobileApp app = apps.get(n);
            final boolean installed = f.installed().contains(app);
            final String word = MobileTexts.say(installed ? MobileTexts.OPEN : MobileTexts.INSTALL);
            final float bw = c.width(word, MobileCanvas.SMALL) + 8;
            icon(c, MobileArt.glyph(app), 6, y, style.size());
            c.text(c.fit(MobileParts.name(app), w - bw - 40, MobileCanvas.SMALL), 32, y + 3, k.darkInk(),
                    MobileCanvas.SMALL, Align.LEFT, true);
            c.text(MobileTexts.say(MobileTexts.FREE), 32, y + 12, k.softInk(), MobileCanvas.SMALL);
            if (installed || f.mirror()) {
                c.rounded(w - bw - 5, y + 6, bw, 11, 5, installed ? k.openFill() : k.storeBlue(), null);
                c.text(word, w - bw / 2 - 5, y + 8.5F, installed ? k.storeBlue() : k.white(), MobileCanvas.SMALL,
                        Align.CENTRE, false);
                c.press(w - bw - 5, y + 6, bw, 11, installed ? MobileTap.open(app) : MobileTap.install(app));
            }
            y += 29;
        }
    }

    /* An app: its bar in its colour with its name, its icon in its window. */
    private void app(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        final MobileApp app = f.app() == null ? MobileApp.NETWORK : f.app();
        final float w = c.width();
        c.rect(0, 0, w, c.height(), k.white());
        final float top = status(c, f, system != MobileSystem.DROIDIX_2019);
        c.rect(0, top, w, 18, MobileArt.colour(app));
        c.text(MobileParts.name(app), 6, top + 5, k.white(), MobileCanvas.NORMAL);
        icon(c, MobileArt.glyph(app), w / 2 - style.size() / 2.0F, c.height() / 2.0F - 24, style.size());
        c.text(MobileParts.name(app), w / 2, c.height() / 2.0F + 2, k.softInk(), MobileCanvas.SMALL, Align.CENTRE,
                false);
    }

    /* Icons on a grid of four (more across a tablet), each with its name; the badge on Alerts. */
    private void grid(final MobileCanvas c, final List<Tile> tiles, final float top, final int ink,
                      final int unseen) {
        final float w = c.width();
        final int cols = w < 150 ? 4 : w < 250 ? 5 : 6;
        final float cell = w / cols;
        final int size = style.size();
        for (int n = 0; n < tiles.size(); n++) {
            final Tile tile = tiles.get(n);
            final float cx = cell * (n % cols) + cell / 2;
            final float cy = top + (n / cols) * ROW_PITCH;
            icon(c, tile.glyph(), cx - size / 2.0F, cy, size);
            c.text(c.fit(tile.name(), cell - 1, MobileCanvas.SMALL), cx, cy + 23, ink, MobileCanvas.SMALL,
                    Align.CENTRE, false);
            if (tile.badged()) {
                MobileParts.badge(c, cx + size / 2.0F - 1, cy + 1, unseen);
            }
            c.press(cx - cell / 2, cy, cell, ROW_PITCH - 2, tile.tap());
        }
    }

    /* The apps the home's grid shows in the look's year: all of them on the first, the store at the end on the last. */
    private List<Tile> homeTiles(final MobileFrame f) {
        final List<Tile> tiles = new ArrayList<>();
        for (final MobileApp app : f.apps()) {
            tiles.add(new Tile(MobileArt.glyph(app), MobileParts.label(app), MobileTap.open(app),
                    app == MobileApp.ALERTS));
        }
        if (system != MobileSystem.DROIDIX_2014) {
            tiles.add(new Tile("store", MobileTexts.say(MobileTexts.STORE), MobileTap.STORE, false));
        }
        if (system == MobileSystem.DROIDIX_2009) {
            tiles.add(new Tile("settings", MobileTexts.say(MobileTexts.SETTINGS), MobileTap.SETTINGS, false));
        }
        return tiles;
    }

    /* A card of settings' rows; returns where the next card starts. */
    private float card(final MobileCanvas c, final float top, final SettingRow[] rows) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        c.rounded(5, top, w - 10, rows.length * 24 + 4, 8, k.white(), null);
        for (int n = 0; n < rows.length; n++) {
            final SettingRow row = rows[n];
            final float y = top + 4 + n * 24;
            icon(c, row.glyph(), 9, y + 2, 14);
            c.text(row.name(), 27, y + 3, k.darkInk(), MobileCanvas.SMALL, Align.LEFT, true);
            c.text(c.fit(row.value(), w - 36, MobileCanvas.SMALL), 27, y + 12, k.softInk(), MobileCanvas.SMALL);
            if (row.tap() != null) {
                c.press(5, y, w - 10, 24, row.tap());
            }
        }
        return top + rows.length * 24 + 10;
    }

    /* The status bar of the look's year; returns where the page under it starts. */
    private float status(final MobileCanvas c, final MobileFrame f, final boolean dark) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        final int ink = dark ? k.white() : k.statusInk();
        if (system == MobileSystem.DROIDIX_2019) {
            c.text(f.clock(), 4, 2.5F, ink, MobileCanvas.SMALL);
            MobileParts.battery(c, w - 15, 3, ink, f.battery(), null);
            MobileParts.wifi(c, f, w - 28, 3, ink);
            return 12;
        }
        c.rect(0, 0, w, 10, system == MobileSystem.DROIDIX_2009 ? k.black() : k.statusBand());
        c.text(f.clock(), w - 2, 1.5F, k.white(), MobileCanvas.SMALL, Align.RIGHT, false);
        MobileParts.battery(c, w - 32, 2, k.white(), f.battery(),
                system == MobileSystem.DROIDIX_2009 ? k.batteryFill() : null);
        MobileParts.wifi(c, f, w - 45, 2, k.white());
        return 10;
    }

    private void wallpaper(final MobileCanvas c, final MobileFrame f) {
        final PortableModel model = f.model();
        c.picture(MobileArt.wallpaper(model), model.screenWidth() * 2, model.screenHeight() * 2, 0, 0, c.width(),
                c.height());
    }

    /* An icon of the look's style, drawn {@code size} pixels square. */
    private void icon(final MobileCanvas c, final String glyph, final float x, final float y, final float size) {
        final int texels = style.size() * 2;
        c.picture(MobileArt.icon(style, glyph), texels, texels, x, y, size, size);
    }

    /* The app a dock's glyph names; null for the settings, which are no app. */
    @Nullable
    private static MobileApp appOf(final String glyph) {
        for (final MobileApp app : MobileApp.values()) {
            if (MobileArt.glyph(app).equals(glyph)) {
                return app;
            }
        }
        return null;
    }

    private static String day(final MobileFrame f) {
        return MobileTexts.say(MobileTexts.DAY.with(f.day()));
    }

    /* An icon on the grid: its glyph, its name, what pressing it does, and whether it bears the badge. */
    private record Tile(String glyph, String name, MobileTap tap, boolean badged) {
    }

    /* A row of the settings: its icon, its name, its value, and what pressing it does. */
    private record SettingRow(String glyph, String name, String value, @Nullable MobileTap tap) {
    }

    /* A quick toggle of the shade: its glyph and what pressing it opens. */
    private record Quick(String[] glyph, MobileTap tap) {
    }

    /** Droidix's colours, as the palette above names them. */
    private record Colours(int white, int black, int darkInk, int softInk, int shadeBlue, int shadeGrey,
                           int storeBlue, int green, int bootBottom, int wordDark, int alertsBox, int dimGround,
                           int track, int trackEdge, int endTab, int unlockWord, int unlockTab, int dayGrey, int card,
                           int cardWords, int searchBar, int searchWords, int drawerFoot, int drawerButton,
                           int drawerEdge, int dockLine, int drawerDisc, int drawerDots, int lightBar,
                           int lightBarWords, int shadeGround, int toggle, int shadeRow, int settingsGround,
                           int storeSearch, int openFill, int statusInk, int statusBand, int batteryFill) {
    }
}
