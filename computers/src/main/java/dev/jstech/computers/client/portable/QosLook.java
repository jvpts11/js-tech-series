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
import org.jetbrains.annotations.Nullable;

/**
 * Quince's qOS on the qPhones and the qPads (computers/docs/design/wireless.md), in three looks as the desktop has
 * them: the black bar, the glossy icons and slide to unlock of 2007; the flat, translucent bars and the Notification
 * Center of 2013; and from 2017 the bar round the notch, the cards on the lock and the widgets. A tablet wears its
 * phone's look of the same year.
 */
@PaletteHolder
public final class QosLook implements IMobileLook {

    private final MobileSystem system;
    private final IconStyle style;
    private final IAppKit kit;

    private static final int ICON = 22;
    private static final int ROW_PITCH = 34;
    private static final int DOCK_H = 30;
    /* The apps the dock keeps from 2013 on, and the 2007 dock's, by name; the home grid shows the rest. */
    private static final List<String> DOCK = List.of("network", "alerts", "messenger", "camera");
    private static final List<String> DOCK_2007 = List.of("network", "alerts", "camera", "settings");
    /**
     * Its colours, as the K0.11 pages drew them: the lock's bands, slider and alerts, the cards, the docks and the
     * widgets over the wallpaper, the Notification Center, the settings' greys and the store's blue.
     */
    private static final Palette<Colours> COLOURS = Palettes.declare(JsComputers.MODID, "mobile/qos",
            new Colours(0xFFFFFFFF, 0xFF000000, 0xFF8E8E93, 0xFF2F7EF0, 0xFFEFEFF4, 0xFFE0E0E4, 0xFF4A4A4E,
                    0x96000000, 0x3CFFFFFF, 0xAA000000, 0x5AFFFFFF, 0xFF2A2A2E, 0xFF5A5A60, 0xFFD8D8DC, 0xFF707078,
                    0xFF9A9AA0, 0xC8FFFFFF, 0xAAFFFFFF, 0xDCFFFFFF, 0x5A000000, 0xBEFFFFFF, 0xA0FFFFFF, 0x6E000000,
                    0x28FFFFFF, 0x46FFFFFF, 0x14FFFFFF, 0x78FFFFFF, 0x32FFFFFF, 0x2DFFFFFF, 0xD2FFFFFF, 0xFFFFCC60,
                    0xE10C0C10, 0x8CFFFFFF, 0xFFC8C7CC, 0xFF6D6D72, 0xFF101010, 0xFFC7C7CC, 0xFFE8E8ED, 0xFFF0F0F6,
                    0xFFE8E8EC, 0xFFF7F7F7, 0xFF4CD964, 0x55FFFFFF, 0x55101010, 0x3CFFFFFF));

    QosLook(final MobileSystem system) {
        this.system = system;
        this.style = system == MobileSystem.QOS_2007 ? IconStyle.QOS_2007
                : system == MobileSystem.QOS_2013 ? IconStyle.QOS_2013 : IconStyle.QOS_2017;
        this.kit = IAppKit.of(system);
    }

    @Override
    public void paint(final MobileCanvas c, final MobileFrame f) {
        switch (f.page()) {
            case LOCK -> lock(c, f);
            case ALERTS -> {
                home(c, f);
                centre(c, f);
            }
            case SETTINGS -> settings(c, f);
            case STORE -> store(c, f);
            case APP -> app(c, f);
            case HOME, START, PROGRAMS -> home(c, f);
        }
    }

    /** Its start: the fruit, and the bar filling under it. */
    @Override
    public void boot(final MobileCanvas c, final MobileFrame f, final float progress) {
        final Colours k = COLOURS.get();
        final PortableModel model = f.model();
        c.picture(MobileArt.start(model), model.screenWidth() * 2, model.screenHeight() * 2, 0, 0, c.width(),
                c.height());
        final float x = c.width() / 2.0F - 22;
        final float y = c.height() * 0.68F;
        c.rounded(x, y, 44, 3, 1.5F, k.bootTrough(), null);
        c.rounded(x, y, Math.max(3, 44 * progress), 3, 1.5F, k.white(), null);
    }

    @Override
    public Unlock unlock() {
        return system == MobileSystem.QOS_2017 ? Unlock.SWIPE_UP : Unlock.SLIDE;
    }

