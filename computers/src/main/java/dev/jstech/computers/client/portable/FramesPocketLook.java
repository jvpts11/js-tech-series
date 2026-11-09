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
import dev.jstech.computers.portable.MobileSystem;
import dev.jstech.computers.portable.PortableAlert;
import dev.jstech.computers.portable.PortableModel;
import dev.jstech.computers.portable.PortableTexts;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Midsoft's Frames Pocket on the PDAs, after the Pocket PC (computers/docs/design/wireless.md): the white Today of 2002
 * with the blue title bar and the Start flag, and the dark blue one of 2007 with its two soft keys. Its alerts come in
 * as a balloon from the title bar and wait on the Today; it is unlocked with the left soft key.
 */
@PaletteHolder
public final class FramesPocketLook implements IMobileLook {

    private final boolean legacy;
    private final IAppKit kit;

    private static final int TITLE_H = 13;
    private static final int BAR_H = 13;
    private static final int ROW = 22;
    private static final int START_W = 92;
    private static final int MENU_ROW = 18;
    private static final int GRID_PITCH = 48;
    private static final int GRID_ROW = 40;
    /**
     * Its colours, as the K0.11 pages drew them: the title bars of each year, the Start flag, the Today's grounds, its
     * lines and its words, the lock, the balloon, the menus, the soft keys' bars and the start's progress bar.
     */
    private static final Palette<Colours> COLOURS = Palettes.declare(JsComputers.MODID, "mobile/frames_pocket",
            new Colours(0xFFFFFFFF, 0xFF000000, 0xFF0A246A, 0xFF3A6EA5, 0xFF2C3E66, 0xFF05070C, 0xFFE8552C,
                    0xFF6BB33A, 0xFF2D8FD8, 0xFFF5B92E, 0xFF0D1E44, 0xFF1C3A78, 0xFF081430, 0xFFA8C0E8, 0xFF505050,
                    0xFFC8C8C8, 0xFF2A4A88, 0xFF7FB3E8, 0xFFD8ECFF, 0x14000000, 0x1EFFFFFF, 0xFFF0C040, 0xFFFFFBD8,
                    0xFF8A8060, 0xFF404040, 0xFFC0C0C0, 0xFFE8ECF4, 0xFFD4D0C8, 0xFF808080, 0xFF3A3D44, 0xFF0C0D10,
                    0xFF5A5E66, 0xFF2A5FC8));

    FramesPocketLook(final MobileSystem system) {
        this.legacy = system == MobileSystem.FRAMES_POCKET_2002;
        this.kit = IAppKit.of(system);
    }

    @Override
    public void paint(final MobileCanvas c, final MobileFrame f) {
        switch (f.page()) {
            case LOCK -> locked(c, f);
            case START -> {
                today(c, f);
                start(c, f);
            }
            case SETTINGS -> settings(c, f);
            case STORE -> store(c, f);
            case PROGRAMS -> programs(c, f);
            case APP -> app(c, f);
            case HOME, ALERTS -> today(c, f);
        }
        if (f.balloon() != null && f.page() != MobilePage.LOCK) {
            balloon(c, f.balloon());
        }
    }

    /** Its start: the mark, and the progress bar filling under it. */
    @Override
    public void boot(final MobileCanvas c, final MobileFrame f, final float progress) {
        final Colours k = COLOURS.get();
        final PortableModel model = f.model();
        c.picture(MobileArt.start(model), model.screenWidth() * 2, model.screenHeight() * 2, 0, 0, c.width(),
                c.height());
        final float x = c.width() * 0.2F;
        final float y = c.height() * 0.78F;
        c.rect(x, y, c.width() * 0.6F, 5, k.legacyLine());
        c.rect(x, y, c.width() * 0.6F * progress, 5, k.progress());
    }

    @Override
    public Unlock unlock() {
        return Unlock.TAP;
    }

    @Override
    public boolean pullsAlerts() {
        return false;
    }

