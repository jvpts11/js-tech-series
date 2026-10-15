/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.data;

import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.gui.flex.FlexBox;
import dev.jstech.core.gui.flex.Placed;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

/**
 * One component of a screen as it was built from its file: its box in the layout, what it draws (a background before
 * the components it holds), what it does once the layout has placed it, whether it shows and can be used, and what it
 * holds.
 *
 * @param id        the name the file gave it, or none
 * @param box       its box in the layout
 * @param component what it draws, in its box; none for a box that only lays others out
 * @param placed    what it does once placed, given its box in the screen's space (a slot moves the menu's slot there)
 * @param visible   whether it is shown
 * @param enabled   whether it can be used
 * @param tooltip   the translation key of what the pointer resting on it shows
 * @param children  what it holds
 */
public record BuiltNode(Optional<String> id, FlexBox box, @Nullable UiComponent component,
                        @Nullable Consumer<Placed> placed, BooleanSupplier visible, BooleanSupplier enabled,
                        Optional<String> tooltip, List<BuiltNode> children) {

    public BuiltNode {
        children = List.copyOf(children);
    }

    /** This node and every node it holds, parents first. */
    public void forEach(final Consumer<BuiltNode> action) {
        action.accept(this);
        for (final BuiltNode child : children) {
            child.forEach(action);
        }
    }
}
