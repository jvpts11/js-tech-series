/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.computers.gui.layout.PrinterLayout;
import dev.jstech.computers.menu.PrinterMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Grounds;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import static dev.jstech.computers.client.PrinterTexts.CANCEL;
import static dev.jstech.computers.client.PrinterTexts.EMPTY_QUEUE;
import static dev.jstech.computers.client.PrinterTexts.FROM_PAGE;
import static dev.jstech.computers.client.PrinterTexts.FROM_PAGES;
import static dev.jstech.computers.client.PrinterTexts.LINKED;
import static dev.jstech.computers.client.PrinterTexts.MORE;
import static dev.jstech.computers.client.PrinterTexts.NOTHING;
import static dev.jstech.computers.client.PrinterTexts.NOW;
import static dev.jstech.computers.client.PrinterTexts.NO_PAPER;
import static dev.jstech.computers.client.PrinterTexts.OFFLINE;
import static dev.jstech.computers.client.PrinterTexts.OUT;
import static dev.jstech.computers.client.PrinterTexts.OUTPUT_FULL;
import static dev.jstech.computers.client.PrinterTexts.PAGE;
import static dev.jstech.computers.client.PrinterTexts.PAGE_OF;
import static dev.jstech.computers.client.PrinterTexts.PAPER;
import static dev.jstech.computers.client.PrinterTexts.PAPER_NOTE_1;
import static dev.jstech.computers.client.PrinterTexts.PAPER_NOTE_2;
import static dev.jstech.computers.client.PrinterTexts.PAUSE;
import static dev.jstech.computers.client.PrinterTexts.PAUSED;
import static dev.jstech.computers.client.PrinterTexts.PRINTING;
import static dev.jstech.computers.client.PrinterTexts.QUEUE;
import static dev.jstech.computers.client.PrinterTexts.RESUME;
import static dev.jstech.computers.client.PrinterTexts.WAITING;

/**
 * A printer's window, in its era's skin and the bus windows' frame: the printer's name and the lamp of its link, the
 * paper in its tray, what prints now and its page, the queue with the machine each document came from, the sheets that
 * came out, and Pause and Cancel job.
 */
public class PrinterScreen extends AbstractComputerScreen<PrinterMenu> {

    /* The window's words live in PrinterTexts, their own holder, where a server can read them without this screen. */