    /* The Today: the day, the network, the alerts, the clock and the owner, each a row with its icon. */
    private void today(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        final int ink;
        final int sub;
        final int line;
        float y;
        c.rect(0, 0, c.width(), c.height(), legacy ? k.white() : k.ground());
        title(c, f, MobileTexts.say(MobileTexts.START), true);
        if (legacy) {
            banner(c, f);
            ink = k.black();
            sub = k.grey();
            line = k.legacyLine();
            y = 46;
        } else {
            c.gradient(0, TITLE_H, c.width(), c.height() - TITLE_H - BAR_H, k.todayTop(), k.todayBottom());
            c.text(day(f), 4, 17, k.white(), MobileCanvas.NORMAL, Align.LEFT, true);
            ink = k.white();
            sub = k.paleBlue();
            line = k.darkLine();
            y = 32;
        }
        final Ink colours = new Ink(ink, sub, line);
        final List<PortableAlert> unseen = f.unseen();
        y = todayItem(c, "network", MobileParts.say(MobileTexts.NETWORK_NAMED.with(MobileParts.network(f))),
                f.link() == null ? MobileParts.say(PortableTexts.NO_ACCESS_POINT.text())
                        : MobileParts.say(MobileTexts.LINKED.with(f.link().accessPoint())),
                y, colours, false, MobileTap.open(MobileApp.NETWORK));
        y = todayItem(c, "alerts", MobileParts.count(unseen.size()),
                unseen.isEmpty() ? "" : MobileParts.title(unseen.getFirst()), y, colours, true,
                MobileTap.open(MobileApp.ALERTS));
        y = todayItem(c, "clock", MobileParts.say(MobileTexts.CLOCK_AND_DAY.with(f.clock(), f.day())),
                MobileTexts.say(MobileTexts.NO_SLEEP), y, colours, false, null);
        todayItem(c, "settings", MobileParts.say(MobileTexts.OWNER.with(f.owner())),
                MobileParts.say(MobileTexts.BATTERY.with(f.battery())), y, colours, false, MobileTap.SETTINGS);
        if (legacy) {
            bar(c, MobileTexts.say(MobileTexts.NEW), null, null, null);
        } else {
            bar(c, MobileParts.name(MobileApp.NETWORK), MobileParts.name(MobileApp.ALERTS),
                    MobileTap.open(MobileApp.NETWORK), MobileTap.open(MobileApp.ALERTS));
        }
    }

    /* The locked Today: its lock line counting the alerts, the newest of them, and Unlock on the left soft key. */
    private void locked(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        final int ink = legacy ? k.black() : k.white();
        final int sub = legacy ? k.grey() : k.paleBlue();
        c.rect(0, 0, c.width(), c.height(), legacy ? k.white() : k.ground());
        title(c, f, MobileTexts.say(MobileTexts.START), false);
        final float top;
        if (legacy) {
            banner(c, f);
            top = 46;
        } else {
            c.gradient(0, TITLE_H, c.width(), c.height() - TITLE_H - BAR_H, k.todayTop(), k.todayBottom());
            c.text(day(f), 4, 17, k.white(), MobileCanvas.NORMAL, Align.LEFT, true);
            top = 32;
        }
        final List<PortableAlert> unseen = f.unseen();
        c.rect(4, top, c.width() - 8, 26, legacy ? k.lockLegacy() : k.lockDark());
        c.glyph(MobileArt.PADLOCK, 8, top + 6, k.padlock(), 1);
        c.text(MobileTexts.say(MobileTexts.DEVICE_LOCKED), 22, top + 3, ink, MobileCanvas.NORMAL, Align.LEFT, true);
        c.text(c.fit(MobileParts.say(MobileTexts.UNLOCK_TO_SEE.with(unseen.size())), c.width() - 26,
                MobileCanvas.SMALL), 22, top + 14, sub, MobileCanvas.SMALL);
        if (!unseen.isEmpty()) {
            final PortableAlert newest = unseen.getFirst();
            icon(c, "alerts", 4, top + 32);
            c.text(c.fit(MobileParts.title(newest), c.width() - 26, MobileCanvas.NORMAL), 24, top + 33, ink,
                    MobileCanvas.NORMAL);
            c.text(c.fit(MobileParts.detail(newest), c.width() - 26, MobileCanvas.SMALL), 24, top + 43, sub,
                    MobileCanvas.SMALL);
        }
        bar(c, MobileTexts.say(MobileTexts.UNLOCK), null, MobileTap.UNLOCK, null);
    }

