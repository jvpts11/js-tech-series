/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.operation.payload.portable.PortableActionPayload;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.PortableAlert;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

/**
 * Where one portable's system is on its player's game: the page it is on and the app open, the tab and the scroll of
 * a list, how far its lock's slider is drawn, and the balloon of an alert just come in. The device locks each time it
 * is held up, as a phone locks when it goes to sleep.
 */
public final class MobileSession {

    private final UUID device;
    private final MobileAppState apps = new MobileAppState();
    private MobilePage page = MobilePage.LOCK;
    @Nullable
    private MobileApp app;
    private int tab = ALL_TAB;
    private int scroll;
    private float slide;
    @Nullable
    private PortableAlert balloon;
    /* The game time of the newest alert a balloon has come up for, or that was already there when it was held up. */
    private long ballooned = Long.MIN_VALUE;

    /** The tab of the alerts' list that shows them all. */
    public static final int ALL_TAB = 1;
    private static final int TABS = 3;

    MobileSession(final UUID device) {
        this.device = device;
    }

    public MobilePage page() {
        return page;
    }

    /** The device it is the system of. */
    public UUID device() {
        return device;
    }

    /** Where its apps are. */
    public MobileAppState apps() {
        return apps;
    }

    @Nullable
    public MobileApp app() {
        return app;
    }

    public int tab() {
        return tab;
    }

    public int scroll() {
        return scroll;
    }

    public float slide() {
        return slide;
    }

    @Nullable
    public PortableAlert balloon() {
        return balloon;
    }

    /** Held up: it shows its lock, and only alerts that come from now on raise a balloon. */
    public void raised(final List<PortableAlert> alerts) {
        page = MobilePage.LOCK;
        slide = 0.0F;
        balloon = null;
        ballooned = alerts.isEmpty() ? Long.MIN_VALUE : alerts.getFirst().at();
    }

    /** An alert newer than any seen so far raises its balloon, on a system that has one. */
    public void alertsCame(final List<PortableAlert> alerts, final boolean balloons) {
        if (alerts.isEmpty() || alerts.getFirst().at() <= ballooned) {
            return;
        }
        ballooned = alerts.getFirst().at();
        if (balloons && !alerts.getFirst().seen()) {
            balloon = alerts.getFirst();
        }
    }

    /** Draws the lock's slider to {@code to}, from 0 at rest to 1 across. */
    public void slideTo(final float to) {
        slide = Math.max(0.0F, Math.min(1.0F, to));
    }

    /** Scrolls a list by {@code rows}, never above its top. */
    public void scrollBy(final int rows) {
        scroll = Math.max(0, scroll + rows);
    }

    /** Does what {@code tap} says; what the server keeps is sent through {@code send}. */
    public void tap(final MobileTap tap, final Consumer<PortableActionPayload> send) {
        switch (tap.action()) {
            case UNLOCK -> {
                go(MobilePage.HOME);
                send.accept(action(PortableActionPayload.Action.UNLOCK, ""));
            }
            case HOME -> {
                if (page != MobilePage.LOCK) {
                    leaveApp(send);
                    go(MobilePage.HOME);
                }
            }
            case BACK -> {
                // An app goes back inside itself first, and only then to the home.
                if (page == MobilePage.APP && apps.back()) {
                    apps.askSoon();
                } else if (page != MobilePage.LOCK && page != MobilePage.HOME) {
                    leaveApp(send);
                    go(MobilePage.HOME);
                }
                balloon = null;
            }
            case OPEN -> {
                leaveApp(send);
                app = tap.app();
                go(MobilePage.APP);
                balloon = null;
                apps.askSoon();
                if (tap.app() == MobileApp.ALERTS) {
                    send.accept(action(PortableActionPayload.Action.SEE_ALERTS, ""));
                }
            }
            case RUN -> {
                if (tap.run() != null) {
                    tap.run().run();
                }
            }
            case ALERTS -> {
                go(MobilePage.ALERTS);
                send.accept(action(PortableActionPayload.Action.SEE_ALERTS, ""));
            }
            case STORE -> go(MobilePage.STORE);
            case SETTINGS -> go(MobilePage.SETTINGS);
            case START -> go(page == MobilePage.START ? MobilePage.HOME : MobilePage.START);
            case PROGRAMS -> go(MobilePage.PROGRAMS);
            case INSTALL -> {
                if (tap.app() != null) {
                    send.accept(action(PortableActionPayload.Action.INSTALL, tap.app().serializedName()));
                }
            }
            case TAB -> {
                tab = Math.floorMod(tap.tab(), TABS);
                scroll = 0;
            }
            case CLEAR_ALERTS -> send.accept(action(PortableActionPayload.Action.CLEAR_ALERTS, ""));
            case DISMISS -> {
                balloon = null;
                send.accept(action(PortableActionPayload.Action.SEE_ALERTS, ""));
            }
        }
    }

    /** The device was put down with an app open: the Messenger lets its room know the player has gone. */
    public void putDown(final Consumer<PortableActionPayload> send) {
        leaveApp(send);
    }

    /* Leaves the app open, if any: the Messenger is told the player left it. */
    private void leaveApp(final Consumer<PortableActionPayload> send) {
        if (page == MobilePage.APP && app == MobileApp.MESSENGER) {
            send.accept(action(PortableActionPayload.Action.LEAVE, ""));
        }
        apps.typeInto(MobileAppState.Field.NONE);
    }

    /* Goes to {@code to}, at the top of it. */
    private void go(final MobilePage to) {
        page = to;
        scroll = 0;
        slide = 0.0F;
    }

    private PortableActionPayload action(final PortableActionPayload.Action action, final String named) {
        return PortableActionPayload.named(device, action, named);
    }
}
