/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.gui.layout.DockLayout;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.machine.DriveTable;
import dev.jstech.computers.menu.DockStationMenu;
import dev.jstech.computers.os.media.DockStationBlockEntity;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Grounds;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import static dev.jstech.computers.client.DockStationTexts.DOCKED;
import static dev.jstech.computers.client.DockStationTexts.EJECT;
import static dev.jstech.computers.client.DockStationTexts.EMPTY;
import static dev.jstech.computers.client.DockStationTexts.EMPTY_DISK;
import static dev.jstech.computers.client.DockStationTexts.FULL_PERCENT;
import static dev.jstech.computers.client.DockStationTexts.HOST;
import static dev.jstech.computers.client.DockStationTexts.LETTERED;
import static dev.jstech.computers.client.DockStationTexts.LINKED;
import static dev.jstech.computers.client.DockStationTexts.MOUNTED;
import static dev.jstech.computers.client.DockStationTexts.NOTE_1;
import static dev.jstech.computers.client.DockStationTexts.NOTE_2;
import static dev.jstech.computers.client.DockStationTexts.NO_HOST;
import static dev.jstech.computers.client.DockStationTexts.OFFLINE;
import static dev.jstech.computers.client.DockStationTexts.USED;

/**
 * The Dock Station's window, in the skin of its era (the Standard): the computer it is docked to, then each tray and
 * the USB port with what it holds, the letter that is on the computer, its name, its era and how full it is, whether
 * the computer has it mounted, and an Eject for each.
 */
public class DockStationScreen extends AbstractComputerScreen<DockStationMenu> {

    /* The window's words live in DockStationTexts, their own holder, where a server can read them without it. */
    private static final TextKey[] ROW_LABELS = {DockStationTexts.BAY_HDD, DockStationTexts.BAY_SSD,
        DockStationTexts.BAY_NVME, DockStationTexts.USB};

