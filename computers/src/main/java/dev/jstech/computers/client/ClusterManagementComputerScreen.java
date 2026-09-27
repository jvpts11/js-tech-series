/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.gui.layout.ClusterManagementComputerLayout;
import dev.jstech.computers.menu.ClusterManagementComputerMenu;
import dev.jstech.computers.operation.payload.RenamePcPayload;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Cluster Management Computer's assembly surface: hardware on the left, and on the right what makes
 * it a cluster master: whether the interface card is in, how many clusters the network reaches, and
 * whether the Cluster Manager is on its disk. Everything about the clusters themselves lives in that
 * program on the linked monitor, not here.
 */
public class ClusterManagementComputerScreen extends AbstractAssemblyScreen<ClusterManagementComputerMenu> {

    public ClusterManagementComputerScreen(final ClusterManagementComputerMenu menu, final Inventory inventory,
                                           final Component title) {
        super(menu, inventory, title);
        this.imageWidth = ClusterManagementComputerLayout.WIDTH;
        this.imageHeight = ClusterManagementComputerLayout.HEIGHT;
        this.titleLabelX = -10000;
        this.inventoryLabelY = -10000;
    }

    @Override
    protected void init() {
        super.init();
        setupNameBox(ClusterManagementComputerLayout.NAME_BOX_X, ClusterManagementComputerLayout.NAME_BOX_Y,
                ClusterManagementComputerLayout.NAME_BOX_W, RenamePcPayload.MAX_LEN,
                GameText.component(AssemblyTexts.NAME_THIS_COMPUTER).withStyle(ChatFormatting.DARK_GRAY),
                menu.customName(),
                s -> PacketDistributor.sendToServer(new RenamePcPayload(menu.computerPos(), s)));
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        JsTechTheme.window(g, x, y, imageWidth, imageHeight);
        JsTechTheme.headerBar(g, x + ClusterManagementComputerLayout.HEADER_X,
                y + ClusterManagementComputerLayout.HEADER_Y, ClusterManagementComputerLayout.HEADER_W);
        nameWell(g, x + ClusterManagementComputerLayout.NAME_WELL_LEFT,
                y + ClusterManagementComputerLayout.NAME_WELL_TOP, x + ClusterManagementComputerLayout.NAME_WELL_RIGHT);
        JsTechTheme.vLine(g, x + ClusterManagementComputerLayout.VLINE_X, y + ClusterManagementComputerLayout.VLINE_Y,
                ClusterManagementComputerLayout.VLINE_H);

        // A cell behind every active hardware slot, reading the menu's own slot positions.
        for (int i = 0; i < ClusterManagementComputerBlockEntity.HARDWARE_SLOTS; i++) {
            final var slot = menu.getSlot(i);
            if (slot.isActive()) {
                JsTechTheme.slot(g, x + slot.x, y + slot.y);
            }
        }

        JsTechTheme.panel(g, x + ClusterManagementComputerLayout.COL_R, y + ClusterManagementComputerLayout.TILE_Y0,
                ClusterManagementComputerLayout.COL_R_W, ClusterManagementComputerLayout.TILE_H);
        JsTechTheme.panel(g, x + ClusterManagementComputerLayout.COL_R, y + ClusterManagementComputerLayout.TILE_Y1,
                ClusterManagementComputerLayout.COL_R_W, ClusterManagementComputerLayout.TILE_H);
        JsTechTheme.panel(g, x + ClusterManagementComputerLayout.COL_R, y + ClusterManagementComputerLayout.TILE_Y2,
                ClusterManagementComputerLayout.COL_R_W, ClusterManagementComputerLayout.TILE_H);
        JsTechTheme.panel(g, x + ClusterManagementComputerLayout.COL_R, y + ClusterManagementComputerLayout.TILE_Y3,
                ClusterManagementComputerLayout.COL_R_W, ClusterManagementComputerLayout.TILE_H);

        final boolean auto = menu.isAutoStart();
        JsTechTheme.button(g, x + ClusterManagementComputerLayout.POWER_X, y + ClusterManagementComputerLayout.POWER_Y,
                ClusterManagementComputerLayout.COL_R_W, ClusterManagementComputerLayout.BTN_H,
                !auto && hover(mouseX, mouseY, ClusterManagementComputerLayout.POWER_X,
                        ClusterManagementComputerLayout.POWER_Y, ClusterManagementComputerLayout.COL_R_W,
                        ClusterManagementComputerLayout.BTN_H));
        JsTechTheme.button(g, x + ClusterManagementComputerLayout.AUTO_X, y + ClusterManagementComputerLayout.AUTO_Y,
                ClusterManagementComputerLayout.COL_R_W, ClusterManagementComputerLayout.BTN_H,
                hover(mouseX, mouseY, ClusterManagementComputerLayout.AUTO_X, ClusterManagementComputerLayout.AUTO_Y,
                        ClusterManagementComputerLayout.COL_R_W, ClusterManagementComputerLayout.BTN_H));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                JsTechTheme.slot(g,
                        x + ClusterManagementComputerLayout.INV_X + col * ClusterManagementComputerLayout.SLOT,
                        y + ClusterManagementComputerLayout.INV_Y + row * ClusterManagementComputerLayout.SLOT);
            }
        }
        for (int col = 0; col < 9; col++) {
            JsTechTheme.slot(g, x + ClusterManagementComputerLayout.INV_X + col * ClusterManagementComputerLayout.SLOT,
                    y + ClusterManagementComputerLayout.INV_Y + ClusterManagementComputerLayout.HOTBAR_GAP);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.TITLE_CLUSTER_MANAGEMENT), 12, 11, JsTechTheme.text());
        final String status;
        final int statusColor;
        if (!menu.buildValid()) {
            status = GameText.resolve(AssemblyTexts.OFFLINE);
            statusColor = JsTechTheme.red();
        } else if (menu.isRunning()) {
            status = GameText.resolve(AssemblyTexts.ONLINE);
            statusColor = JsTechTheme.green();
        } else {
            status = GameText.resolve(AssemblyTexts.READY);
            statusColor = JsTechTheme.amber();
        }
        final int pillX = ClusterManagementComputerLayout.HEADER_W - font.width(status);
        JsTechTheme.text(g, font, status, pillX, 11, statusColor);
        g.fill(pillX - 6, 11, pillX - 2, 15, statusColor);

        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.BOARD), ClusterManagementComputerLayout.MOBO_X,
                ClusterManagementComputerLayout.LABEL_ROW_1_Y,
                menu.hasBoard() ? JsTechTheme.accent() : JsTechTheme.dim());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.CPU), ClusterManagementComputerLayout.RIGHT_X,
                ClusterManagementComputerLayout.LABEL_ROW_1_Y, JsTechTheme.dim());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.PSU), ClusterManagementComputerLayout.MOBO_X,
                ClusterManagementComputerLayout.LABEL_ROW_2_Y, JsTechTheme.dim());
        g.fill(ClusterManagementComputerLayout.PSU_LED_X, ClusterManagementComputerLayout.PSU_LED_Y,
                ClusterManagementComputerLayout.PSU_LED_X + ClusterManagementComputerLayout.PSU_LED_SIZE,
                ClusterManagementComputerLayout.PSU_LED_Y + ClusterManagementComputerLayout.PSU_LED_SIZE, psuColor());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.RAM), ClusterManagementComputerLayout.RIGHT_X,
                ClusterManagementComputerLayout.LABEL_ROW_2_Y, JsTechTheme.dim());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.DISK), ClusterManagementComputerLayout.MOBO_X,
                ClusterManagementComputerLayout.LABEL_ROW_3_Y, JsTechTheme.dim());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.PCIE), ClusterManagementComputerLayout.RIGHT_X,
                ClusterManagementComputerLayout.LABEL_ROW_3_Y,
                menu.hasCard() ? JsTechTheme.accent() : JsTechTheme.dim());

        // Small-font tiles: four fit where the crafting computer keeps three.
        JsTechTheme.tileTextS(g, font, ClusterManagementComputerLayout.COL_R, ClusterManagementComputerLayout.TILE_Y0,
                GameText.resolve(AssemblyTexts.CAPACITY),
                GameText.resolve(AssemblyTexts.CAPACITY_AND_BUFFER.with(JsTechTheme.fmt(menu.capacity()),
                        JsTechTheme.fmt(menu.ramBuffer()))), JsTechTheme.text());
        JsTechTheme.tileTextS(g, font, ClusterManagementComputerLayout.COL_R, ClusterManagementComputerLayout.TILE_Y1,
                GameText.resolve(AssemblyTexts.NETWORK),
                GameText.resolve(menu.isOnNetwork() ? AssemblyTexts.LINKED : AssemblyTexts.NO_LINK),
                menu.isOnNetwork() ? JsTechTheme.green() : JsTechTheme.dim());
        if (!menu.hasCard()) {
            JsTechTheme.tileTextS(g, font, ClusterManagementComputerLayout.COL_R,
                    ClusterManagementComputerLayout.TILE_Y2, GameText.resolve(AssemblyTexts.CLUSTERS_IN_REACH),
                    GameText.resolve(AssemblyTexts.NO_INTERFACE_CARD), JsTechTheme.amber());
        } else {
            JsTechTheme.tileTextS(g, font, ClusterManagementComputerLayout.COL_R,
                    ClusterManagementComputerLayout.TILE_Y2, GameText.resolve(AssemblyTexts.CLUSTERS_IN_REACH),
                    GameText.resolve(
                            AssemblyTexts.CLUSTERS.with(menu.supercomputers(), menu.datacenters(), menu.lanes())),
                    JsTechTheme.text());
        }
        JsTechTheme.tileTextS(g, font, ClusterManagementComputerLayout.COL_R, ClusterManagementComputerLayout.TILE_Y3,
                GameText.resolve(AssemblyTexts.MANAGEMENT),
                GameText.resolve(
                        menu.managerInstalled() ? AssemblyTexts.MANAGER_INSTALLED : AssemblyTexts.MANAGER_MISSING),
                menu.managerInstalled() ? JsTechTheme.accent() : JsTechTheme.dim());

        final boolean auto = menu.isAutoStart();
        final String powerCap = GameText.resolve(auto ? AssemblyTexts.AUTO
                : menu.isRunning() ? AssemblyTexts.TURN_OFF : AssemblyTexts.TURN_ON);
        JsTechTheme.textCenter(g, font, powerCap, ClusterManagementComputerLayout.POWER_X
                + ClusterManagementComputerLayout.COL_R_W / 2, ClusterManagementComputerLayout.POWER_Y + 4,
                auto ? JsTechTheme.dim() : JsTechTheme.accent());
        JsTechTheme.textCenter(g, font, GameText.resolve(auto ? AssemblyTexts.AUTO_ON : AssemblyTexts.AUTO_OFF),
                ClusterManagementComputerLayout.AUTO_X + ClusterManagementComputerLayout.COL_R_W / 2,
                ClusterManagementComputerLayout.AUTO_Y + 4, auto ? JsTechTheme.accent() : JsTechTheme.dim());
    }

    private int psuColor() {
        if (!menu.hasPsu()) {
            return JsTechTheme.dim();
        }
        return menu.buildValid() ? JsTechTheme.green() : JsTechTheme.amber();
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (nameBox != null) {
            if (nameBox.isMouseOver(mouseX, mouseY)) {
                setFocused(nameBox);
                nameBox.setFocused(true);
                return nameBox.mouseClicked(mouseX, mouseY, button);
            }
            nameBox.setFocused(false);
        }
        if (button == 0) {
            if (!menu.isAutoStart() && hover((int) mouseX, (int) mouseY, ClusterManagementComputerLayout.POWER_X,
                    ClusterManagementComputerLayout.POWER_Y, ClusterManagementComputerLayout.COL_R_W,
                    ClusterManagementComputerLayout.BTN_H)) {
                sendButton(ClusterManagementComputerMenu.BUTTON_POWER);
                return true;
            }
            if (hover((int) mouseX, (int) mouseY, ClusterManagementComputerLayout.AUTO_X,
                    ClusterManagementComputerLayout.AUTO_Y, ClusterManagementComputerLayout.COL_R_W,
                    ClusterManagementComputerLayout.BTN_H)) {
                sendButton(ClusterManagementComputerMenu.BUTTON_AUTOSTART);
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
