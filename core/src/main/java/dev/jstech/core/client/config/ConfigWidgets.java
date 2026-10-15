/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import dev.jstech.core.api.client.ISkin;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.style.StyleSkin;
import dev.jstech.core.gui.style.StyleRole;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * The small controls of the settings screen that the component kit has no kind for: a group's header that folds it, a
 * setting's name with its marks, an entry of a list with the cross that takes it out, and the button of a single id
 * with the thing's picture. Each draws through the style the screen wears.
 */
final class ConfigWidgets {

    /** The size of a cross, the mark that takes an entry out. */
    static final int CROSS = 7;
    /** How far apart an entry's parts stand. */
    static final int SPACE = 4;

    private ConfigWidgets() {
    }

    /** The colour of {@code role} in the style the screen wears, or the skin's text for a skin that is not a style. */
    static int colour(final ISkin skin, final StyleRole role) {
        return skin instanceof StyleSkin style ? style.colour(role) : skin.text();
    }

    /** Draws the cross that takes an entry out, lit under the pointer. */
    static void cross(final GuiGraphics g, final ISkin skin, final int x, final int y, final boolean lit) {
        final int ink = lit ? colour(skin, StyleRole.WARNING) : skin.dim();
        for (int i = 0; i < CROSS; i++) {
            g.fill(x + i, y + i, x + i + 1, y + i + 1, ink);
            g.fill(x + CROSS - 1 - i, y + i, x + CROSS - i, y + i + 1, ink);
        }
    }

    /** Draws a caret: pointing down while what it heads is open, right while it is folded. */
    static void caret(final GuiGraphics g, final int x, final int y, final boolean open, final int colour) {
        for (int i = 0; i < 4; i++) {
            if (open) {
                g.fill(x + i, y + i, x + 8 - i, y + i + 1, colour);
            } else {
                g.fill(x + i, y + i, x + i + 1, y + 8 - i, colour);
            }
        }
    }

    /** A group's header: its caret, its title, and how many settings it holds; a click or Enter folds it. */
    static final class FoldHeader extends UiComponent {

        private final Component title;
        private final String count;
        private final BooleanSupplier open;
        private final Runnable onToggle;

        FoldHeader(final Component title, final String count, final BooleanSupplier open, final Runnable onToggle) {
            this.title = Objects.requireNonNull(title, "title");
            this.count = Objects.requireNonNull(count, "count");
            this.open = Objects.requireNonNull(open, "open");
            this.onToggle = Objects.requireNonNull(onToggle, "onToggle");
        }

        @Override
        public boolean focusable() {
            return true;
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            final boolean shown = open.getAsBoolean();
            final int ink = shown || hovered(ctx) || isFocused() ? colour(ctx.skin(), StyleRole.HEADING)
                    : ctx.skin().text();
            final int textY = y() + (height() - 8) / 2;
            caret(g, x(), textY, shown, ink);
            final int countWidth = ctx.skin().textWidth(ctx.font(), count);
            ctx.skin().drawText(g, ctx.font(), Texts.clip(ctx.font(), title.getString(), width() - 20 - countWidth),
                    x() + 12, textY, ink);
            ctx.skin().drawText(g, ctx.font(), count, right() - countWidth, textY, ctx.skin().dim());
        }

        @Override
        public boolean mouseClicked(final double mx, final double my, final int button) {
            if (button != 0) {
                return false;
            }
            onToggle.run();
            return true;
        }