    /* An alert come in: the balloon from the title bar, Dismiss and Show in it and on the soft keys. */
    private void balloon(final MobileCanvas c, final PortableAlert alert) {
        final Colours k = COLOURS.get();
        final float x = 6;
        final float y = 15;
        final float w = c.width() - 12;
        final float h = 62;
        c.rect(x, y, w, h, k.balloon());
        c.rect(x, y, w, 1, k.balloonEdge());
        c.rect(x, y + h - 1, w, 1, k.balloonEdge());
        c.rect(x, y, 1, h, k.balloonEdge());
        c.rect(x + w - 1, y, 1, h, k.balloonEdge());
        icon(c, "alerts", x + 3, y + 3);
        c.text(MobileTexts.say(MobileTexts.NETWORK_ALERT), x + 22, y + 4, k.black(), MobileCanvas.NORMAL, Align.LEFT,
                true);
        c.text(c.fit(MobileParts.title(alert), w - 8, MobileCanvas.NORMAL), x + 4, y + 22, k.black(),
                MobileCanvas.NORMAL);
        c.text(c.fit(MobileParts.detail(alert), w - 8, MobileCanvas.SMALL), x + 4, y + 33, k.grey(),
                MobileCanvas.SMALL);
        final String dismiss = MobileTexts.say(MobileTexts.DISMISS);
        final String show = MobileTexts.say(MobileTexts.SHOW);
        c.text(dismiss, x + 4, y + h - 11, k.ink(), MobileCanvas.SMALL);
        c.text(show, x + w - 4, y + h - 11, k.ink(), MobileCanvas.SMALL, Align.RIGHT, false);
        c.press(x, y + h - 14, w / 2, 14, MobileTap.DISMISS);
        c.press(x + w / 2, y + h - 14, w / 2, 14, MobileTap.open(MobileApp.ALERTS));
        bar(c, dismiss, show, MobileTap.DISMISS, MobileTap.open(MobileApp.ALERTS));
    }

    /* The Start menu dropped from the flag: the recent ones in a row, then each place with its icon. */
    private void start(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        final float y0 = TITLE_H;
        final Entry[] entries = {
            new Entry("clock", MobileTexts.say(MobileTexts.TODAY), MobileTap.HOME),
            new Entry("network", MobileParts.name(MobileApp.NETWORK), MobileTap.open(MobileApp.NETWORK)),
            new Entry("alerts", MobileParts.name(MobileApp.ALERTS), MobileTap.open(MobileApp.ALERTS)),
            new Entry("store", MobileTexts.say(MobileTexts.MARKETPLACE), MobileTap.STORE),
            new Entry("settings", MobileTexts.say(MobileTexts.SETTINGS), MobileTap.SETTINGS),
            new Entry("gallery", MobileTexts.say(MobileTexts.PROGRAMS), MobileTap.PROGRAMS)};
        final float h = MENU_ROW + entries.length * MENU_ROW;
        c.rect(0, y0, START_W, h, k.white());
        c.rect(START_W, y0, 1, h, k.menuEdge());
        c.rect(0, y0 + h, START_W + 1, 1, k.menuEdge());
        final String[] recent = {"network", "alerts", "settings", "store"};
        final MobileTap[] recentTaps = {MobileTap.open(MobileApp.NETWORK), MobileTap.open(MobileApp.ALERTS),
            MobileTap.SETTINGS, MobileTap.STORE};
        for (int n = 0; n < recent.length; n++) {
            icon(c, recent[n], 4 + n * 20, y0 + 1);
            c.press(4 + n * 20, y0 + 1, 16, 16, recentTaps[n]);
        }
        c.rect(2, y0 + 17, START_W - 4, 1, k.menuRule());
        for (int n = 0; n < entries.length; n++) {
            final float yy = y0 + 19 + n * MENU_ROW;
            final boolean lit = f.over(1, yy - 1, START_W - 2, MENU_ROW);
            if (lit) {
                c.rect(1, yy - 1, START_W - 2, MENU_ROW, k.ink());
            }
            icon(c, entries[n].glyph(), 3, yy);
            c.text(entries[n].name(), 22, yy + 4, lit ? k.white() : k.black(), MobileCanvas.NORMAL);
            c.press(1, yy - 1, START_W - 2, MENU_ROW, entries[n].tap());
        }
    }