    /** The Notification Center came with the 2013 look; the first qOS showed its alerts on the lock alone. */
    @Override
    public boolean pullsAlerts() {
        return system != MobileSystem.QOS_2007;
    }

    /* The lock: the clock and the day, the newest alerts, and how it lets go. */
    private void lock(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        wallpaper(c, f);
        final float top = status(c, f, true);
        final List<PortableAlert> unseen = f.unseen();
        final float w = c.width();
        final float h = c.height();
        if (system == MobileSystem.QOS_2007) {
            c.rect(0, top, w, 42, k.lockBand());
            c.rect(0, top + 42, w, 1, k.lockBandLine());
            c.text(f.clock(), w / 2, top + 4, k.white(), MobileCanvas.HUGE, Align.CENTRE, false);
            c.text(day(f), w / 2, top + 31, k.white(), MobileCanvas.SMALL, Align.CENTRE, false);
            if (!unseen.isEmpty()) {
                c.rounded(8, top + 56, w - 16, 30, 6, k.alertBox(), k.alertBoxEdge());
                c.text(MobileParts.name(MobileApp.NETWORK), 14, top + 60, k.white(), MobileCanvas.SMALL,
                        Align.LEFT, true);
                c.text(c.fit(MobileParts.title(unseen.getFirst()), w - 28, MobileCanvas.SMALL), 14, top + 71,
                        k.white(), MobileCanvas.SMALL);
            }
            c.rect(0, h - 34, w, 34, k.lockBand());
            c.rounded(8, h - 26, w - 16, 18, 5, k.sliderTrack(), k.sliderEdge());
            final float knob = 10 + f.slide() * (w - 46);
            c.rounded(knob, h - 24, 26, 14, 4, k.knob(), null);
            c.glyph(MobileArt.SLIDER_ARROW, knob + 11, h - 20, k.knobArrow(), 1);
            if (f.slide() < 0.2F) {
                c.text(MobileTexts.say(MobileTexts.SLIDE_TO_UNLOCK), 40, h - 21, k.sliderWords(), MobileCanvas.SMALL);
            }
            return;
        }
        final boolean cards = system == MobileSystem.QOS_2017;
        c.text(f.clock(), w / 2, top + (cards ? 14 : 10), k.white(), MobileCanvas.HUGE, Align.CENTRE, false);
        c.text(day(f), w / 2, top + (cards ? 41 : 37), k.white(), MobileCanvas.SMALL, Align.CENTRE, false);
        float y = top + (cards ? 60 : 54);
        for (final PortableAlert alert : MobileParts.first(unseen, cards ? 3 : 2)) {
            final String when = f.justNow(alert) ? MobileTexts.say(MobileTexts.NOW) : f.clock();
            if (cards) {
                c.rounded(5, y, w - 10, 34, 7, k.card(), null);
                icon(c, MobileArt.glyph(MobileApp.NETWORK), 9, y + 4, 10);
                c.text(MobileTexts.say(MobileTexts.NETWORK_CAPS), 22, y + 5, k.capsBright(), MobileCanvas.SMALL);
                c.text(when, w - 9, y + 5, k.whenBright(), MobileCanvas.SMALL, Align.RIGHT, false);
                c.text(c.fit(MobileParts.title(alert), w - 18, MobileCanvas.SMALL), 9, y + 16, k.white(),
                        MobileCanvas.SMALL, Align.LEFT, true);
                c.text(c.fit(MobileParts.detail(alert), w - 18, MobileCanvas.SMALL), 9, y + 25, k.detailBright(),
                        MobileCanvas.SMALL);
                y += 38;
            } else {
                c.rect(0, y, w, 30, k.row());
                icon(c, MobileArt.glyph(MobileApp.NETWORK), 4, y + 4, 10);
                c.text(MobileTexts.say(MobileTexts.NETWORK_CAPS), 17, y + 5, k.caps(), MobileCanvas.SMALL);
                c.text(when, w - 4, y + 5, k.when(), MobileCanvas.SMALL, Align.RIGHT, false);
                c.text(c.fit(MobileParts.title(alert), w - 8, MobileCanvas.SMALL), 4, y + 17, k.white(),
                        MobileCanvas.SMALL);
                y += 32;
            }
        }
        if (cards) {
            // The torch and the camera, and the home bar to swipe up from.
            for (final float x : new float[] {12, w - 24}) {
                c.disc(x + 6, h - 20, 8, k.torch());
            }
            c.glyph(MobileArt.TORCH, 16, h - 23, k.white(), 1);
            c.glyph(MobileArt.CAMERA, w - 24, h - 24, k.white(), 1);
            c.rounded(w / 2 - 18, h - 4, 36, 2, 1, k.white(), null);
        } else {
            c.text(MobileTexts.say(MobileTexts.SLIDE_TO_UNLOCK_ARROW), w / 2 + f.slide() * w / 2, h - 14, k.white(),
                    MobileCanvas.SMALL, Align.CENTRE, false);
        }
    }

