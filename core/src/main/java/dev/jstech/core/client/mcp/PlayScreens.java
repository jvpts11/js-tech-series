/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.data.BuiltNode;
import dev.jstech.core.client.gui.data.IDataScreen;
import dev.jstech.core.client.gui.screen.IShownParts;
import dev.jstech.core.client.gui.screen.ShownPart;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * What a screen shows an agent beyond the components of its file: the widgets it holds, also those nested in the
 * lists and bars that hold them, and the parts it, or what it holds, names as it draws them ({@link IShownParts});
 * and the point a name or a thing's words stand for, to click or to drag at.
 */
final class PlayScreens {

    /* How deep widgets are looked for in the widgets that hold them, past any loop of them. */
    private static final int MOST_DEPTH = 8;

    private PlayScreens() {
    }

    /** Its widgets and its named parts, as it draws them now. */
    static Seen seen(final Screen screen) {
        final Seen seen = new Seen(new ArrayList<>(), new ArrayList<>());
        if (screen instanceof IShownParts shown) {
            seen.parts().addAll(shown.shownParts());
        }
        walk(screen.children(), null, seen, 0);
        return seen;
    }

    /** The point the component or the part named {@code name} is clicked at: a file's component, else a named part. */
    static Optional<double[]> named(final Screen screen, final String name) {
        if (screen instanceof IDataScreen data) {
            final Optional<BuiltNode> node = data.find(name);
            if (node.isPresent() && node.get().component() != null) {
                final int[] middle = node.get().component().center();
                return Optional.of(new double[] {middle[0], middle[1]});
            }
        }
        return seen(screen).parts().stream().filter(part -> part.name().equals(name)).findFirst()
                .map(part -> new double[] {part.pointX(), part.pointY()});
    }

    /**
     * The point of the first thing shown that takes a click and reads {@code words}, whatever their case: a file's
     * component, a named part, then a widget.
     */
    static Optional<double[]> worded(final Screen screen, final String words) {
        if (screen instanceof IDataScreen data) {
            final List<double[]> found = new ArrayList<>();
            data.view().tree().ifPresent(root -> root.forEach(node -> {
                final UiComponent component = node.component();
                if (found.isEmpty() && component != null && node.visible().getAsBoolean()
                        && node.enabled().getAsBoolean() && component.narration().getString()
                        .equalsIgnoreCase(words)) {
                    final int[] middle = component.center();
                    found.add(new double[] {middle[0], middle[1]});
                }
            }));
            if (!found.isEmpty()) {
                return Optional.of(found.getFirst());
            }
        }
        final Seen seen = seen(screen);
        final Optional<double[]> part = seen.parts().stream().filter(each -> each.usable()
                && each.words().equalsIgnoreCase(words)).findFirst()
                .map(each -> new double[] {each.pointX(), each.pointY()});
        if (part.isPresent()) {
            return part;
        }
        return seen.widgets().stream().filter(widget -> widget.visible && widget.active
                && widget.getMessage().getString().equalsIgnoreCase(words)).findFirst()
                .map(widget -> new double[] {widget.getX() + widget.getWidth() / 2.0,
                    widget.getY() + widget.getHeight() / 2.0});
    }

    /*
     * The widgets among {@code listeners} and in what holds them, and the parts any names, into {@code seen}; one held
     * by {@code holder} kept only while its middle stands in the holder's box, as a list's rows scrolled off are not.
     */
    private static void walk(final List<? extends GuiEventListener> listeners, @Nullable final AbstractWidget holder,
                             final Seen seen, final int depth) {
        for (final GuiEventListener each : listeners) {
            if (each instanceof AbstractWidget widget) {
                if (holder != null && !inside(widget, holder)) {
                    continue;
                }
                seen.widgets().add(widget);
            }
            if (each instanceof IShownParts shown) {
                seen.parts().addAll(shown.shownParts());
            }
            if (each instanceof ContainerEventHandler holding && depth < MOST_DEPTH) {
                walk(holding.children(), each instanceof AbstractWidget widget ? widget : holder, seen, depth + 1);
            }
        }
    }

    private static boolean inside(final AbstractWidget widget, final AbstractWidget holder) {
        final double x = widget.getX() + widget.getWidth() / 2.0;
        final double y = widget.getY() + widget.getHeight() / 2.0;
        return x >= holder.getX() && x < holder.getRight() && y >= holder.getY() && y < holder.getBottom();
    }

    /**
     * What a screen shows.
     *
     * @param widgets its widgets, nested ones too
     * @param parts   the parts it and what it holds name
     */
    record Seen(List<AbstractWidget> widgets, List<ShownPart> parts) {
    }
}
