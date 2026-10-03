/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.client.theme.MonitorFrameStyle;
import dev.jstech.computers.menu.RedstoneInterfaceMenu;
import dev.jstech.computers.operation.payload.RenameRedstoneInterfacePayload;
import dev.jstech.computers.program.RedstoneScript;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Grounds;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.CELLS;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.CELLS_Y;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.CELL_H;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.CELL_W;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.CODE_PITCH;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.CODE_Y;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.COMPUTER_X;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.HEADER_Y;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.HEIGHT;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.IN_X;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.MARGIN;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.MODE_H;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.MODE_LABEL_Y;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.MODE_W;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.MODE_Y;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.NAME_H;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.NAME_LABEL_Y;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.NAME_Y;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.NOTE_Y;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.NUMBER_X;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.OUT_X;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.SIGNAL_X;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.SOFTWARE_H;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.SOFTWARE_Y;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.STRENGTH_LABEL_Y;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.TILE_H;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.TILE_W;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.TILE_Y;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.TITLE_Y;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.WIDTH;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.cellAt;
import static dev.jstech.computers.gui.layout.RedstoneInterfaceLayout.cellX;

/**
 * A Redstone Interface's screen, in its housing's era: the name a program finds it by, typed straight into the field
 * and taken when the screen closes unless its computer already has an interface by that name; the computer it hangs
 * from and the signal at its lens; whether it reads or emits, chosen with the two mode buttons; the strength it emits,
 * chosen by clicking a cell while it emits; and the same setting written the software way.
 */
public final class RedstoneInterfaceScreen extends AbstractComputerScreen<RedstoneInterfaceMenu> {

    private EditBox nameBox;

    private static final long CARET_BLINK_MILLIS = 500L;

