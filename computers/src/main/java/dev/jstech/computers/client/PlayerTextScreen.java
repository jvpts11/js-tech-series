/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.operation.payload.desktop.PlayerScreenPayloads;
import dev.jstech.computers.os.player.SystemScreen;
import dev.jstech.computers.vm.program.InputSent;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The text screen of a system a player wrote with a shell of its own (computers/docs/design/player-systems.md):
 * the grid of characters the system writes with {@code Screen}, each in its own colours, and its cursor. What is typed,
 * and where the mouse moves and clicks, goes back to the system.
 */
@PaletteHolder
public final class PlayerTextScreen extends AbstractComputerScreen<MonitorSessionMenu> {

    private final BlockPos host;
    private final BlockPos monitor;
    /* The last cell the mouse was over, so a move is told once a cell rather than once a pixel. */
    private int lastColumn = -1;
    private int lastRow = -1;

    private static final int W = MonitorGlass.WIDTH;
    private static final int H = MonitorGlass.HEIGHT;
    private static final float SCALE = 0.75f;
    private static final int ROW_H = H / SystemScreen.ROWS;
    private static final int ESCAPE = 256;
    /* The colours a system writes are its own, given without their opacity: drawn fully opaque. */
    private static final int OPAQUE = 0xFF << 24;

    /* The glass before the system writes on it, the colour of its words until it chooses one, and its cursor. */
    private static final Palette<Glass> GLASS = Palettes.declare(JsComputers.MODID, "os/player_screen",
            new Glass(0xFF000000, 0xFFAAAAAA, 0xFFAAAAAA));

    /* What each machine's system shows, as it was last sent. */
    private static final Map<BlockPos, PlayerScreenPayloads.Screen> SHOWN = new HashMap<>();

    public PlayerTextScreen(final MonitorSessionMenu session, final Inventory inventory, final Component title) {
        super(session, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
        this.host = session.hostPos();
        this.monitor = session.monitorPos();
    }

    /** Takes what a system's screen shows now. */
    public static void accept(final PlayerScreenPayloads.Screen screen) {
        SHOWN.put(screen.host(), screen);
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = this.leftPos;
        final int y = this.topPos;
        final Era era = screenEra() == null ? HardwareEra.STANDARD : screenEra();
        MonitorFrame.renderBody(g, x, y, W, H, era, font, screenPos());
        final Glass glass = GLASS.get();
        g.fill(x, y, x + W, y + H, glass.ground());
        final PlayerScreenPayloads.Screen screen = SHOWN.get(this.host);
        if (screen == null) {
            return;
        }
        final List<String> rows = screen.rows();
        for (int r = 0; r < rows.size(); r++) {
            final String row = rows.get(r);
            for (int c = 0; c < row.length() && c < SystemScreen.COLUMNS; c++) {
                final int at = r * SystemScreen.COLUMNS + c;
                final int cx = x + c * W / SystemScreen.COLUMNS;
                final int cy = y + r * ROW_H;
                final int next = x + (c + 1) * W / SystemScreen.COLUMNS;
                if (at < screen.back().length && screen.back()[at] != 0) {
                    g.fill(cx, cy, next, cy + ROW_H, OPAQUE | screen.back()[at]);
                }
                final char ch = row.charAt(c);
                if (ch != ' ') {
                    Draw.textScaled(g, font, String.valueOf(ch), cx, cy + 1,
                            at < screen.fore().length ? OPAQUE | screen.fore()[at] : glass.text(), SCALE);
                }
            }
        }
        // The cursor blinks where the next character goes, a bar under its cell.
        if (screen.cursor().length == 2 && (System.currentTimeMillis() / 500) % 2 == 0) {
            final int cx = x + screen.cursor()[0] * W / SystemScreen.COLUMNS;
            final int cy = y + screen.cursor()[1] * ROW_H + ROW_H - 2;
            g.fill(cx, cy, cx + W / SystemScreen.COLUMNS, cy + 1, glass.cursor());
        }
    }

    @Override
    public boolean keyPressed(final int key, final int scan, final int mods) {
        if (key == ESCAPE) {
            this.onClose();
            return true;
        }
        this.send(new InputSent(InputSent.KEY, key, "", 0, 0, 0));
        return true;
    }

    @Override
    public boolean charTyped(final char typed, final int mods) {
        this.send(new InputSent(InputSent.CHAR, 0, String.valueOf(typed), 0, 0, 0));
        return true;
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final int[] cell = this.cellAt(mouseX, mouseY);
        if (cell == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        this.send(new InputSent(InputSent.CLICK, 0, "", cell[0], cell[1], button));
        return true;
    }

    @Override
    public void mouseMoved(final double mouseX, final double mouseY) {
        final int[] cell = this.cellAt(mouseX, mouseY);
        if (cell != null && (cell[0] != this.lastColumn || cell[1] != this.lastRow)) {
            this.lastColumn = cell[0];
            this.lastRow = cell[1];
            this.send(new InputSent(InputSent.MOVE, 0, "", cell[0], cell[1], 0));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** The machine's own generation, so the bezel is the monitor that machine would really have. */
    @Override
    @Nullable
    protected Era screenEra() {
        return this.getMenu().hardwareEra();
    }

    private void send(final InputSent sent) {
        send(new PlayerScreenPayloads.Input(this.host, this.monitor, sent));
    }

    /* The cell under the pointer, {column, row}, or null when it is off the glass. */
    @Nullable
    private int[] cellAt(final double mouseX, final double mouseY) {
        final int dx = (int) mouseX - this.leftPos;
        final int dy = (int) mouseY - this.topPos;
        if (dx < 0 || dy < 0 || dx >= W || dy >= H) {
            return null;
        }
        return new int[]{dx * SystemScreen.COLUMNS / W, Math.min(SystemScreen.ROWS - 1, dy / ROW_H)};
    }

    /** The glass's colours: its ground, the words before the system chooses a colour, and the cursor. */
    private record Glass(int ground, int text, int cursor) {
    }
}
