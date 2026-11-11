/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.blockentity.ExtendedCdDriveBlockEntity;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.gui.layout.ExtendedCdDriveLayout;
import dev.jstech.computers.menu.ExtendedCdDriveMenu;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.jstechseries.theme.client.JsTechTheme;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * An Extended CD Drive's window, in its era's skin and the look of the mod's devices: its name and the copy it is on;
 * on the left the reading tray and its disc, the speed and the copies, Copy and Eject; on the right the seven writing
 * trays, each with its bar and how far; under them the inventory and what it writes, has burned and refuses.
 */
public class ExtendedCdDriveScreen extends AbstractComputerScreen<ExtendedCdDriveMenu> {

    private static final int TICKS_A_SECOND = 20;
    private static final long BYTES_A_KILOBYTE = 1_024L;
    private static final long BYTES_A_MEGABYTE = 1_048_576L;

    public ExtendedCdDriveScreen(final ExtendedCdDriveMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = ExtendedCdDriveLayout.WIDTH;
        this.imageHeight = ExtendedCdDriveLayout.HEIGHT;
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
        final boolean idle = tower().work() == ExtendedCdDriveBlockEntity.Work.IDLE;
        if (button == 0 && idle) {
            final int mx = (int) mouseX;
            final int my = (int) mouseY;
            if (hover(mx, my, ExtendedCdDriveLayout.COPY_X, ExtendedCdDriveLayout.BUTTON_Y,
                    ExtendedCdDriveLayout.BUTTON_W, ExtendedCdDriveLayout.BUTTON_H)) {
                sendButton(ExtendedCdDriveMenu.BUTTON_COPY);
                return true;
            }
            if (hover(mx, my, ExtendedCdDriveLayout.EJECT_X, ExtendedCdDriveLayout.BUTTON_Y,
                    ExtendedCdDriveLayout.BUTTON_W, ExtendedCdDriveLayout.BUTTON_H)) {
                sendButton(ExtendedCdDriveMenu.BUTTON_EJECT);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** The tower's own era, whose skin its window wears. */
    @Override
    @Nullable
    protected Era screenEra() {
        return tower().era();
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        final ExtendedCdDriveBlockEntity tower = tower();
        JsTechTheme.window(g, x, y, imageWidth, imageHeight);
        JsTechTheme.headerBar(g, x + ExtendedCdDriveLayout.HEADER_X, y + ExtendedCdDriveLayout.HEADER_Y,
                ExtendedCdDriveLayout.HEADER_W);
        Draw.text(g, font, title.getString().toUpperCase(Locale.ROOT), x + ExtendedCdDriveLayout.TITLE_X,
                y + ExtendedCdDriveLayout.TITLE_Y, JsTechTheme.text());
        drawState(g, tower);
        JsTechTheme.panel(g, x + ExtendedCdDriveLayout.LEFT_X, y + ExtendedCdDriveLayout.LEFT_Y,
                ExtendedCdDriveLayout.LEFT_W, ExtendedCdDriveLayout.PANEL_H);
        JsTechTheme.panel(g, x + ExtendedCdDriveLayout.RIGHT_X, y + ExtendedCdDriveLayout.LEFT_Y,
                ExtendedCdDriveLayout.RIGHT_W, ExtendedCdDriveLayout.PANEL_H);
        for (final Slot slot : menu.slots) {
            if (slot.isActive()) {
                JsTechTheme.slot(g, x + slot.x, y + slot.y);
            }
        }
        drawLeft(g, tower, mouseX, mouseY);
        drawTrays(g, tower);
        drawReadouts(g, tower);
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Everything is drawn with the window in renderBg; the container's own two labels are not used.
    }

    private void drawState(final GuiGraphics g, final ExtendedCdDriveBlockEntity tower) {
        final TextKey state;
        final int colour;
        switch (tower.work()) {
            case READING -> {
                state = ExtendedCdDriveTexts.STATE_READING;
                colour = JsTechTheme.amber();
            }
            case WRITING -> {
                state = null;
                colour = JsTechTheme.amber();
            }
            default -> {
                state = switch (tower.problem()) {
                    case NOTHING_TO_READ -> ExtendedCdDriveTexts.STATE_NOTHING;
                    case STORAGE -> ExtendedCdDriveTexts.STATE_STORAGE;
                    case NO_BLANKS -> ExtendedCdDriveTexts.STATE_NO_BLANKS;
                    case NONE -> ExtendedCdDriveTexts.STATE_READY;
                };
                colour = tower.problem() == ExtendedCdDriveBlockEntity.Problem.NONE ? JsTechTheme.green()
                        : JsTechTheme.red();
            }
        }
        final String words = state == null ? GameText.resolve(ExtendedCdDriveTexts.STATE_COPYING.with(
                Math.max(1, (int) Math.ceil(tower.done() * tower.copies())), tower.copies())) : GameText.resolve(state);
        final int right = leftPos + ExtendedCdDriveLayout.STATE_RIGHT;
        final int width = font.width(words);
        Draw.text(g, font, words, right - width, topPos + ExtendedCdDriveLayout.TITLE_Y, colour);
        final int lampX = right - width - ExtendedCdDriveLayout.LAMP_SIZE - 3;
        final int lampY = topPos + ExtendedCdDriveLayout.TITLE_Y + 2;
        g.fill(lampX, lampY, lampX + ExtendedCdDriveLayout.LAMP_SIZE, lampY + ExtendedCdDriveLayout.LAMP_SIZE, colour);
    }

    private void drawLeft(final GuiGraphics g, final ExtendedCdDriveBlockEntity tower, final int mouseX,
                          final int mouseY) {
        final int dim = JsTechTheme.dim();
        small(g, GameText.resolve(ExtendedCdDriveTexts.READ), ExtendedCdDriveLayout.LABEL_X,
                ExtendedCdDriveLayout.READ_LABEL_Y, dim);
        final ItemStack disc = tower.readTray().getStackInSlot(0);
        final int room = ExtendedCdDriveLayout.LEFT_X + ExtendedCdDriveLayout.LEFT_W - 3 - ExtendedCdDriveLayout.DISC_X;
        if (disc.isEmpty()) {
            small(g, GameText.resolve(ExtendedCdDriveTexts.NO_DISC), ExtendedCdDriveLayout.DISC_X,
                    ExtendedCdDriveLayout.DISC_Y, dim);
            small(g, GameText.resolve(ExtendedCdDriveTexts.NO_DISC_2), ExtendedCdDriveLayout.DISC_X,
                    ExtendedCdDriveLayout.DISC_Y + ExtendedCdDriveLayout.LINE, dim);
        } else {
            small(g, clip(disc.getHoverName().getString(), room), ExtendedCdDriveLayout.DISC_X,
                    ExtendedCdDriveLayout.DISC_Y, JsTechTheme.text());
            final MediaFormat format = disc.getItem() instanceof FormattedMediaItem item ? item.format() : null;
            if (format != null) {
                small(g, clip(GameText.resolve(ExtendedCdDriveTexts.SIZE.with(format.text(),
                        String.format(Locale.ROOT, "%,d", tower.bytes() / BYTES_A_MEGABYTE))), room),
                        ExtendedCdDriveLayout.DISC_X, ExtendedCdDriveLayout.DISC_Y + ExtendedCdDriveLayout.LINE, dim);
            }
        }
        final MediaFormat slowest = slowestBlank(tower);
        box(g, ExtendedCdDriveLayout.SPEED_Y, ExtendedCdDriveTexts.SPEED, slowest == null ? "-"
                : GameText.resolve(ExtendedCdDriveTexts.KB.with(String.format(Locale.ROOT, "%,d",
                (long) PatternEncoderBlockEntity.bytesPerTick(slowest) * TICKS_A_SECOND / BYTES_A_KILOBYTE))));
        int done = 0;
        for (int tray = 0; tray < ExtendedCdDriveBlockEntity.TRAYS; tray++) {
            done += tower.written(tray) ? 1 : 0;
        }
        box(g, ExtendedCdDriveLayout.COPIES_Y, ExtendedCdDriveTexts.COPIES, GameText.resolve(
                ExtendedCdDriveTexts.COPIES_OF.with(done, Math.max(tower.copies(), done))));
        final boolean idle = tower.work() == ExtendedCdDriveBlockEntity.Work.IDLE;
        button(g, ExtendedCdDriveLayout.COPY_X, ExtendedCdDriveTexts.COPY, idle && !disc.isEmpty(), mouseX, mouseY);
        button(g, ExtendedCdDriveLayout.EJECT_X, ExtendedCdDriveTexts.EJECT, idle && !disc.isEmpty(), mouseX, mouseY);
        small(g, GameText.resolve(ExtendedCdDriveTexts.NOTE_1), ExtendedCdDriveLayout.LABEL_X,
                ExtendedCdDriveLayout.NOTE_Y, dim);
        small(g, GameText.resolve(ExtendedCdDriveTexts.NOTE_2), ExtendedCdDriveLayout.LABEL_X,
                ExtendedCdDriveLayout.NOTE_Y + ExtendedCdDriveLayout.LINE, dim);
    }

    /* Each writing tray: its name, its bar and how far, done, or empty. */
    private void drawTrays(final GuiGraphics g, final ExtendedCdDriveBlockEntity tower) {
        for (int tray = 0; tray < ExtendedCdDriveBlockEntity.TRAYS; tray++) {
            final int y = ExtendedCdDriveLayout.ROW_Y + tray * ExtendedCdDriveLayout.ROW_STEP;
            small(g, GameText.resolve(ExtendedCdDriveTexts.TRAY.with(tray + 1)), ExtendedCdDriveLayout.TRAY_LABEL_X,
                    y + 6, JsTechTheme.dim());
            final int bx = leftPos + ExtendedCdDriveLayout.BAR_X;
            final int by = topPos + y + 7;
            g.fill(bx, by, bx + ExtendedCdDriveLayout.BAR_W, by + ExtendedCdDriveLayout.BAR_H, JsTechTheme.track());
            final boolean empty = tower.writeTrays().getStackInSlot(tray).isEmpty();
            final float share = tower.written(tray) ? 1F : tower.writing(tray)
                    && tower.work() == ExtendedCdDriveBlockEntity.Work.WRITING ? tower.done() : 0F;
            g.fill(bx, by, bx + (int) (ExtendedCdDriveLayout.BAR_W * share), by + ExtendedCdDriveLayout.BAR_H,
                    tower.written(tray) ? JsTechTheme.green() : JsTechTheme.accent());
            final String state = tower.written(tray) ? GameText.resolve(ExtendedCdDriveTexts.DONE)
                    : empty ? GameText.resolve(ExtendedCdDriveTexts.EMPTY)
                    : tower.writing(tray) && tower.work() == ExtendedCdDriveBlockEntity.Work.WRITING
                    ? GameText.resolve(ExtendedCdDriveTexts.PERCENT.with((int) (tower.done() * 100)))
                    : GameText.resolve(ExtendedCdDriveTexts.READY);
            JsTechTheme.textSRight(g, font, state, leftPos + ExtendedCdDriveLayout.STATE_X, topPos + y + 6,
                    tower.written(tray) ? JsTechTheme.green() : JsTechTheme.dim());
        }
    }

    private void drawReadouts(final GuiGraphics g, final ExtendedCdDriveBlockEntity tower) {
        final List<String> formats = new ArrayList<>();
        for (final MediaFormat format : ExtendedCdDriveBlockEntity.formats(tower.era())) {
            formats.add(GameText.resolve(format.text()));
        }
        readout(g, 0, ExtendedCdDriveTexts.WRITES, String.join(", ", formats));
        readout(g, 1, ExtendedCdDriveTexts.BURNED, GameText.resolve(ExtendedCdDriveTexts.DISCS.with(
                String.format(Locale.ROOT, "%,d", tower.burned()))));
        readout(g, 2, ExtendedCdDriveTexts.REFUSES, GameText.resolve(ExtendedCdDriveTexts.STORAGE));
    }

    /* The slowest format among the blanks in the writing trays, which sets the copy's pace; or none. */
    @Nullable
    private static MediaFormat slowestBlank(final ExtendedCdDriveBlockEntity tower) {
        MediaFormat slowest = null;
        for (int tray = 0; tray < ExtendedCdDriveBlockEntity.TRAYS; tray++) {
            if (tower.writeTrays().getStackInSlot(tray).getItem() instanceof FormattedMediaItem item
                    && (slowest == null || PatternEncoderBlockEntity.bytesPerTick(item.format())
                    < PatternEncoderBlockEntity.bytesPerTick(slowest))) {
                slowest = item.format();
            }
        }
        return slowest;
    }

    private void readout(final GuiGraphics g, final int index, final TextKey label, final String value) {
        final int x = leftPos + ExtendedCdDriveLayout.READOUT_X;
        final int y = topPos + ExtendedCdDriveLayout.READOUT_Y + index * ExtendedCdDriveLayout.READOUT_STEP;
        JsTechTheme.panel(g, x, y, ExtendedCdDriveLayout.READOUT_W, ExtendedCdDriveLayout.READOUT_H);
        JsTechTheme.textS(g, font, GameText.resolve(label), x + 3, y + 2, JsTechTheme.dim());
        Draw.text(g, font, value, x + ExtendedCdDriveLayout.READOUT_W - 3 - font.width(value), y + 11,
                JsTechTheme.text());
    }

    private void box(final GuiGraphics g, final int y, final TextKey label, final String value) {
        final int x = leftPos + ExtendedCdDriveLayout.BOX_X;
        final int top = topPos + y;
        JsTechTheme.panel(g, x, top, ExtendedCdDriveLayout.BOX_W, ExtendedCdDriveLayout.BOX_H);
        JsTechTheme.textS(g, font, GameText.resolve(label), x + 3, top + 2, JsTechTheme.dim());
        Draw.text(g, font, value, x + ExtendedCdDriveLayout.BOX_W - 3 - font.width(value), top + 10,
                JsTechTheme.text());
    }

    private void button(final GuiGraphics g, final int bx, final TextKey label, final boolean active,
                        final int mouseX, final int mouseY) {
        final int x = leftPos + bx;
        final int y = topPos + ExtendedCdDriveLayout.BUTTON_Y;
        final boolean lit = active && mouseX >= x && mouseX < x + ExtendedCdDriveLayout.BUTTON_W && mouseY >= y
                && mouseY < y + ExtendedCdDriveLayout.BUTTON_H;
        JsTechTheme.button(g, x, y, ExtendedCdDriveLayout.BUTTON_W, ExtendedCdDriveLayout.BUTTON_H, lit);
        final int colour = !active ? JsTechTheme.dim() : lit ? JsTechTheme.text() : JsTechTheme.accent();
        final String words = GameText.resolve(label);
        Draw.text(g, font, words, x + (ExtendedCdDriveLayout.BUTTON_W - font.width(words)) / 2, y + 3, colour);
    }

    private ExtendedCdDriveBlockEntity tower() {
        return menu.tower();
    }

    private void small(final GuiGraphics g, final String text, final int x, final int y, final int colour) {
        JsTechTheme.textS(g, font, text, leftPos + x, topPos + y, colour);
    }

    private String clip(final String text, final int room) {
        final int limit = (int) (room / ExtendedCdDriveLayout.SMALL);
        return font.width(text) <= limit ? text : font.plainSubstrByWidth(text, limit - font.width("...")) + "...";
    }
}
