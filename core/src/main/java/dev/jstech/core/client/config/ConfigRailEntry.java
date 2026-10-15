/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.gui.layout.ConfigScreenLayout;
import dev.jstech.core.gui.style.StyleRole;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A section of the settings screen's rail: its name, how many settings it holds, and a mark while one of them holds a
 * change; lit while it is open, and framed while the keyboard rests on it.
 */
final class ConfigRailEntry extends UiComponent {

    private final int index;
    private final ConfigScreenModel.Section section;
    private final BooleanSupplier open;
    private final BooleanSupplier changed;
    private final Runnable onSelect;
    private boolean keyboard;

    /** Where the mark of a change sits, in from the entry's right end. */
    private static final int CHANGED_MARK = 26;

    ConfigRailEntry(final int index, final ConfigScreenModel.Section section, final BooleanSupplier open,
                    final BooleanSupplier changed, final Runnable onSelect) {
        this.index = index;
        this.section = section;
        this.open = open;
        this.changed = changed;
        this.onSelect = onSelect;
    }

    /** Its place on the rail, counted from the top. */
    int index() {
        return index;
    }

    /** Opens its section, as a click does. */
    void select() {
        onSelect.run();
    }

    /** Whether the keyboard rests on it. */
    boolean keyboard() {
        return keyboard;
    }

    void setKeyboard(final boolean value) {
        keyboard = value;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        if (!visible()) {
            return;
        }
        final boolean lit = open.getAsBoolean();
        ctx.skin().listRow(g, x(), y(), width(), height(), hovered(ctx) || keyboard, lit);
        final String count = String.valueOf(section.count());
        final int countWidth = ctx.skin().textWidth(ctx.font(), count);
        final int textY = y() + (height() - 8) / 2;
        // A name too long for one line takes two, which the entry is tall enough for; what is left is cut short.
        final int room = width() - 2 * ConfigScreenLayout.RAIL_INSET - ConfigScreenLayout.RAIL_COUNT;
        final List<String> lines = ConfigScreenLayout.nameLines(section.title().getString(), room,
                text -> ctx.skin().textWidth(ctx.font(), text));
        final int firstY = y() + (height() - 8 - (lines.size() - 1) * ConfigScreenLayout.LINE) / 2;
        for (int i = 0; i < lines.size(); i++) {
            ctx.skin().drawText(g, ctx.font(), Texts.clip(ctx.font(), lines.get(i), room),
                    x() + ConfigScreenLayout.RAIL_INSET - 2, firstY + i * ConfigScreenLayout.LINE,
                    ctx.skin().listRowText(lit));
        }
        ctx.skin().drawText(g, ctx.font(), count, right() - 6 - countWidth, textY, ctx.skin().dim());
        if (changed.getAsBoolean()) {
            final int mark = right() - CHANGED_MARK;
            g.fill(mark, textY + 2, mark + 4, textY + 6, ConfigWidgets.colour(ctx.skin(), StyleRole.NOTICE));
        }
        if (keyboard) {
            Draw.outline(g, x() - 1, y() - 1, width() + 2, height() + 2, ConfigWidgets.colour(ctx.skin(),
                    StyleRole.MENU_TEXT));
        }
    }
}