    /* The home: the apps on a grid, the dock under them, the network's widget from 2017, the badge on Alerts. */
    private void home(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        wallpaper(c, f);
        final float top = status(c, f, true);
        final float w = c.width();
        final float h = c.height();
        final int cols = w < 150 ? 4 : 5;
        final float cell = w / cols;
        final boolean tablet = w >= 150;
        final List<Tile> grid = new ArrayList<>();
        final List<Tile> dock = new ArrayList<>();
        final List<String> docked = system == MobileSystem.QOS_2007 ? DOCK_2007 : DOCK;
        for (final MobileApp app : f.apps()) {
            final Tile tile = new Tile(MobileArt.glyph(app), MobileParts.label(app), MobileTap.open(app),
                    app == MobileApp.ALERTS);
            if (docked.contains(tile.glyph())) {
                dock.add(tile);
                if (system == MobileSystem.QOS_2007) {
                    grid.add(tile);
                }
            } else {
                grid.add(tile);
            }
        }
        grid.add(new Tile("store", MobileTexts.say(MobileTexts.STORE), MobileTap.STORE, false));
        final Tile settings = new Tile("settings", MobileTexts.say(MobileTexts.SETTINGS), MobileTap.SETTINGS, false);
        grid.add(settings);
        if (system == MobileSystem.QOS_2007) {
            dock.add(settings);
        }
        float y = top + 8;
        final int unseen = f.unseen().size();
        if (system == MobileSystem.QOS_2017 && !tablet) {
            c.rounded(6, y, w - 12, 34, 7, k.widget(), null);
            icon(c, "network", 10, y + 4, 12);
            c.text(MobileParts.say(MobileTexts.NETWORK_HEADER.with(MobileParts.network(f))), 25, y + 6,
                    k.capsBright(), MobileCanvas.SMALL);
            c.text(MobileParts.count(unseen), 10, y + 18, k.white(), MobileCanvas.SMALL);
            c.press(6, y, w - 12, 34, MobileTap.open(MobileApp.NETWORK));
            y += 42;
        }
        for (int n = 0; n < grid.size(); n++) {
            final Tile tile = grid.get(n);
            final float cx = cell * (n % cols) + cell / 2;
            final float cy = y + (n / cols) * ROW_PITCH;
            icon(c, tile.glyph(), cx - ICON / 2.0F, cy, ICON);
            c.text(c.fit(tile.name(), cell - 1, MobileCanvas.SMALL), cx, cy + 24, k.white(), MobileCanvas.SMALL,
                    Align.CENTRE, false);
            if (tile.badged()) {
                MobileParts.badge(c, cx + 10, cy + 1, unseen);
            }
            c.press(cx - cell / 2, cy, cell, ROW_PITCH - 2, tile.tap());
        }
        final float dy = h - DOCK_H - (system == MobileSystem.QOS_2017 ? 6 : 0);
        switch (system) {
            case QOS_2007 -> {
                c.gradient(0, dy, w, DOCK_H, k.dockTop(), k.dockBottom());
                c.rect(0, dy, w, 1, k.dockLine());
            }
            case QOS_2013 -> c.rect(0, dy, w, DOCK_H, k.card());
            default -> c.rounded(4, dy, w - 8, DOCK_H, 9, k.dock(), null);
        }
        final float dcell = w / 4.0F;
        for (int n = 0; n < dock.size() && n < 4; n++) {
            final float cx = dcell * n + dcell / 2;
            icon(c, dock.get(n).glyph(), cx - ICON / 2.0F, dy + 4, ICON);
            if (dock.get(n).badged()) {
                MobileParts.badge(c, cx + 10, dy + 5, unseen);
            }
            c.press(cx - dcell / 2, dy, dcell, DOCK_H, dock.get(n).tap());
        }
        if (system == MobileSystem.QOS_2017 && tablet) {
            // The tablet's widget, wider, under the apps.
            final float wy = y + ((grid.size() + cols - 1) / cols) * ROW_PITCH + 6;
            c.rounded(6, wy, w - 12, 22, 9, k.tabletWidget(), null);
            icon(c, "network", 11, wy + 5, 12);
            c.text(MobileParts.say(MobileTexts.NETWORK_HEADER.with(MobileParts.network(f))), 26, wy + 7,
                    k.tabletCaps(), MobileCanvas.SMALL);
            c.text(MobileParts.count(unseen), w - 12, wy + 7, k.widgetAlerts(), MobileCanvas.SMALL, Align.RIGHT,
                    false);
            c.press(6, wy, w - 12, 22, MobileTap.open(MobileApp.NETWORK));
        }
        if (system == MobileSystem.QOS_2017) {
            c.rounded(w / 2 - 18, h - 4, 36, 2, 1, k.white(), null);
        }
    }

