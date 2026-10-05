/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.UiWindowPayload;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * What a window of a Σ# program keeps on the screen showing it and nowhere else: what is being typed before the
 * program has it, which widget has the keyboard, how far each list is scrolled, which nodes of a tree are folded and
 * which menu is open. None of it is the program's; a second viewer of the same window keeps their own.
 */
final class SigmaUiState {

    /** A popup that is open: a combo box's list, a menu of a menu bar or a context menu. */
    enum PopupKind { NONE, COMBO, MENU, CONTEXT }

    /** What is being typed in a box, until the program has it. */
    final Map<Long, String> typing = new HashMap<>();
    /** How far down each list, table, tree, log, text area, picker or scroll view is scrolled, in rows or pixels. */
    final Map<Long, Integer> scrolled = new HashMap<>();
    /** How far each scroll view can be scrolled, worked out at its last layout. */
    final Map<Long, Integer> rooms = new HashMap<>();
    /** The nodes of each tree that are folded shut; every node is open until the player folds it. */
    final Map<Long, Set<Integer>> folded = new HashMap<>();
    /** What each item picker's search line holds. */
    final Map<Long, String> filters = new HashMap<>();
    /** The dialogs shown, by their widget, at the showing each was put up for. */
    final Map<Long, Integer> dialogsShown = new HashMap<>();
    /** Whether the player scrolled each log away from its newest line, which keeps it from following. */
    final Set<Long> logsHeld = new HashSet<>();
    long focused;
    long pressed;
    /** The slider being dragged, and the value last sent for it. */
    long dragging;
    int dragSent = Integer.MIN_VALUE;
    PopupKind popup = PopupKind.NONE;
    /** The widget the open popup belongs to, the menu of a menu bar that is open, and where a context menu opened. */
    long popupOwner;
    String popupMenu = "";
    int popupX;
    int popupY;
    /** How far down a long combo list is scrolled. */
    int popupScroll;

    /** Shuts whatever popup is open. */
    void closePopup() {
        this.popup = PopupKind.NONE;
        this.popupOwner = 0;
        this.popupMenu = "";
        this.popupScroll = 0;
    }

    /** Whether a popup is open. */
    boolean popupOpen() {
        return this.popup != PopupKind.NONE;
    }

    /** How far that widget is scrolled. */
    int scroll(final long id) {
        return this.scrolled.getOrDefault(id, 0);
    }

    /** Moves that widget's scroll by {@code by}, never above the top. */
    void scrollBy(final long id, final int by) {
        this.scrolled.put(id, Math.max(0, this.scroll(id) + by));
    }

    /** Whether a node of a tree is folded shut. */
    boolean isFolded(final long tree, final int node) {
        return this.folded.getOrDefault(tree, Set.of()).contains(node);
    }

    /** Folds a node of a tree shut, or opens it. */
    void toggleFold(final long tree, final int node) {
        final Set<Integer> shut = this.folded.computeIfAbsent(tree, id -> new HashSet<>());
        if (!shut.remove(node)) {
            shut.add(node);
        }
    }

    /** What a box shows: what is being typed in it, or what the program says it holds. */
    String textOf(final UiWindowPayload.Widget widget) {
        final String typed = this.typing.get(widget.id());
        if (typed != null) {
            return typed;
        }
        return "TextArea".equals(widget.kind()) ? widget.joined() : widget.text();
    }
}
