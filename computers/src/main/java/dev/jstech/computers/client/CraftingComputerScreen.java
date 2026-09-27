/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.gui.layout.CraftingComputerLayout;
import dev.jstech.computers.menu.CraftingComputerMenu;
import dev.jstech.computers.operation.payload.RenamePcPayload;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

/**
 * Screen for the Crafting Computer's assembly surface, the same flat-dark "computer OS" skin as the
 * Personal Computer. This is a hardware-only surface: recipe files are managed by the Crafting Manager
 * program on the linked monitor, not here.
 */
public class CraftingComputerScreen extends AbstractAssemblyScreen<CraftingComputerMenu> {

    public CraftingComputerScreen(final CraftingComputerMenu menu, final Inventory inventory,
                                  final Component title) {
        super(menu, inventory, title);
        this.imageWidth = CraftingComputerLayout.WIDTH;
        this.imageHeight = CraftingComputerLayout.HEIGHT;
        this.titleLabelX = -10000;
        this.inventoryLabelY = -10000;
    }

    @Override
    protected void init() {
        super.init();
        // Name field in the header, since computers are renamed here, never via an anvil.
        setupNameBox(CraftingComputerLayout.NAME_BOX_X, CraftingComputerLayout.NAME_BOX_Y,
                CraftingComputerLayout.NAME_BOX_W, RenamePcPayload.MAX_LEN,
                GameText.component(AssemblyTexts.NAME_THIS_COMPUTER).withStyle(ChatFormatting.DARK_GRAY),
                menu.customName(),
                s -> PacketDistributor.sendToServer(new RenamePcPayload(menu.computerPos(), s)));
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        JsTechTheme.window(g, x, y, imageWidth, imageHeight);
        JsTechTheme.headerBar(g, x + CraftingComputerLayout.HEADER_X, y + CraftingComputerLayout.HEADER_Y,
                CraftingComputerLayout.HEADER_W);
        // Name field background (the EditBox is drawn over this).
        nameWell(g, x + CraftingComputerLayout.NAME_WELL_LEFT, y + CraftingComputerLayout.NAME_WELL_TOP,
                x + CraftingComputerLayout.NAME_WELL_RIGHT);
        JsTechTheme.vLine(g, x + CraftingComputerLayout.VLINE_X, y + CraftingComputerLayout.VLINE_Y,
                CraftingComputerLayout.VLINE_H);

        // A cell behind every active hardware slot, reading the menu's own slot positions.
        for (int i = 0; i < CraftingComputerBlockEntity.HARDWARE_SLOTS; i++) {
            final var slot = menu.getSlot(i);
            if (slot.isActive()) {
                JsTechTheme.slot(g, x + slot.x, y + slot.y);
            }
        }

        JsTechTheme.panel(g, x + CraftingComputerLayout.COL_R, y + CraftingComputerLayout.TILE_Y0,
                CraftingComputerLayout.COL_R_W, CraftingComputerLayout.TILE_H);  // CAPACITY
        JsTechTheme.panel(g, x + CraftingComputerLayout.COL_R, y + CraftingComputerLayout.TILE_Y1,
                CraftingComputerLayout.COL_R_W, CraftingComputerLayout.TILE_H);  // CRAFTING
        JsTechTheme.panel(g, x + CraftingComputerLayout.COL_R, y + CraftingComputerLayout.TILE_Y2,
                CraftingComputerLayout.COL_R_W, CraftingComputerLayout.TILE_H);  // RECIPE ROM

        final boolean auto = menu.isAutoStart();
        JsTechTheme.button(g, x + CraftingComputerLayout.POWER_X, y + CraftingComputerLayout.POWER_Y,
                CraftingComputerLayout.COL_R_W, CraftingComputerLayout.BTN_H,
                !auto && hover(mouseX, mouseY, CraftingComputerLayout.POWER_X, CraftingComputerLayout.POWER_Y,
                        CraftingComputerLayout.COL_R_W, CraftingComputerLayout.BTN_H));
        JsTechTheme.button(g, x + CraftingComputerLayout.AUTO_X, y + CraftingComputerLayout.AUTO_Y,
                CraftingComputerLayout.COL_R_W, CraftingComputerLayout.BTN_H,
                hover(mouseX, mouseY, CraftingComputerLayout.AUTO_X, CraftingComputerLayout.AUTO_Y,
                        CraftingComputerLayout.COL_R_W, CraftingComputerLayout.BTN_H));

        // Player inventory slots.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                JsTechTheme.slot(g, x + CraftingComputerLayout.INV_X + col * CraftingComputerLayout.SLOT,
                        y + CraftingComputerLayout.INV_Y + row * CraftingComputerLayout.SLOT);
            }
        }
        for (int col = 0; col < 9; col++) {
            JsTechTheme.slot(g, x + CraftingComputerLayout.INV_X + col * CraftingComputerLayout.SLOT,
                    y + CraftingComputerLayout.INV_Y + CraftingComputerLayout.HOTBAR_GAP);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.TITLE_CRAFTING), 12, 11, JsTechTheme.text());
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
        final int pillX = CraftingComputerLayout.HEADER_W - font.width(status);
        JsTechTheme.text(g, font, status, pillX, 11, statusColor);
        g.fill(pillX - 6, 11, pillX - 2, 15, statusColor);

        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.BOARD), CraftingComputerLayout.MOBO_X,
                CraftingComputerLayout.LABEL_ROW_1_Y, menu.hasBoard() ? JsTechTheme.accent() : JsTechTheme.dim());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.CPU), CraftingComputerLayout.RIGHT_X,
                CraftingComputerLayout.LABEL_ROW_1_Y, JsTechTheme.dim());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.PSU), CraftingComputerLayout.MOBO_X,
                CraftingComputerLayout.LABEL_ROW_2_Y, JsTechTheme.dim());
        g.fill(CraftingComputerLayout.PSU_LED_X, CraftingComputerLayout.PSU_LED_Y,
                CraftingComputerLayout.PSU_LED_X + CraftingComputerLayout.PSU_LED_SIZE,
                CraftingComputerLayout.PSU_LED_Y + CraftingComputerLayout.PSU_LED_SIZE, psuColor());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.RAM), CraftingComputerLayout.RIGHT_X,
                CraftingComputerLayout.LABEL_ROW_2_Y, JsTechTheme.dim());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.DISK), CraftingComputerLayout.MOBO_X,
                CraftingComputerLayout.LABEL_ROW_3_Y, JsTechTheme.dim());
        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.PCIE), CraftingComputerLayout.RIGHT_X,
                CraftingComputerLayout.LABEL_ROW_3_Y,
                menu.craftFactorX100() > 0 ? JsTechTheme.amber() : JsTechTheme.dim());

        final String perTick = GameText.resolve(AssemblyTexts.ITEMS_PER_TICK);
        JsTechTheme.tileText(g, font, CraftingComputerLayout.COL_R, CraftingComputerLayout.TILE_Y0,
                GameText.resolve(AssemblyTexts.CAPACITY), JsTechTheme.fmt(menu.capacity()), perTick,
                JsTechTheme.text());
        final int factor = menu.craftFactorX100();
        if (factor > 0) {
            /*
             * The Crafting Card is an accelerator with two stats: throughput (this tile's value, factor x CPU) and
             * threads (how many of a craft's stages this computer runs at once, summed over the installed cards).
             */
            JsTechTheme.tileText(g, font, CraftingComputerLayout.COL_R, CraftingComputerLayout.TILE_Y1,
                    GameText.resolve(AssemblyTexts.CRAFT_CARD.with(menu.craftThreads(), formatFactor(factor))),
                    JsTechTheme.fmt(menu.craftThroughput()), perTick, JsTechTheme.accent());
        } else {
            // No Crafting Card installed: the computer runs but cannot craft.
            JsTechTheme.tileText(g, font, CraftingComputerLayout.COL_R, CraftingComputerLayout.TILE_Y1,
                    GameText.resolve(AssemblyTexts.CRAFT), GameText.resolve(AssemblyTexts.NO_CARD), "",
                    JsTechTheme.dim());
        }
        JsTechTheme.tileText(g, font, CraftingComputerLayout.COL_R, CraftingComputerLayout.TILE_Y2,
                GameText.resolve(AssemblyTexts.RECIPE_ROM),
                GameText.resolve(AssemblyTexts.OF.with(menu.romUsed(), menu.romLimit())), "", JsTechTheme.text());

        JsTechTheme.text(g, font, GameText.resolve(AssemblyTexts.NETWORK), CraftingComputerLayout.COL_R,
                CraftingComputerLayout.NETWORK_Y, JsTechTheme.dim());
        if (menu.isOnNetwork()) {
            JsTechTheme.textRight(g, font, GameText.resolve(AssemblyTexts.LINKED),
                    CraftingComputerLayout.COL_R + CraftingComputerLayout.COL_R_W, CraftingComputerLayout.NETWORK_Y,
                    JsTechTheme.green());
        } else {
            JsTechTheme.textRight(g, font, "--", CraftingComputerLayout.COL_R + CraftingComputerLayout.COL_R_W,
                    CraftingComputerLayout.NETWORK_Y, JsTechTheme.dim());
        }

        final boolean auto = menu.isAutoStart();
        final String powerCap = GameText.resolve(auto ? AssemblyTexts.AUTO
                : menu.isRunning() ? AssemblyTexts.TURN_OFF : AssemblyTexts.TURN_ON);
        JsTechTheme.textCenter(g, font, powerCap, CraftingComputerLayout.POWER_X + CraftingComputerLayout.COL_R_W / 2,
                CraftingComputerLayout.POWER_Y + 4, auto ? JsTechTheme.dim() : JsTechTheme.accent());
        JsTechTheme.textCenter(g, font, GameText.resolve(auto ? AssemblyTexts.AUTO_ON : AssemblyTexts.AUTO_OFF),
                CraftingComputerLayout.AUTO_X + CraftingComputerLayout.COL_R_W / 2,
                CraftingComputerLayout.AUTO_Y + 4, auto ? JsTechTheme.accent() : JsTechTheme.dim());
    }

    private static String formatFactor(final int factorX100) {
        if (factorX100 % 100 == 0) {
            return Integer.toString(factorX100 / 100);
        }
        return String.format(Locale.ROOT, "%.1f", factorX100 / 100.0);
    }

    private int psuColor() {
        if (!menu.hasPsu()) {
            return JsTechTheme.dim();
        }
        return menu.buildValid() ? JsTechTheme.green() : JsTechTheme.amber();
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        // Computer rename field interaction.
        if (nameBox != null) {
            if (nameBox.isMouseOver(mouseX, mouseY)) {
                setFocused(nameBox);
                nameBox.setFocused(true);
                return nameBox.mouseClicked(mouseX, mouseY, button);
            }
            nameBox.setFocused(false);
        }
        if (button == 0) {
            if (!menu.isAutoStart() && hover((int) mouseX, (int) mouseY, CraftingComputerLayout.POWER_X,
                    CraftingComputerLayout.POWER_Y, CraftingComputerLayout.COL_R_W, CraftingComputerLayout.BTN_H)) {
                sendButton(CraftingComputerMenu.BUTTON_POWER);
                return true;
            }
            if (hover((int) mouseX, (int) mouseY, CraftingComputerLayout.AUTO_X, CraftingComputerLayout.AUTO_Y,
                    CraftingComputerLayout.COL_R_W, CraftingComputerLayout.BTN_H)) {
                sendButton(CraftingComputerMenu.BUTTON_AUTOSTART);
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