    public RedstoneInterfaceScreen(final RedstoneInterfaceMenu menu, final Inventory inventory,
                                   final Component title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    /** The plain panel, with no monitor bezel: this screen is the interface's own, not a monitor's. */
    @Override
    public MonitorFrameStyle.Geometry frameBounds() {
        return new MonitorFrameStyle.Geometry(leftPos, topPos, imageWidth, imageHeight, topPos + imageHeight);
    }

    @Override
    public boolean keyPressed(final int key, final int scan, final int mods) {
        // While a name is typed every key goes to it, so the inventory key types instead of closing the screen.
        if (nameBox.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) {
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
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            final int mx = (int) mouseX;
            final int my = (int) mouseY;
            if (hover(mx, my, IN_X, MODE_Y, MODE_W, MODE_H)) {
                sendButton(RedstoneInterfaceMenu.BUTTON_IN);
                return true;
            }
            if (hover(mx, my, OUT_X, MODE_Y, MODE_W, MODE_H)) {
                sendButton(RedstoneInterfaceMenu.BUTTON_OUT);
                return true;
            }
            final int strength = cellAt(mx - leftPos, my - topPos);
            if (strength >= 0 && settable()) {
                sendButton(RedstoneInterfaceMenu.BUTTON_STRENGTH + strength);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Where the IN button's middle is on the screen, for a test to click. */
    public int[] inCenter() {
        return new int[] {leftPos + IN_X + MODE_W / 2, topPos + MODE_Y + MODE_H / 2};
    }

    /** Where the OUT button's middle is on the screen, for a test to click. */
    public int[] outCenter() {
        return new int[] {leftPos + OUT_X + MODE_W / 2, topPos + MODE_Y + MODE_H / 2};
    }

    /** Where the middle of the cell of {@code strength} is on the screen, for a test to click. */
    public int[] cellCenter(final int strength) {
        return new int[] {leftPos + cellX(strength) + CELL_W / 2, topPos + CELLS_Y + CELL_H / 2};
    }

    /** Where the name field's middle is on the screen, for a test to click. */
    public int[] nameCenter() {
        return new int[] {leftPos + WIDTH / 2, topPos + NAME_Y + NAME_H / 2};
    }

    /** The two lines its Software box shows, the IQL statement first. */
    public String[] softwareLines() {
        final RedstoneInterfaceBlockEntity sensor = menu.sensor();
        final String name = shownName();
        return new String[] {RedstoneScript.iql(name, sensor.emits(), sensor.strength()),
                RedstoneScript.sigma(name, sensor.emits(), sensor.strength())};
    }

    /** The interface's era, which this screen wears as its skin. */
    @Override
    protected HardwareEra screenEra() {
        return menu.opening().era();
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = -1000;
        this.inventoryLabelY = -1000;
        /*
         * The box takes the keyboard and the click that focuses it, but is never drawn: vanilla draws its text with a
         * dark copy of the letters as the shadow, a smear on a light era's field. The field is drawn in renderLabels.
         */
        nameBox = new EditBox(font, leftPos + MARGIN + 2, topPos + NAME_Y, WIDTH - 2 * MARGIN - 4, NAME_H,
                GameText.component(RedstoneInterfaceTexts.NAME_FIELD));
        nameBox.setBordered(false);
        nameBox.setMaxLength(RedstoneInterfaceBlockEntity.MAX_NAME);
        nameBox.setValue(menu.opening().name());
        nameBox.setResponder(name ->
                PacketDistributor.sendToServer(new RenameRedstoneInterfacePayload(menu.sensorPos(), name)));
        addWidget(nameBox);
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        final RedstoneInterfaceBlockEntity sensor = menu.sensor();
        JsTechTheme.window(g, x, y, WIDTH, HEIGHT);
        JsTechTheme.headerBar(g, x, y + HEADER_Y, WIDTH);
        Grounds.fill(g, x + MARGIN, y + NAME_Y, x + WIDTH - MARGIN, y + NAME_Y + NAME_H, JsTechTheme.slotBg());
        g.fill(x + MARGIN, y + NAME_Y + NAME_H - 1, x + WIDTH - MARGIN, y + NAME_Y + NAME_H,
                menu.nameClashes() ? JsTechTheme.red() : JsTechTheme.line());
        JsTechTheme.panel(g, x + COMPUTER_X, y + TILE_Y, TILE_W, TILE_H);
        JsTechTheme.panel(g, x + SIGNAL_X, y + TILE_Y, TILE_W, TILE_H);
        mode(g, x + IN_X, y + MODE_Y, !sensor.emits(), JsTechTheme.green(), mouseX, mouseY);
        mode(g, x + OUT_X, y + MODE_Y, sensor.emits(), JsTechTheme.amber(), mouseX, mouseY);
        final boolean live = sensor.live();
        final int shown = sensor.shownStrength();
        for (int strength = 0; strength < CELLS; strength++) {
            cell(g, x + cellX(strength), y + CELLS_Y, strength, live && strength > 0 && strength <= shown, live);
        }
        JsTechTheme.panel(g, x + MARGIN, y + SOFTWARE_Y, WIDTH - 2 * MARGIN, SOFTWARE_H);
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        final RedstoneInterfaceBlockEntity sensor = menu.sensor();
        final boolean live = sensor.live();
        final boolean glass = JsTechTheme.active().style().glassBands();
        JsTechTheme.text(g, font, GameText.resolve(RedstoneInterfaceTexts.TITLE), MARGIN, TITLE_Y,
                JsTechTheme.text());
        JsTechTheme.textRight(g, font, GameText.resolve(menu.opening().era().text()).toUpperCase(Locale.ROOT),
                WIDTH - MARGIN, TITLE_Y, glass ? JsTechTheme.text() : JsTechTheme.accent());
        JsTechTheme.textS(g, font, GameText.resolve(RedstoneInterfaceTexts.NAME), MARGIN + 1, NAME_LABEL_Y,
                JsTechTheme.dim());
        renderName(g);
        final boolean clash = menu.nameClashes();
        JsTechTheme.textS(g, font, fit(GameText.resolve(clash ? RedstoneInterfaceTexts.CLASH
                : RedstoneInterfaceTexts.HINT), WIDTH - 2 * MARGIN), MARGIN, NOTE_Y,
                clash ? JsTechTheme.red() : JsTechTheme.dim());

        JsTechTheme.tileTextS(g, font, COMPUTER_X, TILE_Y, GameText.resolve(RedstoneInterfaceTexts.COMPUTER),
                fit(live ? computerName() : GameText.resolve(RedstoneInterfaceTexts.NO_COMPUTER), TILE_W - 6),
                live ? JsTechTheme.accent() : JsTechTheme.red());
        final String signal;
        final int signalColor;
        if (!live) {
            signal = GameText.resolve(RedstoneInterfaceTexts.NO_POWER);
            signalColor = JsTechTheme.dim();
        } else if (sensor.emits()) {
            signal = GameText.resolve(RedstoneInterfaceTexts.EMITS.with(sensor.strength()));
            signalColor = JsTechTheme.amber();
        } else {
            signal = GameText.resolve(RedstoneInterfaceTexts.READS.with(sensor.reading()));
            signalColor = JsTechTheme.green();
        }
        JsTechTheme.tileTextS(g, font, SIGNAL_X, TILE_Y, GameText.resolve(RedstoneInterfaceTexts.SIGNAL),
                fit(signal, TILE_W - 6), signalColor);

        JsTechTheme.textS(g, font, GameText.resolve(RedstoneInterfaceTexts.MODE), MARGIN + 1, MODE_LABEL_Y,
                JsTechTheme.dim());
        if (!sensor.setBy().isEmpty()) {
            JsTechTheme.textS(g, font, fit(GameText.resolve(RedstoneInterfaceTexts.SET_BY.with(sensor.setBy())),
                    MODE_W), OUT_X, MODE_LABEL_Y, JsTechTheme.amber());
        }
        modeLabel(g, IN_X, RedstoneInterfaceTexts.IN, RedstoneInterfaceTexts.IN_DOES, !sensor.emits(),
                JsTechTheme.green(), live);
        modeLabel(g, OUT_X, RedstoneInterfaceTexts.OUT, RedstoneInterfaceTexts.OUT_DOES, sensor.emits(),
                JsTechTheme.amber(), live);

        JsTechTheme.textS(g, font, GameText.resolve(RedstoneInterfaceTexts.STRENGTH), MARGIN + 1, STRENGTH_LABEL_Y,
                JsTechTheme.dim());
        JsTechTheme.text(g, font, Integer.toString(sensor.shownStrength()), NUMBER_X, CELLS_Y,
                live ? JsTechTheme.text() : JsTechTheme.dim());

        JsTechTheme.textS(g, font, GameText.resolve(RedstoneInterfaceTexts.SOFTWARE), MARGIN + 3, SOFTWARE_Y + 3,
                JsTechTheme.dim());
        final String[] lines = softwareLines();
        for (int i = 0; i < lines.length; i++) {
            JsTechTheme.textS(g, font, fit(lines[i], WIDTH - 2 * MARGIN - 6), MARGIN + 3, CODE_Y + i * CODE_PITCH,
                    JsTechTheme.accent2());
        }
    }

    /* Whether a click on a cell sets the strength: only while the interface is live and emits. */
    private boolean settable() {
        return menu.sensor().live() && menu.sensor().emits();
    }

    /* A mode button: the chosen one on the ground of a selected tab, framed in its mode's colour. */
    private void mode(final GuiGraphics g, final int x, final int y, final boolean on, final int color,
                      final int mouseX, final int mouseY) {
        if (on) {
            JsTechTheme.selectedTab(g, x, y, MODE_W, MODE_H);
            Draw.outline(g, x, y, MODE_W, MODE_H, color);
        } else {
            JsTechTheme.button(g, x, y, MODE_W, MODE_H, hover(mouseX, mouseY, x - leftPos, y - topPos, MODE_W, MODE_H));
            Draw.outline(g, x, y, MODE_W, MODE_H, JsTechTheme.slotEdge());
        }
    }

    /* A mode button's words: the keyword IQL writes it with, and what it does, in the mode's colour when chosen. */
    private void modeLabel(final GuiGraphics g, final int x, final TextKey keyword, final TextKey does,
                           final boolean on, final int color, final boolean live) {
        final int shown = on && live ? color : JsTechTheme.dim();
        JsTechTheme.textS(g, font, GameText.resolve(keyword) + "  " + GameText.resolve(does), x + 5,
                MODE_Y + (MODE_H - 7) / 2, shown);
    }

    /*
     * One cell of the strength's row: the first crossed, for 0; the others lit in redstone's red up to the strength
     * shown, hollow past it, and all of them hollow and faint while nothing powers the interface.
     */
    private static void cell(final GuiGraphics g, final int x, final int y, final int strength, final boolean lit,
                             final boolean live) {
        final int edge = live ? JsTechTheme.slotEdge() : JsTechTheme.line();
        if (lit) {
            Grounds.fill(g, x, y, x + CELL_W, y + CELL_H, JsTechTheme.red());
            return;
        }
        Grounds.fill(g, x, y, x + CELL_W, y + CELL_H, JsTechTheme.track());
        Draw.outline(g, x, y, CELL_W, CELL_H, edge);
        if (strength == 0) {
            for (int step = 0; step < CELL_W; step++) {
                final int dy = CELL_H - 1 - step * CELL_H / CELL_W;
                g.fill(x + step, y + dy, x + step + 1, y + dy + 1, edge);
            }
        }
    }

    /*
     * The name being typed, or the word for it dimmed while there is none, with a caret where the next letter goes. A
     * name wider than the field shows the part around the caret.
     */
    private void renderName(final GuiGraphics g) {
        final int room = WIDTH - 2 * MARGIN - 4;
        final int x = MARGIN + 2;
        final int y = NAME_Y + 3;
        final String value = nameBox.getValue();
        final int cursor = Math.min(nameBox.getCursorPosition(), value.length());
        final String before = Texts.tail(font, value.substring(0, cursor), room);
        String after = value.substring(cursor);
        while (!after.isEmpty() && font.width(before + after) > room) {
            after = after.substring(0, after.length() - 1);
        }
        if (value.isEmpty()) {
            Draw.text(g, font, GameText.resolve(RedstoneInterfaceBlockEntity.DEFAULT_NAME), x, y, JsTechTheme.dim(),
                    JsTechTheme.slotBg());
        } else {
            Draw.text(g, font, before + after, x, y, JsTechTheme.text(), JsTechTheme.slotBg());
        }
        if (nameBox.isFocused() && Util.getMillis() / CARET_BLINK_MILLIS % 2 == 0) {
            final int caretX = x + font.width(before);
            g.fill(caretX, y - 1, caretX + 1, y + 9, JsTechTheme.text());
        }
    }

    /* The name a program finds it by now: the one being typed, or the word for it while there is none. */
    private String shownName() {
        final String typed = nameBox == null ? menu.opening().name() : nameBox.getValue().strip();
        return typed.isEmpty() ? RedstoneInterfaceBlockEntity.DEFAULT_NAME.text().english() : typed;
    }

    /* The computer it hangs from, by the name a player gave it, or by what it is. */
    private String computerName() {
        final RedstoneInterfaceMenu.Opening opening = menu.opening();
        if (!opening.computerName().isEmpty()) {
            return opening.computerName();
        }
        if (!opening.computerKind().isEmpty()) {
            return Component.translatable(opening.computerKind()).getString();
        }
        final BlockPos owner = menu.sensor().ownerPos();
        return owner == null || minecraft == null || minecraft.level == null ? ""
                : minecraft.level.getBlockState(owner).getBlock().getName().getString();
    }

    /* As much of a small line as fits in {@code pixels}. */
    private String fit(final String text, final int pixels) {
        return Texts.clip(font, text, (int) (pixels / JsTechTheme.small()));
    }
}
