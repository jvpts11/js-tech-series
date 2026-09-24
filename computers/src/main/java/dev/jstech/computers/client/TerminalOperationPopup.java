/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.gui.layout.ComputerTerminalLayout;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What one Operation is made of: the sub-operations it was broken into, each with the server doing it and
 * how far that share has got.
 *
 * <p>An Operation in flight is followed rather than frozen: the record that was clicked is replaced each
 * frame by the newest live one for the same work, so a craft opened half way through fills in while it is
 * being watched. A finished one has no sub-operations left, only the moves it made, and those are what it
 * shows instead.
 */
final class TerminalOperationPopup {

    private static final int POPUP_W = ComputerTerminalLayout.POPUP_W;
    private static final int POPUP_H = ComputerTerminalLayout.POPUP_H;
    private static final int ROWS = 7;

    private final ComputerTerminalScreen screen;
    private final ComputerTerminalMenu menu;

    @Nullable
    private OperationRecord shown;
    private int scroll;

    TerminalOperationPopup(final ComputerTerminalScreen screen, final ComputerTerminalMenu menu) {
        this.screen = screen;
        this.menu = menu;
    }

    boolean isOpen() {
        return this.shown != null;
    }

    void open(final OperationRecord op) {
        this.shown = op;
        this.scroll = 0;
    }

    void close() {
        this.shown = null;
    }

    boolean scrolled(final double dy) {
        if (this.shown == null || dy == 0) {
            return false;
        }
        this.scroll = Math.max(0, this.scroll - (int) Math.signum(dy));
        return true;
    }

    /** Modal: a right click closes it, a click outside it closes it, and nothing else gets through. */
    boolean clicked(final double mouseX, final double mouseY, final int button) {
        if (button == 1) {
            close();
            return true;
        }
        if (button == 0) {
            final int px = x();
            final int py = y();
            if (mouseX < px || mouseX >= px + POPUP_W || mouseY < py || mouseY >= py + POPUP_H) {
                close();
            }
        }
        return true;
    }

    void render(final GuiGraphics g) {
        if (this.shown == null) {
            return;
        }
        follow();
        final OperationRecord op = this.shown;
        /*
         * Push above the item grid and dim the screen, so the rail and the bar behind never show through
         * the panel (flat fills draw below items otherwise).
         */
        g.pose().pushPose();
        g.pose().translate(0, 0, 350);
        final int left = screen.left();
        final int top = screen.top();
        g.fill(left, top, left + ComputerTerminalLayout.WIDTH, top + ComputerTerminalLayout.HEIGHT,
                TerminalPopupPalette.get().veil());
        final int px = x();
        final int py = y();
        g.fill(px - 2, py - 2, px + POPUP_W + 2, py + POPUP_H + 2, TerminalPopupPalette.get().rim());
        g.fill(px, py, px + POPUP_W, py + POPUP_H, JsTechTheme.panel());
        g.fill(px, py, px + POPUP_W, py + 1, JsTechTheme.accent());
        screen.drawDataIcon(g, op.key(), -1L, px + 6, py + 5);
        g.drawString(screen.tabFont(), ComputerTerminalScreen.opTypeLabel(op.type()) + "  "
                + op.name().getString(), px + 28, py + 6, JsTechTheme.text(), false);
        g.drawString(screen.tabFont(), GameText.resolve(TerminalUpkeepTexts.MOVED_OF.with(
                        ComputerTerminalScreen.fmt(op.moved()), ComputerTerminalScreen.fmt(op.requested()),
                        ComputerTerminalScreen.statusLabel(op.status()))),
                px + 28, py + 17, screen.statusColor(op.status()), false);
        final double f = op.requested() <= 0
                ? (op.status() == OperationRecord.STATUS_PROCESSING ? 0 : 1)
                : Math.min(1.0, (double) op.moved() / op.requested());
        screen.track(g, px + 6, py + 30, POPUP_W - 12, f,
                op.status() == OperationRecord.STATUS_PROCESSING
                        ? JsTechTheme.accent2() : screen.statusColor(op.status()));
        g.drawString(screen.tabFont(), GameText.resolve(TerminalUpkeepTexts.SUBOPERATIONS), px + 6, py + 42,
                JsTechTheme.dim(), false);
        /*
         * A live Operation carries its real SubOperation rows (per-server share, progress, state);
         * a finished log entry carries only its provenance moves, so render whichever it has.
         */
        final List<OperationRecord.SubRow> subs = op.subs();
        final List<OperationRecord.MoveRow> moves = op.moves();
        if (!subs.isEmpty()) {
            final int start = clampScroll(subs.size());
            for (int i = 0; i < ROWS && start + i < subs.size(); i++) {
                subRow(g, px, py + 56 + i * 12, subs.get(start + i));
            }
        } else if (moves.isEmpty()) {
            g.drawString(screen.tabFont(), GameText.resolve(TerminalUpkeepTexts.NO_MOVEMENT), px + 6, py + 56,
                    JsTechTheme.dim(), false);
        } else {
            final int start = clampScroll(moves.size());
            for (int i = 0; i < ROWS && start + i < moves.size(); i++) {
                screen.moveRow(g, px, py + 56 + i * 12, moves.get(start + i));
            }
        }
        final String hint = GameText.resolve(TerminalUpkeepTexts.RIGHT_CLICK_TO_CLOSE);
        g.drawString(screen.tabFont(), hint, px + POPUP_W - screen.tabFont().width(hint) - 6,
                py + POPUP_H - 10, JsTechTheme.dim(), false);
        g.pose().popPose();
    }