    /* The Notification Center pulled down over the home: its tabs, the network's alerts, the handle to put it away. */
    private void centre(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        c.rect(0, 0, w, c.height(), k.centreGround());
        status(c, f, true);
        final float tx = (w - 96) / 2;
        c.rounded(tx, 13, 96, 12, 3, null, k.white());
        final String[] tabs = {MobileTexts.say(MobileTexts.TODAY), MobileTexts.say(MobileTexts.ALL),
            MobileTexts.say(MobileTexts.MISSED)};
        for (int n = 0; n < tabs.length; n++) {
            if (n == f.tab()) {
                c.rect(tx + n * 32, 13, 32, 12, k.white());
            }
            c.text(tabs[n], tx + 16 + n * 32, 15.5F, n == f.tab() ? k.black() : k.white(), MobileCanvas.SMALL,
                    Align.CENTRE, false);
            c.press(tx + n * 32, 13, 32, 12, MobileTap.tab(n));
        }
        c.text(MobileParts.say(MobileTexts.NETWORK_HEADER.with(MobileParts.network(f))), 4, 32, k.whenBright(),
                MobileCanvas.SMALL);
        float y = 42;
        final List<PortableAlert> listed = f.listed();
        final int rows = (int) ((c.height() - 50) / 24);
        for (int n = f.scroll(); n < listed.size() && n < f.scroll() + rows; n++) {
            final PortableAlert alert = listed.get(n);
            icon(c, "network", 4, y + 2, 10);
            c.text(c.fit(MobileParts.title(alert), w - 22, MobileCanvas.SMALL), 17, y + 2, k.white(),
                    MobileCanvas.SMALL, Align.LEFT, true);
            c.text(c.fit(MobileParts.detail(alert), w - 22, MobileCanvas.SMALL), 17, y + 11, k.caps(),
                    MobileCanvas.SMALL);
            c.press(0, y, w, 22, MobileTap.open(MobileApp.ALERTS));
            y += 24;
            c.rect(17, y - 3, w - 17, 1, k.widget());
        }
        c.rounded(w / 2 - 10, c.height() - 6, 20, 2, 1, k.handle(), null);
        c.press(0, c.height() - 12, w, 12, MobileTap.BACK);
    }

