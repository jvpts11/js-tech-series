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
import dev.jstech.computers.wan.FederatedOperation;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

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

    /** The Operation whose detail is showing, or null while none is; the list shifts as Operations finish. */
    private UUID selectedId;

    /** How many of a supercomputer's parallel craft slots are taken, and how many it has. */
    private int slotsUsed;
    private int slotsTotal;

    private final NetworkInteractorApp app;

    InteractorOps(final NetworkInteractorApp app) {
        this.app = app;
    }

    /** The short word a row ends with, which is where an Operation has got to. */
    static String statusShort(final byte status) {
        return GameText.resolve(switch (status) {
            case OperationRecord.STATUS_COMPLETED -> NetworkInteractorTexts.SHORT_DONE;
            case OperationRecord.STATUS_PARTIAL -> NetworkInteractorTexts.SHORT_PARTIAL;
            case OperationRecord.STATUS_FAILED -> NetworkInteractorTexts.SHORT_FAILED;
            case OperationRecord.STATUS_PROCESSING -> NetworkInteractorTexts.SHORT_RUNNING;
            case OperationRecord.STATUS_WAITING -> NetworkInteractorTexts.SHORT_WAITING;
            case OperationRecord.STATUS_RESOURCE_LOCKED -> NetworkInteractorTexts.SHORT_LOCKED;
            case OperationRecord.STATUS_PENDING -> NetworkInteractorTexts.SHORT_PENDING;
            default -> NetworkInteractorTexts.SHORT_DROPPED;
        });
    }

    /** The same thing written out, for the detail panel where there is room for it. */
    static String statusLong(final byte status) {
        return GameText.resolve(switch (status) {
            case OperationRecord.STATUS_COMPLETED -> NetworkInteractorTexts.COMPLETED;
            case OperationRecord.STATUS_PARTIAL -> NetworkInteractorTexts.COMPLETED_PARTIAL;
            case OperationRecord.STATUS_FAILED -> NetworkInteractorTexts.FAILED;
            case OperationRecord.STATUS_PROCESSING -> NetworkInteractorTexts.PROCESSING;
            case OperationRecord.STATUS_WAITING -> NetworkInteractorTexts.WAITING;
            case OperationRecord.STATUS_RESOURCE_LOCKED -> NetworkInteractorTexts.RESOURCE_LOCKED;
            case OperationRecord.STATUS_PENDING -> NetworkInteractorTexts.PENDING;
            default -> NetworkInteractorTexts.DISCARDED;
        });
    }

    /** Green for finished, amber for still going, red for anything that ended badly. */
    static int statusColor(final byte status) {
        final InteractorPalette.Colours c = InteractorPalette.get();
        return switch (status) {
            case OperationRecord.STATUS_COMPLETED -> c.online();
            case OperationRecord.STATUS_PARTIAL, OperationRecord.STATUS_PROCESSING, OperationRecord.STATUS_WAITING,
                 OperationRecord.STATUS_PENDING -> c.opPending();
            case OperationRecord.STATUS_FAILED, OperationRecord.STATUS_RESOURCE_LOCKED,
                 OperationRecord.STATUS_DISCARDED -> c.opFailed();
            default -> c.opOther();
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
        if (selectedId == null) {
            return -1;
        }
        for (int i = 0; i < active.size(); i++) {
            if (active.get(i).id().equals(selectedId)) {
                return i;
            }
        }
        for (int i = 0; i < recent.size(); i++) {
            if (recent.get(i).id().equals(selectedId)) {
                return active.size() + i;
            }
        }
        // Not listed (yet): the active and recent lists arrive separately, so the id is kept for the next look.
        return -1;
    }

    /** Puts the detail away, which leaving the tab does. */
    void clearSelection() {
        selectedId = null;
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
                    listLeft + 2, gridTop - 9,
                    slotsUsed >= slotsTotal ? InteractorPalette.get().amber() : app.panelSkin().dim());
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
        final InteractorPalette.Colours c = InteractorPalette.get();
        if (op.id().equals(selectedId)) {
            g.fill(x, y, x + w, y + h, c.rowChosen());
        } else if (hovered) {
            g.fill(x, y, x + w, y + h, c.rowHover());
        }
        g.fill(x + 1, y + 4, x + 4, y + 7, live ? c.live() : c.idle());
        final Font font = ctx.font();
        final String type = OperationPalette.labelFor(op.type());
        Draw.text(g, font, type, x + 7, y + 2, OperationPalette.colorFor(op.type()));
        final int nameX = x + 8 + font.width(type) + 3;
        final String st = statusShort(op.status());
        final int stW = font.width(st);
        // One that crosses to a federated network says where, beside its status.
        final String where = FederatedOperation.KIND.equals(op.kind()) ? op.machine() : "";
        final int whereW = where.isEmpty() ? 0 : font.width(where) + 6;
        Draw.text(g, font, Texts.trim(font, op.name().getString(), x + w - nameX - stW - whereW - 6), nameX, y + 2,
                ctx.skin().text());
        if (!where.isEmpty()) {
            Draw.text(g, font, where, x + w - stW - whereW - 2, y + 2, ctx.skin().dim());
        }
        Draw.text(g, font, st, x + w - stW - 2, y + 2, statusColor(op.status()));
    }

    /** Clicking a row opens its detail, and clicking the open one closes it again. */
    void clicked(final int index, final int button, final double mx, final double my) {
        final List<OperationRecord> all = all();
        final UUID clicked = index < 0 || index >= all.size() ? null : all.get(index).id();
        selectedId = clicked == null || clicked.equals(selectedId) ? null : clicked;
    }

    /** The picked Operation's detail: amounts, status, and either its machines or where its items came from. */
    void renderDetails(final GuiGraphics g, final Font font, final int dx, final int dy, final int dw,
                       final int dh) {
        final List<OperationRecord> all = all();
        final int px = dx + 5;
        final int selected = selected();
        if (selected < 0) {
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
        if (FederatedOperation.KIND.equals(op.kind())) {
            // Where a crossing has got to: on the line, waiting for the far side, or what became of it.
            py = app.sectionRuleIn(g, font, px, py, dw, GameText.resolve(NetworkInteractorTexts.CROSSING_TO
                    .with(op.machine())));
            final String note = op.failureText().map(Component::getString).orElse("");
            for (final FormattedText line : font.getSplitter().splitLines(note, Texts.smallFits(dw - 10),
                    Style.EMPTY)) {
                Texts.small(g, font, line.getString(), px, py, app.panelSkin().text());
                py += 9;
            }
            return;
        }
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
