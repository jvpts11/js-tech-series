/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.client.os.BorderLinkTexts;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.gui.layout.ComputerTerminalLayout;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.operation.payload.NetworkEventsPayload;
import dev.jstech.computers.operation.payload.wan.BorderLinkStatePayload;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.time.GameCalendar;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * BorderLink's Log heading on MC-NET: the lines the gateway wrote into its network's log, newest first, its lines
 * coming up and going down, each with the world's clock and its level.
 */
final class BorderLinkLogTab extends AbstractTerminalTab {

    /* How far down the list is scrolled, in rows. */
    private int scroll;

    private static final int HEADING_Y = 19;
    private static final int ROWS_Y = 32;
    private static final int ROW = 11;
    /* The space has no inventory under it: the list runs to the foot of the glass. */
    private static final int BOTTOM = ComputerTerminalLayout.HEIGHT - 4;
    private static final int TIME_W = 40;
    private static final int LEVEL_W = 36;

    BorderLinkLogTab(final ComputerTerminalScreen screen, final ComputerTerminalMenu menu) {
        super(screen, menu);
    }

    @Override
    public void renderTabBg(final GuiGraphics g, final int x, final int y, final int cx, final int cy, final int cw,
                            final int mouseX, final int mouseY, final float partialTick) {
        g.fill(cx, cy + ROWS_Y - 3, cx + cw, cy + ROWS_Y - 2, LINE());
    }

    @Override
    public void renderTabLabels(final GuiGraphics g, final int cx, final int cy, final int cw) {
        final BorderLinkStatePayload state = menu.borderLink();
        Draw.text(g, font(), GameText.resolve(BorderLinkTexts.LOG), cx, cy + HEADING_Y, DIM());
        if (state == null) {
            Draw.text(g, font(), GameText.resolve(BorderLinkTexts.ASKING), cx, cy + ROWS_Y, DIM());
            return;
        }
        final String whose = GameText.resolve(BorderLinkTexts.LOG_WHOSE.with(state.name().toUpperCase(Locale.ROOT)));
        Draw.text(g, font(), whose, cx + cw - font().width(whose), cy + HEADING_Y, DIM());
        final List<NetworkEventsPayload.Row> rows = state.log();
        if (rows.isEmpty()) {
            Draw.text(g, font(), GameText.resolve(BorderLinkTexts.NOTHING_LOGGED), cx, cy + ROWS_Y, DIM());
            return;
        }
        scroll = Math.max(0, Math.min(scroll, rows.size() - visible()));
        for (int i = 0; i < visible() && scroll + i < rows.size(); i++) {
            final NetworkEventsPayload.Row row = rows.get(scroll + i);
            final int ry = cy + ROWS_Y + i * ROW;
            final EventLevel level = EventLevel.byId(row.level());
            Draw.text(g, font(), timeOf(row), cx, ry, DIM());
            Draw.text(g, font(), GameText.resolve(level.label()), cx + TIME_W, ry, colourOf(level));
            final String message = GameText.resolve(row.message());
            final int width = cw - TIME_W - LEVEL_W;
            Draw.text(g, font(), font().width(message) <= width ? message
                    : font().plainSubstrByWidth(message, Math.max(0, width - 6)) + "..", cx + TIME_W + LEVEL_W, ry,
                    TEXT());
        }
    }

    @Override
    public boolean onMouseScrolled(final double mouseX, final double mouseY, final double dy) {
        scroll = Math.max(0, scroll - (int) Math.signum(dy));
        return true;
    }

    /* How many rows fit between the heading and the foot of the glass. */
    private static int visible() {
        return (BOTTOM - ROWS_Y) / ROW;
    }

    private int colourOf(final EventLevel level) {
        return switch (level) {
            case INFO -> ACCENT2();
            case WARN -> AMBER();
            case ERROR -> RED();
        };
    }

    /* The world's clock for an event of today, else its day. */
    private static String timeOf(final NetworkEventsPayload.Row row) {
        final Minecraft mc = Minecraft.getInstance();
        final long today = mc.level == null ? -1L : GameCalendar.day(mc.level.getDayTime());
        final long day = GameCalendar.day(row.dayTime());
        if (day == today) {
            return String.format(Locale.ROOT, "%02d:%02d", GameCalendar.hourOf(row.dayTime()),
                    GameCalendar.minuteOf(row.dayTime()));
        }
        return GameText.resolve(BorderLinkTexts.DAY.with(day));
    }
}