    /* The settings, grouped in white on grey: the Wi-Fi, the alerts, the battery, the store, the storage, about. */
    private void settings(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        final PortableModel model = f.model();
        c.rect(0, 0, w, c.height(), k.settingsGround());
        status(c, f, false);
        c.text(MobileTexts.say(MobileTexts.SETTINGS), w / 2, 13, k.black(), MobileCanvas.NORMAL, Align.CENTRE, true);
        c.rect(0, 25, w, 1, k.settingsLine());
        float y = 30;
        y = group(c, y, new SettingRow[] {
            new SettingRow("network", MobileTexts.say(MobileTexts.WIFI), MobileParts.accessPoint(f), null),
            new SettingRow("alerts", MobileTexts.say(MobileTexts.ALERTS_ROW), "", MobileTap.open(MobileApp.ALERTS)),
            new SettingRow("settings", MobileTexts.say(MobileTexts.BATTERY_WORD), MobileParts.say(
                    MobileTexts.PERCENT.with(f.battery())), null)});
        y = group(c, y, new SettingRow[] {
            new SettingRow("store", MobileTexts.say(MobileTexts.STORE), "", MobileTap.STORE),
            new SettingRow("gallery", MobileTexts.say(MobileTexts.STORAGE), MobileParts.storage(model), null)});
        c.text(MobileTexts.say(MobileTexts.ABOUT_CAPS), 6, y, k.aboutWords(), MobileCanvas.SMALL);
        y += 9;
        final String[][] about = {
            {MobileTexts.say(MobileTexts.MODEL), MobileParts.deviceName(model)},
            {MobileTexts.say(MobileTexts.CHIP), MobileParts.say(model.chip())},
            {MobileTexts.say(MobileTexts.MEMORY), MobileParts.say(PortableModel.size(model.memoryMb()))}};
        c.rect(0, y, w, about.length * 15 + 1, k.white());
        for (int n = 0; n < about.length; n++) {
            c.text(about[n][0], 6, y + 4 + n * 15, k.black(), MobileCanvas.SMALL);
            c.text(c.fit(about[n][1], w / 2, MobileCanvas.SMALL), w - 5, y + 4 + n * 15, k.grey(),
                    MobileCanvas.SMALL, Align.RIGHT, false);
            if (n < about.length - 1) {
                c.rect(6, y + 14 + n * 15, w - 6, 1, k.rowLine());
            }
        }
    }

    /* The Quince Store: what the Mirror serves for the qOS, to get or, when on the device, to open. */
    private void store(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        c.rect(0, 0, w, c.height(), k.white());
        status(c, f, false);
        c.text(MobileParts.say(MobileFamily.QOS.store().text()), 6, 13, k.black(), MobileCanvas.NORMAL, Align.LEFT,
                true);
        c.rounded(4, 24, w - 8, 11, 3, k.search(), null);
        c.text(MobileTexts.say(f.mirror() ? MobileTexts.SEARCH_THE_MIRROR : MobileTexts.MIRROR_OUT_OF_REACH), 9,
                26.5F, k.grey(), MobileCanvas.SMALL);
        final List<MobileApp> apps = f.storeApps();
        final int rows = (int) ((c.height() - 40) / 25);
        float y = 40;
        for (int n = f.scroll(); n < apps.size() && n < f.scroll() + rows; n++) {
            final MobileApp app = apps.get(n);
            final boolean installed = f.installed().contains(app);
            icon(c, MobileArt.glyph(app), 4, y, ICON);
            c.text(c.fit(MobileParts.name(app), w - 62, MobileCanvas.SMALL), 29, y + 3, k.black(),
                    MobileCanvas.SMALL);
            c.text(MobileTexts.say(MobileTexts.THE_MIRROR), 29, y + 12, k.grey(), MobileCanvas.SMALL);
            if (installed || f.mirror()) {
                final String word = MobileTexts.say(installed ? MobileTexts.OPEN_CAPS : MobileTexts.GET_CAPS);
                final float bw = c.width(word, MobileCanvas.SMALL) + 8;
                c.rounded(w - bw - 4, y + 6, bw, 10, 3, installed ? k.openFill() : null, k.storeBlue());
                c.text(word, w - bw / 2 - 4, y + 8.5F, k.storeBlue(), MobileCanvas.SMALL, Align.CENTRE, false);
                c.press(w - bw - 4, y + 6, bw, 10, installed ? MobileTap.open(app) : MobileTap.install(app));
            }
            y += 25;
            c.rect(29, y - 2, w - 29, 1, k.storeLine());
        }
    }

    /*
     * An app, drawn by its own view in the qOS's kit under the status bar, which is drawn again over a page that fills
     * the screen, as the maps do; one whose inside comes later shows its name on its bar and its icon in its window.
     */
    private void app(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        final MobileApp app = f.app() == null ? MobileApp.NETWORK : f.app();
        if (MobileApps.of(app) != null) {
            kit.ground(c, app);
            MobileApps.paint(c, f, kit, status(c, f, kit.dark(app)));
            status(c, f, kit.dark(app));
            return;
        }
        final float w = c.width();
        c.rect(0, 0, w, c.height(), k.white());
        final float top = status(c, f, false);
        c.rect(0, top, w, 16, k.appBar());
        c.rect(0, top + 16, w, 1, k.settingsLine());
        c.text(MobileParts.name(app), w / 2, top + 4, k.black(), MobileCanvas.NORMAL, Align.CENTRE, true);
        icon(c, MobileArt.glyph(app), w / 2 - ICON / 2.0F, c.height() / 2.0F - 24, ICON);
        c.text(MobileParts.name(app), w / 2, c.height() / 2.0F + 2, k.grey(), MobileCanvas.SMALL, Align.CENTRE,
                false);
    }

