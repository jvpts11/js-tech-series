/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import static dev.jstech.computers.client.AsicTexts.ADDRESS;
import static dev.jstech.computers.client.AsicTexts.ADDRESS_HINT;
import static dev.jstech.computers.client.AsicTexts.BLOCKS;
import static dev.jstech.computers.client.AsicTexts.BOARD;
import static dev.jstech.computers.client.AsicTexts.CELSIUS;
import static dev.jstech.computers.client.AsicTexts.DAYS;
import static dev.jstech.computers.client.AsicTexts.DRAW;
import static dev.jstech.computers.client.AsicTexts.FANS;
import static dev.jstech.computers.client.AsicTexts.FIRST_POOL;
import static dev.jstech.computers.client.AsicTexts.FOUND;
import static dev.jstech.computers.client.AsicTexts.HASHBOARDS;
import static dev.jstech.computers.client.AsicTexts.HASHRATE;
import static dev.jstech.computers.client.AsicTexts.HOURS;
import static dev.jstech.computers.client.AsicTexts.IDLE;
import static dev.jstech.computers.client.AsicTexts.MAKER;
import static dev.jstech.computers.client.AsicTexts.MINING;
import static dev.jstech.computers.client.AsicTexts.MINUTES;
import static dev.jstech.computers.client.AsicTexts.NONE;
import static dev.jstech.computers.client.AsicTexts.NO_ADDRESS;
import static dev.jstech.computers.client.AsicTexts.NO_POOL;
import static dev.jstech.computers.client.AsicTexts.NO_POWER;
import static dev.jstech.computers.client.AsicTexts.ONE_BLOCK;
import static dev.jstech.computers.client.AsicTexts.PAYS_TO;
import static dev.jstech.computers.client.AsicTexts.POOL;
import static dev.jstech.computers.client.AsicTexts.POOL_ON;
import static dev.jstech.computers.client.AsicTexts.RPM;
import static dev.jstech.computers.client.AsicTexts.SET;
import static dev.jstech.computers.client.AsicTexts.SOLO;
import static dev.jstech.computers.client.AsicTexts.SYSTEM;
import static dev.jstech.computers.client.AsicTexts.UPTIME;
import static dev.jstech.computers.client.AsicTexts.WAITING;
import static dev.jstech.computers.client.AsicTexts.WATTS;

import dev.jstech.computers.blockentity.AsicBlockEntity;
import dev.jstech.computers.client.assembly.AssemblyPainter;
import dev.jstech.computers.gui.layout.AsicLayout;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Box;
import dev.jstech.computers.menu.AsicMenu;
import dev.jstech.computers.operation.payload.teracoin.AsicAddressPayload;
import dev.jstech.computers.teracoin.AsicModel;
import dev.jstech.computers.teracoin.TeracoinAddress;
import dev.jstech.computers.teracoin.TeracoinHashrate;
import dev.jstech.core.jstechseries.theme.client.JsTechTheme;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/**
 * An ASIC's window (computers/docs/design/teracoin.md), as approved: Blockwright's name and the model in the header
 * with what it does; where it pays, typed and SET, and its pool, the button moving it to the network's next one, Solo
 * mining alone; its hashboards, each one's share of its work and its heat; its hashrate, draw and fans; the blocks it
 * found, that it has no system, and how long it has mined.
 */
public class AsicScreen extends AbstractAssemblyScreen<AsicMenu> {

    /* A day of the game and an hour of it, in ticks, and the minutes of an hour. */
    private static final long TICKS_A_DAY = 24_000L;
    private static final long TICKS_AN_HOUR = 1_000L;
    private static final long MINUTES_AN_HOUR = 60L;
    private static final int FIELD_INSET = 3;
    private static final int LAMP = 7;

    public AsicScreen(final AsicMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = AsicLayout.WIDTH;
        this.imageHeight = AsicLayout.HEIGHT;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
    }