    public DockStationScreen(final DockStationMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = DockLayout.WIDTH;
        this.imageHeight = DockLayout.HEIGHT;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (hover(mouseX, mouseY, DockLayout.LAMP_X, DockLayout.LAMP_Y, DockLayout.LAMP_SIZE, DockLayout.LAMP_SIZE)) {
            g.renderTooltip(font, GameText.component(linked() ? LINKED : OFFLINE), mouseX, mouseY);
        }
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0) {
            for (int row = 0; row < DockLayout.ROWS; row++) {
                if (!menu.held(row).isEmpty() && hover((int) mouseX, (int) mouseY, DockLayout.EJECT_X,
                        DockLayout.rowY(row) + 4, DockLayout.EJECT_W, DockLayout.EJECT_H)) {
                    sendButton(row);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** The dock's own era, the Standard, whose skin its window wears. */
    @Override
    @Nullable
    protected HardwareEra screenEra() {
        return HardwareEra.STANDARD;
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        final DockStationBlockEntity dock = menu.dock();
        JsTechTheme.window(g, x, y, imageWidth, imageHeight);
        JsTechTheme.headerBar(g, x + DockLayout.HEADER_X, y + DockLayout.HEADER_Y, DockLayout.HEADER_W);
        final boolean titleBar = JsTechTheme.active().style().doubleBevel();
        if (titleBar) {
            Grounds.fill(g, x + DockLayout.HEADER_X, y + DockLayout.HEADER_Y, x + DockLayout.HEADER_X
                    + DockLayout.HEADER_W, y + DockLayout.HEADER_Y + DockLayout.HEADER_H, JsTechTheme.tabOn());
        }
        Draw.text(g, font, title.getString().toUpperCase(Locale.ROOT), x + DockLayout.TITLE_X,
                y + DockLayout.TITLE_Y, titleBar ? JsTechTheme.tabLabelOn() : JsTechTheme.text());
        LinkLamp.draw(g, x + DockLayout.LAMP_X, y + DockLayout.LAMP_Y, DockLayout.LAMP_SIZE, linked());
        for (final Slot slot : menu.slots) {
            if (slot.isActive()) {
                JsTechTheme.slot(g, x + slot.x - 1, y + slot.y - 1);
            }
        }
        final int dim = JsTechTheme.dim();
        small(g, GameText.resolve(HOST), DockLayout.LABEL_X, DockLayout.HOST_Y, dim);
        final String host = dock.hostName();
        small(g, host.isEmpty() ? GameText.resolve(NO_HOST) : host,
                DockLayout.CONTROL_X, DockLayout.HOST_Y, host.isEmpty() ? dim : JsTechTheme.text(), DockLayout.RIGHT);
        for (int row = 0; row < DockLayout.ROWS; row++) {
            drawRow(g, dock, row, mouseX, mouseY);
        }
        small(g, GameText.resolve(NOTE_1), DockLayout.LABEL_X, DockLayout.NOTE_Y, dim, DockLayout.RIGHT);
        small(g, GameText.resolve(NOTE_2), DockLayout.LABEL_X, DockLayout.NOTE_Y + 8, dim, DockLayout.RIGHT);
        small(g, playerInventoryTitle.getString(), DockLayout.INV_X, DockLayout.INV_LABEL_Y, dim);
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Everything is drawn with the window in renderBg; the container's own two labels are not used.
    }

    /* A tray or the port: its label, what it holds with its letter, its era and how full it is, its state, Eject. */
    private void drawRow(final GuiGraphics g, final DockStationBlockEntity dock, final int row, final int mouseX,
                         final int mouseY) {
        final int y = DockLayout.rowY(row);
        small(g, GameText.resolve(ROW_LABELS[row]), DockLayout.LABEL_X, y + 7, JsTechTheme.dim());
        final ItemStack held = menu.held(row);
        final char letter = dock.letter(row);
        final String state = GameText.resolve(held.isEmpty() ? EMPTY : letter != ' ' ? MOUNTED : DOCKED);
        final int stateColour = held.isEmpty() ? JsTechTheme.dim() : letter != ' ' ? JsTechTheme.green()
                : JsTechTheme.amber();
        // The name has the first line to itself; the state stands at the right end of the details under it.
        final int stateW = JsTechTheme.widthS(font, state);
        JsTechTheme.textS(g, font, state, leftPos + DockLayout.STATE_RIGHT - stateW, topPos + y + 11, stateColour);
        if (!held.isEmpty()) {
            final String name = held.getHoverName().getString();
            small(g, letter == ' ' ? name : GameText.resolve(LETTERED.with(String.valueOf(letter), name)),
                    DockLayout.TEXT_X, y + 3, JsTechTheme.text(), DockLayout.STATE_RIGHT);
            small(g, detail(held), DockLayout.TEXT_X, y + 11, JsTechTheme.dim(), DockLayout.STATE_RIGHT - stateW - 4);
        }
        final int bx = leftPos + DockLayout.EJECT_X;
        final int by = topPos + y + 4;
        final boolean active = !held.isEmpty();
        final boolean lit = active && mouseX >= bx && mouseX < bx + DockLayout.EJECT_W && mouseY >= by
                && mouseY < by + DockLayout.EJECT_H;
        JsTechTheme.button(g, bx, by, DockLayout.EJECT_W, DockLayout.EJECT_H, lit);
        JsTechTheme.textSCenter(g, font, GameText.resolve(EJECT), bx + DockLayout.EJECT_W / 2, by + 3,
                !active ? JsTechTheme.dim() : lit ? JsTechTheme.text() : JsTechTheme.accent());
    }

    /* A disk's era and how much of it is used; a stick's share used. */
    private static String detail(final ItemStack held) {
        final long free = DriveTable.freeWeightOf(held);
        if (held.getItem() instanceof DiskItem disk) {
            final DiskSpec spec = disk.spec();
            final long capacity = spec.capacityItems() * StorageKey.MB_EQ_PER_ITEM;
            final long usedMb = capacity <= 0 ? 0L : spec.capacityMb() * (capacity - free) / capacity;
            final String era = GameText.resolve(spec.era().text());
            return GameText.resolve(usedMb <= 0 ? EMPTY_DISK.with(era) : USED.with(era, DiskSpec.sizeLabel(usedMb)));
        }
        if (held.getItem() instanceof FormattedMediaItem medium) {
            final long capacity = medium.format().capacityItems() * StorageKey.MB_EQ_PER_ITEM;
            final long percent = capacity <= 0 ? 0L : (capacity - free) * 100L / capacity;
            return GameText.resolve(FULL_PERCENT.with(percent));
        }
        return "";
    }

    private boolean linked() {
        return menu.dock().ownerPos() != null;
    }

    private void small(final GuiGraphics g, final String text, final int x, final int y, final int colour) {
        JsTechTheme.textS(g, font, text, leftPos + x, topPos + y, colour);
    }

    /* The same, cut short to end before {@code right}. */
    private void small(final GuiGraphics g, final String text, final int x, final int y, final int colour,
                       final int right) {
        final int limit = (int) ((right - x) / DockLayout.SMALL);
        final String shown = font.width(text) <= limit ? text
                : font.plainSubstrByWidth(text, limit - font.width("...")) + "...";
        small(g, shown, x, y, colour);
    }
}
