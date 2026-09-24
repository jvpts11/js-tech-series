/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.program.OperationPalette;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The Operations tab of the Network Interactor: what the network is doing and what it has just done.
 *
 * <p>Two lists arrive from the machine and read as one. The ones under way come first and the ones already
 * finished after them, because a player opening this tab is almost always asking about something happening
 * now, and a log that buried it under yesterday's crafts would be answering a question nobody asked.
 *
 * <p>A row says four things at a glance: whether it is live, what kind of Operation it is, what it is on,
 * and where it has got to. Opening one shows how much of what it asked for has moved, and then either the
 * machines sharing the work or the places the items came from, depending on which of the two it has.
 *
 * <p>The short and long names of a status live here rather than with the status itself, because they are
 * what this tab shows and nothing else reads them. The colours say the same thing a second way: green for
 * finished, amber for still going, red for anything that ended badly.
 */
final class InteractorOps {

    /** What the network has finished lately, and what it is doing now. */
    private final List<OperationRecord> recent = new ArrayList<>();
    private final List<OperationRecord> active = new ArrayList<>();

    /** The row whose detail is showing, or -1 while none is. */
    private int selected = -1;

    /** How many of a supercomputer's parallel craft slots are taken, and how many it has. */
    private int slotsUsed;
    private int slotsTotal;

    private static final int AMBER = 0xFFE6A93A;
    private static final int ONLINE_GREEN = 0xFF2E8B45;

    private final NetworkInteractorApp app;

    InteractorOps(final NetworkInteractorApp app) {
        this.app = app;
    }

    /** The short word a row ends with, which is where an Operation has got to. */
    static String statusShort(final byte status) {
        return GameText.resolve(switch (status) {
            case 0 -> NetworkInteractorTexts.SHORT_DONE;
            case 1 -> NetworkInteractorTexts.SHORT_PARTIAL;
            case 2 -> NetworkInteractorTexts.SHORT_FAILED;
            case 3 -> NetworkInteractorTexts.SHORT_RUNNING;
            case 4 -> NetworkInteractorTexts.SHORT_WAITING;
            case 5 -> NetworkInteractorTexts.SHORT_LOCKED;
            case 6 -> NetworkInteractorTexts.SHORT_PENDING;
            default -> NetworkInteractorTexts.SHORT_DROPPED;
        });
    }

    /** The same thing written out, for the detail panel where there is room for it. */
    static String statusLong(final byte status) {
        return GameText.resolve(switch (status) {
            case 0 -> NetworkInteractorTexts.COMPLETED;
            case 1 -> NetworkInteractorTexts.COMPLETED_PARTIAL;
            case 2 -> NetworkInteractorTexts.FAILED;
            case 3 -> NetworkInteractorTexts.PROCESSING;
            case 4 -> NetworkInteractorTexts.WAITING;
            case 5 -> NetworkInteractorTexts.RESOURCE_LOCKED;
            case 6 -> NetworkInteractorTexts.PENDING;
            default -> NetworkInteractorTexts.DISCARDED;
        });
    }

    /** Green for finished, amber for still going, red for anything that ended badly. */
    static int statusColor(final byte status) {
        return switch (status) {
            case 0 -> ONLINE_GREEN;        // completed
            case 1, 3, 4, 6 -> 0xFFB8860B; // partial / processing / waiting / pending: amber
            case 2, 5, 7 -> 0xFFB23A3A;    // failed / locked / discarded: red
            default -> 0xFF6A7280;
        };
    }

    /** Where one machine's share of a distributed Operation has got to. */
    static String subStateLabel(final byte state) {
        return GameText.resolve(switch (state) {
            case 1 -> NetworkInteractorTexts.SUB_READING;
            case 2 -> NetworkInteractorTexts.SUB_STREAMING;
            case 3 -> NetworkInteractorTexts.SUB_DONE;
            default -> NetworkInteractorTexts.SUB_QUEUED;
        });
    }

    /** Every Operation the tab lists: the live ones first, then the recent log. */
    List<OperationRecord> all() {
        final List<OperationRecord> out = new ArrayList<>(active.size() + recent.size());
        out.addAll(active);
        out.addAll(recent);
        return out;
    }

    /** The log the machine has just sent. */
    void setRecent(final List<OperationRecord> ops) {
        recent.clear();
        recent.addAll(ops);
    }

    /** What the machine is doing now, with how much of a supercomputer's parallel capacity it is using. */
    void setActive(final List<OperationRecord> ops, final int used, final int total) {
        active.clear();
        active.addAll(ops);
        slotsUsed = used;
        slotsTotal = total;
    }

    /** The row whose detail is showing, or -1. */
    int selected() {
        return selected;
    }

    /** Puts the detail away, which leaving the tab does. */
    void clearSelection() {
        selected = -1;
    }

