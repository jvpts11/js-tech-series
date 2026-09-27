/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.datacenter.LoadBalanceMode;
import dev.jstech.computers.gui.layout.ServerRouterLayout;
import dev.jstech.computers.menu.ServerRouterMenu;
import dev.jstech.computers.operation.payload.RenameServerRouterPayload;
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Config GUI for the Server Router: a flat-dark panel matching the computer-OS theme.
 */
public final class ServerRouterScreen extends CoreContainerScreen<ServerRouterMenu> {

    private EditBox nameBox;

    public ServerRouterScreen(final ServerRouterMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = ServerRouterLayout.WIDTH;
        this.imageHeight = ServerRouterLayout.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        // No vanilla labels, the panel draws its own.
        this.titleLabelY = -1000;
        this.inventoryLabelY = -1000;

        nameBox = new EditBox(font, leftPos + ServerRouterLayout.NAME_X + 2, topPos + ServerRouterLayout.NAME_Y + 3,
                ServerRouterLayout.WIDTH - 2 * ServerRouterLayout.NAME_X - 4, ServerRouterLayout.NAME_H - 4,
                GameText.component(ServerRouterTexts.NAME_FIELD));
        nameBox.setBordered(false);
        nameBox.setMaxLength(RenameServerRouterPayload.MAX_LEN);
        nameBox.setValue(menu.initialName());
        nameBox.setResponder(s ->
                PacketDistributor.sendToServer(new RenameServerRouterPayload(menu.routerPos(), s)));
        addRenderableWidget(nameBox);
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        JsTechTheme.window(g, x, y, ServerRouterLayout.WIDTH, ServerRouterLayout.HEIGHT);
        JsTechTheme.headerBar(g, x + ServerRouterLayout.HEADER_X, y + ServerRouterLayout.HEADER_Y,
                ServerRouterLayout.WIDTH - 2 * ServerRouterLayout.HEADER_X);

        // Name field background (the EditBox is drawn over this).
        g.fill(x + ServerRouterLayout.NAME_X, y + ServerRouterLayout.NAME_Y,
                x + ServerRouterLayout.WIDTH - ServerRouterLayout.NAME_X,
                y + ServerRouterLayout.NAME_Y + ServerRouterLayout.NAME_H, JsTechTheme.slotBg());
        g.fill(x + ServerRouterLayout.NAME_X, y + ServerRouterLayout.NAME_Y,
                x + ServerRouterLayout.WIDTH - ServerRouterLayout.NAME_X, y + ServerRouterLayout.NAME_Y + 1,
                JsTechTheme.line());

        // Two status tiles.
        JsTechTheme.panel(g, x + ServerRouterLayout.TILE1_X, y + ServerRouterLayout.TILE_Y,
                ServerRouterLayout.TILE_W, ServerRouterLayout.TILE_H);
        JsTechTheme.panel(g, x + ServerRouterLayout.TILE2_X, y + ServerRouterLayout.TILE_Y,
                ServerRouterLayout.TILE_W, ServerRouterLayout.TILE_H);

        // Section list separator.
        JsTechTheme.hLine(g, x + ServerRouterLayout.NAME_X, y + ServerRouterLayout.SEPARATOR_Y,
                ServerRouterLayout.WIDTH - 2 * ServerRouterLayout.NAME_X);

        // One mode button per section row.
        for (int i = 0; i < menu.sectionCount(); i++) {
            final int rowY = ServerRouterLayout.ROW_Y0 + i * ServerRouterLayout.ROW_PITCH;
            final boolean hovered = hover(mouseX, mouseY, ServerRouterLayout.MODE_X, rowY,
                    ServerRouterLayout.MODE_W, ServerRouterLayout.MODE_H);
            JsTechTheme.button(g, x + ServerRouterLayout.MODE_X, y + rowY, ServerRouterLayout.MODE_W,
                    ServerRouterLayout.MODE_H, hovered);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Header.
        JsTechTheme.text(g, font, GameText.resolve(ServerRouterTexts.TITLE), ServerRouterLayout.TITLE_X,
                ServerRouterLayout.TITLE_Y, JsTechTheme.text());
        JsTechTheme.textRight(g, font, GameText.resolve(ServerRouterTexts.TIER),
                ServerRouterLayout.WIDTH - ServerRouterLayout.TITLE_X, ServerRouterLayout.TITLE_Y,
                JsTechTheme.accent());

        JsTechTheme.textS(g, font, GameText.resolve(ServerRouterTexts.NAME), ServerRouterLayout.LABEL_X,
                ServerRouterLayout.NAME_LABEL_Y, JsTechTheme.dim());

        // Input + rack-budget tiles.
        final Direction in = menu.inputFace();
        JsTechTheme.tileTextS(g, font, ServerRouterLayout.TILE1_X, ServerRouterLayout.TILE_Y,
                GameText.resolve(ServerRouterTexts.INPUT),
                GameText.resolve(in == null ? ServerRouterTexts.NONE : inputName(in)), JsTechTheme.accent2());
        final int max = menu.maxRacks();
        final String racks = GameText.resolve(ServerRouterTexts.RACKS_OF.with(menu.managedRacks(), max));
        JsTechTheme.tileTextS(g, font, ServerRouterLayout.TILE2_X, ServerRouterLayout.TILE_Y,
                GameText.resolve(ServerRouterTexts.RACKS),
                racks, menu.overCapacity() ? JsTechTheme.red() : JsTechTheme.green());

        JsTechTheme.textS(g, font, GameText.resolve(ServerRouterTexts.SECTIONS), ServerRouterLayout.LABEL_X,
                ServerRouterLayout.SECTIONS_LABEL_Y, JsTechTheme.dim());

        final int count = menu.sectionCount();
        if (count == 0) {
            JsTechTheme.textS(g, font, GameText.resolve(ServerRouterTexts.NO_SECTIONS),
                    ServerRouterLayout.NO_SECTIONS_X, ServerRouterLayout.NO_SECTIONS_Y, JsTechTheme.dim());
            return;
        }
        for (int i = 0; i < count; i++) {
            final int ry = ServerRouterLayout.ROW_Y0 + i * ServerRouterLayout.ROW_PITCH;
            final Direction face = menu.sectionFace(i);
            JsTechTheme.textS(g, font, face == null ? "?" : GameText.resolve(faceName(face)),
                    ServerRouterLayout.ROW_FACE_X, ry + 3, JsTechTheme.text());
            JsTechTheme.textS(g, font, GameText.resolve(ServerRouterTexts.SECTION_SIZE.with(menu.sectionRacks(i),
                    menu.sectionServers(i))), ServerRouterLayout.ROW_COUNT_X, ry + 3, JsTechTheme.dim());
            final LoadBalanceMode mode = menu.sectionMode(i);
            JsTechTheme.textSCenter(g, font, GameText.resolve(mode.text()),
                    ServerRouterLayout.MODE_X + ServerRouterLayout.MODE_W / 2, ry + 3, modeColor(mode));
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0) {
            for (int i = 0; i < menu.sectionCount(); i++) {
                final int rowY = ServerRouterLayout.ROW_Y0 + i * ServerRouterLayout.ROW_PITCH;
                if (hover((int) mouseX, (int) mouseY, ServerRouterLayout.MODE_X, rowY, ServerRouterLayout.MODE_W,
                        ServerRouterLayout.MODE_H)) {
                    final Direction face = menu.sectionFace(i);
                    if (face != null) {
                        // Cycle this section's load-balance mode via the menu button channel.
                        sendButton(face.get3DDataValue());
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(final int key, final int scan, final int mods) {
        /*
         * While the name field has focus, route typing to it and never let a key (the inventory key 'E') reach the
         * screen and close the GUI. ESC just unfocuses the field.
         */
        if (nameBox.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                nameBox.setFocused(false);
                setFocused(null);
                return true;
            }
            nameBox.keyPressed(key, scan, mods);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(final char c, final int mods) {
        if (nameBox.isFocused()) {
            return nameBox.charTyped(c, mods);
        }
        return super.charTyped(c, mods);
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        // The field takes its colour from the theme drawing this frame, the same as every label around it.
        nameBox.setTextColor(JsTechTheme.text());
        super.render(g, mouseX, mouseY, partialTick);
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
