/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.data;

import dev.jstech.core.menu.CoreMenu;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
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
    private final Map<String, Consumer<Object>> handlers = new LinkedHashMap<>();
    private final List<Preview> previews = new ArrayList<>();

    ScreenWiring(@Nullable final M menu) {
        this.menu = menu;
    }

    /** What is done with a value a component hands over under {@code name}: a number, a choice, an item. */
    public ScreenWiring<M> on(final String name, final Consumer<Object> handler) {
        handlers.put(name, handler);
        return this;
    }

    /** What is done with a number a slider hands over under {@code name}. */
    public ScreenWiring<M> onNumber(final String name, final DoubleConsumer handler) {
        return on(name, value -> handler.accept(((Number) value).doubleValue()));
    }

    /** What is done with the place of the option chosen in a list or a group named {@code name}. */
    public ScreenWiring<M> onChoice(final String name, final IntConsumer handler) {
        return on(name, value -> handler.accept(((Number) value).intValue()));
    }

    /** What is done with a colour picked on a colour picker named {@code name}. */
    public ScreenWiring<M> onColour(final String name, final IntConsumer handler) {
        return onChoice(name, handler);
    }

    /** What is done with the item a ghost slot named {@code name} is set to, an empty stack when cleared. */
    public ScreenWiring<M> onItem(final String name, final Consumer<ItemStack> handler) {
        return on(name, value -> handler.accept((ItemStack) value));
    }

    /** What is done with the point a graph editor named {@code name} asks a new node for, in its own space. */
    public ScreenWiring<M> onPoint(final String name, final BiConsumer<Integer, Integer> handler) {
        return on(name, value -> {
            final int[] point = (int[]) value;
            handler.accept(point[0], point[1]);
        });
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

    /**
     * A state the developer's inspector can put the screen in to see it so, as "working" or "jammed" for a machine's:
     * what {@code show} does sets the values the screen reads. Only a development run shows it.
     */
    public ScreenWiring<M> preview(final String name, final Runnable show) {
        previews.add(new Preview(name, show));
        return this;
    }

    /** The states the inspector can put the screen in, in the order they were given. */
    public List<Preview> previews() {
        return List.copyOf(previews);
    }

    /**
     * A state the inspector can put a screen in.
     *
     * @param name what it is called
     * @param show what puts the screen in it
     */
    public record Preview(String name, Runnable show) {
    }

    /** The action {@code name}, or none. */
    Optional<Runnable> action(final String name) {
        return Optional.ofNullable(actions.get(name));
    }

    /** What is done with the text of {@code name}, or none. */
    Optional<Consumer<String>> text(final String name) {
        return Optional.ofNullable(texts.get(name));
    }

    /** What is done with a value handed over under {@code name}, or none. */
    Optional<Consumer<Object>> handler(final String name) {
        return Optional.ofNullable(handlers.get(name));
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