    public PrinterScreen(final PrinterMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = PrinterLayout.WIDTH;
        this.imageHeight = PrinterLayout.HEIGHT;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (hover(mouseX, mouseY, PrinterLayout.LAMP_X, PrinterLayout.LAMP_Y, PrinterLayout.LAMP_SIZE,
                PrinterLayout.LAMP_SIZE)) {
            g.renderTooltip(font, GameText.component(linked() ? LINKED : OFFLINE), mouseX, mouseY);
        }
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0) {
            final int mx = (int) mouseX;
            final int my = (int) mouseY;
            if (hover(mx, my, PrinterLayout.PAUSE_X, PrinterLayout.BUTTON_Y, PrinterLayout.PAUSE_W,
                    PrinterLayout.BUTTON_H)) {
                sendButton(PrinterMenu.BUTTON_PAUSE);
                return true;
            }
            if (canCancel() && hover(mx, my, PrinterLayout.CANCEL_X, PrinterLayout.BUTTON_Y, PrinterLayout.CANCEL_W,
                    PrinterLayout.BUTTON_H)) {
                sendButton(PrinterMenu.BUTTON_CANCEL);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** The printer's own era, whose skin its window wears. */
    @Override
    @Nullable
    protected HardwareEra screenEra() {
        return menu.printer().model().era();
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        final PrinterBlockEntity printer = menu.printer();
        JsTechTheme.window(g, x, y, imageWidth, imageHeight);
        JsTechTheme.headerBar(g, x + PrinterLayout.HEADER_X, y + PrinterLayout.HEADER_Y, PrinterLayout.HEADER_W);
        // The early-PC skin's windows have a title bar in the system blue, with the title in cream on it, as a bus's.
        final boolean titleBar = JsTechTheme.active().style().doubleBevel();
        if (titleBar) {
            Grounds.fill(g, x + PrinterLayout.HEADER_X, y + PrinterLayout.HEADER_Y, x + PrinterLayout.HEADER_X
                    + PrinterLayout.HEADER_W, y + PrinterLayout.HEADER_Y + PrinterLayout.HEADER_H,
                    JsTechTheme.tabOn());
        }
        final String name = title.getString().toUpperCase(Locale.ROOT);
        Draw.text(g, font, clip(name, PrinterLayout.LAMP_X - PrinterLayout.TITLE_X - 4, 1.0F),
                x + PrinterLayout.TITLE_X, y + PrinterLayout.TITLE_Y,
                titleBar ? JsTechTheme.tabLabelOn() : JsTechTheme.text());
        LinkLamp.draw(g, x + PrinterLayout.LAMP_X, y + PrinterLayout.LAMP_Y, PrinterLayout.LAMP_SIZE, linked());
        for (final Slot slot : menu.slots) {
            if (slot.isActive()) {
                JsTechTheme.slot(g, x + slot.x - 1, y + slot.y - 1);
            }
        }
        final int dim = JsTechTheme.dim();
        small(g, GameText.resolve(PAPER), PrinterLayout.LABEL_X, PrinterLayout.PAPER_Y + 6, dim);
        small(g, GameText.resolve(PAPER_NOTE_1), PrinterLayout.PAPER_NOTE_X, PrinterLayout.PAPER_Y + 3, dim);
        small(g, GameText.resolve(PAPER_NOTE_2), PrinterLayout.PAPER_NOTE_X, PrinterLayout.PAPER_Y + 11, dim);
        final List<PrinterBlockEntity.QueueRow> rows = printer.queueRows();
        final PrinterBlockEntity.QueueRow current = rows.isEmpty() ? null : rows.getFirst();
        small(g, GameText.resolve(NOW), PrinterLayout.LABEL_X, PrinterLayout.NOW_Y, dim);
        small(g, current == null ? GameText.resolve(NOTHING) : current.title(), PrinterLayout.CONTROL_X,
                PrinterLayout.NOW_Y, current == null ? dim : JsTechTheme.accent(), PrinterLayout.RIGHT);
        small(g, GameText.resolve(PAGE), PrinterLayout.LABEL_X, PrinterLayout.PAGE_Y + 1, dim);
        drawPage(g, printer, current);
        small(g, GameText.resolve(printer.model().pace()), PrinterLayout.PACE_X, PrinterLayout.PAGE_Y + 1, dim);
        small(g, GameText.resolve(QUEUE), PrinterLayout.LABEL_X, PrinterLayout.QUEUE_LABEL_Y, dim);
        drawQueue(g, printer, rows);
        small(g, GameText.resolve(OUT), PrinterLayout.LABEL_X, PrinterLayout.OUT_Y + 6, dim);
        drawButton(g, PrinterLayout.PAUSE_X, PrinterLayout.PAUSE_W, printer.paused() ? RESUME : PAUSE, true,
                mouseX, mouseY);
        drawButton(g, PrinterLayout.CANCEL_X, PrinterLayout.CANCEL_W, CANCEL, canCancel(), mouseX, mouseY);
        small(g, playerInventoryTitle.getString(), PrinterLayout.INV_X, PrinterLayout.INV_LABEL_Y, dim);
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Everything is drawn with the window in renderBg; the container's own two labels are not used.
    }

    /* The page count in its track, filled as far as the job has come. */
    private void drawPage(final GuiGraphics g, final PrinterBlockEntity printer,
                          @Nullable final PrinterBlockEntity.QueueRow current) {
        final int x = leftPos + PrinterLayout.CONTROL_X;
        final int y = topPos + PrinterLayout.PAGE_Y;
        JsTechTheme.panel(g, x, y, PrinterLayout.PAGE_W, PrinterLayout.PAGE_H);
        if (current == null) {
            return;
        }
        final int total = Math.max(1, current.sheets());
        final int done = Math.min(total, printer.sheetsDone());
        final int fill = (PrinterLayout.PAGE_W - 2) * done / total;
        g.fill(x + 1, y + 1, x + 1 + fill, y + PrinterLayout.PAGE_H - 1, JsTechTheme.track());
        final String count = GameText.resolve(PAGE_OF.with(Math.min(total, done + 1), total));
        JsTechTheme.textSCenter(g, font, count, x + PrinterLayout.PAGE_W / 2, y + 2, JsTechTheme.text());
    }

    /* The queue: the document printing first, each with where it came from, how many pages, and its state. */
    private void drawQueue(final GuiGraphics g, final PrinterBlockEntity printer,
                           final List<PrinterBlockEntity.QueueRow> rows) {
        final int x = leftPos + PrinterLayout.QUEUE_X;
        final int y = topPos + PrinterLayout.QUEUE_Y;
        JsTechTheme.panel(g, x, y, PrinterLayout.QUEUE_W, PrinterLayout.QUEUE_H);
        if (rows.isEmpty()) {
            small(g, GameText.resolve(EMPTY_QUEUE), PrinterLayout.QUEUE_X + 3, PrinterLayout.QUEUE_Y + 3,
                    JsTechTheme.dim());
            return;
        }
        final int shown = rows.size() > PrinterLayout.QUEUE_ROWS ? PrinterLayout.QUEUE_ROWS - 1 : rows.size();
        for (int i = 0; i < shown; i++) {
            final PrinterBlockEntity.QueueRow row = rows.get(i);
            final int ry = PrinterLayout.QUEUE_Y + 2 + i * PrinterLayout.QUEUE_ROW;
            final int right = PrinterLayout.QUEUE_X + PrinterLayout.QUEUE_W - 3;
            final String state = GameText.resolve(status(printer, row));
            final int stateColour = row.printing() && printer.printing() ? JsTechTheme.green() : JsTechTheme.amber();
            JsTechTheme.textSRight(g, font, state, leftPos + right, topPos + ry + 1, stateColour);
            final String title = clip(row.title(), (int) (PrinterLayout.QUEUE_TITLE_CHARS * 4.5F), PrinterLayout.SMALL);
            small(g, title, PrinterLayout.QUEUE_X + 3, ry + 1, JsTechTheme.text());
            final String from = GameText.resolve(row.sheets() == 1 ? FROM_PAGE.with(row.from())
                    : FROM_PAGES.with(row.from(), row.sheets()));
            final int fromX = PrinterLayout.QUEUE_X + 3 + JsTechTheme.widthS(font, title) + 4;
            small(g, from, fromX, ry + 1, JsTechTheme.dim(), right - PrinterLayout.QUEUE_STATUS_W);
        }
        if (shown < rows.size()) {
            small(g, GameText.resolve(MORE.with(rows.size() - shown)), PrinterLayout.QUEUE_X + 3,
                    PrinterLayout.QUEUE_Y + 2 + shown * PrinterLayout.QUEUE_ROW + 1, JsTechTheme.dim());
        }
    }

    /* What a row of the queue is doing: printing, or waiting, and why the first one waits. */
    private static TextKey status(final PrinterBlockEntity printer, final PrinterBlockEntity.QueueRow row) {
        if (!row.printing()) {
            return WAITING;
        }
        return switch (printer.waiting()) {
            case PAUSED -> PAUSED;
            case NO_PAPER -> NO_PAPER;
            case OUTPUT_FULL -> OUTPUT_FULL;
            case NONE -> printer.printing() ? PRINTING : WAITING;
        };
    }

    private void drawButton(final GuiGraphics g, final int bx, final int width, final TextKey label,
                            final boolean active, final int mouseX, final int mouseY) {
        final int x = leftPos + bx;
        final int y = topPos + PrinterLayout.BUTTON_Y;
        final boolean lit = active && mouseX >= x && mouseX < x + width && mouseY >= y
                && mouseY < y + PrinterLayout.BUTTON_H;
        JsTechTheme.button(g, x, y, width, PrinterLayout.BUTTON_H, lit);
        final int colour = !active ? JsTechTheme.dim() : lit ? JsTechTheme.text() : JsTechTheme.accent();
        JsTechTheme.textSCenter(g, font, GameText.resolve(label), x + width / 2, y + 3, colour);
    }

    private boolean canCancel() {
        return !menu.printer().queueRows().isEmpty();
    }

    private boolean linked() {
        return menu.printer().ownerPos() != null;
    }

    /* A line at the small letters at a point of the window. */
    private void small(final GuiGraphics g, final String text, final int x, final int y, final int colour) {
        JsTechTheme.textS(g, font, text, leftPos + x, topPos + y, colour);
    }

    /* The same, cut short to end before {@code right}. */
    private void small(final GuiGraphics g, final String text, final int x, final int y, final int colour,
                       final int right) {
        small(g, clip(text, right - x, PrinterLayout.SMALL), x, y, colour);
    }

    /* A line cut to the room it has, with an ellipsis, at the given scale. */
    private String clip(final String text, final int room, final float scale) {
        final int limit = (int) (room / scale);
        if (font.width(text) <= limit) {
            return text;
        }
        return font.plainSubstrByWidth(text, limit - font.width("...")) + "...";
    }
}
