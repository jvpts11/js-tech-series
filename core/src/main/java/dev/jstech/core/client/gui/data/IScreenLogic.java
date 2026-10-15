/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.data;

/**
 * What a screen declared in a file does, the only Java such a screen has: what its buttons' actions do, what the values
 * it shows beyond its menu's are, what it does with typed text. Handed over by name, explicitly, when the screen is
 * built:
 *
 * <pre>{@code
 * screen -> screen
 *         .onAction("start", () -> START.send(true))
 *         .value("speed_text", () -> menu.speed() + " rpm");
 * }</pre>
 *
 * @param <M> the screen's menu, or {@code Void} for a screen with none
 */
@FunctionalInterface
public interface IScreenLogic<M> {

    /** No logic: a screen that only shows its menu's values. */
    static <M> IScreenLogic<M> none() {
        return screen -> {
        };
    }

    /** Hands {@code screen} what its file's names do. */
    void wire(ScreenWiring<M> screen);
}
