/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.bus;

import dev.jstech.computers.bus.BusActivity;
import dev.jstech.computers.gui.layout.BusLayout;
import dev.jstech.computers.menu.AbstractBusMenu;
import dev.jstech.computers.operation.payload.BusStatePayload;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * The Activity tab of a bus's window: what the bus did lately, newest first, each move as the Operation it was and each
 * time it held back with why, at the hour of the day it happened.
 */
final class BusActivityView {

    private final AbstractBusMenu menu;
    private final Font font;
    private int scroll;

    /** The ticks of a game day, and of one of its hours. */
    private static final long DAY = 24_000L;
    private static final long HOUR = 1_000L;
    private static final int HOURS_BEFORE_DAWN = 6;
    private static final int MINUTES = 60;

    BusActivityView(final AbstractBusMenu menu, final Font font) {
        this.menu = menu;
        this.font = font;
    }

    void render(final GuiGraphics g, final int left, final int top) {
        final List<BusActivity.Entry> entries = entries();
        final int viewTop = top + BusLayout.VIEW_Y;
        final int content = entries.size() * BusLayout.ENTRY_H;
        scroll = Math.max(0, Math.min(scroll, content - BusLayout.ACTIVITY_VIEW_H));
        if (entries.isEmpty()) {
            BusDraw.small(g, font, GameText.resolve(BusTexts.NO_ACTIVITY), left + BusLayout.LABEL_X, viewTop + 2,
                    JsTechTheme.dim());
        }
        g.enableScissor(left + BusLayout.LABEL_X, viewTop, left + BusLayout.RIGHT,
                viewTop + BusLayout.ACTIVITY_VIEW_H);
        for (int i = 0; i < entries.size(); i++) {
            final int y = viewTop + i * BusLayout.ENTRY_H - scroll;
            if (y + BusLayout.ENTRY_H > viewTop && y < viewTop + BusLayout.ACTIVITY_VIEW_H) {
                drawEntry(g, entries.get(i), left, y);
            }
        }
        g.disableScissor();
        BusDraw.scrollbar(g, left + BusLayout.SCROLL_X, viewTop, BusLayout.ACTIVITY_VIEW_H, scroll, content);
        final List<String> note = BusDraw.lines(font, GameText.resolve(BusTexts.ACTIVITY_NOTE), BusLayout.ROW_W);
        for (int i = 0; i < Math.min(BusLayout.ACTIVITY_NOTE_LINES, note.size()); i++) {
            BusDraw.small(g, font, note.get(i), left + BusLayout.LABEL_X,
                    top + BusLayout.ACTIVITY_NOTE_Y + i * BusLayout.LINE, JsTechTheme.dim());
        }
    }

    boolean scrolled(final double mx, final double my, final double delta, final int left, final int top) {
        final int content = entries().size() * BusLayout.ENTRY_H;
        if (!BusDraw.inside(mx, my, left + BusLayout.LABEL_X, top + BusLayout.VIEW_Y, BusLayout.ROW_W + 6,
                BusLayout.ACTIVITY_VIEW_H) || content <= BusLayout.ACTIVITY_VIEW_H) {
            return false;
        }
        scroll = Math.max(0, Math.min(content - BusLayout.ACTIVITY_VIEW_H,
                scroll - (int) Math.signum(delta) * BusLayout.ENTRY_H));
        return true;
    }

    /* One line of the log: when and what on the first, why and how it ended on the second. */
    private void drawEntry(final GuiGraphics g, final BusActivity.Entry entry, final int left, final int y) {
        BusDraw.bar(g, left + BusLayout.LABEL_X, y, BusLayout.ROW_W, BusLayout.ENTRY_H - 1, false);
        final String time = clock(entry.time());
        final int x = left + BusLayout.LABEL_X + 3;
        BusDraw.small(g, font, time, x, y + 2, JsTechTheme.dim());
        final int whatX = x + BusDraw.width(font, time) + 5;
        final String what = what(entry);
        BusDraw.small(g, font, BusDraw.clip(font, what, left + BusLayout.RIGHT - 3 - whatX), whatX, y + 2,
                JsTechTheme.text());
        final String status = GameText.resolve(status(entry.status()));
        final int statusW = BusDraw.width(font, status);
        BusDraw.smallRight(g, font, status, left + BusLayout.RIGHT - 3, y + 10, colour(entry.status()));
        BusDraw.small(g, font, BusDraw.clip(font, reason(entry), BusLayout.ROW_W - 11 - statusW), x, y + 10,
                JsTechTheme.dim());
    }

    /** The hour of the day {@code time} was, as the clock read: what happened at a game time, read on today's. */
    private static String clock(final long time) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return "--:--";
        }
        final long day = Math.floorMod(mc.level.getDayTime() - (mc.level.getGameTime() - time), DAY);
        final long hour = (day / HOUR + HOURS_BEFORE_DAWN) % 24L;
        final long minute = day % HOUR * MINUTES / HOUR;
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    private static String what(final BusActivity.Entry entry) {
        if (entry.what().isEmpty()) {
            return "";
        }
        final Component name = BusKeys.name(entry.what());
        return entry.amount() > 0L ? GameText.resolve(BusTexts.AMOUNT.with(name.getString(), entry.amount()))
                : name.getString();
    }

    private String reason(final BusActivity.Entry entry) {
        return GameText.resolve(switch (entry.reason()) {
            case BusActivity.MOVED -> menu.exports() ? BusTexts.INTO_CHEST.text() : BusTexts.INTO_NETWORK.text();
            case BusActivity.KEEPS -> BusTexts.CHEST_KEEPS.with(entry.detail());
            case BusActivity.FULL -> menu.exports() ? BusTexts.CHEST_FULL.text() : BusTexts.NETWORK_FULL.text();
            default -> BusTexts.CONDITION_HOLDS.text();
        });
    }

    private static TextKey status(final byte status) {
        return switch (status) {
            case BusActivity.COMPLETED -> BusTexts.STATUS_COMPLETED;
            case BusActivity.PARTIAL -> BusTexts.STATUS_PARTIAL;
            case BusActivity.LOCKED -> BusTexts.STATUS_LOCKED;
            default -> BusTexts.STATUS_WAITING;
        };
    }

    private static int colour(final byte status) {
        return switch (status) {
            case BusActivity.COMPLETED, BusActivity.PARTIAL -> JsTechTheme.green();
            case BusActivity.LOCKED -> JsTechTheme.red();
            default -> JsTechTheme.amber();
        };
    }

    private List<BusActivity.Entry> entries() {
        final BusStatePayload state = menu.state();
        return state == null ? List.of() : state.activity();
    }
}
