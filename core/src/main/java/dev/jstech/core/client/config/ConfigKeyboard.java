/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiComponent;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

/**
 * Where the keyboard is on the settings screen, and how it moves: through the controls in the order they read, the
 * rail's sections first, then the row over the list, the list's controls, and the footer's buttons, round to the start
 * again. A section of the rail is not in a panel; the keyboard rests on it by a flag of its own.
 */
final class ConfigKeyboard {

    private final Panel root;
    private final Supplier<List<Stop>> order;
    private final Supplier<List<ConfigRailEntry>> rail;
    private final Consumer<UiComponent> onReached;

    /**
     * @param root      the screen's root panel, whose children are the panels the controls are in
     * @param order     the controls the keyboard goes through, in reading order
     * @param rail      the rail's sections
     * @param onReached what is done when the keyboard reaches a control: the list scrolls to it
     */
    ConfigKeyboard(final Panel root, final Supplier<List<Stop>> order, final Supplier<List<ConfigRailEntry>> rail,
                   final Consumer<UiComponent> onReached) {
        this.root = root;
        this.order = order;
        this.rail = rail;
        this.onReached = onReached;
    }

    /**
     * A control the keyboard stops at, and the panel that hands it the keyboard; none for a section of the rail.
     *
     * @param panel     the panel it is in
     * @param component the control
     */
    record Stop(@Nullable Panel panel, UiComponent component) {
    }

    /** The control that has the keyboard, inside whichever panel holds it, or none. */
    @Nullable
    UiComponent focused() {
        final UiComponent top = root.focusedChild();
        return top instanceof Panel panel ? panel.focusedChild() : top;
    }

    /** The rail's section the keyboard rests on, or none. */
    Optional<ConfigRailEntry> railFocus() {
        return rail.get().stream().filter(ConfigRailEntry::keyboard).findFirst();
    }

    /** Whether a field has the keyboard, which then takes the keys a field uses. */
    boolean typing() {
        return focused() instanceof TextField;
    }

    /** Moves the keyboard to the next control ({@code direction} 1) or the one before (-1), round the ends. */
    void move(final int direction) {
        final List<Stop> stops = order.get();
        if (stops.isEmpty()) {
            return;
        }
        final UiComponent now = railFocus().map(entry -> (UiComponent) entry).orElseGet(this::focused);
        int at = -1;
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).component() == now) {
                at = i;
            }
        }
        final Stop next = stops.get(Math.floorMod(at + direction, stops.size()));
        if (next.component() instanceof ConfigRailEntry entry) {
            clearRail();
            root.focus(null);
            entry.setKeyboard(true);
            return;
        }
        focus(next.panel(), next.component());
    }

    /** Hands the keyboard to {@code component} of {@code panel}. */
    void focus(final Panel panel, final UiComponent component) {
        clearRail();
        root.focus(panel);
        panel.focus(component);
        onReached.accept(component);
    }

    /** Takes the keyboard from whatever has it. */
    void release() {
        clearRail();
        root.focus(null);
    }

    private void clearRail() {
        rail.get().forEach(entry -> entry.setKeyboard(false));
    }
}