    /* The settings: the tabs, the Links tab's icons, and the device's link, battery and hardware under them. */
    private void settings(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        c.rect(0, 0, c.width(), c.height(), k.white());
        title(c, f, MobileTexts.say(MobileTexts.SETTINGS), false);
        c.rect(0, TITLE_H, c.width(), 14, k.tabBar());
        float x = 4;
        final String[] tabs = {MobileTexts.say(MobileTexts.PERSONAL), MobileTexts.say(MobileTexts.SYSTEM),
            MobileTexts.say(MobileTexts.LINKS)};
        for (int n = 0; n < tabs.length; n++) {
            final float w = c.width(tabs[n], MobileCanvas.SMALL) + 8;
            final boolean chosen = n == tabs.length - 1;
            if (chosen) {
                c.rect(x - 1, 14, w, 13, k.white());
                c.rect(x - 1, 14, w, 1, k.darkLine());
            }
            c.text(tabs[n], x + 3, 18, chosen ? k.black() : k.grey(), MobileCanvas.SMALL);
            x += w + 2;
        }
        final Entry[] items = {
            new Entry("network", MobileTexts.say(MobileTexts.WIFI), null),
            new Entry("alerts", MobileParts.name(MobileApp.ALERTS), MobileTap.open(MobileApp.ALERTS)),
            new Entry("store", MobileTexts.say(MobileTexts.MARKETPLACE), MobileTap.STORE),
            new Entry("settings", MobileTexts.say(MobileTexts.POWER), null),
            new Entry("clock", MobileTexts.say(MobileTexts.CLOCK), null),
            new Entry("gallery", MobileTexts.say(MobileTexts.ABOUT), null)};
        grid(c, items, 34, k.black());
        final float y = 118;
        final PortableModel model = f.model();
        c.rect(4, y, c.width() - 8, 1, k.legacyLine());
        c.text(c.fit(MobileParts.say(MobileTexts.WIFI_NAMED.with(f.link() == null
                ? MobileParts.say(PortableTexts.NO_ACCESS_POINT.text()) : f.link().accessPoint())), c.width() - 10,
                MobileCanvas.NORMAL), 6, y + 4, k.black(), MobileCanvas.NORMAL);
        if (f.link() != null) {
            c.text(MobileParts.say(MobileTexts.WIFI_SPEED.with(f.link().speed().accessPointName(),
                    f.link().speed().itemsPerTick())), 6, y + 15, k.grey(), MobileCanvas.SMALL);
        }
        c.text(MobileParts.say(MobileTexts.BATTERY_COLON.with(f.battery())), 6, y + 28, k.black(),
                MobileCanvas.NORMAL);
        c.text(c.fit(MobileParts.say(MobileTexts.HARDWARE_SHORT.with(model.chip(), PortableModel.size(
                model.memoryMb()), PortableModel.size(model.storageMb()))), c.width() - 10, MobileCanvas.SMALL), 6,
                y + 39, k.grey(), MobileCanvas.SMALL);
        if (model.cardMb() > 0) {
            c.text(MobileParts.say(MobileTexts.STORAGE_CARD.with(PortableModel.size(model.cardMb()))), 6, y + 50,
                    k.grey(), MobileCanvas.SMALL);
        }
        bar(c, MobileTexts.say(MobileTexts.DONE), null, MobileTap.BACK, null);
    }

