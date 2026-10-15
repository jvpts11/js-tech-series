/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.notification;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The notifications of one player's game: the history of the recent ones, newest first, and those on the screen now.
 *
 * <ul>
 *   <li>Alike notifications, of one group, become one while they keep coming within a minute of each other: its count
 *       goes up, it moves to the top, and it stays on the screen anew. It sounds again only if it had left the
 *       screen.</li>
 *   <li>Notifications of a silenced kind or mod, and every notification while the player does not want to be
 *       disturbed, never come on the screen and never sound; they are kept in the history all the same.</li>
 *   <li>At most {@value #MOST_SHOWN} are on the screen at once, and the history keeps only so many.</li>
 * </ul>
 *
 * <p>Pure: no game types, and the time is handed in, in milliseconds, so it is checked without the game.
 *
 * @param <T> what a notification is
 */
public final class NotificationBoard<T> {

    private final List<Entry<T>> history = new ArrayList<>();
    private final Set<String> silencedKinds = new HashSet<>();
    private final Set<String> silencedMods = new HashSet<>();
    private int mostKept = DEFAULT_KEPT;
    private boolean doNotDisturb;
    private long nextId = 1L;

    /** How many notifications are on the screen at once, at most. */
    public static final int MOST_SHOWN = 5;
    /** How many the history keeps unless it is told otherwise. */
    public static final int DEFAULT_KEPT = 100;
    /** How close alike notifications come to become one, in milliseconds. */
    public static final long GROUP_MILLIS = 60_000L;

    /**
     * Posts a notification at {@code now}.
     *
     * @param item   the notification
     * @param group  what makes two notifications alike
     * @param kind   its kind, which the player may silence
     * @param mod    the mod it came from, which the player may silence
     * @param level  how much it matters
     * @param lasts  how long it stays on the screen, in milliseconds
     * @param now    the time, in milliseconds
     * @return its entry, whether it comes on the screen and whether it sounds
     */
    public synchronized Posted<T> post(final T item, final String group, final String kind, final String mod,
                                       final NotificationLevel level, final long lasts, final long now) {
        final boolean quiet = this.doNotDisturb || silenced(kind, mod);
        final long until = quiet ? now : now + lasts;
        for (int i = 0; i < this.history.size(); i++) {
            final Entry<T> earlier = this.history.get(i);
            if (earlier.group().equals(group) && now - earlier.lastAt() <= GROUP_MILLIS) {
                this.history.remove(i);
                final Entry<T> merged = new Entry<>(earlier.id(), item, group, kind, mod, level, earlier.firstAt(), now,
                        until, earlier.count() + 1);
                this.history.addFirst(merged);
                return new Posted<>(merged, !quiet, !quiet && earlier.until() <= now);
            }
        }
        final Entry<T> fresh = new Entry<>(this.nextId++, item, group, kind, mod, level, now, now, until, 1);
        this.history.addFirst(fresh);
        while (this.history.size() > this.mostKept) {
            this.history.removeLast();
        }
        return new Posted<>(fresh, !quiet, !quiet);
    }

    /** The notifications on the screen at {@code now}, newest first. */
    public synchronized List<Entry<T>> showing(final long now) {
        final List<Entry<T>> out = new ArrayList<>();
        if (this.doNotDisturb) {
            return out;
        }
        for (final Entry<T> entry : this.history) {
            if (entry.until() > now && !silenced(entry.kind(), entry.mod())) {
                out.add(entry);
                if (out.size() == MOST_SHOWN) {
                    break;
                }
            }
        }
        return out;
    }

    /** Every notification kept, newest first. */
    public synchronized List<Entry<T>> history() {
        return List.copyOf(this.history);
    }

    /** Takes the notification {@code id} off the screen, keeping it in the history. */
    public synchronized void dismiss(final long id) {
        for (int i = 0; i < this.history.size(); i++) {
            final Entry<T> entry = this.history.get(i);
            if (entry.id() == id) {
                this.history.set(i, new Entry<>(entry.id(), entry.item(), entry.group(), entry.kind(), entry.mod(),
                        entry.level(), entry.firstAt(), entry.lastAt(), 0L, entry.count()));
                return;
            }
        }
    }

    /** Takes the notification {@code id} out of the history, and off the screen. */
    public synchronized void forget(final long id) {
        this.history.removeIf(entry -> entry.id() == id);
    }

    /** Forgets every notification. */
    public synchronized void clear() {
        this.history.clear();
    }

    /** Whether notifications of {@code kind}, or from {@code mod}, are silenced. */
    public synchronized boolean silenced(final String kind, final String mod) {
        return this.silencedKinds.contains(kind) || this.silencedMods.contains(mod);
    }

    /** Silences these kinds and these mods, and no others. */
    public synchronized void silence(final Set<String> kinds, final Set<String> mods) {
        this.silencedKinds.clear();
        this.silencedKinds.addAll(kinds);
        this.silencedMods.clear();
        this.silencedMods.addAll(mods);
    }

    /** Whether the player does not want to be disturbed. */
    public synchronized boolean doNotDisturb() {
        return this.doNotDisturb;
    }

    public synchronized void setDoNotDisturb(final boolean value) {
        this.doNotDisturb = value;
    }

    /** Keeps at most {@code most} notifications in the history, the oldest going first. */
    public synchronized void setMostKept(final int most) {
        this.mostKept = Math.max(1, most);
        while (this.history.size() > this.mostKept) {
            this.history.removeLast();
        }
    }

    /** How much of its time on the screen {@code entry} has left at {@code now}: 1 as it comes, 0 as it goes. */
    public static float left(final Entry<?> entry, final long now) {
        final long span = entry.until() - entry.lastAt();
        if (span <= 0L) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, (entry.until() - now) / (float) span));
    }

    /**
     * One notification kept.
     *
     * @param id      what it is told apart by, for the whole game
     * @param item    the notification, the newest of its group
     * @param group   what makes two alike
     * @param kind    its kind
     * @param mod     the mod it came from
     * @param level   how much it matters
     * @param firstAt when the first of its group came, in milliseconds
     * @param lastAt  when the newest came
     * @param until   when it leaves the screen
     * @param count   how many of its group came
     * @param <T>     what a notification is
     */
    public record Entry<T>(long id, T item, String group, String kind, String mod, NotificationLevel level,
                           long firstAt, long lastAt, long until, int count) {
    }

    /**
     * What posting a notification did.
     *
     * @param entry   its entry, new or grown
     * @param shown   whether it comes on the screen
     * @param audible whether it sounds
     * @param <T>     what a notification is
     */
    public record Posted<T>(Entry<T> entry, boolean shown, boolean audible) {
    }
}
