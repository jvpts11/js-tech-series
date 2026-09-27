/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.gui.layout.MainframeLayout;
import dev.jstech.computers.menu.MainframeMenu;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.network.FailoverRole;
import dev.jstech.core.text.GameText;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Mainframe: a flat-dark "computer OS" hardware-assembly surface.
 */
public class MainframeScreen extends AbstractComputerScreen<MainframeMenu> {

    /** Window-relative centre of the POWER button (client tests press it the way the player does). */
    public static int powerButtonX() {
        return MainframeLayout.POWER_X + MainframeLayout.BTN_W / 2;
    }

    public static int powerButtonY() {
        return MainframeLayout.BTN_Y + MainframeLayout.BTN_H / 2;
    }

    public MainframeScreen(final MainframeMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = MainframeLayout.WIDTH;
        this.imageHeight = MainframeLayout.HEIGHT;
        this.titleLabelX = -10000;
        this.inventoryLabelY = -10000;
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        JsTechTheme.window(g, x, y, imageWidth, imageHeight);
        JsTechTheme.headerBar(g, x + MainframeLayout.HEADER_X, y + MainframeLayout.HEADER_Y, MainframeLayout.HEADER_W);
        JsTechTheme.vLine(g, x + MainframeLayout.VLINE_X, y + MainframeLayout.VLINE_Y, MainframeLayout.VLINE_H);

        // A cell behind every active hardware slot, reading the menu's own slot positions.
        for (int i = 0; i < MainframeBlockEntity.TOTAL_SLOTS; i++) {
            final var slot = menu.getSlot(i);
            if (slot.isActive()) {
                JsTechTheme.slot(g, x + slot.x, y + slot.y);
            }
        }

        // Right spec column.
        JsTechTheme.panel(g, x + MainframeLayout.COL_R, y + MainframeLayout.TILE_Y_CAPACITY, MainframeLayout.COL_R_W,
                MainframeLayout.TILE_H_CAPACITY);
        JsTechTheme.panel(g, x + MainframeLayout.COL_R, y + MainframeLayout.TILE_Y_QUEUES, MainframeLayout.COL_R_W,
                MainframeLayout.TILE_H_QUEUES);
        JsTechTheme.panel(g, x + MainframeLayout.COL_R, y + MainframeLayout.TILE_Y_RAM_BUFFER,
                MainframeLayout.COL_R_W, MainframeLayout.TILE_H_RAM_BUFFER);
        JsTechTheme.panel(g, x + MainframeLayout.COL_R, y + MainframeLayout.TILE_Y_OPERATIONS,
                MainframeLayout.COL_R_W, MainframeLayout.TILE_H_OPERATIONS);

        final boolean auto = menu.isAutoStart();
        JsTechTheme.button(g, x + MainframeLayout.POWER_X, y + MainframeLayout.BTN_Y, MainframeLayout.BTN_W,
                MainframeLayout.BTN_H, !auto && hover(mouseX, mouseY, MainframeLayout.POWER_X, MainframeLayout.BTN_Y,
                        MainframeLayout.BTN_W, MainframeLayout.BTN_H));
        JsTechTheme.button(g, x + MainframeLayout.AUTO_X, y + MainframeLayout.BTN_Y, MainframeLayout.BTN_W,
                MainframeLayout.BTN_H, hover(mouseX, mouseY, MainframeLayout.AUTO_X, MainframeLayout.BTN_Y,
                        MainframeLayout.BTN_W, MainframeLayout.BTN_H));
        JsTechTheme.button(g, x + MainframeLayout.FAILOVER_X, y + MainframeLayout.BTN_Y, MainframeLayout.BTN_W,
                MainframeLayout.BTN_H, hover(mouseX, mouseY, MainframeLayout.FAILOVER_X, MainframeLayout.BTN_Y,
                        MainframeLayout.BTN_W, MainframeLayout.BTN_H));

        // Player inventory.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                JsTechTheme.slot(g, x + MainframeLayout.INV_X + col * MainframeLayout.SLOT,
                        y + MainframeLayout.INV_Y + row * MainframeLayout.SLOT);
            }
        }
        for (int col = 0; col < 9; col++) {
            JsTechTheme.slot(g, x + MainframeLayout.INV_X + col * MainframeLayout.SLOT,
                    y + MainframeLayout.INV_Y + MainframeLayout.HOTBAR_GAP);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.TITLE_MAINFRAME), 12, 11, JsTechTheme.text());
        final String status;
        final int statusColor;
        if (menu.networkState() == MainframeBlockEntity.NET_STATE_CONFLICT) {
            status = GameText.resolve(AssemblyTexts.CONFLICT);
            statusColor = JsTechTheme.red();
        } else if (!menu.buildValid()) {
            status = GameText.resolve(AssemblyTexts.OFFLINE);
            statusColor = JsTechTheme.red();
        } else if (menu.isRunning() && menu.failoverRole() == FailoverRole.PASSIVE) {
            // A Passive Failover member: powered and synced, not orchestrating.
            status = GameText.resolve(AssemblyTexts.STANDBY);
            statusColor = JsTechTheme.amber();
        } else if (menu.isRunning()) {
            status = GameText.resolve(AssemblyTexts.ONLINE);
            statusColor = JsTechTheme.green();
        } else {
            status = GameText.resolve(AssemblyTexts.READY);
            statusColor = JsTechTheme.amber();
        }
        final int pillX = MainframeLayout.HEADER_W - font.width(status);
        JsTechTheme.text(g, font, status, pillX, 11, statusColor);
        g.fill(pillX - 6, 11, pillX - 2, 15, statusColor);

        // Hardware group labels (no counters, the drawn cells show installed vs available).
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.BOARD), MainframeLayout.MOBO_X,
                MainframeLayout.LABEL_ROW_1_Y, menu.hasBoard() ? JsTechTheme.accent() : JsTechTheme.dim());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.CPU), MainframeLayout.RIGHT_X,
                MainframeLayout.LABEL_ROW_1_Y, JsTechTheme.dim());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.PSU), MainframeLayout.MOBO_X,
                MainframeLayout.LABEL_ROW_2_Y, JsTechTheme.dim());
        g.fill(MainframeLayout.PSU_LED_X, MainframeLayout.PSU_LED_Y,
                MainframeLayout.PSU_LED_X + MainframeLayout.PSU_LED_SIZE,
                MainframeLayout.PSU_LED_Y + MainframeLayout.PSU_LED_SIZE, psuColor());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.RAM), MainframeLayout.RIGHT_X,
                MainframeLayout.LABEL_ROW_2_Y, JsTechTheme.dim());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.DISK), MainframeLayout.MOBO_X,
                MainframeLayout.LABEL_ROW_3_Y, JsTechTheme.dim());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.GPU), MainframeLayout.RIGHT_X,
                MainframeLayout.LABEL_ROW_3_Y, JsTechTheme.dim());

        // Right column: spec tiles.
        JsTechTheme.tileText(g, font, MainframeLayout.COL_R, MainframeLayout.TILE_Y_CAPACITY,
                GameText.resolve(AssemblyTexts.CAPACITY), JsTechTheme.fmt(menu.capacity()),
                GameText.resolve(AssemblyTexts.ITEMS_PER_TICK), JsTechTheme.text());
        JsTechTheme.tileText(g, font, MainframeLayout.COL_R, MainframeLayout.TILE_Y_QUEUES,
                GameText.resolve(AssemblyTexts.QUEUES), String.valueOf(menu.parallelQueues()), "", JsTechTheme.text());
        JsTechTheme.tileText(g, font, MainframeLayout.COL_R, MainframeLayout.TILE_Y_RAM_BUFFER,
                GameText.resolve(AssemblyTexts.RAM_BUFFER), JsTechTheme.fmt(menu.ramBuffer()),
                GameText.resolve(AssemblyTexts.ITEMS), JsTechTheme.text());

        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.NETWORK), MainframeLayout.COL_R,
                MainframeLayout.NETWORK_Y, JsTechTheme.dim());
        final int net = menu.networkState();
        final String netStr = net == MainframeBlockEntity.NET_STATE_CONFLICT ? GameText.resolve(AssemblyTexts.CONFLICT)
                : net == MainframeBlockEntity.NET_STATE_LINKED ? GameText.resolve(AssemblyTexts.LINKED) : "--";
        final int netColor = net == MainframeBlockEntity.NET_STATE_CONFLICT ? JsTechTheme.red()
                : net == MainframeBlockEntity.NET_STATE_LINKED ? JsTechTheme.green() : JsTechTheme.dim();
        JsTechTheme.textRight(g, font, netStr, MainframeLayout.COL_R + MainframeLayout.COL_R_W,
                MainframeLayout.NETWORK_Y, netColor);

        // Operations dispatch: one row per metric (label left, value right).
        final int running = menu.runningOps();
        opRow(g, GameText.resolve(AssemblyTexts.QUEUED), String.valueOf(menu.pendingOps()),
                MainframeLayout.OPS_ROW_Y0, JsTechTheme.text());
        opRow(g, GameText.resolve(AssemblyTexts.RUNNING), String.valueOf(running),
                MainframeLayout.OPS_ROW_Y0 + MainframeLayout.OPS_ROW_PITCH,
                running > 0 ? JsTechTheme.green() : JsTechTheme.text());
        opRow(g, GameText.resolve(AssemblyTexts.DONE), JsTechTheme.fmt(menu.completedOps()),
                MainframeLayout.OPS_ROW_Y0 + 2 * MainframeLayout.OPS_ROW_PITCH, JsTechTheme.text());

        // Control row captions.
        final boolean auto = menu.isAutoStart();
        final String powerCap = GameText.resolve(auto ? AssemblyTexts.AUTO
                : menu.isManualOn() ? AssemblyTexts.TURN_OFF : AssemblyTexts.TURN_ON);
        JsTechTheme.textCenter(g, font, powerCap, MainframeLayout.POWER_X + MainframeLayout.BTN_W / 2,
                MainframeLayout.BTN_Y + 4, auto ? JsTechTheme.dim() : JsTechTheme.accent());
        JsTechTheme.textCenter(g, font,
                GameText.resolve(auto ? AssemblyTexts.AUTO_ON_SHORT : AssemblyTexts.AUTO_OFF_SHORT),
                MainframeLayout.AUTO_X + MainframeLayout.BTN_W / 2, MainframeLayout.BTN_Y + 4,
                auto ? JsTechTheme.accent() : JsTechTheme.dim());
        final boolean failover = menu.failoverEnabled();
        JsTechTheme.textCenter(g, font,
                GameText.resolve(failover ? AssemblyTexts.FAILOVER_ON : AssemblyTexts.FAILOVER_OFF),
                MainframeLayout.FAILOVER_X + MainframeLayout.BTN_W / 2, MainframeLayout.BTN_Y + 4,
                failover ? JsTechTheme.accent() : JsTechTheme.dim());
    }

    private void opRow(final GuiGraphics g, final String key, final String value, final int y, final int valueColor) {
        JsTechTheme.text(g, font, key, MainframeLayout.COL_R + MainframeLayout.TILE_INSET, y, JsTechTheme.dim());
        JsTechTheme.textRight(g, font, value,
                MainframeLayout.COL_R + MainframeLayout.COL_R_W - MainframeLayout.TILE_INSET, y, valueColor);
    }

    private int psuColor() {
        if (!menu.hasPsu()) {
            return JsTechTheme.dim();
        }
        return menu.buildValid() ? JsTechTheme.green() : JsTechTheme.amber();
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0) {
            if (!menu.isAutoStart() && hover((int) mouseX, (int) mouseY, MainframeLayout.POWER_X, MainframeLayout.BTN_Y,
                    MainframeLayout.BTN_W, MainframeLayout.BTN_H)) {
                sendButton(MainframeMenu.BUTTON_POWER);
                return true;
            }
            if (hover((int) mouseX, (int) mouseY, MainframeLayout.AUTO_X, MainframeLayout.BTN_Y,
                    MainframeLayout.BTN_W, MainframeLayout.BTN_H)) {
                sendButton(MainframeMenu.BUTTON_AUTOSTART);
                return true;
            }
            if (hover((int) mouseX, (int) mouseY, MainframeLayout.FAILOVER_X, MainframeLayout.BTN_Y,
                    MainframeLayout.BTN_W, MainframeLayout.BTN_H)) {
                sendButton(MainframeMenu.BUTTON_FAILOVER);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected HardwareEra screenEra() {
        return menu.hardwareEra();
    }
}