    /** How many Operations are live, which the network's own card reports. */
    int liveCount() {
        return active.size();
    }

    /** What the tab's caption says it is showing. */
    String caption() {
        return GameText.resolve(NetworkInteractorTexts.OPS_CAPTION.with(active.size(), recent.size()));
    }

    /** The strip above the list: a supercomputer's craft capacity, or a word when there is nothing to show. */
    void renderHeader(final GuiGraphics g, final Font font, final int listLeft, final int gridTop) {
        // Supercomputer parallel craft-slot capacity, amber once every slot is taken.
        if (slotsTotal > 0) {
            Texts.small(g, font,
                    GameText.resolve(NetworkInteractorTexts.SUPERCOMPUTER_SLOTS.with(slotsUsed, slotsTotal)),
                    listLeft + 2, gridTop - 9, slotsUsed >= slotsTotal ? AMBER : app.panelSkin().dim());
        }
        if (all().isEmpty()) {
            Texts.small(g, font, GameText.resolve(NetworkInteractorTexts.NO_OPERATIONS), listLeft + 2, gridTop + 4,
                    app.panelSkin().dim());
        }
    }

    /** One row: a live mark, the kind of Operation, what it is on, and where it has got to. */
    void renderRow(final GuiGraphics g, final UiContext ctx, final OperationRecord op, final int index,
                   final int x, final int y, final int w, final int h, final boolean hovered,
                   final boolean picked) {
        final boolean live = index < active.size();
        if (index == selected) {
            g.fill(x, y, x + w, y + h, 0x552F6AC6);
        } else if (hovered) {
            g.fill(x, y, x + w, y + h, 0x22000000);
        }
        g.fill(x + 1, y + 4, x + 4, y + 7, live ? 0xFF49E07A : 0xFF8A93A4);
        final Font font = ctx.font();
        final String type = OperationPalette.labelFor(op.type());
        g.drawString(font, type, x + 7, y + 2, OperationPalette.colorFor(op.type()), false);
        final int nameX = x + 8 + font.width(type) + 3;
        final String st = statusShort(op.status());
        final int stW = font.width(st);
        g.drawString(font, Texts.trim(font, op.name().getString(), x + w - nameX - stW - 6), nameX, y + 2,
                ctx.skin().text(), false);
        g.drawString(font, st, x + w - stW - 2, y + 2, statusColor(op.status()), false);
    }

    /** Clicking a row opens its detail, and clicking the open one closes it again. */
    void clicked(final int index, final int button, final double mx, final double my) {
        selected = index < 0 || selected == index ? -1 : index;
    }

    /** The picked Operation's detail: amounts, status, and either its machines or where its items came from. */
    void renderDetails(final GuiGraphics g, final Font font, final int dx, final int dy, final int dw,
                       final int dh) {
        final List<OperationRecord> all = all();
        final int px = dx + 5;
        if (selected < 0 || selected >= all.size()) {
            app.renderNetworkCardIn(g, font, dx, dy, dw, dh);
            return;
        }
        final OperationRecord op = all.get(selected);
        int py = app.panelHeaderIn(g, font, dx, dy, dw, op.key(), op.name().getString(),
                OperationPalette.labelFor(op.type()), OperationPalette.colorFor(op.type()), false);
        Texts.small(g, font, GameText.resolve(NetworkInteractorTexts.MOVED.with(
                NetworkInteractorApp.formatCount(op.moved()), NetworkInteractorApp.formatCount(op.requested()))),
                px, py, app.panelSkin().text());
        py += 10;
        Texts.small(g, font, statusLong(op.status()), px, py, statusColor(op.status()));
        py += 12;
        if (!op.subs().isEmpty()) {
            py = app.sectionRuleIn(g, font, px, py, dw, GameText.resolve(NetworkInteractorTexts.SUBOPERATIONS));
            for (final var sub : op.subs()) {
                if (py > dy + dh - 9) {
                    break;
                }
                final String line = GameText.resolve(NetworkInteractorTexts.SUB_LINE.with(sub.server(),
                        sub.moved(), sub.planned(), subStateLabel(sub.state())));
                Texts.small(g, font, Texts.trim(font, line, Texts.smallFits(dw - 10)), px, py,
                        app.panelSkin().text());
                py += 9;
            }
            return;
        }
        if (!op.moves().isEmpty()) {
            py = app.sectionRuleIn(g, font, px, py, dw, GameText.resolve(NetworkInteractorTexts.SOURCES));
            for (final var mv : op.moves()) {
                if (py > dy + dh - 9) {
                    break;
                }
                Texts.small(g, font, Texts.trim(font, mv.from() + " " + mv.qty() + " -> " + mv.to(),
                        Texts.smallFits(dw - 10)), px, py, app.panelSkin().text());
                py += 9;
            }
        }
    }
}