    /* The Frames Marketplace: what the Mirror serves for the device, each free, to install or to open. */
    private void store(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        c.rect(0, 0, c.width(), c.height(), k.white());
        title(c, f, MobileTexts.say(MobileTexts.MARKETPLACE), false);
        c.rect(0, TITLE_H, c.width(), 14, k.tabBar());
        c.text(MobileTexts.say(f.mirror() ? MobileTexts.FROM_THE_MIRROR : MobileTexts.MIRROR_OUT_OF_REACH), 4, 17,
                f.mirror() ? k.black() : k.grey(), MobileCanvas.SMALL);
        final List<MobileApp> apps = f.storeApps();
        final int rows = (int) ((c.height() - TITLE_H - 14 - BAR_H) / 24);
        float y = TITLE_H + 16;
        for (int n = f.scroll(); n < apps.size() && n < f.scroll() + rows; n++) {
            final MobileApp app = apps.get(n);
            final boolean installed = f.installed().contains(app);
            icon(c, MobileArt.glyph(app), 4, y + 2);
            c.text(c.fit(MobileParts.name(app), c.width() - 70, MobileCanvas.NORMAL), 24, y + 3, k.black(),
                    MobileCanvas.NORMAL);
            c.text(MobileTexts.say(MobileTexts.FREE), 24, y + 13, k.grey(), MobileCanvas.SMALL);
            if (installed || f.mirror()) {
                final String word = MobileTexts.say(installed ? MobileTexts.OPEN : MobileTexts.INSTALL);
                final float bw = c.width(word, MobileCanvas.SMALL) + 8;
                final float bx = c.width() - bw - 4;
                c.rect(bx, y + 6, bw, 11, k.ink());
                c.rect(bx + 1, y + 7, bw - 2, 9, k.tabBar());
                c.text(word, bx + bw / 2, y + 8.5F, k.ink(), MobileCanvas.SMALL, Align.CENTRE, false);
                c.press(bx, y + 6, bw, 11, installed ? MobileTap.open(app) : MobileTap.install(app));
            }
            y += 24;
            c.rect(4, y - 2, c.width() - 8, 1, k.legacyLine());
        }
        bar(c, MobileTexts.say(MobileTexts.DONE), null, MobileTap.BACK, null);
    }

    /* The Programs folder: every app on the device, an icon each. */
    private void programs(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        c.rect(0, 0, c.width(), c.height(), legacy ? k.white() : k.ground());
        title(c, f, MobileTexts.say(MobileTexts.PROGRAMS), false);
        final List<MobileApp> apps = f.apps();
        final Entry[] items = new Entry[apps.size()];
        for (int n = 0; n < items.length; n++) {
            items[n] = new Entry(MobileArt.glyph(apps.get(n)), MobileParts.label(apps.get(n)),
                    MobileTap.open(apps.get(n)));
        }
        grid(c, items, TITLE_H + 8, legacy ? k.black() : k.white());
        bar(c, MobileTexts.say(MobileTexts.DONE), null, MobileTap.BACK, null);
    }

