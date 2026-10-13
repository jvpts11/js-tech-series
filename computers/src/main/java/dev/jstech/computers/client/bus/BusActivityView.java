/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.bus;

import dev.jstech.computers.bus.BusActivity;
import dev.jstech.computers.crafting.CraftingLog;
import dev.jstech.computers.gui.layout.BusLayout;
import dev.jstech.computers.menu.AbstractBusMenu;
import dev.jstech.computers.operation.payload.BusStatePayload;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The Activity tab of a bus's window: what the bus did lately, newest first, each move as the Operation it was and each
 * time it held back with why, at the hour of the day it happened.
 */
final class BusActivityView {

    private final AbstractBusMenu menu;
    private final Font font;
    private int scroll;
    private final ScrollGrab bar = new ScrollGrab();

    BusActivityView(final AbstractBusMenu menu, final Font font) {
        this.menu = menu;
        this.font = font;
    }

    void render(final GuiGraphics g, final int left, final int top) {
        final boolean credits = menu.window() == BusLayout.Window.RECEIVING;
        final List<BusActivity.Entry> entries = entries();
        final List<CraftingLog.Entry> creditEntries = creditEntries();
        final int count = credits ? creditEntries.size() : entries.size();
        final int viewTop = top + BusLayout.VIEW_Y;
        final int content = count * BusLayout.ENTRY_H;
        scroll = Math.max(0, Math.min(scroll, content - BusLayout.ACTIVITY_VIEW_H));
        if (count == 0) {
            BusDraw.small(g, font, GameText.resolve(BusTexts.NO_ACTIVITY), left + BusLayout.LABEL_X, viewTop + 2,
                    JsTechTheme.dim());
        }
        g.enableScissor(left + BusLayout.LABEL_X, viewTop, left + BusLayout.RIGHT,
                viewTop + BusLayout.ACTIVITY_VIEW_H);
        for (int i = 0; i < count; i++) {
            final int y = viewTop + i * BusLayout.ENTRY_H - scroll;
            if (y + BusLayout.ENTRY_H > viewTop && y < viewTop + BusLayout.ACTIVITY_VIEW_H) {
                if (credits) {
                    drawCredit(g, creditEntries.get(i), left, y);
                } else {
                    drawEntry(g, entries.get(i), left, y);
                }
            }
        }
        g.disableScissor();
        BusDraw.scrollbar(g, left + BusLayout.SCROLL_X, viewTop, BusLayout.ACTIVITY_VIEW_H, scroll, content);
        bar.drawn(left + BusLayout.SCROLL_X, viewTop, BusLayout.ACTIVITY_VIEW_H, content);
        final List<String> note = BusDraw.lines(font, GameText.resolve(credits ? BusTexts.CREDIT_NOTE
                : BusTexts.ACTIVITY_NOTE), BusLayout.ROW_W);
        for (int i = 0; i < Math.min(BusLayout.ACTIVITY_NOTE_LINES, note.size()); i++) {
            BusDraw.small(g, font, note.get(i), left + BusLayout.LABEL_X,
                    top + BusLayout.ACTIVITY_NOTE_Y + i * BusLayout.LINE, JsTechTheme.dim());
        }
    }

    /** A press on the scrollbar takes hold of it and puts the thumb under the pointer; false for a press elsewhere. */
    boolean pressBar(final double mx, final double my) {
        if (!bar.press(mx, my)) {
            return false;
        }
        scroll = bar.scrollAt(my);
        return true;
    }

    /** The pointer dragged with the scrollbar held: the list follows; false while it is not held. */
    boolean dragBar(final double my) {
        if (!bar.held()) {
            return false;
        }
        scroll = bar.scrollAt(my);
        return true;
    }

    void releaseBar() {
        bar.release();
    }

    boolean scrolled(final double mx, final double my, final double delta, final int left, final int top) {
        final int content = (menu.window() == BusLayout.Window.RECEIVING ? creditEntries().size()
                : entries().size()) * BusLayout.ENTRY_H;
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
        final String time = BusDraw.clock(entry.time());
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

    /* One arrival a Receiving Bus credited: when and what on the first line, where it went on the second. */
    private void drawCredit(final GuiGraphics g, final CraftingLog.Entry entry, final int left, final int y) {
        BusDraw.bar(g, left + BusLayout.LABEL_X, y, BusLayout.ROW_W, BusLayout.ENTRY_H - 1, false);
        final String time = BusDraw.clock(entry.time());
        final int x = left + BusLayout.LABEL_X + 3;
        BusDraw.small(g, font, time, x, y + 2, JsTechTheme.dim());
        final int whatX = x + BusDraw.width(font, time) + 5;
        final String what = GameText.resolve(BusTexts.AMOUNT.with(BusKeys.name(entry.what()).getString(),
                entry.amount()));
        BusDraw.small(g, font, BusDraw.clip(font, what, left + BusLayout.RIGHT - 3 - whatX), whatX, y + 2,
                JsTechTheme.text());
        final TextKey word = switch (entry.kind()) {
            case CraftingLog.CREDITED -> BusTexts.STATUS_CREDITED;
            case CraftingLog.LATE -> BusTexts.STATUS_LATE;
            default -> BusTexts.STATUS_UNEXPECTED;
        };
        final String status = GameText.resolve(word);
        final int statusW = BusDraw.width(font, status);
        BusDraw.smallRight(g, font, status, left + BusLayout.RIGHT - 3, y + 10,
                entry.kind() == CraftingLog.CREDITED ? JsTechTheme.green() : JsTechTheme.amber());
        final String where = GameText.resolve(switch (entry.kind()) {
            case CraftingLog.CREDITED -> BusTexts.CREDITED.with(entry.note());
            case CraftingLog.LATE -> BusTexts.LATE_TO.text();
            default -> BusTexts.UNEXPECTED_TO.text();
        });
        BusDraw.small(g, font, BusDraw.clip(font, where, BusLayout.ROW_W - 11 - statusW), x, y + 10,
                JsTechTheme.dim());
    }

    private List<CraftingLog.Entry> creditEntries() {
        final BusStatePayload state = menu.state();
        return state == null ? List.of() : state.crafting().log();
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
