/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * One heading within the {@link ComputerTerminalScreen}. Each implementation holds the heading's own
 * state and draws into the content area that sits to the right of the rail and under the status bar.
 *
 * <p>Two render passes are required because {@code AbstractContainerScreen} draws slots between
 * {@code renderBg} and {@code renderLabels}; merging them into one call would place text below
 * the item-stack z layer. {@link ComputerTerminalScreen} invokes each pass at the right moment.
 */
interface ITerminalTab {

    /**
     * Draws the background for this heading. Called from {@code renderBg}, before slots are rendered.
     *
     * @param g           graphics context with world-coordinate origin
     * @param x           screen left edge in world coords ({@code leftPos})
     * @param y           screen top edge in world coords ({@code topPos})
     * @param cx          content area left edge ({@code x + CONTENT_X})
     * @param cy          screen top edge again, which every offset here is measured from
     * @param cw          content area width
     * @param mouseX      cursor X in world coords
     * @param mouseY      cursor Y in world coords
     * @param partialTick the render partial tick, for a heading that draws something that moves
     */
    void renderTabBg(GuiGraphics g, int x, int y, int cx, int cy, int cw, int mouseX, int mouseY,
                     float partialTick);

    /**
     * Draws text labels for this heading. Called from {@code renderLabels}, after slots are rendered.
     *
     * @param g  graphics context with screen-relative origin ({@code leftPos/topPos} already applied)
     * @param cx content area left edge relative to screen origin ({@code CONTENT_X})
     * @param cy the screen's own top ({@code 0}), which every offset here is measured from
     * @param cw content area width
     */
    void renderTabLabels(GuiGraphics g, int cx, int cy, int cw);

    /**
     * Handles a mouse press routed from the parent screen's {@code mouseClicked}, after any
     * open popup has already claimed the event. Returns {@code true} if consumed.
     */
    default boolean onMouseClicked(final double mouseX, final double mouseY, final int button) {
        return false;
    }

    /**
     * Handles a mouse scroll routed from the parent screen's {@code mouseScrolled}. Returns
     * {@code true} if consumed.
     */
    default boolean onMouseScrolled(final double mouseX, final double mouseY, final double dy) {
        return false;
    }

    /**
     * Handles a mouse release routed from the parent screen's {@code mouseReleased}. Returns
     * {@code true} if consumed.
     */
    default boolean onMouseReleased(final double mouseX, final double mouseY, final int button) {
        return false;
    }

    /**
     * Handles a mouse drag routed from the parent screen. Returns {@code true} if consumed.
     */
    default boolean onMouseDragged(final double mouseX, final double mouseY, final int button) {
        return false;
    }

    /**
     * Handles a key press before the screen's own shortcuts. A heading that is typed into, such as the
     * prompt, takes every key it is given here. Returns {@code true} if consumed.
     */
    default boolean onKeyPressed(final int key, final int scanCode, final int modifiers) {
        return false;
    }

    /** Handles a key release, which an editor holding the glass reads. Returns {@code true} if consumed. */
    default boolean onKeyReleased(final int key, final int scanCode, final int modifiers) {
        return false;
    }

    /** Handles a typed character. Returns {@code true} if consumed. */
    default boolean onCharTyped(final char c, final int modifiers) {
        return false;
    }

    /** Whether this heading wants Escape for itself, which stops Escape closing the whole screen. */
    default boolean wantsEscape() {
        return false;
    }

    /** Called each game tick from {@code containerTick} so the heading can sync its state. */
    default void onContainerTick() {
    }

    /** Called when the screen closes, so a heading holding something of the machine's can let it go. */
    default void onRemoved() {
    }
}
