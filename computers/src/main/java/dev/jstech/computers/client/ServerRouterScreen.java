/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.datacenter.LoadBalanceMode;
import dev.jstech.computers.menu.ServerRouterMenu;
import dev.jstech.computers.operation.payload.RenameServerRouterPayload;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Config GUI for the Server Router: a flat-dark panel matching the computer-OS theme.
 */
public final class ServerRouterScreen extends AbstractContainerScreen<ServerRouterMenu> {

    private static final int W = 190;
    private static final int H = 176;
    private static final int ROW_Y0 = 96;
    private static final int ROW_PITCH = 15;
    private static final int MODE_X = 108;
    private static final int MODE_W = 74;
    private static final int MODE_H = 12;

    private EditBox nameBox;

    public ServerRouterScreen(final ServerRouterMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    @Override
    protected void init() {
        super.init();
        // No vanilla labels, the panel draws its own.
        this.titleLabelY = -1000;
        this.inventoryLabelY = -1000;

        nameBox = new EditBox(font, leftPos + 10, topPos + 39, 170, 10, GameText.component(ServerRouterTexts.NAME_FIELD));
        nameBox.setBordered(false);
        nameBox.setMaxLength(RenameServerRouterPayload.MAX_LEN);
        nameBox.setTextColor(JsTechTheme.text());
        nameBox.setValue(menu.initialName());
        nameBox.setResponder(s ->
                PacketDistributor.sendToServer(new RenameServerRouterPayload(menu.routerPos(), s)));
        addRenderableWidget(nameBox);
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        JsTechTheme.window(g, x, y, W, H);
        JsTechTheme.headerBar(g, x + 6, y + 6, W - 12);

        // Name field background (the EditBox is drawn over this).
        g.fill(x + 8, y + 36, x + W - 8, y + 50, JsTechTheme.slotBg());
        g.fill(x + 8, y + 36, x + W - 8, y + 37, JsTechTheme.line());

        // Two status tiles.
        JsTechTheme.panel(g, x + 8, y + 56, 84, 22);
        JsTechTheme.panel(g, x + 98, y + 56, 84, 22);

        // Section list separator.
        JsTechTheme.hLine(g, x + 8, y + 93, W - 16);

        // One mode button per section row.
        for (int i = 0; i < menu.sectionCount(); i++) {
            final int by = y + ROW_Y0 + i * ROW_PITCH;
            final boolean hovered = mouseX >= x + MODE_X && mouseX < x + MODE_X + MODE_W
                    && mouseY >= by && mouseY < by + MODE_H;
            JsTechTheme.button(g, x + MODE_X, by, MODE_W, MODE_H, hovered);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Header.
        JsTechTheme.text(g, font, GameText.resolve(ServerRouterTexts.TITLE), 12, 10, JsTechTheme.text());
        JsTechTheme.textRight(g, font, GameText.resolve(ServerRouterTexts.TIER), W - 12, 10, JsTechTheme.accent());

        JsTechTheme.textS(g, font, GameText.resolve(ServerRouterTexts.NAME), 10, 28, JsTechTheme.dim());

        // Input + rack-budget tiles.
        final Direction in = menu.inputFace();
        JsTechTheme.tileTextS(g, font, 8, 56, GameText.resolve(ServerRouterTexts.INPUT),
                GameText.resolve(in == null ? ServerRouterTexts.NONE : inputName(in)), JsTechTheme.accent2());
        final int max = menu.maxRacks();
        final String racks = GameText.resolve(ServerRouterTexts.RACKS_OF.with(menu.managedRacks(), max));
        JsTechTheme.tileTextS(g, font, 98, 56, GameText.resolve(ServerRouterTexts.RACKS),
                racks, menu.overCapacity() ? JsTechTheme.red() : JsTechTheme.green());

        JsTechTheme.textS(g, font, GameText.resolve(ServerRouterTexts.SECTIONS), 10, 84, JsTechTheme.dim());

        final int count = menu.sectionCount();
        if (count == 0) {
            JsTechTheme.textS(g, font, GameText.resolve(ServerRouterTexts.NO_SECTIONS), 12, ROW_Y0 + 3, JsTechTheme.dim());
            return;
        }
        for (int i = 0; i < count; i++) {
            final int ry = ROW_Y0 + i * ROW_PITCH;
            final Direction face = menu.sectionFace(i);
            JsTechTheme.textS(g, font, face == null ? "?" : GameText.resolve(faceName(face)),
                    10, ry + 3, JsTechTheme.text());
            JsTechTheme.textS(g, font, GameText.resolve(ServerRouterTexts.SECTION_SIZE.with(menu.sectionRacks(i),
                    menu.sectionServers(i))), 40, ry + 3, JsTechTheme.dim());
            final LoadBalanceMode mode = menu.sectionMode(i);
            JsTechTheme.textSCenter(g, font, GameText.resolve(mode.text()), MODE_X + MODE_W / 2, ry + 3, modeColor(mode));
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0) {
            for (int i = 0; i < menu.sectionCount(); i++) {
                final int by = topPos + ROW_Y0 + i * ROW_PITCH;
                if (mouseX >= leftPos + MODE_X && mouseX < leftPos + MODE_X + MODE_W
                        && mouseY >= by && mouseY < by + MODE_H) {
                    final Direction face = menu.sectionFace(i);
                    if (face != null && minecraft != null && minecraft.gameMode != null) {
                        // Cycle this section's load-balance mode via the menu button channel.
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, face.get3DDataValue());
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    private static int modeColor(final LoadBalanceMode mode) {
        return switch (mode) {
            case ROUND_ROBIN -> JsTechTheme.accent2();
            case LEAST_LOADED -> JsTechTheme.amber();
            case MANUAL -> JsTechTheme.dim();
        };
    }

    /** A section's face, as its row names it. */
    private static TextKey faceName(final Direction face) {
        return switch (face) {
            case DOWN -> ServerRouterTexts.DOWN;
            case UP -> ServerRouterTexts.UP;
            case NORTH -> ServerRouterTexts.NORTH;
            case SOUTH -> ServerRouterTexts.SOUTH;
            case WEST -> ServerRouterTexts.WEST;
            case EAST -> ServerRouterTexts.EAST;
        };
    }

    /** The input face, as its tile names it. */
    private static TextKey inputName(final Direction face) {
        return switch (face) {
            case DOWN -> ServerRouterTexts.INPUT_DOWN;
            case UP -> ServerRouterTexts.INPUT_UP;
            case NORTH -> ServerRouterTexts.INPUT_NORTH;
            case SOUTH -> ServerRouterTexts.INPUT_SOUTH;
            case WEST -> ServerRouterTexts.INPUT_WEST;
            case EAST -> ServerRouterTexts.INPUT_EAST;
        };
    }
}
