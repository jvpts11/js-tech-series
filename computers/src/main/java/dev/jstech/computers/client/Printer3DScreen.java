/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import static dev.jstech.computers.client.Printer3DTexts.AN_ITEM;
import static dev.jstech.computers.client.Printer3DTexts.CANCEL;
import static dev.jstech.computers.client.Printer3DTexts.HOLDS;
import static dev.jstech.computers.client.Printer3DTexts.IDLE;
import static dev.jstech.computers.client.Printer3DTexts.ITEMS;
import static dev.jstech.computers.client.Printer3DTexts.ITEM_OF;
import static dev.jstech.computers.client.Printer3DTexts.ITEM_SUB;
import static dev.jstech.computers.client.Printer3DTexts.JOB;
import static dev.jstech.computers.client.Printer3DTexts.JOB_STATE;
import static dev.jstech.computers.client.Printer3DTexts.LAYER;
import static dev.jstech.computers.client.Printer3DTexts.LAYER_OF;
import static dev.jstech.computers.client.Printer3DTexts.LEFT;
import static dev.jstech.computers.client.Printer3DTexts.LINKED;
import static dev.jstech.computers.client.Printer3DTexts.NEEDS;
import static dev.jstech.computers.client.Printer3DTexts.NOTE;
import static dev.jstech.computers.client.Printer3DTexts.NOTHING;
import static dev.jstech.computers.client.Printer3DTexts.NOW;
import static dev.jstech.computers.client.Printer3DTexts.NOW_LINE;
import static dev.jstech.computers.client.Printer3DTexts.NO_FILE;
import static dev.jstech.computers.client.Printer3DTexts.OUT;
import static dev.jstech.computers.client.Printer3DTexts.OUTPUT_FULL;
import static dev.jstech.computers.client.Printer3DTexts.PAUSE;
import static dev.jstech.computers.client.Printer3DTexts.PAUSED;
import static dev.jstech.computers.client.Printer3DTexts.PRINTED;
import static dev.jstech.computers.client.Printer3DTexts.PRINTING_DONE;
import static dev.jstech.computers.client.Printer3DTexts.QUEUE;
import static dev.jstech.computers.client.Printer3DTexts.RESUME;
import static dev.jstech.computers.client.Printer3DTexts.SECONDS;
import static dev.jstech.computers.client.Printer3DTexts.TAKE_OUT;
import static dev.jstech.computers.client.Printer3DTexts.TRC;
import static dev.jstech.computers.client.Printer3DTexts.WAITING;
import static dev.jstech.computers.client.Printer3DTexts.WAITING_TRC;
import static dev.jstech.computers.client.Printer3DTexts.WAITING_WALLET;
import static dev.jstech.computers.client.Printer3DTexts.WAITS_FILE;
import static dev.jstech.computers.client.Printer3DTexts.WAITS_FOR;

import dev.jstech.computers.blockentity.Printer3DBlockEntity;
import dev.jstech.computers.blockentity.Printer3DBlockEntity.Job;
import dev.jstech.computers.client.assembly.AssemblyPainter;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Box;
import dev.jstech.computers.gui.layout.Printer3DLayout;
import dev.jstech.computers.menu.Printer3DMenu;
import dev.jstech.computers.teracoin.Printer3DModel;
import dev.jstech.computers.teracoin.TeracoinSettings;
import dev.jstech.computers.teracoin.Trc;
import dev.jstech.core.jstechseries.theme.client.JsTechTheme;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A 3D printer's window (computers/docs/design/teracoin.md, "The 3D Printer"), as approved: the item printing now, its
 * layer, or the price its wallet waits to hold; the time an item takes, PAUSE and CANCEL; the queue, each job's item,
 * copies and the wallet that pays it; the job now with its bar; the output, what it is linked to and what it printed.
 */
public class Printer3DScreen extends AbstractComputerScreen<Printer3DMenu> {

    /** The layers a print is told in, whatever its item. */
    public static final int LAYERS = 16;
    private static final int TICKS_A_SECOND = 20;
    private static final int NAME_W = 64;

