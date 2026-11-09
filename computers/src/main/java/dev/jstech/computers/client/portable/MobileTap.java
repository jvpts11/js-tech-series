/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.portable.MobileApp;
import org.jetbrains.annotations.Nullable;

/**
 * What pressing a place on a portable's screen does: unlock it, go home or back, open a page or an app, install an
 * app from the store, choose a tab of the alerts' list, let the alerts go, or put away the balloon an alert came in.
 *
 * @param action what it does
 * @param app    the app it opens or installs; null for the rest
 * @param tab    the tab it chooses, for {@link Action#TAB}
 */
public record MobileTap(Action action, @Nullable MobileApp app, int tab) {

    public static final MobileTap UNLOCK = of(Action.UNLOCK);
    public static final MobileTap HOME = of(Action.HOME);
    public static final MobileTap BACK = of(Action.BACK);
    public static final MobileTap STORE = of(Action.STORE);
    public static final MobileTap SETTINGS = of(Action.SETTINGS);
    public static final MobileTap ALERTS = of(Action.ALERTS);
    public static final MobileTap START = of(Action.START);
    public static final MobileTap PROGRAMS = of(Action.PROGRAMS);
    public static final MobileTap CLEAR_ALERTS = of(Action.CLEAR_ALERTS);
    public static final MobileTap DISMISS = of(Action.DISMISS);

    /** Opens {@code app}. */
    public static MobileTap open(final MobileApp app) {
        return new MobileTap(Action.OPEN, app, 0);
    }

    /** Installs {@code app} from the store. */
    public static MobileTap install(final MobileApp app) {
        return new MobileTap(Action.INSTALL, app, 0);
    }

    /** Chooses the tab {@code tab} of the alerts' list. */
    public static MobileTap tab(final int tab) {
        return new MobileTap(Action.TAB, null, tab);
    }

    private static MobileTap of(final Action action) {
        return new MobileTap(action, null, 0);
    }

    /** What a press can do. */
    public enum Action {
        UNLOCK,
        HOME,
        BACK,
        OPEN,
        STORE,
        SETTINGS,
        ALERTS,
        START,
        PROGRAMS,
        INSTALL,
        TAB,
        CLEAR_ALERTS,
        DISMISS
    }
}