        @Override
        public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
                onToggle.run();
                return true;
            }
            return false;
        }
    }

    /**
     * A setting's name, and after it when a change takes hold, and under it the setting it is active under; greyed
     * while it is not active, marked at its left while it holds a change not kept.
     */
    static final class SettingName extends UiComponent {

        private final Supplier<String> name;
        private final List<Mark> marks;
        private final String note;
        private final BooleanSupplier active;
        private final BooleanSupplier changed;
        private final Runnable onClick;

        /**
         * @param name    the setting's name
         * @param marks   the marks after it: when a change takes hold
         * @param note    the line under it, or empty for none
         * @param active  whether the setting is active
         * @param changed whether it holds a change not kept
         * @param onClick what a click on it does: its whole description, pinned
         */
        SettingName(final Supplier<String> name, final List<Mark> marks, final String note,
                    final BooleanSupplier active, final BooleanSupplier changed, final Runnable onClick) {
            this.name = Objects.requireNonNull(name, "name");
            this.marks = List.copyOf(marks);
            this.note = Objects.requireNonNull(note, "note");
            this.active = Objects.requireNonNull(active, "active");
            this.changed = Objects.requireNonNull(changed, "changed");
            this.onClick = Objects.requireNonNull(onClick, "onClick");
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            final ISkin skin = ctx.skin();
            final boolean on = active.getAsBoolean();
            if (changed.getAsBoolean()) {
                g.fill(x() - 4, y() + 2, x() - 2, bottom() - 2, colour(skin, StyleRole.NOTICE));
            }
            final int nameY = note.isEmpty() ? y() + (height() - 8) / 2 : y() + 4;
            final String shown = Texts.clip(ctx.font(), name.get(), width());
            skin.drawText(g, ctx.font(), shown, x(), nameY, on ? skin.text() : skin.dim());
            int markX = x() + skin.textWidth(ctx.font(), shown) + SPACE + 2;
            for (final Mark mark : marks) {
                final int room = right() - markX;
                final int w = skin.textWidth(ctx.font(), mark.text());
                if (w > room) {
                    break;
                }
                skin.drawText(g, ctx.font(), mark.text(), markX, nameY, colour(skin, mark.role()));
                markX += w + SPACE + 2;
            }
            if (!note.isEmpty()) {
                skin.drawText(g, ctx.font(), Texts.clip(ctx.font(), note, width()), x(), nameY + 11,
                        colour(skin, StyleRole.NOTICE));
            }
        }

        @Override
        public boolean mouseClicked(final double mx, final double my, final int button) {
            if (button != 0) {
                return false;
            }
            onClick.run();
            return true;
        }
    }

    /**
     * A mark after a setting's name.
     *
     * @param text what it says
     * @param role the colour it says it in
     */
    record Mark(String text, StyleRole role) {
    }

    /**
     * An entry of a list: the thing's picture in a slot when it names one, what it says, and the cross that takes it
     * out, which a click on it or Delete does.
     */
    static final class Chip extends UiComponent {

        private final String text;
        private final ItemStack icon;
        private final boolean slotted;
        private final List<Component> tip;
        private final Runnable onRemove;

        Chip(final String text, final ItemStack icon, final boolean slotted, final List<Component> tip,
             final Runnable onRemove) {
            this.text = Objects.requireNonNull(text, "text");
            this.icon = icon.copy();
            this.slotted = slotted;
            this.tip = List.copyOf(tip);
            this.onRemove = Objects.requireNonNull(onRemove, "onRemove");
        }

        /** How wide a chip of {@code text} is in {@code skin}, with a slot or without. */
        static int widthOf(final ISkin skin, final Font font, final String text, final boolean slotted) {
            return (slotted ? ISkin.SLOT_SIZE + SPACE : SPACE) + skin.textWidth(font, text) + SPACE + CROSS + 2;
        }

        @Override
        public boolean focusable() {
            return true;
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            final ISkin skin = ctx.skin();
            final int textY = y() + (height() - 8) / 2;
            int textX = x() + SPACE;
            if (slotted) {
                skin.slot(g, x() + 1, y() + (height() - 16) / 2);
                if (!icon.isEmpty()) {
                    g.renderItem(icon, x() + 1, y() + (height() - 16) / 2);
                }
                textX = x() + ISkin.SLOT_SIZE + SPACE;
            } else {
                skin.field(g, x(), y() + 1, width(), height() - 2, isFocused());
            }
            final int ink = slotted ? skin.text() : skin.fieldText();
            skin.drawText(g, ctx.font(), text, textX, textY, ink);
            final int crossX = right() - CROSS - 3;
            cross(g, skin, crossX, y() + (height() - CROSS) / 2, ctx.over(crossX - 1, y(), CROSS + 3, height())
                    || isFocused());
        }

        @Override
        public boolean mouseClicked(final double mx, final double my, final int button) {
            if (button != 0) {
                return false;
            }
            if (mx >= right() - CROSS - 4) {
                onRemove.run();
            }
            return true;
        }

        @Override
        public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
            if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
                onRemove.run();
                return true;
            }
            return false;
        }

        @Override
        public List<Component> tooltip(final double mx, final double my) {
            return tip;
        }
    }

    /** The button of a single id: the thing's picture and name; a click or Enter opens the picker. */
    static final class ThingButton extends UiComponent {

        private final Supplier<ConfigThings.Thing> thing;
        private final Runnable onPress;

        ThingButton(final Supplier<ConfigThings.Thing> thing, final Runnable onPress) {
            this.thing = Objects.requireNonNull(thing, "thing");
            this.onPress = Objects.requireNonNull(onPress, "onPress");
        }

        @Override
        public boolean focusable() {
            return true;
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            final ConfigThings.Thing now = thing.get();
            ctx.skin().button(g, ctx.font(), x(), y(), width(), height(), "", hovered(ctx) || isFocused(), false,
                    false, enabled());
            final int iconY = y() + (height() - 16) / 2;
            if (!now.icon().isEmpty()) {
                g.renderItem(now.icon(), x() + 3, iconY);
            }
            final String name = Texts.clip(ctx.font(), now.name().getString(), width() - 26);
            ctx.skin().drawText(g, ctx.font(), name, x() + 22, y() + (height() - 8) / 2,
                    colour(ctx.skin(), StyleRole.BUTTON_TEXT));
        }

        @Override
        public boolean mouseClicked(final double mx, final double my, final int button) {
            if (button != 0) {
                return false;
            }
            onPress.run();
            return true;
        }

        @Override
        public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
                onPress.run();
                return true;
            }
            return false;
        }

        @Override
        public List<Component> tooltip(final double mx, final double my) {
            return List.of(Component.literal(thing.get().id()));
        }
    }
}
