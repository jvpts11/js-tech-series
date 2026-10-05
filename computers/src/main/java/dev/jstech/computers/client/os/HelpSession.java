/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.help.HelpTarget;
import dev.jstech.computers.gui.help.HelpTree;
import dev.jstech.core.guide.ManualReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;

/**
 * What a machine's help windows are showing: the page, the way back and forward, the books open in the tree, what was
 * searched for, the pages read and those kept as favorites.
 *
 * <p>One per machine, so Frames 95's two windows (Help Topics and the topic it opens) show one reading, and a window
 * closed and opened again comes back where it was.
 */
final class HelpSession {

    private final HelpSource source;
    private final List<HelpTarget> back = new ArrayList<>();
    private final List<HelpTarget> forward = new ArrayList<>();
    private final List<HelpTarget> history = new ArrayList<>();
    private final List<HelpTarget> favorites = new ArrayList<>();
    private final Set<String> open = new LinkedHashSet<>();
    private HelpTarget shown;
    private Special special = Special.NONE;
    private String searched = "";
    /** How many times the machine has answered, so a window knows to lay its page out again. */
    private int answers;
    private int windows;

    /** Every machine's session, while it has a window. */
    private static final Map<BlockPos, HelpSession> SESSIONS = new HashMap<>();

    private HelpSession(final BlockPos host) {
        this.source = new HelpSource(host, () -> this.answers++);
        final List<ManualReader> manuals = HelpBooks.all();
        this.shown = HelpTarget.contents(manuals.isEmpty() ? "" : manuals.getFirst().manualId());
        this.open.add(this.shown.written());
    }

    /** What a page shows instead of a manual's or a command's: nothing else, the results, history or favorites. */
    enum Special {
        NONE, RESULTS, HISTORY, FAVORITES
    }

    /** The session of a machine, started when its first help window opens. */
    static HelpSession open(final BlockPos host) {
        final HelpSession session = SESSIONS.computeIfAbsent(host, HelpSession::new);
        session.windows++;
        return session;
    }

    /** A window of the session closed; the session ends with its last one. */
    static void closed(final BlockPos host) {
        final HelpSession session = SESSIONS.get(host);
        if (session != null && --session.windows <= 0) {
            session.source.close();
            SESSIONS.remove(host);
        }
    }

    HelpSource source() {
        return this.source;
    }

    HelpTarget shown() {
        return this.shown;
    }

    Special special() {
        return this.special;
    }

    String searched() {
        return this.searched;
    }

    int answers() {
        return this.answers;
    }

    List<HelpTarget> history() {
        return this.history;
    }

    List<HelpTarget> favorites() {
        return this.favorites;
    }

    boolean canGoBack() {
        return !this.back.isEmpty() || this.special != Special.NONE;
    }

    boolean canGoForward() {
        return !this.forward.isEmpty();
    }

    /** Whether a book of the tree is open. */
    boolean isOpen(final String key) {
        return this.open.contains(key);
    }

    /** The books of the tree that are open. */
    Set<String> openBooks() {
        return this.open;
    }

    /** Opens a book of the tree, or shuts it. */
    void toggle(final String key) {
        if (!this.open.remove(key)) {
            this.open.add(key);
        }
    }

    /** Shows a page, opening the books it sits in. */
    void go(final HelpTarget target) {
        if (this.special == Special.NONE && target.equals(this.shown)) {
            return;
        }
        if (this.special == Special.NONE) {
            this.back.add(this.shown);
        }
        this.forward.clear();
        this.shown = target;
        this.special = Special.NONE;
        this.read(target);
    }

    /** The page shown before. */
    void back() {
        if (this.special != Special.NONE) {
            this.special = Special.NONE;
            return;
        }
        if (!this.back.isEmpty()) {
            this.forward.add(this.shown);
            this.shown = this.back.removeLast();
            this.read(this.shown);
        }
    }

    /** The page Back left. */
    void forward() {
        if (!this.forward.isEmpty()) {
            this.back.add(this.shown);
            this.shown = this.forward.removeLast();
            this.special = Special.NONE;
            this.read(this.shown);
        }
    }

    /** The first manual's top page. */
    void home() {
        final List<ManualReader> manuals = HelpBooks.all();
        if (!manuals.isEmpty()) {
            this.go(HelpTarget.contents(manuals.getFirst().manualId()));
        }
    }

    /** Shows what answers to the words typed, in place of the page. */
    void search(final String words) {
        this.searched = words.strip();
        this.special = this.searched.isEmpty() ? Special.NONE : Special.RESULTS;
    }

    /** Shows the pages read, or the favorites, in place of the page. */
    void showSpecial(final Special which) {
        this.special = which;
    }

    /** Keeps the page shown among the favorites. */
    void addFavorite() {
        if (!this.favorites.contains(this.shown)) {
            this.favorites.add(this.shown);
        }
    }

    private void read(final HelpTarget target) {
        this.history.remove(target);
        this.history.addFirst(target);
        this.open.addAll(HelpTree.holding(target, HelpBooks.all(), this.source.commands()));
        if (target.kind() == HelpTarget.Kind.COMMAND && !target.id().isEmpty()) {
            this.source.commandPage(target.id());
        }
    }
}
