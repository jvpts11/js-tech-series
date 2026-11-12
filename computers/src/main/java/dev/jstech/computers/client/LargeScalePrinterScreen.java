/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.gui.layout.LargeScalePrinterLayout;
import dev.jstech.computers.menu.LargeScalePrinterMenu;
import dev.jstech.computers.printer.BookJob;
import dev.jstech.computers.printer.LargeScalePrinterBlockEntity;
import dev.jstech.computers.printer.LargeScalePrinters;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.jstechseries.theme.client.JsTechTheme;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * The Large Scale Printer's window, in the Legacy's skin and the look of the mod's devices: its name and the book it is
 * on in the header; on the left the feeder, the sheets it holds and what a book takes, Pause and Cancel, its pace; on
 * the right the queue with the machine each book came from, and the book printing now with its bar; under them the
 * inventory, the switch of where the books go, the books printed, and whether the server lets enchanted books print.
 */
public class LargeScalePrinterScreen extends AbstractComputerScreen<LargeScalePrinterMenu> {

    /* A book's pages go through the press for most of its time; the rest is the binding. */
    private static final float PRESS_SHARE = 0.85F;

    public LargeScalePrinterScreen(final LargeScalePrinterMenu menu, final Inventory inventory,
                                   final Component title) {
        super(menu, inventory, title);
        this.imageWidth = LargeScalePrinterLayout.WIDTH;
        this.imageHeight = LargeScalePrinterLayout.HEIGHT;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (hover(mouseX, mouseY, LargeScalePrinterLayout.READOUT_X, LargeScalePrinterLayout.READOUT_Y,
                LargeScalePrinterLayout.READOUT_W, LargeScalePrinterLayout.READOUT_H)) {
            g.renderTooltip(font, GameText.component(LargeScalePrinterTexts.SWITCH_TIP), mouseX, mouseY);
        }
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0) {
            final int mx = (int) mouseX;
            final int my = (int) mouseY;
            if (hover(mx, my, LargeScalePrinterLayout.PAUSE_X, LargeScalePrinterLayout.BUTTON_Y,
                    LargeScalePrinterLayout.BUTTON_W, LargeScalePrinterLayout.BUTTON_H)) {
                sendButton(LargeScalePrinterMenu.BUTTON_PAUSE);
                return true;
            }
            if (!printer().rows().isEmpty() && hover(mx, my, LargeScalePrinterLayout.CANCEL_X,
                    LargeScalePrinterLayout.BUTTON_Y, LargeScalePrinterLayout.BUTTON_W,
                    LargeScalePrinterLayout.BUTTON_H)) {
                sendButton(LargeScalePrinterMenu.BUTTON_CANCEL);
                return true;
            }
            if (hover(mx, my, LargeScalePrinterLayout.READOUT_X, LargeScalePrinterLayout.READOUT_Y,
                    LargeScalePrinterLayout.READOUT_W, LargeScalePrinterLayout.READOUT_H)) {
                sendButton(LargeScalePrinterMenu.BUTTON_DESTINATION);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** The Legacy's skin: the printer is a machine of that era. */
    @Override
    @Nullable
    protected Era screenEra() {
        return HardwareEra.LEGACY;
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        final LargeScalePrinterBlockEntity printer = printer();
        JsTechTheme.window(g, x, y, imageWidth, imageHeight);
        JsTechTheme.headerBar(g, x + LargeScalePrinterLayout.HEADER_X, y + LargeScalePrinterLayout.HEADER_Y,
                LargeScalePrinterLayout.HEADER_W);
        // The machine's own name, not its terminal block's, in its era's accent.
        Draw.text(g, font, GameText.resolve(LargeScalePrinters.NAME).toUpperCase(Locale.ROOT),
                x + LargeScalePrinterLayout.TITLE_X, y + LargeScalePrinterLayout.TITLE_Y, JsTechTheme.accent());
        drawState(g, printer);
        JsTechTheme.panel(g, x + LargeScalePrinterLayout.LEFT_X, y + LargeScalePrinterLayout.LEFT_Y,
                LargeScalePrinterLayout.LEFT_W, LargeScalePrinterLayout.PANEL_H);
        JsTechTheme.panel(g, x + LargeScalePrinterLayout.RIGHT_X, y + LargeScalePrinterLayout.LEFT_Y,
                LargeScalePrinterLayout.RIGHT_W, LargeScalePrinterLayout.PANEL_H);
        for (final Slot slot : menu.slots) {
            if (slot.isActive()) {
                JsTechTheme.slot(g, x + slot.x, y + slot.y);
            }
        }
        drawLeft(g, printer, mouseX, mouseY);
        drawQueue(g, printer);
        drawNow(g, printer);
        drawReadouts(g, printer);
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Everything is drawn with the window in renderBg; the container's own two labels are not used.
    }

    /* The header's right end: the copy it is on while it prints, or why it stands. */
    private void drawState(final GuiGraphics g, final LargeScalePrinterBlockEntity printer) {
        final List<LargeScalePrinterBlockEntity.Row> rows = printer.rows();
        final String words;
        final int colour;
        if (!printer.formed()) {
            words = GameText.resolve(LargeScalePrinterTexts.STATE_APART);
            colour = JsTechTheme.red();
        } else if (printer.paused() && !rows.isEmpty()) {
            words = GameText.resolve(LargeScalePrinterTexts.STATE_PAUSED);
            colour = JsTechTheme.amber();
        } else if (printer.status() == MachineStatus.WORKING && !rows.isEmpty()) {
            words = GameText.resolve(LargeScalePrinterTexts.STATE_BOOK.with(printer.done() + 1,
                    rows.getFirst().copies()));
            colour = JsTechTheme.amber();
        } else if (!rows.isEmpty() && printer.sheets() < LargeScalePrinterBlockEntity.SHEETS_A_BOOK) {
            words = GameText.resolve(LargeScalePrinterTexts.STATE_NO_PAPER);
            colour = JsTechTheme.red();
        } else {
            words = GameText.resolve(LargeScalePrinterTexts.STATE_READY);
            colour = JsTechTheme.green();
        }
        final int right = leftPos + LargeScalePrinterLayout.STATE_RIGHT;
        final int width = font.width(words);
        Draw.text(g, font, words, right - width, topPos + LargeScalePrinterLayout.TITLE_Y, colour);
        final int lampX = right - width - LargeScalePrinterLayout.LAMP_SIZE - 3;
        final int lampY = topPos + LargeScalePrinterLayout.TITLE_Y + 2;
        g.fill(lampX, lampY, lampX + LargeScalePrinterLayout.LAMP_SIZE, lampY + LargeScalePrinterLayout.LAMP_SIZE,
                colour);
    }

    /* The feeder, its sheets, a book's sheets, the buttons and the pace. */
    private void drawLeft(final GuiGraphics g, final LargeScalePrinterBlockEntity printer, final int mouseX,
                          final int mouseY) {
        final int dim = JsTechTheme.dim();
        small(g, GameText.resolve(LargeScalePrinterTexts.FEEDER), LargeScalePrinterLayout.LABEL_X,
                LargeScalePrinterLayout.FEEDER_LABEL_Y, dim);
        final TextKey[] notes = {LargeScalePrinterTexts.FEEDER_1, LargeScalePrinterTexts.FEEDER_2,
                LargeScalePrinterTexts.FEEDER_3};
        for (int i = 0; i < notes.length; i++) {
            small(g, GameText.resolve(notes[i]), LargeScalePrinterLayout.FEEDER_NOTE_X,
                    LargeScalePrinterLayout.FEEDER_NOTE_Y + i * LargeScalePrinterLayout.NOTE_LINE,
                    i == 0 ? JsTechTheme.text() : dim);
        }
        box(g, LargeScalePrinterLayout.SHEETS_Y, LargeScalePrinterTexts.SHEETS,
                String.format(Locale.ROOT, "%,d", printer.sheets()));
        box(g, LargeScalePrinterLayout.BOOK_Y, LargeScalePrinterTexts.A_BOOK,
                GameText.resolve(LargeScalePrinterTexts.THREE_SHEETS.with(LargeScalePrinterBlockEntity.SHEETS_A_BOOK)));
        drawButton(g, LargeScalePrinterLayout.PAUSE_X, printer.paused() ? LargeScalePrinterTexts.RESUME
                : LargeScalePrinterTexts.PAUSE, true, JsTechTheme.accent(), mouseX, mouseY);
        final boolean queued = !printer.rows().isEmpty();
        drawButton(g, LargeScalePrinterLayout.CANCEL_X, LargeScalePrinterTexts.CANCEL, queued,
                queued ? JsTechTheme.text() : dim, mouseX, mouseY);
        small(g, GameText.resolve(LargeScalePrinterTexts.PACE_1), LargeScalePrinterLayout.LABEL_X,
                LargeScalePrinterLayout.PACE_Y, dim);
        small(g, GameText.resolve(LargeScalePrinterTexts.PACE_2.with(printer.machine().seconds())),
                LargeScalePrinterLayout.LABEL_X, LargeScalePrinterLayout.PACE_Y + LargeScalePrinterLayout.NOTE_LINE,
                dim);
    }

    /* The queue: up to three books, each with its icon, its title and copies, and where it came from. */
    private void drawQueue(final GuiGraphics g, final LargeScalePrinterBlockEntity printer) {
        final int dim = JsTechTheme.dim();
        small(g, GameText.resolve(LargeScalePrinterTexts.QUEUE), LargeScalePrinterLayout.TEXT_X,
                LargeScalePrinterLayout.QUEUE_LABEL_Y, dim);
        final List<LargeScalePrinterBlockEntity.Row> rows = printer.rows();
        if (rows.isEmpty()) {
            small(g, GameText.resolve(LargeScalePrinterTexts.EMPTY_QUEUE), LargeScalePrinterLayout.TEXT_X,
                    LargeScalePrinterLayout.ROW_Y, dim);
            small(g, GameText.resolve(LargeScalePrinterTexts.EMPTY_QUEUE_2), LargeScalePrinterLayout.TEXT_X,
                    LargeScalePrinterLayout.ROW_Y + LargeScalePrinterLayout.NOTE_LINE, dim);
            return;
        }
        final int shown = rows.size() > LargeScalePrinterLayout.ROWS ? LargeScalePrinterLayout.ROWS - 1 : rows.size();
        final int room = LargeScalePrinterLayout.RIGHT_X + LargeScalePrinterLayout.RIGHT_W - 4
                - LargeScalePrinterLayout.ROW_TEXT_X;
        for (int i = 0; i < shown; i++) {
            final LargeScalePrinterBlockEntity.Row row = rows.get(i);
            final int y = LargeScalePrinterLayout.ROW_Y + i * LargeScalePrinterLayout.ROW_STEP;
            JsTechTheme.slot(g, leftPos + LargeScalePrinterLayout.TEXT_X, topPos + y);
            g.renderItem(icon(row.kind()), leftPos + LargeScalePrinterLayout.TEXT_X, topPos + y);
            final int lineY = y + LargeScalePrinterLayout.ROW_TEXT_DY;
            small(g, clip(GameText.resolve(LargeScalePrinterTexts.TIMES.with(row.title(), row.copies())), room),
                    LargeScalePrinterLayout.ROW_TEXT_X, lineY, JsTechTheme.text());
            // An enchanted book the server has since turned off waits in the queue, saying why it will not print.
            final boolean refused = row.kind() == BookJob.Kind.ENCHANTED && !printer.enchantedAllowed();
            final String state = refused ? GameText.resolve(LargeScalePrinterTexts.OFF_HERE)
                    : GameText.resolve((row.printing() ? LargeScalePrinterTexts.FROM_PRINTING
                            : LargeScalePrinterTexts.FROM_WAITING).with(row.from()));
            small(g, clip(state, room), LargeScalePrinterLayout.ROW_TEXT_X, lineY + LargeScalePrinterLayout.NOTE_LINE,
                    refused ? JsTechTheme.red() : row.printing() ? JsTechTheme.amber() : dim);
        }
        if (shown < rows.size()) {
            small(g, GameText.resolve(LargeScalePrinterTexts.MORE.with(rows.size() - shown)),
                    LargeScalePrinterLayout.ROW_TEXT_X, LargeScalePrinterLayout.ROW_Y
                            + shown * LargeScalePrinterLayout.ROW_STEP, dim);
        }
    }

    /* The book printing now, the copy it is on, its bar, and how far through its pages or binding it is. */
    private void drawNow(final GuiGraphics g, final LargeScalePrinterBlockEntity printer) {
        final int dim = JsTechTheme.dim();
        small(g, GameText.resolve(LargeScalePrinterTexts.NOW), LargeScalePrinterLayout.TEXT_X,
                LargeScalePrinterLayout.NOW_LABEL_Y, dim);
        final List<LargeScalePrinterBlockEntity.Row> rows = printer.rows();
        final int barX = leftPos + LargeScalePrinterLayout.TEXT_X;
        final int barY = topPos + LargeScalePrinterLayout.BAR_Y;
        g.fill(barX, barY, barX + LargeScalePrinterLayout.BAR_W, barY + LargeScalePrinterLayout.BAR_H,
                JsTechTheme.track());
        if (rows.isEmpty()) {
            small(g, GameText.resolve(LargeScalePrinterTexts.NOTHING), LargeScalePrinterLayout.TEXT_X,
                    LargeScalePrinterLayout.NOW_Y, dim);
            return;
        }
        final LargeScalePrinterBlockEntity.Row now = rows.getFirst();
        final int room = LargeScalePrinterLayout.RIGHT_X + LargeScalePrinterLayout.RIGHT_W - 4
                - LargeScalePrinterLayout.TEXT_X;
        small(g, clip(GameText.resolve(LargeScalePrinterTexts.COPY_OF.with(now.title(), printer.done() + 1,
                now.copies())), room), LargeScalePrinterLayout.TEXT_X, LargeScalePrinterLayout.NOW_Y,
                JsTechTheme.text());
        final float progress = printer.status() == MachineStatus.WORKING ? printer.progress() : 0F;
        g.fill(barX, barY, barX + (int) (LargeScalePrinterLayout.BAR_W * progress), barY
                + LargeScalePrinterLayout.BAR_H, JsTechTheme.accent());
        final int pages = Math.max(1, now.pages());
        final String where = progress < PRESS_SHARE ? GameText.resolve(LargeScalePrinterTexts.PAGES_OF.with(
                Math.min(pages, 1 + (int) (pages * progress / PRESS_SHARE)), pages))
                : GameText.resolve(LargeScalePrinterTexts.BINDING);
        small(g, clip(where, room), LargeScalePrinterLayout.TEXT_X, LargeScalePrinterLayout.PAGES_Y, dim);
    }

    /* The switch of where the books go, the books printed, and the server's word on enchanted books. */
    private void drawReadouts(final GuiGraphics g, final LargeScalePrinterBlockEntity printer) {
        readout(g, 0, LargeScalePrinterTexts.GO_TO, GameText.resolve(printer.toNetwork()
                ? LargeScalePrinterTexts.THE_NETWORK : LargeScalePrinterTexts.THE_CHEST), JsTechTheme.text());
        readout(g, 1, LargeScalePrinterTexts.PRINTED, GameText.resolve(printer.printed() == 1
                ? LargeScalePrinterTexts.ONE_BOOK.text()
                : LargeScalePrinterTexts.BOOKS.with(String.format(Locale.ROOT, "%,d", printer.printed()))),
                JsTechTheme.text());
        readout(g, 2, LargeScalePrinterTexts.ENCHANTED, GameText.resolve(printer.enchantedAllowed()
                ? LargeScalePrinterTexts.ALLOWED : LargeScalePrinterTexts.OFF),
                printer.enchantedAllowed() ? JsTechTheme.text() : JsTechTheme.red());
    }

    private void readout(final GuiGraphics g, final int index, final TextKey label, final String value,
                         final int colour) {
        final int x = leftPos + LargeScalePrinterLayout.READOUT_X;
        final int y = topPos + LargeScalePrinterLayout.READOUT_Y + index * LargeScalePrinterLayout.READOUT_STEP;
        JsTechTheme.panel(g, x, y, LargeScalePrinterLayout.READOUT_W, LargeScalePrinterLayout.READOUT_H);
        JsTechTheme.textS(g, font, GameText.resolve(label), x + 3, y + 2, JsTechTheme.dim());
        final String shown = clipFull(value, LargeScalePrinterLayout.READOUT_W - 6);
        Draw.text(g, font, shown, x + LargeScalePrinterLayout.READOUT_W - 3 - font.width(shown),
                y + LargeScalePrinterLayout.VALUE_Y, colour);
    }

    /* A labelled box of the left panel, its value at its right edge. */
    private void box(final GuiGraphics g, final int y, final TextKey label, final String value) {
        final int x = leftPos + LargeScalePrinterLayout.BOX_X;
        final int top = topPos + y;
        JsTechTheme.panel(g, x, top, LargeScalePrinterLayout.BOX_W, LargeScalePrinterLayout.BOX_H);
        JsTechTheme.textS(g, font, GameText.resolve(label), x + 3, top + 2, JsTechTheme.dim());
        Draw.text(g, font, value, x + LargeScalePrinterLayout.BOX_W - 3 - font.width(value),
                top + LargeScalePrinterLayout.VALUE_Y, JsTechTheme.text());
    }

    /* A key of the window; {@code pressable} keys light up under the mouse. */
    private void drawButton(final GuiGraphics g, final int bx, final TextKey label, final boolean pressable,
                            final int colour, final int mouseX, final int mouseY) {
        final int x = leftPos + bx;
        final int y = topPos + LargeScalePrinterLayout.BUTTON_Y;
        final boolean lit = pressable && mouseX >= x && mouseX < x + LargeScalePrinterLayout.BUTTON_W && mouseY >= y
                && mouseY < y + LargeScalePrinterLayout.BUTTON_H;
        JsTechTheme.button(g, x, y, LargeScalePrinterLayout.BUTTON_W, LargeScalePrinterLayout.BUTTON_H, lit);
        final String words = GameText.resolve(label);
        Draw.text(g, font, words, x + (LargeScalePrinterLayout.BUTTON_W - font.width(words)) / 2, y + 3,
                lit ? JsTechTheme.text() : colour);
    }

    /* The book a row prints, as its icon: a system's book is drawn as a plain book. */
    private static ItemStack icon(final BookJob.Kind kind) {
        return new ItemStack(switch (kind) {
            case WRITTEN -> Items.WRITTEN_BOOK;
            case QUILL -> Items.WRITABLE_BOOK;
            case ENCHANTED -> Items.ENCHANTED_BOOK;
            case MANUAL -> Items.BOOK;
        });
    }

    private LargeScalePrinterBlockEntity printer() {
        return menu.printer();
    }

    private void small(final GuiGraphics g, final String text, final int x, final int y, final int colour) {
        JsTechTheme.textS(g, font, text, leftPos + x, topPos + y, colour);
    }

    /* A small line cut to the room it has, with an ellipsis. */
    private String clip(final String text, final int room) {
        final int limit = (int) (room / LargeScalePrinterLayout.SMALL);
        return font.width(text) <= limit ? text : font.plainSubstrByWidth(text, limit - font.width("...")) + "...";
    }

    /* A full-size line cut to the room it has. */
    private String clipFull(final String text, final int room) {
        return font.width(text) <= room ? text : font.plainSubstrByWidth(text, room - font.width("...")) + "...";
    }
}