    /** Keeps an Operation still in flight on its newest record, so the popup fills in while it is watched. */
    private void follow() {
        final OperationRecord op = this.shown;
        if (op == null || (op.status() != OperationRecord.STATUS_PROCESSING
                && op.status() != OperationRecord.STATUS_WAITING
                && op.status() != OperationRecord.STATUS_PENDING)) {
            return;
        }
        for (final OperationRecord live : menu.activeOps()) {
            if (live.type() == op.type() && live.key().equals(op.key())
                    && live.requested() == op.requested()) {
                this.shown = live;
                return;
            }
        }
    }

    private int x() {
        return screen.left() + (ComputerTerminalLayout.WIDTH - POPUP_W) / 2;
    }

    private int y() {
        return screen.top() + (ComputerTerminalLayout.HEIGHT - POPUP_H) / 2;
    }

    private int clampScroll(final int size) {
        this.scroll = Math.max(0, Math.min(Math.max(0, size - ROWS), this.scroll));
        return this.scroll;
    }

    private void subRow(final GuiGraphics g, final int px, final int my, final OperationRecord.SubRow sub) {
        g.drawString(screen.tabFont(), screen.tabFont().plainSubstrByWidth(sub.server(), 62), px + 6, my,
                JsTechTheme.dim(), false);
        g.drawString(screen.tabFont(), ComputerTerminalScreen.fmt(sub.moved()) + " / "
                + ComputerTerminalScreen.fmt(sub.planned()), px + 72, my, JsTechTheme.text(), false);
        final String state = GameText.resolve(switch (sub.state()) {
            case OperationRecord.SubRow.SUB_READING -> TerminalUpkeepTexts.READING;
            case OperationRecord.SubRow.SUB_STREAMING -> TerminalUpkeepTexts.STREAMING;
            case OperationRecord.SubRow.SUB_COMPLETED -> TerminalUpkeepTexts.SUB_DONE;
            default -> TerminalUpkeepTexts.QUEUED;
        });
        final int color = switch (sub.state()) {
            case OperationRecord.SubRow.SUB_READING -> JsTechTheme.amber();
            case OperationRecord.SubRow.SUB_STREAMING -> JsTechTheme.accent2();
            case OperationRecord.SubRow.SUB_COMPLETED -> JsTechTheme.green();
            default -> JsTechTheme.dim();
        };
        g.drawString(screen.tabFont(), state, px + POPUP_W - screen.tabFont().width(state) - 6, my,
                color, false);
    }
}
