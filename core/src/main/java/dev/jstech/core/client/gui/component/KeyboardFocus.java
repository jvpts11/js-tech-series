/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;

/**
 * The components of a panel the keyboard reaches, handed to the game's own screen as its widgets are, so that the
 * game moves the keyboard among them: Tab and Shift+Tab in the order they read, the arrows to the nearest one that
 * way, Enter and Space pressing what has it. The game's narrator reads each as it reads its own widgets, and a
 * controller (Controlify among them) moves among them as it moves among the game's. The components keep drawing and
 * taking the pointer themselves; only the keyboard and the narrator go through here.
 *
 * <pre>{@code
 * focus = KeyboardFocus.of(root);
 * focus.entries().forEach(this::addWidget);   // in the screen's init, once its components are laid out
 * }</pre>
 */
public final class KeyboardFocus {

    private final List<Entry> entries;

    private KeyboardFocus(final List<Entry> entries) {
        this.entries = List.copyOf(entries);
    }

    /** The components under {@code root} the keyboard reaches, in the order they were added, which is the order read. */
    public static KeyboardFocus of(final Panel root) {
        final List<Entry> found = new ArrayList<>();
        collect(root, new ArrayList<>(), found);
        return new KeyboardFocus(found);
    }

    /** What the screen adds as its widgets. */
    public List<Entry> entries() {
        return entries;
    }

    /** The one that has the keyboard now, as a click on a component may have handed it; null for none. */
    @Nullable
    public Entry focused() {
        for (final Entry entry : entries) {
            if (entry.isFocused()) {
                return entry;
            }
        }
        return null;
    }

    private static void collect(final Panel panel, final List<Panel> above, final List<Entry> found) {
        final List<Panel> path = new ArrayList<>(above);
        path.add(panel);
        for (final UiComponent child : panel.children()) {
            if (child instanceof Panel inner) {
                collect(inner, path, found);
            } else if (child.focusable()) {
                found.add(new Entry(child, path));
            }
        }
    }

    /** One component the keyboard reaches, as the game's screen sees a widget. */
    public static final class Entry implements GuiEventListener, NarratableEntry {

        private final UiComponent component;
        /* The panels from the root down to the component's own. */
        private final List<Panel> panels;

        private Entry(final UiComponent component, final List<Panel> panels) {
            this.component = component;
            this.panels = List.copyOf(panels);
        }

        /** The component. */
        public UiComponent component() {
            return component;
        }

        @Override
        public void setFocused(final boolean on) {
            if (on) {
                // Each panel on the way down hands the keyboard to the next, the last to the component.
                for (int i = 0; i < panels.size(); i++) {
                    panels.get(i).focus(i + 1 < panels.size() ? panels.get(i + 1) : component);
                }
            } else if (component.isFocused()) {
                panels.get(panels.size() - 1).blur(component);
            }
        }

        @Override
        public boolean isFocused() {
            return component.isFocused();
        }

        @Override
        public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
            return component.keyPressed(key, scanCode, modifiers);
        }

        @Override
        public boolean keyReleased(final int key, final int scanCode, final int modifiers) {
            return component.keyReleased(key, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(final char c, final int modifiers) {
            return component.charTyped(c);
        }

        @Override
        public ScreenRectangle getRectangle() {
            return new ScreenRectangle(component.x(), component.y(), component.width(), component.height());
        }

        @Nullable
        @Override
        public ComponentPath nextFocusPath(final FocusNavigationEvent event) {
            return reachable() && !isFocused() ? ComponentPath.leaf(this) : null;
        }

        @Override
        public NarrationPriority narrationPriority() {
            if (isFocused()) {
                return NarrationPriority.FOCUSED;
            }
            return reachable() && pointedAt() ? NarrationPriority.HOVERED : NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(final NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, component.narration());
            if (isFocused() && !component.narrationUsage().getString().isEmpty()) {
                output.add(NarratedElementType.USAGE, component.narrationUsage());
            }
        }

        @Override
        public boolean isActive() {
            return reachable();
        }

        private boolean reachable() {
            return component.visible() && component.enabled();
        }

        /* Whether the pointer rests on the component, in the GUI's pixels. */
        private boolean pointedAt() {
            final Minecraft game = Minecraft.getInstance();
            final double x = game.mouseHandler.xpos() * game.getWindow().getGuiScaledWidth()
                    / game.getWindow().getScreenWidth();
            final double y = game.mouseHandler.ypos() * game.getWindow().getGuiScaledHeight()
                    / game.getWindow().getScreenHeight();
            return component.contains(x, y);
        }
    }
}