    @Override
    protected void init() {
        super.init();
        setupNameBox(AsicLayout.ADDRESS_X + FIELD_INSET, AsicLayout.ADDRESS_Y + FIELD_INSET,
                AsicLayout.ADDRESS_W - 2 * FIELD_INSET, TeracoinAddress.LENGTH, GameText.component(ADDRESS_HINT),
                menu.asic().address(), typed -> { });
        // The field is the payout address, not a name, and a narrator says so.
        nameBox.setMessage(GameText.component(ADDRESS));
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (clickNameBox(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            final int mx = (int) mouseX;
            final int my = (int) mouseY;
            if (hover(mx, my, AsicLayout.POOL_X, AsicLayout.BUTTON_Y, AsicLayout.POOL_W, AsicLayout.BUTTON_H)) {
                sendButton(AsicMenu.BUTTON_NEXT_POOL);
                return true;
            }
            if (hover(mx, my, AsicLayout.SOLO_X, AsicLayout.BUTTON_Y, AsicLayout.SOLO_W, AsicLayout.BUTTON_H)) {
                sendButton(AsicMenu.BUTTON_SOLO);
                return true;
            }
            if (nameBox != null
                    && hover(mx, my, AsicLayout.SET_X, AsicLayout.BUTTON_Y, AsicLayout.SET_W, AsicLayout.BUTTON_H)) {
                send(new AsicAddressPayload(menu.asicPos(), nameBox.getValue()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    /** The ASIC's own era, whose skin its window wears. */
    @Override
    @Nullable
    protected Era screenEra() {
        return menu.asic().model().era();
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        final AsicBlockEntity asic = menu.asic();
        final AsicModel model = asic.model();
        JsTechTheme.window(g, x, y, imageWidth, imageHeight);
        JsTechTheme.headerBar(g, x + AsicLayout.HEADER_X, y + AsicLayout.HEADER_Y, AsicLayout.HEADER_W);
        JsTechTheme.text(g, font, GameText.resolve(MAKER) + " " + model.name().toUpperCase(Locale.ROOT),
                x + AsicLayout.TITLE_X, y + AsicLayout.TITLE_Y, JsTechTheme.accent());
        final String state = GameText.resolve(stateWord(asic));
        final int stateColour = asic.mining() ? JsTechTheme.green() : JsTechTheme.amber();
        final int stateX = x + AsicLayout.HEADER_X + AsicLayout.HEADER_W - 4 - font.width(state);
        JsTechTheme.text(g, font, state, stateX, y + AsicLayout.TITLE_Y, stateColour);
        AssemblyPainter.drawLamp(g, stateX - LAMP, y + AsicLayout.TITLE_Y + 3, stateColour, true);
        payPanel(g, x, y, asic, mouseX, mouseY);
        boards(g, x, y, asic, model);
        final boolean on = asic.mining();
        final int dim = JsTechTheme.dim();
        readout(g, x, y, AsicLayout.READOUT_X, AsicLayout.BOARDS_Y, HASHRATE,
                on ? TeracoinHashrate.format(model.hashrate()) : "--", on ? JsTechTheme.accent() : dim);
        readout(g, x, y, AsicLayout.READOUT_X, AsicLayout.BOARDS_Y + AsicLayout.READOUT_PITCH, DRAW,
                GameText.resolve(WATTS.with(String.format(Locale.ROOT, "%,d", asic.powered() ? model.watts() : 0))),
                JsTechTheme.text());
        readout(g, x, y, AsicLayout.READOUT_X, AsicLayout.BOARDS_Y + 2 * AsicLayout.READOUT_PITCH, FANS,
                GameText.resolve(RPM.with(String.format(Locale.ROOT, "%,d", on ? model.fanRpm() : 0))),
                JsTechTheme.text());
        readout(g, x, y, AsicLayout.LOWER_X, AsicLayout.LOWER_Y, FOUND, GameText.resolve(asic.found() == 1
                ? ONE_BLOCK.text() : BLOCKS.with(asic.found())), JsTechTheme.text());
        readout(g, x, y, AsicLayout.LOWER_X, AsicLayout.LOWER_Y + AsicLayout.LOWER_PITCH, SYSTEM,
                GameText.resolve(NONE), dim);
        readout(g, x, y, AsicLayout.LOWER_X, AsicLayout.LOWER_Y + 2 * AsicLayout.LOWER_PITCH, UPTIME,
                uptime(asic.uptime()), JsTechTheme.text());
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                JsTechTheme.slot(g, x + AsicLayout.INVENTORY_X + col * 18, y + AsicLayout.INVENTORY_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            JsTechTheme.slot(g, x + AsicLayout.INVENTORY_X + col * 18, y + AsicLayout.INVENTORY_Y + 58);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
    }

    /* Where it pays, in its well, and its pool with Solo and SET beside it. */
    private void payPanel(final GuiGraphics g, final int x, final int y, final AsicBlockEntity asic, final int mouseX,
                          final int mouseY) {
        JsTechTheme.panel(g, x + AsicLayout.PAYS_X, y + AsicLayout.PAYS_Y, AsicLayout.PAYS_W, AsicLayout.PAYS_H);
        final int dim = JsTechTheme.dim();
        JsTechTheme.textS(g, font, GameText.resolve(PAYS_TO), x + AsicLayout.ADDRESS_X, y + AsicLayout.PAYS_CAPTION_Y,
                dim);
        g.fill(x + AsicLayout.ADDRESS_X, y + AsicLayout.ADDRESS_Y, x + AsicLayout.ADDRESS_X + AsicLayout.ADDRESS_W,
                y + AsicLayout.ADDRESS_Y + AsicLayout.ADDRESS_H, JsTechTheme.track());
        AssemblyPainter.frame(g, x + AsicLayout.ADDRESS_X, y + AsicLayout.ADDRESS_Y,
                x + AsicLayout.ADDRESS_X + AsicLayout.ADDRESS_W, y + AsicLayout.ADDRESS_Y + AsicLayout.ADDRESS_H,
                JsTechTheme.line());
        JsTechTheme.textS(g, font, GameText.resolve(POOL), x + AsicLayout.ADDRESS_X, y + AsicLayout.POOL_CAPTION_Y,
                dim);
        button(g, x, y, AsicLayout.POOL_X, AsicLayout.POOL_W, poolWord(asic), !asic.alone(), JsTechTheme.text(),
                mouseX, mouseY);
        button(g, x, y, AsicLayout.SOLO_X, AsicLayout.SOLO_W, GameText.resolve(SOLO), asic.alone(),
                asic.alone() ? JsTechTheme.text() : dim, mouseX, mouseY);
        button(g, x, y, AsicLayout.SET_X, AsicLayout.SET_W, GameText.resolve(SET), false, JsTechTheme.accent(),
                mouseX, mouseY);
    }

    /* Each hashboard: its number, its share's bar, its share of the hashrate and its heat. */
    private void boards(final GuiGraphics g, final int x, final int y, final AsicBlockEntity asic,
                        final AsicModel model) {
        JsTechTheme.panel(g, x + AsicLayout.BOARDS_X, y + AsicLayout.BOARDS_Y, AsicLayout.BOARDS_W,
                AsicLayout.BOARDS_H);
        final int dim = JsTechTheme.dim();
        JsTechTheme.textS(g, font, GameText.resolve(HASHBOARDS), x + AsicLayout.BOARDS_X + 4,
                y + AsicLayout.BOARDS_Y + 4, dim);
        final boolean on = asic.mining();
        for (int i = 0; i < model.boards(); i++) {
            final int row = y + AsicLayout.BOARD_ROW_Y + i * AsicLayout.BOARD_PITCH;
            JsTechTheme.textS(g, font, GameText.resolve(BOARD.with(i + 1)), x + AsicLayout.BOARDS_X + 4, row,
                    JsTechTheme.text());
            final int barX = x + AsicLayout.BOARD_BAR_X;
            g.fill(barX, row + 2, barX + AsicLayout.BOARD_BAR_W, row + 6, JsTechTheme.track());
            if (on) {
                // Each board does nearly its whole share; the last few hashes of each are lost to its stale shares.
                final int filled = (int) (AsicLayout.BOARD_BAR_W * (0.96 + 0.01 * i));
                g.fill(barX, row + 2, barX + filled, row + 6, JsTechTheme.accent());
            }
            JsTechTheme.textS(g, font, on ? TeracoinHashrate.format(model.boardHashrate()) : "--",
                    x + AsicLayout.BOARD_RATE_X, row, JsTechTheme.text());
            JsTechTheme.textS(g, font, on ? GameText.resolve(CELSIUS.with(model.heatOf(i))) : "--",
                    x + AsicLayout.BOARD_HEAT_X, row, dim);
        }
    }

    private void readout(final GuiGraphics g, final int x, final int y, final int left, final int top,
                         final TextKey label, final String value, final int colour) {
        AssemblyPainter.drawReadout(g, font, x, y, new Box(left, top, AsicLayout.READOUT_W, AsicLayout.READOUT_H),
                GameText.resolve(label), value, colour);
    }

    private void button(final GuiGraphics g, final int x, final int y, final int left, final int width,
                        final String word, final boolean pressed, final int colour, final int mouseX,
                        final int mouseY) {
        final boolean over = hover(mouseX, mouseY, left, AsicLayout.BUTTON_Y, width, AsicLayout.BUTTON_H);
        if (pressed) {
            JsTechTheme.selectedTab(g, x + left, y + AsicLayout.BUTTON_Y, width, AsicLayout.BUTTON_H);
        } else {
            JsTechTheme.button(g, x + left, y + AsicLayout.BUTTON_Y, width, AsicLayout.BUTTON_H, over);
        }
        final String shown = font.plainSubstrByWidth(word, width - 10);
        JsTechTheme.text(g, font, shown, x + left + (width - font.width(shown)) / 2, y + AsicLayout.BUTTON_Y + 4,
                colour);
    }

    /* The pool it mines for and the server it is on, or the network's first before it mined for one. */
    private static String poolWord(final AsicBlockEntity asic) {
        if (asic.poolShown().isEmpty()) {
            return GameText.resolve(FIRST_POOL);
        }
        return asic.poolHost().isEmpty() ? asic.poolShown()
                : GameText.resolve(POOL_ON.with(asic.poolShown(), asic.poolHost()));
    }

    /* What the header says it does: mining, or why not. */
    private static TextKey stateWord(final AsicBlockEntity asic) {
        if (!asic.powered()) {
            return NO_POWER;
        }
        if (asic.mining()) {
            return MINING;
        }
        return switch (asic.status()) {
            case NO_ADDRESS -> NO_ADDRESS;
            case NO_POOL -> NO_POOL;
            case WAITING_FOR_OWNER -> WAITING;
            default -> IDLE;
        };
    }

    /* How long it has mined, in the game's time: minutes, hours, then days. */
    private static String uptime(final long ticks) {
        if (ticks >= TICKS_A_DAY) {
            return GameText.resolve(DAYS.with(ticks / TICKS_A_DAY));
        }
        if (ticks >= TICKS_AN_HOUR) {
            return GameText.resolve(HOURS.with(ticks / TICKS_AN_HOUR));
        }
        return GameText.resolve(MINUTES.with(ticks * MINUTES_AN_HOUR / TICKS_AN_HOUR));
    }
}