    /* A group of settings' rows, white on the grey; returns where the next group starts. */
    private float group(final MobileCanvas c, final float top, final SettingRow[] rows) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        float y = top;
        c.rect(0, y, w, rows.length * 15, k.white());
        for (int n = 0; n < rows.length; n++) {
            final SettingRow row = rows[n];
            icon(c, row.glyph(), 4, y + 2, 11);
            c.text(row.name(), 19, y + 4, k.black(), MobileCanvas.SMALL);
            c.text(c.fit(row.value(), w / 2, MobileCanvas.SMALL), w - 9, y + 4, k.grey(), MobileCanvas.SMALL,
                    Align.RIGHT, false);
            c.glyph(MobileArt.CHEVRON, w - 6, y + 5, k.chevron(), 1);
            if (n < rows.length - 1) {
                c.rect(19, y + 14, w - 19, 1, k.rowLine());
            }
            if (row.tap() != null) {
                c.press(0, y, w, 15, row.tap());
            }
            y += 15;
        }
        return y + 6;
    }

    /* The status bar of the look's year across the top; returns where the page under it starts. */
    private float status(final MobileCanvas c, final MobileFrame f, final boolean dark) {
        final Colours k = COLOURS.get();
        final float w = c.width();
        final int ink = dark ? k.white() : k.darkInk();
        switch (system) {
            case QOS_2007 -> {
                c.rect(0, 0, w, 10, k.black());
                MobileParts.bars(c, f, 2, 2, k.white());
                c.text(MobileParts.deviceName(f.model()), 13, 1.5F, k.white(), MobileCanvas.SMALL);
                c.text(f.clock(), w / 2, 1.5F, k.white(), MobileCanvas.SMALL, Align.CENTRE, false);
                MobileParts.battery(c, w - 14, 2, k.white(), f.battery(), k.batteryFill());
                return 10;
            }
            case QOS_2013 -> {
                for (int n = 0; n < 5; n++) {
                    c.disc(3 + n * 3.2F, 5, 1.2F, f.link() != null ? ink : dark ? k.dimWhite() : k.dimDark());
                }
                c.text(f.clock(), w / 2, 1.5F, ink, MobileCanvas.SMALL, Align.CENTRE, false);
                MobileParts.battery(c, w - 14, 2, ink, f.battery(), null);
                MobileParts.wifi(c, f, w - 27, 2, ink);
                return 10;
            }
            default -> {
                c.text(f.clock(), 9, 2.5F, ink, MobileCanvas.SMALL);
                MobileParts.battery(c, w - 15, 3, ink, f.battery(), null);
                MobileParts.wifi(c, f, w - 28, 3, ink);
                return 12;
            }
        }
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

    private static String day(final MobileFrame f) {
        return MobileTexts.say(MobileTexts.DAY.with(f.day()));
    }

    /* An icon on the home or the dock: its glyph, its name, what pressing it does, and whether it bears the badge. */
    private record Tile(String glyph, String name, MobileTap tap, boolean badged) {
    }

    /* A row of the settings: its icon, its name, its value, and what pressing it does. */
    private record SettingRow(String glyph, String name, String value, @Nullable MobileTap tap) {
    }

    /** The qOS's colours, as the palette above names them. */
    private record Colours(int white, int black, int grey, int storeBlue, int settingsGround, int rowLine,
                           int bootTrough, int lockBand, int lockBandLine, int alertBox, int alertBoxEdge,
                           int sliderTrack, int sliderEdge, int knob, int knobArrow, int sliderWords, int capsBright,
                           int whenBright, int detailBright, int row, int caps, int when, int torch, int widget,
                           int dockTop, int dockBottom, int dockLine, int dock, int tabletWidget, int tabletCaps,
                           int widgetAlerts, int centreGround, int handle, int settingsLine, int aboutWords,
                           int darkInk, int chevron, int search, int openFill, int storeLine, int appBar,
                           int batteryFill, int dimWhite, int dimDark, int card) {
    }
}
