/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.PortableAlert;
import dev.jstech.computers.portable.PortableLink;
import dev.jstech.computers.portable.PortableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Everything a portable's screen shows in one frame, read once: which device, the page it is on and the app open on
 * it, its apps and alerts, its battery and link, the world's time, its owner, where the pointer is, and the balloon of
 * an alert just come in.
 *
 * @param model     the device
 * @param page      the page shown
 * @param app       the app open, on {@link MobilePage#APP}
 * @param installed the apps on it
 * @param alerts    its alerts, the newest first
 * @param mirror    whether its network's Mirror serves its store
 * @param battery   its battery's share, from 0 to 100
 * @param link      its link, or null with no access point in reach
 * @param clock     the world's time, as {@code 07:42}
 * @param day       the world's day, counting from one
 * @param owner     the name of the player who carries it
 * @param mouseX    the pointer across the screen, in its pixels; negative when it is off the screen
 * @param mouseY    the pointer down the screen
 * @param balloon   the alert whose balloon is up, or null
 * @param slide     how far the lock's slider is drawn, from 0 at rest to 1 across
 * @param tab       the tab of the alerts' list chosen: 0 today's, 1 all, 2 the missed ones
 * @param scroll    how many rows a long list is scrolled down
 * @param ticks     the game's ticks, for what blinks and runs
 */
public record MobileFrame(PortableModel model, MobilePage page, @Nullable MobileApp app, Set<MobileApp> installed,
                          List<PortableAlert> alerts, boolean mirror, int battery, @Nullable PortableLink link,
                          String clock, long day, String owner, double mouseX, double mouseY,
                          @Nullable PortableAlert balloon, float slide, int tab, int scroll, long ticks) {

    /* A game day, and an hour of it, in ticks. */
    private static final long TICKS_A_DAY = 24_000L;
    private static final long TICKS_AN_HOUR = 1_000L;

    /** The alerts its player has not seen, the newest first. */
    public List<PortableAlert> unseen() {
        final List<PortableAlert> out = new ArrayList<>();
        for (final PortableAlert alert : alerts) {
            if (!alert.seen()) {
                out.add(alert);
            }
        }
        return out;
    }

    /** Whether the pointer is on the rectangle at {@code (x, y)}. */
    public boolean over(final float x, final float y, final float w, final float h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /** The apps a home screen shows: those on it, in their order. */
    public List<MobileApp> apps() {
        return List.copyOf(installed);
    }

    /** The apps its store lists: every one the device can have, on it or not. */
    public List<MobileApp> storeApps() {
        final List<MobileApp> out = new ArrayList<>();
        for (final MobileApp app : MobileApp.values()) {
            if (app.runsOn(model)) {
                out.add(app);
            }
        }
        return out;
    }

    /** The alerts the chosen tab of the list shows: those of the last day, all of them, or the ones not seen. */
    public List<PortableAlert> listed() {
        return switch (tab) {
            case 0 -> alerts.stream().filter(alert -> ticks - alert.at() < TICKS_A_DAY).toList();
            case 2 -> unseen();
            default -> alerts;
        };
    }

    /** Whether {@code alert} came within the last game hour, which a list calls now. */
    public boolean justNow(final PortableAlert alert) {
        return ticks - alert.at() < TICKS_AN_HOUR;
    }
}
