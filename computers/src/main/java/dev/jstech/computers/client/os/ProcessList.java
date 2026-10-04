/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.program.OperationPalette;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.ScrollBar;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.skin.ISkin;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The network's Operations in flight as a list, one a row: the verb, the level when it is not the usual one, the
 * item, how far along it is and how it stands, with the craft slots in use above. The Network Manager's Processes tab
 * and the IQL Server Management Studio's Activity Monitor both show this one list, so the two always read alike.
 */
@PaletteHolder
final class ProcessList extends Panel {

    private final Label slots;
    private final Label live;
    private final Label none;
    private final ListView<OperationRecord> rows;
    private final ScrollBar bar;
    private List<OperationRecord> operations = List.of();
    private int slotsUsed;
    private int slotsTotal;
    private BiConsumer<OperationRecord, Integer> onPick = (operation, button) -> { };

    /** How tall a row is. */
    static final int ROW_H = 13;
    private static final int BAR_W = 3;
    private static final int HEAD_H = 12;
    /** The colours of how an Operation stands, {@code jsc:component/process_list}: going, held up, and stopped. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "component/process_list",
            new Colours(0xFF2EA043, 0xFFE0A020, 0xFFD1495B));

    ProcessList() {
        slots = add(new Label(() -> GameText.resolve(NetworkManagerTexts.CRAFT_SLOTS.with(slotsUsed, slotsTotal)),
                Label.Tone.DIM));
        live = add(new Label(() -> GameText.resolve(NetworkManagerTexts.RUNNING_COUNT.with(operations.size())),
                Label.Tone.DIM).setAlign(Label.Align.RIGHT));
        none = add(new Label(GameText.resolve(NetworkManagerTexts.NO_OPERATIONS), Label.Tone.DIM));
        rows = add(new ListView<OperationRecord>(() -> operations, ROW_H, this::renderRow).setOnClick(this::clicked));
        bar = add(new ScrollBar(() -> Math.max(0, operations.size() - rows.visibleRows()), rows::scroll,
                rows::setScroll));
    }

    /** What a state is called in the list, in the player's language. */
    static String statusLabel(final byte status) {
        final TextKey word = switch (status) {
            case OperationRecord.STATUS_COMPLETED -> NetworkManagerTexts.DONE;
            case OperationRecord.STATUS_PARTIAL -> NetworkManagerTexts.PARTIAL;
            case OperationRecord.STATUS_FAILED -> NetworkManagerTexts.FAILED;
            case OperationRecord.STATUS_PROCESSING -> NetworkManagerTexts.RUNNING;
            case OperationRecord.STATUS_WAITING -> NetworkManagerTexts.WAITING;
            case OperationRecord.STATUS_RESOURCE_LOCKED -> NetworkManagerTexts.LOCKED;
            case OperationRecord.STATUS_PENDING -> NetworkManagerTexts.QUEUED;
            case OperationRecord.STATUS_DISCARDED -> NetworkManagerTexts.DISCARDED;
            default -> null;
        };
        return word == null ? "" : GameText.resolve(word);
    }

    /** The colour a state is written in: going or done, held up, or stopped; anything else in the skin's text. */
    static int statusColour(final byte status, final ISkin skin) {
        final Colours c = PALETTE.get();
        return switch (status) {
            case OperationRecord.STATUS_PROCESSING, OperationRecord.STATUS_COMPLETED -> c.going();
            case OperationRecord.STATUS_PARTIAL, OperationRecord.STATUS_WAITING, OperationRecord.STATUS_PENDING ->
                    c.heldUp();
            case OperationRecord.STATUS_FAILED, OperationRecord.STATUS_RESOURCE_LOCKED,
                    OperationRecord.STATUS_DISCARDED -> c.stopped();
            default -> skin.text();
        };
    }

    /** Shows {@code list}, with {@code used} of {@code total} craft slots taken. */
    void show(final List<OperationRecord> list, final int used, final int total) {
        this.operations = List.copyOf(list);
        this.slotsUsed = used;
        this.slotsTotal = total;
    }

    List<OperationRecord> operations() {
        return operations;
    }

    /** Says what to do when a row is clicked, with the button it was clicked with. */
    ProcessList setOnPick(final BiConsumer<OperationRecord, Integer> action) {
        this.onPick = action;
        return this;
    }

    /** Lays the list out in its rectangle: the slots and the count along the top, the rows under them. */
    void place(final int x, final int y, final int w, final int h) {
        setBounds(x, y, w, h);
        slots.setBounds(x + 2, y, w / 2, 8);
        live.setBounds(x + w / 2, y, w / 2, 8);
        none.setBounds(x + 2, y + HEAD_H + 2, w, 8);
        rows.setBounds(x, y + HEAD_H, w - BAR_W, Math.max(ROW_H, h - HEAD_H));
        bar.setBounds(x + w - BAR_W, y + HEAD_H, BAR_W, rows.visibleRows() * ROW_H);
        none.setVisible(operations.isEmpty());
        rows.setVisible(!operations.isEmpty());
        bar.setVisible(!operations.isEmpty() && operations.size() > rows.visibleRows());
    }

    /** Moves the rows by {@code step}, as the wheel anywhere over the window does. */
    void scrollBy(final int step) {
        rows.setScroll(rows.scroll() + step);
    }

    /** The middle of row {@code index}, where a test clicks it. */
    int[] rowCenter(final int index) {
        return rows.rowCenter(index);
    }

    private void renderRow(final GuiGraphics g, final UiContext ctx, final OperationRecord op, final int index,
                           final int x, final int y, final int w, final int h, final boolean hovered,
                           final boolean selected) {
        final Font font = ctx.font();
        ctx.skin().listRow(g, x, y, w, h, hovered, false);
        final int ground = hovered ? ctx.skin().listHover() : ctx.skin().panelBg();
        final String type = OperationPalette.labelFor(op.type());
        Draw.text(g, font, type, x + 4, y + 3, OperationPalette.colorFor(op.type()), ground);
        int nameX = x + 4 + font.width(type) + 4;
        // A level other than the usual one is worth a tag: raised in amber, lowered dimmed.
        if (op.priority() != OperationPriority.DEFAULT) {
            final String tag = GameText.resolve(op.priority().text());
            Draw.text(g, font, tag, nameX, y + 3, op.priority().compareTo(OperationPriority.DEFAULT) > 0
                    ? PALETTE.get().heldUp() : ctx.skin().dim(), ground);
            nameX += font.width(tag) + 4;
        }
        final int barX = x + w / 2 + 4;
        Draw.text(g, font, Texts.clip(font, op.name().getString(), barX - nameX - 4), nameX, y + 3,
                ctx.skin().text(), ground);
        final int barLen = w / 2 - 40;
        final double frac = op.requested() > 0 ? Math.min(1.0, (double) op.moved() / op.requested()) : 0.0;
        g.fill(barX, y + 4, barX + barLen, y + h - 3, ctx.skin().fieldBg());
        g.fill(barX, y + 4, barX + (int) (barLen * frac), y + h - 3, statusColour(op.status(), ctx.skin()));
        final String st = statusLabel(op.status());
        Draw.text(g, font, st, x + w - font.width(st), y + 3, statusColour(op.status(), ctx.skin()), ground);
    }

    private void clicked(final int index, final int button, final double mx, final double my) {
        if (index >= 0 && index < operations.size()) {
            onPick.accept(operations.get(index), button);
        }
    }

    /** How an Operation stands, as a colour: going or done, held up or waiting, and stopped or failed. */
    private record Colours(int going, int heldUp, int stopped) {
    }
}