    public Printer3DScreen(final Printer3DMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = Printer3DLayout.WIDTH;
        this.imageHeight = Printer3DLayout.HEIGHT;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0) {
            final int mx = (int) mouseX;
            final int my = (int) mouseY;
            if (hover(mx, my, Printer3DLayout.PAUSE_X, Printer3DLayout.KEYS_Y, Printer3DLayout.KEY_W,
                    Printer3DLayout.KEY_H)) {
                sendButton(Printer3DMenu.BUTTON_PAUSE);
                return true;
            }
            if (hover(mx, my, Printer3DLayout.CANCEL_X, Printer3DLayout.KEYS_Y, Printer3DLayout.KEY_W,
                    Printer3DLayout.KEY_H)) {
                sendButton(Printer3DMenu.BUTTON_CANCEL);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** The printer's own era, whose skin its window wears. */
    @Override
    @Nullable
    protected Era screenEra() {
        return menu.printer().model().era();
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        final Printer3DBlockEntity printer = menu.printer();
        final Printer3DModel model = printer.model();
        final List<Job> jobs = printer.jobs();
        final Job job = jobs.isEmpty() ? null : jobs.getFirst();
        JsTechTheme.window(g, x, y, imageWidth, imageHeight);
        JsTechTheme.headerBar(g, x + Printer3DLayout.HEADER_X, y + Printer3DLayout.HEADER_Y, Printer3DLayout.HEADER_W);
        JsTechTheme.text(g, font, model.name().toUpperCase(Locale.ROOT), x + Printer3DLayout.TITLE_X,
                y + Printer3DLayout.TITLE_Y, JsTechTheme.accent());
        final TextKey state = pill(printer, job);
        final String pill = (job != null && state == ITEM_OF
                ? GameText.resolve(ITEM_OF.with(job.done() + 1, job.copies())) : GameText.resolve(state))
                .toUpperCase(Locale.ROOT);
        final int pillColour = pillColour(printer);
        final int pillX = x + Printer3DLayout.HEADER_X + Printer3DLayout.HEADER_W - 4 - font.width(pill);
        JsTechTheme.text(g, font, pill, pillX, y + Printer3DLayout.TITLE_Y, pillColour);
        AssemblyPainter.drawLamp(g, pillX - 7, y + Printer3DLayout.TITLE_Y + 3, pillColour, true);
        left(g, x, y, printer, job, mouseX, mouseY);
        right(g, x, y, printer, jobs, job);
        final int dim = JsTechTheme.dim();
        JsTechTheme.textS(g, font, GameText.resolve(OUT), x + Printer3DLayout.OUT_X, y + Printer3DLayout.OUT_CAPTION_Y,
                dim);
        for (final Slot slot : menu.slots) {
            if (slot.isActive()) {
                JsTechTheme.slot(g, x + slot.x, y + slot.y);
            }
        }
        readout(g, x, y, Printer3DLayout.OUT_X, Printer3DLayout.LINKED_Y, Printer3DLayout.LOWER_W, LINKED,
                printer.linkedName().isEmpty() ? "--" : printer.linkedName(), JsTechTheme.text());
        readout(g, x, y, Printer3DLayout.OUT_X, Printer3DLayout.PRINTED_Y, Printer3DLayout.LOWER_W, PRINTED,
                GameText.resolve(ITEMS.with(printer.printed())), JsTechTheme.text());
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
    }

    /* The item now, its layer or what its wallet must hold, the time an item takes, the keys and the note. */
    private void left(final GuiGraphics g, final int x, final int y, final Printer3DBlockEntity printer,
                      @Nullable final Job job, final int mouseX, final int mouseY) {
        JsTechTheme.panel(g, x + Printer3DLayout.LEFT_X, y + Printer3DLayout.LEFT_Y, Printer3DLayout.LEFT_W,
                Printer3DLayout.PANEL_H);
        final int dim = JsTechTheme.dim();
        JsTechTheme.textS(g, font, GameText.resolve(NOW), x + Printer3DLayout.NOW_SLOT_X,
                y + Printer3DLayout.LEFT_Y + 4, dim);
        JsTechTheme.slot(g, x + Printer3DLayout.NOW_SLOT_X, y + Printer3DLayout.NOW_SLOT_Y);
        final Item item = job == null ? null : job.itemOf();
        if (item != null) {
            final ItemStack shown = new ItemStack(item);
            g.renderItem(shown, x + Printer3DLayout.NOW_SLOT_X + 1, y + Printer3DLayout.NOW_SLOT_Y + 1);
            final List<FormattedText> name = font.getSplitter().splitLines(shown.getHoverName().getString(),
                    (int) (NAME_W / JsTechTheme.small()), Style.EMPTY);
            int line = 0;
            for (; line < name.size() && line < 2; line++) {
                JsTechTheme.textS(g, font, name.get(line).getString(), x + Printer3DLayout.NOW_TEXT_X,
                        y + Printer3DLayout.NOW_SLOT_Y + line * Printer3DLayout.LINE, JsTechTheme.text());
            }
            JsTechTheme.textS(g, font, GameText.resolve(ITEM_SUB.with(job.done() + 1, job.copies())),
                    x + Printer3DLayout.NOW_TEXT_X, y + Printer3DLayout.NOW_SLOT_Y + line * Printer3DLayout.LINE, dim);
        } else {
            JsTechTheme.textS(g, font, GameText.resolve(NOTHING), x + Printer3DLayout.NOW_TEXT_X,
                    y + Printer3DLayout.NOW_SLOT_Y + 4, dim);
        }
        if (printer.phase() == Printer3DBlockEntity.WAITING_TRC && item != null) {
            readout(g, x, y, Printer3DLayout.READOUT_X, Printer3DLayout.LAYER_Y, Printer3DLayout.READOUT_W, NEEDS,
                    GameText.resolve(TRC.with(Trc.format(TeracoinSettings.priceOf(item), 2))), JsTechTheme.red());
        } else {
            readout(g, x, y, Printer3DLayout.READOUT_X, Printer3DLayout.LAYER_Y, Printer3DLayout.READOUT_W, LAYER,
                    item == null ? "--" : GameText.resolve(LAYER_OF.with(layer(printer), LAYERS)), JsTechTheme.text());
        }
        readout(g, x, y, Printer3DLayout.READOUT_X, Printer3DLayout.AN_ITEM_Y, Printer3DLayout.READOUT_W, AN_ITEM,
                GameText.resolve(SECONDS.with(printer.model().itemSeconds())), JsTechTheme.text());
        key(g, x, y, Printer3DLayout.PAUSE_X, printer.paused() ? RESUME : PAUSE, JsTechTheme.accent(), mouseX, mouseY);
        key(g, x, y, Printer3DLayout.CANCEL_X, CANCEL, JsTechTheme.text(), mouseX, mouseY);
        final List<FormattedText> note = font.getSplitter().splitLines(GameText.resolve(NOTE),
                (int) ((Printer3DLayout.LEFT_W - 8) / JsTechTheme.small()), Style.EMPTY);
        for (int i = 0; i < note.size() && i < 2; i++) {
            JsTechTheme.textS(g, font, note.get(i).getString(), x + Printer3DLayout.READOUT_X,
                    y + Printer3DLayout.NOTE_Y + i * Printer3DLayout.LINE, dim);
        }
    }

    /* The queue, each job's item, copies and wallet, and the job now with its bar and its line. */
    private void right(final GuiGraphics g, final int x, final int y, final Printer3DBlockEntity printer,
                       final List<Job> jobs, @Nullable final Job job) {
        JsTechTheme.panel(g, x + Printer3DLayout.RIGHT_X, y + Printer3DLayout.RIGHT_Y, Printer3DLayout.RIGHT_W,
                Printer3DLayout.PANEL_H);
        final int dim = JsTechTheme.dim();
        JsTechTheme.textS(g, font, GameText.resolve(QUEUE), x + Printer3DLayout.RIGHT_X + 4,
                y + Printer3DLayout.RIGHT_Y + 4, dim);
        for (int k = 0; k < jobs.size() && k < Printer3DLayout.QUEUE_ROWS; k++) {
            final Job row = jobs.get(k);
            final int rowY = y + Printer3DLayout.QUEUE_Y + k * Printer3DLayout.QUEUE_PITCH;
            JsTechTheme.slot(g, x + Printer3DLayout.RIGHT_X + 4, rowY);
            final Item item = row.itemOf();
            final String title = item == null ? row.item() : new ItemStack(item).getHoverName().getString();
            if (item != null) {
                g.renderItem(new ItemStack(item), x + Printer3DLayout.RIGHT_X + 5, rowY + 1);
            }
            JsTechTheme.textS(g, font, GameText.resolve(JOB.with(title, row.copies())),
                    x + Printer3DLayout.QUEUE_TEXT_X, rowY + 1, JsTechTheme.text());
            final TextKey state = k > 0 ? null : jobState(printer);
            final String says = state == null ? GameText.resolve(WAITING)
                    : state == PRINTING_DONE ? GameText.resolve(PRINTING_DONE.with(row.done()))
                    : state == WAITS_FOR ? GameText.resolve(WAITS_FOR.with(
                    Trc.format(item == null ? 0L : TeracoinSettings.priceOf(item), 2))) : GameText.resolve(state);
            final int colour = k > 0 ? dim : state == PRINTING_DONE ? JsTechTheme.amber() : JsTechTheme.red();
            JsTechTheme.textS(g, font, GameText.resolve(JOB_STATE.with(row.walletName(), says)),
                    x + Printer3DLayout.QUEUE_TEXT_X, rowY + 9, colour);
        }
        final int nowY = y + Printer3DLayout.JOB_Y;
        JsTechTheme.textS(g, font, GameText.resolve(NOW), x + Printer3DLayout.RIGHT_X + 4, nowY, dim);
        final Item item = job == null ? null : job.itemOf();
        final int barX = x + Printer3DLayout.RIGHT_X + 4;
        g.fill(barX, y + Printer3DLayout.BAR_Y, barX + Printer3DLayout.BAR_W, y + Printer3DLayout.BAR_Y + 4,
                JsTechTheme.track());
        if (item == null) {
            JsTechTheme.textS(g, font, GameText.resolve(NOTHING), barX, nowY + 9, dim);
            return;
        }
        JsTechTheme.textS(g, font, GameText.resolve(NOW_LINE.with(new ItemStack(item).getHoverName().getString(),
                GameText.resolve(ITEM_SUB.with(job.done() + 1, job.copies())))), barX, nowY + 9, JsTechTheme.text());
        final double progress = printer.progress();
        if (progress > 0) {
            g.fill(barX, y + Printer3DLayout.BAR_Y, barX + (int) (Printer3DLayout.BAR_W * progress),
                    y + Printer3DLayout.BAR_Y + 4, JsTechTheme.accent());
        }
        final String line;
        int colour = dim;
        switch (printer.phase()) {
            case Printer3DBlockEntity.WAITING_TRC -> {
                line = GameText.resolve(HOLDS.with(job.walletName(), Trc.format(printer.walletHolds(), 2)));
                colour = JsTechTheme.red();
            }
            case Printer3DBlockEntity.WAITING_WALLET -> {
                line = GameText.resolve(NO_FILE.with(job.walletName()));
                colour = JsTechTheme.red();
            }
            case Printer3DBlockEntity.OUTPUT_FULL -> {
                line = GameText.resolve(TAKE_OUT);
                colour = JsTechTheme.red();
            }
            default -> {
                final int left = Math.max(0, printer.model().itemTicks() - (int) (progress
                        * printer.model().itemTicks())) / TICKS_A_SECOND;
                line = GameText.resolve(LEFT.with(layer(printer), LAYERS, left));
            }
        }
        JsTechTheme.textS(g, font, line, barX, y + Printer3DLayout.JOB_Y + 28, colour);
    }

    private void readout(final GuiGraphics g, final int x, final int y, final int left, final int top, final int w,
                         final TextKey label, final String value, final int colour) {
        AssemblyPainter.drawReadout(g, font, x, y, new Box(left, top, w, Printer3DLayout.READOUT_H),
                GameText.resolve(label), value, colour);
    }

    private void key(final GuiGraphics g, final int x, final int y, final int left, final TextKey word,
                     final int colour, final int mouseX, final int mouseY) {
        JsTechTheme.button(g, x + left, y + Printer3DLayout.KEYS_Y, Printer3DLayout.KEY_W, Printer3DLayout.KEY_H,
                hover(mouseX, mouseY, left, Printer3DLayout.KEYS_Y, Printer3DLayout.KEY_W, Printer3DLayout.KEY_H));
        final String shown = GameText.resolve(word);
        JsTechTheme.textCenter(g, font, shown, x + left + Printer3DLayout.KEY_W / 2, y + Printer3DLayout.KEYS_Y + 3,
                colour);
    }

    /* The layer printing now, from 1. */
    private static int layer(final Printer3DBlockEntity printer) {
        return Math.min(LAYERS, (int) (printer.progress() * LAYERS) + 1);
    }

    /* What the header says it does. */
    private static TextKey pill(final Printer3DBlockEntity printer, @Nullable final Job job) {
        if (printer.paused()) {
            return PAUSED;
        }
        return switch (printer.phase()) {
            case Printer3DBlockEntity.WAITING_TRC -> WAITING_TRC;
            case Printer3DBlockEntity.WAITING_WALLET -> WAITING_WALLET;
            case Printer3DBlockEntity.OUTPUT_FULL -> OUTPUT_FULL;
            default -> job == null ? IDLE : ITEM_OF;
        };
    }

    private static int pillColour(final Printer3DBlockEntity printer) {
        return switch (printer.phase()) {
            case Printer3DBlockEntity.WAITING_TRC, Printer3DBlockEntity.WAITING_WALLET,
                 Printer3DBlockEntity.OUTPUT_FULL -> JsTechTheme.red();
            case Printer3DBlockEntity.IDLE -> JsTechTheme.green();
            default -> JsTechTheme.amber();
        };
    }

    /* What the first job's line says it does: printing, or what it waits on. */
    private static TextKey jobState(final Printer3DBlockEntity printer) {
        return switch (printer.phase()) {
            case Printer3DBlockEntity.WAITING_TRC -> WAITS_FOR;
            case Printer3DBlockEntity.WAITING_WALLET -> WAITS_FILE;
            default -> PRINTING_DONE;
        };
    }
}
