/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * A disk tool's dialog (computers/docs/design/storage-devices.md): rows of a label and what goes beside it, a figure, a
 * field, a choice or a box to tick, a note broken over the width it has, and two buttons at its foot. Disk Management's
 * Format, Shrink and Extend, the KDE Partition Manager's Resize/Move and GNOME Disks' Format Disk are each one.
 */
final class DiskForm extends Popup {

    private final List<Row> rows = new ArrayList<>();
    @Nullable
    private Button ok;
    @Nullable
    private Button cancel;

    /* How far apart the rows sit, how tall a control is, and the height a note takes. */
    private static final int ROW = 14;
    private static final int CONTROL_H = 12;
    private static final int NOTE_LINES = 2;
    private static final int TEXT_ROW = 9;
    private static final int PAD = 6;
    private static final int BUTTON_W = 50;

    DiskForm(final Supplier<String> title, final int width, final int height) {
        super(title, width, height);
        this.setCloseOnOutsideClick(false);
        this.setLayouter(popup -> this.place());
    }

    /** A row with a label and a figure beside it. */
    DiskForm info(final Supplier<String> label, final Supplier<String> value) {
        return this.row(label, this.add(new Label(value)), null, 0);
    }

    /** A row with a label and a control beside it: a field, a choice. */
    DiskForm control(final Supplier<String> label, final UiComponent control) {
        return this.row(label, this.add(control), null, 0);
    }

    /** A row that is a control across the width: a box to tick. */
    DiskForm wide(final UiComponent control) {
        return this.row(null, this.add(control), null, 0);
    }

    /** A note across the width, broken over two lines, in that colour. */
    DiskForm note(final Supplier<String> text, final int colour) {
        return this.row(null, null, text, colour);
    }

    /** The two buttons at its foot: the one that does it, and Cancel. */
    DiskForm buttons(final Supplier<String> doIt, final Runnable action, final Supplier<String> leave) {
        this.ok = this.add(new Button(doIt, () -> {
            this.close();
            action.run();
        }));
        this.cancel = this.add(new Button(leave, this::close));
        return this;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        super.render(g, ctx);
        int ty = this.contentTop();
        for (final Row row : this.rows) {
            if (row.label() != null) {
                Draw.text(g, ctx.font(), row.label().get(), this.x() + PAD, ty + 2, ctx.skin().text(),
                        ctx.skin().windowBg());
            }
            if (row.note() != null) {
                int line = 0;
                for (final String part : wrap(ctx, row.note().get(), this.width() - 2 * PAD)) {
                    if (line++ < NOTE_LINES) {
                        Draw.text(g, ctx.font(), part, this.x() + PAD, ty + (line - 1) * TEXT_ROW, row.colour(),
                                ctx.skin().windowBg());
                    }
                }
                ty += NOTE_LINES * TEXT_ROW - ROW;
            }
            ty += ROW;
        }
    }

    /* Puts every row's control beside its label, and the buttons at the foot, on the right. */
    private void place() {
        int ty = this.contentTop();
        final int half = this.x() + this.width() / 2;
        for (final Row row : this.rows) {
            if (row.control() != null) {
                final int left = row.label() == null ? this.x() + PAD : half;
                row.control().setBounds(left, ty, this.right() - PAD - left, CONTROL_H);
            }
            ty += row.note() != null ? NOTE_LINES * TEXT_ROW : ROW;
        }
        final int by = this.bottom() - CONTROL_H - PAD;
        if (this.cancel != null) {
            this.cancel.setBounds(this.right() - PAD - BUTTON_W, by, BUTTON_W, CONTROL_H);
        }
        if (this.ok != null) {
            this.ok.setBounds(this.right() - 2 * PAD - 2 * BUTTON_W, by, BUTTON_W, CONTROL_H);
        }
    }

    private DiskForm row(@Nullable final Supplier<String> label, @Nullable final UiComponent control,
                         @Nullable final Supplier<String> note, final int colour) {
        this.rows.add(new Row(label, control, note, colour));
        return this;
    }

    private static List<String> wrap(final UiContext ctx, final String text, final int room) {
        final List<String> out = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        for (final String word : text.split(" ")) {
            final String tried = line.isEmpty() ? word : line + " " + word;
            if (ctx.font().width(tried) > room && !line.isEmpty()) {
                out.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(tried);
            }
        }
        if (!line.isEmpty()) {
            out.add(line.toString());
        }
        return out;
    }

    /** One row: its label, its control, or a note across it in its colour. */
    private record Row(@Nullable Supplier<String> label, @Nullable UiComponent control,
                       @Nullable Supplier<String> note, int colour) {
    }
}