    /*
     * An app, its window under the title bar with its name, drawn by its own view in Frames Pocket's kit; one whose
     * inside comes later shows its icon and its name.
     */
    private void app(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        final MobileApp app = f.app() == null ? MobileApp.NETWORK : f.app();
        if (MobileApps.of(app) != null) {
            kit.ground(c, app);
            title(c, f, MobileParts.name(app), false);
            MobileApps.paint(c, f, kit, TITLE_H);
            return;
        }
        c.rect(0, 0, c.width(), c.height(), legacy ? k.white() : k.ground());
        title(c, f, MobileParts.name(app), false);
        icon(c, MobileArt.glyph(app), c.width() / 2.0F - 8, c.height() / 2.0F - 20);
        c.text(MobileParts.name(app), c.width() / 2.0F, c.height() / 2.0F, legacy ? k.black() : k.white(),
                MobileCanvas.NORMAL, Align.CENTRE, false);
        bar(c, MobileTexts.say(MobileTexts.DONE), null, MobileTap.BACK, null);
    }

    /* The title bar: the flag and the title on the left, the link and the clock on the right; the flag opens Start. */
    private void title(final MobileCanvas c, final MobileFrame f, final String title, final boolean opensStart) {
        final Colours k = COLOURS.get();
        if (legacy) {
            c.gradient(0, 0, c.width(), TITLE_H, k.ink(), k.titleLegacyBottom());
        } else {
            c.gradient(0, 0, c.width(), TITLE_H, k.titleTop(), k.titleBottom());
        }
        final int[][] flag = {{k.flagRed(), k.flagGreen()}, {k.flagBlue(), k.flagYellow()}};
        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < 2; i++) {
                c.rect(3 + i * 4, 2 + j * 4, 3, 3, flag[j][i]);
            }
        }
        c.text(c.fit(title, c.width() - 52, MobileCanvas.NORMAL), 13, 2.5F, k.white(), MobileCanvas.NORMAL,
                Align.LEFT, legacy);
        c.text(f.clock(), c.width() - 3, 3.5F, k.white(), MobileCanvas.SMALL, Align.RIGHT, false);
        MobileParts.wifi(c, f, c.width() - 37, 4, k.white());
        if (opensStart) {
            c.press(0, 0, 13 + c.width(title, MobileCanvas.NORMAL), TITLE_H, MobileTap.START);
        }
    }

    /* The Legacy Today's banner: the sky over a hill, the day, the system's name. */
    private void banner(final MobileCanvas c, final MobileFrame f) {
        final Colours k = COLOURS.get();
        c.gradient(0, TITLE_H, c.width(), 30, k.skyTop(), k.skyBottom());
        c.rect(0, 35, c.width(), 8, k.flagGreen());
        c.rect(20, 31, 30, 4, k.flagGreen());
        c.text(day(f), 4, 16, k.ink(), MobileCanvas.NORMAL, Align.LEFT, true);
        c.text(MobileParts.say(PortableTexts.FRAMES_POCKET.text()), c.width() - 4, 28, k.ink(), MobileCanvas.SMALL,
                Align.RIGHT, false);
    }

    /* An item of the Today: its icon, what it says and the line under it; pressed, it does {@code tap}. */
    private float todayItem(final MobileCanvas c, final String glyph, final String said, final String under,
                        final float y, final Ink ink, final boolean bold, @Nullable final MobileTap tap) {
        icon(c, glyph, 3, y + 1);
        c.text(c.fit(said, c.width() - 26, MobileCanvas.NORMAL), 23, y + 1, ink.text(), MobileCanvas.NORMAL,
                Align.LEFT, bold);
        c.text(c.fit(under, c.width() - 26, MobileCanvas.SMALL), 23, y + 11, ink.under(), MobileCanvas.SMALL);
        if (tap != null) {
            c.press(0, y, c.width(), ROW - 2, tap);
        }
        c.rect(4, y + ROW - 2, c.width() - 8, 1, ink.line());
        return y + ROW;
    }

    /* The soft keys' bar of this look's year. */
    private void bar(final MobileCanvas c, final String left, @Nullable final String right,
                     @Nullable final MobileTap leftTap, @Nullable final MobileTap rightTap) {
        softKeys(c, legacy, left, right, leftTap, rightTap);
    }

    /**
     * The soft keys' bar at the foot of the screen, of 2002's look when {@code legacy}: a word on the left and one on
     * the right, each pressing its tap. The apps' pages draw it too.
     */
    static void softKeys(final MobileCanvas c, final boolean legacy, final String left, @Nullable final String right,
                         @Nullable final MobileTap leftTap, @Nullable final MobileTap rightTap) {
        final Colours k = COLOURS.get();
        final float y = c.height() - BAR_H;
        if (legacy) {
            c.rect(0, y, c.width(), BAR_H, k.legacyBar());
            c.rect(0, y, c.width(), 1, k.white());
            c.text(left, 4, y + 3, k.black(), MobileCanvas.NORMAL);
            // The keyboard's button.
            c.rect(c.width() - 18, y + 3, 14, 7, k.keyboard());
            c.rect(c.width() - 17, y + 4, 12, 5, k.white());
            for (int n = 0; n < 5; n++) {
                c.rect(c.width() - 16 + n * 2, y + 5, 1, 1, k.menuEdge());
            }
        } else {
            c.gradient(0, y, c.width(), BAR_H, k.barTop(), k.barBottom());
            c.text(left, 6, y + 3, k.white(), MobileCanvas.NORMAL);
            if (right != null) {
                c.text(right, c.width() - 6, y + 3, k.white(), MobileCanvas.NORMAL, Align.RIGHT, false);
            }
            c.rect(c.width() / 2.0F - 7, y + 3, 14, 7, k.barKey());
        }
        if (leftTap != null) {
            c.press(0, y, c.width() / 2.0F - 8, BAR_H, leftTap);
        }
        if (rightTap != null) {
            c.press(c.width() / 2.0F + 8, y, c.width() / 2.0F - 8, BAR_H, rightTap);
        }
    }

    /* Icons three to a row, each with its name under it, from {@code top}. */
    private void grid(final MobileCanvas c, final Entry[] items, final float top, final int ink) {
        for (int n = 0; n < items.length; n++) {
            final float cx = 24 + (n % 3) * GRID_PITCH;
            final float cy = top + (n / 3) * GRID_ROW;
            icon(c, items[n].glyph(), cx - 8, cy);
            c.text(c.fit(items[n].name(), GRID_PITCH - 2, MobileCanvas.SMALL), cx, cy + 19, ink, MobileCanvas.SMALL,
                    Align.CENTRE, false);
            if (items[n].tap() != null) {
                c.press(cx - GRID_PITCH / 2.0F, cy, GRID_PITCH, GRID_ROW - 6, items[n].tap());
            }
        }
    }

    private static void icon(final MobileCanvas c, final String glyph, final float x, final float y) {
        final int size = IconStyle.FRAMES_POCKET.size();
        c.picture(MobileArt.icon(IconStyle.FRAMES_POCKET, glyph), size * 2, size * 2, x, y, size, size);
    }

    private static String day(final MobileFrame f) {
        return MobileTexts.say(MobileTexts.DAY.with(f.day()));
    }

    /* A place in a menu or a grid: its icon, its name, and what pressing it does. */
    private record Entry(String glyph, String name, @Nullable MobileTap tap) {
    }

    /* The ink of the Today's entries: what they say, the line under it, and the rule between them. */
    private record Ink(int text, int under, int line) {
    }

    /** Frames Pocket's colours, as the palette above names them. */
    private record Colours(int white, int black, int ink, int titleLegacyBottom, int titleTop, int titleBottom,
                           int flagRed, int flagGreen, int flagBlue, int flagYellow, int ground, int todayTop,
                           int todayBottom, int paleBlue, int grey, int legacyLine, int darkLine, int skyTop,
                           int skyBottom, int lockLegacy, int lockDark, int padlock, int balloon, int balloonEdge,
                           int menuEdge, int menuRule, int tabBar, int legacyBar, int keyboard, int barTop,
                           int barBottom, int barKey, int progress) {
    }
}
