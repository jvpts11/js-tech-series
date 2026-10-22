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
import java.util.Locale;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * An entry of the settings screen's rail: a section, or another mod with all its settings of the tab, with its name,
 * how many settings it holds, and a mark while one of them holds a change, lit while it is open and framed while the
 * keyboard rests on it; or a heading over the entries after it, the series' and the other mods', which takes no
 * click.
 */
final class ConfigRailEntry extends UiComponent {

    private final int index;
    private final Component title;
    private final int count;
    private final boolean heading;
    private final BooleanSupplier open;
    private final BooleanSupplier changed;
    private final Runnable onSelect;
    private boolean keyboard;

    /** Where the mark of a change sits, in from the entry's right end. */
    private static final int CHANGED_MARK = 26;

    ConfigRailEntry(final int index, final ConfigScreenModel.Section section, final BooleanSupplier open,
                    final BooleanSupplier changed, final Runnable onSelect) {
        this(index, section.title(), section.count(), false, open, changed, onSelect);
    }

    ConfigRailEntry(final int index, final Component title, final int count, final BooleanSupplier open,
                    final BooleanSupplier changed, final Runnable onSelect) {
        this(index, title, count, false, open, changed, onSelect);
    }

    private ConfigRailEntry(final int index, final Component title, final int count, final boolean heading,
                            final BooleanSupplier open, final BooleanSupplier changed, final Runnable onSelect) {
        this.index = index;
        this.title = title;
        this.count = count;
        this.heading = heading;
        this.open = open;
        this.changed = changed;
        this.onSelect = onSelect;
    }

    /** A heading of the rail, over the entries after it. */
    static ConfigRailEntry heading(final int index, final Component title) {
        return new ConfigRailEntry(index, title, 0, true, () -> false, () -> false, () -> { });
    }

    /** Its place on the rail, counted from the top. */
    int index() {
        return index;
    }

    /** Whether it is a heading, which takes no click and no keyboard. */
    boolean isHeading() {
        return heading;
    }

    /** Whether what it is stands open. */
    boolean isOpen() {
        return open.getAsBoolean();
    }

    /** How tall it stands on the rail. */
    int slot() {
        return heading ? ConfigScreenLayout.RAIL_HEADING : ConfigScreenLayout.RAIL_ITEM;
    }

    /** Opens what it is, as a click does. */
    void select() {
        onSelect.run();
    }

    /** Whether the keyboard rests on it. */
    boolean keyboard() {
        return keyboard;
    }

    void setKeyboard(final boolean value) {
        keyboard = value && !heading;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        if (!visible()) {
            return;
        }
        if (heading) {
            Draw.textScaled(g, ctx.font(), title.getString().toUpperCase(Locale.ROOT), x() + ConfigScreenLayout
                    .RAIL_INSET - 2, y() + (height() - 6) / 2, ctx.skin().dim(), ConfigScreenLayout.HEADING_SCALE);
            return;
        }
        final boolean lit = open.getAsBoolean();
        ctx.skin().listRow(g, x(), y(), width(), height(), hovered(ctx) || keyboard, lit);
        final String shownCount = String.valueOf(count);
        final int countWidth = ctx.skin().textWidth(ctx.font(), shownCount);
        final int textY = y() + (height() - 8) / 2;
        // A name too long for one line takes two, which the entry is tall enough for; what is left is cut short.
        final int room = width() - 2 * ConfigScreenLayout.RAIL_INSET - ConfigScreenLayout.RAIL_COUNT;
        final List<String> lines = ConfigScreenLayout.nameLines(title.getString(), room,
                text -> ctx.skin().textWidth(ctx.font(), text));
        final int firstY = y() + (height() - 8 - (lines.size() - 1) * ConfigScreenLayout.LINE) / 2;
        for (int i = 0; i < lines.size(); i++) {
            ctx.skin().drawText(g, ctx.font(), Texts.clip(ctx.font(), lines.get(i), room),
                    x() + ConfigScreenLayout.RAIL_INSET - 2, firstY + i * ConfigScreenLayout.LINE,
                    ctx.skin().listRowText(lit));
        }
        ctx.skin().drawText(g, ctx.font(), shownCount, right() - 6 - countWidth, textY, ctx.skin().dim());
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
