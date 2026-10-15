/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.data;

import dev.jstech.core.menu.CoreMenu;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

/**
 * What a screen's code hands its file's names: the actions its buttons and boxes name, the values it shows beyond its
 * menu's, and what to do with the text typed in its fields. A name the file uses and nobody handed over does nothing,
 * and the developer's inspector says so.
 *
 * @param <M> the screen's menu, or {@code Void} for a screen with none
 */
public final class ScreenWiring<M> {

    @Nullable
    private final M menu;
    private final Map<String, Runnable> actions = new LinkedHashMap<>();
    private final Map<String, Consumer<String>> texts = new LinkedHashMap<>();
    private final Map<String, Supplier<?>> values = new LinkedHashMap<>();

    ScreenWiring(@Nullable final M menu) {
        this.menu = menu;
    }

    /** The screen's menu, or none for a screen with no menu. */
    public Optional<M> menu() {
        return Optional.ofNullable(menu);
    }

    /** What the action {@code name} does, when a button or a check box naming it is pressed. */
    public ScreenWiring<M> onAction(final String name, final Runnable action) {
        actions.put(name, action);
        return this;
    }

    /** What is done with the text of a field naming {@code name} when it is committed (Enter, or leaving it). */
    public ScreenWiring<M> onText(final String name, final Consumer<String> action) {
        texts.put(name, action);
        return this;
    }

    /** A value the screen shows under {@code name}, over a value of its menu of the same name. */
    public ScreenWiring<M> value(final String name, final Supplier<?> value) {
        values.put(name, value);
        return this;
    }

    /** The action {@code name}, or none. */
    Optional<Runnable> action(final String name) {
        return Optional.ofNullable(actions.get(name));
    }

    /** What is done with the text of {@code name}, or none. */
    Optional<Consumer<String>> text(final String name) {
        return Optional.ofNullable(texts.get(name));
    }

    /** The value {@code name}: the screen's own, else its menu's, else none. */
    Optional<Supplier<?>> value(final String name) {
        final Supplier<?> own = values.get(name);
        if (own != null) {
            return Optional.of(own);
        }
        return menu instanceof CoreMenu core ? core.bound(name) : Optional.empty();
    }
}
