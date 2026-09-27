/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.gui.layout.PersonalComputerLayout;
import dev.jstech.computers.menu.PersonalComputerMenu;
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
 * Screen for the Personal Computer: a flat-dark "computer OS" hardware-assembly surface, the same skin as
 * the Mainframe.
 */
public class PersonalComputerScreen extends AbstractAssemblyScreen<PersonalComputerMenu> {

    public PersonalComputerScreen(final PersonalComputerMenu menu, final Inventory inventory,
                                  final Component title) {
        super(menu, inventory, title);
        this.imageWidth = PersonalComputerLayout.WIDTH;
        this.imageHeight = PersonalComputerLayout.HEIGHT;
        this.titleLabelX = -10000;
        this.inventoryLabelY = -10000;
    }

    @Override
    protected void init() {
        super.init();
        // Name field in the header, since a PC is renamed here, in its assembly GUI, never via an anvil.
        setupNameBox(PersonalComputerLayout.NAME_BOX_X, PersonalComputerLayout.NAME_BOX_Y,
                PersonalComputerLayout.NAME_BOX_W, RenamePcPayload.MAX_LEN,
                GameText.component(AssemblyTexts.NAME_THIS_PC).withStyle(ChatFormatting.DARK_GRAY),
                menu.customName(),
                s -> PacketDistributor.sendToServer(new RenamePcPayload(menu.computerPos(), s)));
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        JsTechTheme.window(g, x, y, imageWidth, imageHeight);
        JsTechTheme.headerBar(g, x + PersonalComputerLayout.HEADER_X, y + PersonalComputerLayout.HEADER_Y,
                PersonalComputerLayout.HEADER_W);
        // Name field background (the EditBox is drawn over this).
        nameWell(g, x + PersonalComputerLayout.NAME_WELL_LEFT, y + PersonalComputerLayout.NAME_WELL_TOP,
                x + PersonalComputerLayout.NAME_WELL_RIGHT);
        JsTechTheme.vLine(g, x + PersonalComputerLayout.VLINE_X, y + PersonalComputerLayout.VLINE_Y,
                PersonalComputerLayout.VLINE_H);

        // A cell behind every active hardware slot, reading the menu's own slot positions.
        for (int i = 0; i < PersonalComputerBlockEntity.HARDWARE_SLOTS; i++) {
            final var slot = menu.getSlot(i);
            if (slot.isActive()) {
                JsTechTheme.slot(g, x + slot.x, y + slot.y);
            }
        }

        JsTechTheme.panel(g, x + PersonalComputerLayout.COL_R, y + PersonalComputerLayout.TILE_Y_CAPACITY,
                PersonalComputerLayout.COL_R_W, PersonalComputerLayout.TILE_H_CAPACITY);
        JsTechTheme.panel(g, x + PersonalComputerLayout.COL_R, y + PersonalComputerLayout.TILE_Y_RAM_BUFFER,
                PersonalComputerLayout.COL_R_W, PersonalComputerLayout.TILE_H_RAM_BUFFER);

        final boolean auto = menu.isAutoStart();
        JsTechTheme.button(g, x + PersonalComputerLayout.POWER_X, y + PersonalComputerLayout.POWER_Y,
                PersonalComputerLayout.COL_R_W, PersonalComputerLayout.BTN_H,
                !auto && hover(mouseX, mouseY, PersonalComputerLayout.POWER_X, PersonalComputerLayout.POWER_Y,
                        PersonalComputerLayout.COL_R_W, PersonalComputerLayout.BTN_H));
        JsTechTheme.button(g, x + PersonalComputerLayout.AUTO_X, y + PersonalComputerLayout.AUTO_Y,
                PersonalComputerLayout.COL_R_W, PersonalComputerLayout.BTN_H,
                hover(mouseX, mouseY, PersonalComputerLayout.AUTO_X, PersonalComputerLayout.AUTO_Y,
                        PersonalComputerLayout.COL_R_W, PersonalComputerLayout.BTN_H));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                JsTechTheme.slot(g, x + PersonalComputerLayout.INV_X + col * PersonalComputerLayout.SLOT,
                        y + PersonalComputerLayout.INV_Y + row * PersonalComputerLayout.SLOT);
            }
        }
        for (int col = 0; col < 9; col++) {
            JsTechTheme.slot(g, x + PersonalComputerLayout.INV_X + col * PersonalComputerLayout.SLOT,
                    y + PersonalComputerLayout.INV_Y + PersonalComputerLayout.HOTBAR_GAP);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.TITLE_PC), 12, 11, JsTechTheme.text());
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
        final int pillX = PersonalComputerLayout.HEADER_W - font.width(status);
        JsTechTheme.text(g, font, status, pillX, 11, statusColor);
        g.fill(pillX - 6, 11, pillX - 2, 15, statusColor);

        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.BOARD), PersonalComputerLayout.MOBO_X,
                PersonalComputerLayout.LABEL_ROW_1_Y, menu.hasBoard() ? JsTechTheme.accent() : JsTechTheme.dim());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.CPU), PersonalComputerLayout.RIGHT_X,
                PersonalComputerLayout.LABEL_ROW_1_Y, JsTechTheme.dim());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.PSU), PersonalComputerLayout.MOBO_X,
                PersonalComputerLayout.LABEL_ROW_2_Y, JsTechTheme.dim());
        g.fill(PersonalComputerLayout.PSU_LED_X, PersonalComputerLayout.PSU_LED_Y,
                PersonalComputerLayout.PSU_LED_X + PersonalComputerLayout.PSU_LED_SIZE,
                PersonalComputerLayout.PSU_LED_Y + PersonalComputerLayout.PSU_LED_SIZE, psuColor());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.RAM), PersonalComputerLayout.RIGHT_X,
                PersonalComputerLayout.LABEL_ROW_2_Y, JsTechTheme.dim());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.DISK), PersonalComputerLayout.MOBO_X,
                PersonalComputerLayout.LABEL_ROW_3_Y, JsTechTheme.dim());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.GPU), PersonalComputerLayout.RIGHT_X,
                PersonalComputerLayout.LABEL_ROW_3_Y, JsTechTheme.dim());

        JsTechTheme.tileText(g, font, PersonalComputerLayout.COL_R, PersonalComputerLayout.TILE_Y_CAPACITY,
                GameText.resolve(AssemblyTexts.CAPACITY), JsTechTheme.fmt(menu.capacity()),
                GameText.resolve(AssemblyTexts.ITEMS_PER_TICK), JsTechTheme.text());
        JsTechTheme.tileText(g, font, PersonalComputerLayout.COL_R, PersonalComputerLayout.TILE_Y_RAM_BUFFER,
                GameText.resolve(AssemblyTexts.RAM_BUFFER), JsTechTheme.fmt(menu.ramBuffer()),
                GameText.resolve(AssemblyTexts.ITEMS), JsTechTheme.text());

        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.NETWORK), PersonalComputerLayout.COL_R,
                PersonalComputerLayout.NETWORK_Y, JsTechTheme.dim());
        if (menu.isOnNetwork()) {
            JsTechTheme.textRight(g, font, GameText.resolve(AssemblyTexts.LINKED),
                    PersonalComputerLayout.COL_R + PersonalComputerLayout.COL_R_W, PersonalComputerLayout.NETWORK_Y,
                    JsTechTheme.green());
            final int n = menu.networkServerCount();
            JsTechTheme.textRight(g, font, GameText.resolve((n == 1 ? AssemblyTexts.ONE_SERVER : AssemblyTexts.SERVERS)
                    .with(n)), PersonalComputerLayout.COL_R + PersonalComputerLayout.COL_R_W,
                    PersonalComputerLayout.NETWORK_VALUE_Y, JsTechTheme.dim());
        } else {
            JsTechTheme.textRight(g, font, "--", PersonalComputerLayout.COL_R + PersonalComputerLayout.COL_R_W,
                    PersonalComputerLayout.NETWORK_Y, JsTechTheme.dim());
        }

        final boolean auto = menu.isAutoStart();
        final String powerCap = GameText.resolve(auto ? AssemblyTexts.AUTO
                : menu.isRunning() ? AssemblyTexts.TURN_OFF : AssemblyTexts.TURN_ON);
        JsTechTheme.textCenter(g, font, powerCap, PersonalComputerLayout.POWER_X + PersonalComputerLayout.COL_R_W / 2,
                PersonalComputerLayout.POWER_Y + 4, auto ? JsTechTheme.dim() : JsTechTheme.accent());
        JsTechTheme.textCenter(g, font, GameText.resolve(auto ? AssemblyTexts.AUTO_ON : AssemblyTexts.AUTO_OFF),
                PersonalComputerLayout.AUTO_X + PersonalComputerLayout.COL_R_W / 2,
                PersonalComputerLayout.AUTO_Y + 4, auto ? JsTechTheme.accent() : JsTechTheme.dim());
    }

    private int psuColor() {
        if (!menu.hasPsu()) {
            return JsTechTheme.dim();
        }
        return menu.buildValid() ? JsTechTheme.green() : JsTechTheme.amber();
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        // Clicking the name field selects it for typing; clicking anywhere else deselects it.
        if (nameBox != null) {
            if (nameBox.isMouseOver(mouseX, mouseY)) {
                setFocused(nameBox);
                nameBox.setFocused(true);
                return nameBox.mouseClicked(mouseX, mouseY, button);
            }
            nameBox.setFocused(false);
        }
        if (button == 0) {
            if (!menu.isAutoStart() && hover((int) mouseX, (int) mouseY, PersonalComputerLayout.POWER_X,
                    PersonalComputerLayout.POWER_Y, PersonalComputerLayout.COL_R_W, PersonalComputerLayout.BTN_H)) {
                sendButton(PersonalComputerMenu.BUTTON_POWER);
                return true;
            }
            if (hover((int) mouseX, (int) mouseY, PersonalComputerLayout.AUTO_X, PersonalComputerLayout.AUTO_Y,
                    PersonalComputerLayout.COL_R_W, PersonalComputerLayout.BTN_H)) {
                sendButton(PersonalComputerMenu.BUTTON_AUTOSTART);
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
