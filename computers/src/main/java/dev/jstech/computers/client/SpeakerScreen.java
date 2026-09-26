/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.computers.menu.SpeakerMenu;
import dev.jstech.computers.operation.payload.RenameSpeakerPayload;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.theme.EraTheme;
import dev.jstech.core.client.gui.theme.EraThemes;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import static dev.jstech.computers.gui.layout.SpeakerLayout.CHANNEL_X;
import static dev.jstech.computers.gui.layout.SpeakerLayout.COMPUTER_X;
import static dev.jstech.computers.gui.layout.SpeakerLayout.HEIGHT;
import static dev.jstech.computers.gui.layout.SpeakerLayout.NAME_H;
import static dev.jstech.computers.gui.layout.SpeakerLayout.NAME_LABEL_Y;
import static dev.jstech.computers.gui.layout.SpeakerLayout.NAME_X;
import static dev.jstech.computers.gui.layout.SpeakerLayout.NAME_Y;
import static dev.jstech.computers.gui.layout.SpeakerLayout.NOTE_Y;
import static dev.jstech.computers.gui.layout.SpeakerLayout.PLAYS_W;
import static dev.jstech.computers.gui.layout.SpeakerLayout.PLAYS_Y;
import static dev.jstech.computers.gui.layout.SpeakerLayout.TILE_H;
import static dev.jstech.computers.gui.layout.SpeakerLayout.TILE_W;
import static dev.jstech.computers.gui.layout.SpeakerLayout.TILE_Y;
import static dev.jstech.computers.gui.layout.SpeakerLayout.WIDTH;

/**
 * A speaker's screen, in its model's era: the name a program finds it by, typed straight into the field and taken when
 * the screen closes, unless its computer already has a speaker by that name, which the screen says while it is typed;
 * then the computer it plays for, the side it plays, and how well.
 */
public final class SpeakerScreen extends AbstractContainerScreen<SpeakerMenu> {

    private EraTheme theme = EraThemes.STANDARD;
    private EditBox nameBox;

    private static final long CARET_BLINK_MILLIS = 500L;

    public SpeakerScreen(final SpeakerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        JsTechTheme.bind(theme);
        try {
            super.render(g, mouseX, mouseY, partialTick);
            renderTooltip(g, mouseX, mouseY);
        } finally {
            JsTechTheme.unbind();
        }
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = -1000;
        this.inventoryLabelY = -1000;
        theme = EraThemes.of(menu.opening().era());
        /*
         * The box takes the keyboard and the click that focuses it, but is never drawn: vanilla draws its text with
         * a dark copy of the letters as the shadow, a smear on a light era's field. The field is drawn in
         * renderLabels instead.
         */
        nameBox = new EditBox(font, leftPos + NAME_X + 2, topPos + NAME_Y, WIDTH - 2 * NAME_X - 4, NAME_H,
                GameText.component(SpeakerTexts.NAME_FIELD));
        // Unbordered, its text starts where the field draws it, so a click lands the caret on the letter clicked.
        nameBox.setBordered(false);
        nameBox.setMaxLength(SpeakerBlockEntity.MAX_NAME);
        nameBox.setValue(menu.opening().name());
        nameBox.setResponder(name ->
                PacketDistributor.sendToServer(new RenameSpeakerPayload(menu.speakerPos(), name)));
        addWidget(nameBox);
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
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        JsTechTheme.window(g, x, y, WIDTH, HEIGHT);
        JsTechTheme.headerBar(g, x + 6, y + 6, WIDTH - 12);
        final int edge = menu.nameClashes() ? JsTechTheme.red() : JsTechTheme.line();
        g.fill(x + NAME_X, y + NAME_Y, x + WIDTH - NAME_X, y + NAME_Y + NAME_H, JsTechTheme.slotBg());
        g.fill(x + NAME_X, y + NAME_Y, x + WIDTH - NAME_X, y + NAME_Y + 1, edge);
        if (menu.nameClashes()) {
            g.fill(x + NAME_X, y + NAME_Y + NAME_H - 1, x + WIDTH - NAME_X, y + NAME_Y + NAME_H, edge);
        }
        JsTechTheme.panel(g, x + COMPUTER_X, y + TILE_Y, TILE_W, TILE_H);
        JsTechTheme.panel(g, x + CHANNEL_X, y + TILE_Y, TILE_W, TILE_H);
        JsTechTheme.panel(g, x + NAME_X, y + PLAYS_Y, PLAYS_W, TILE_H);
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        final boolean legacy = menu.opening().era() == HardwareEra.LEGACY;
        JsTechTheme.text(g, font, GameText.resolve(SpeakerTexts.TITLE), 12, 10, JsTechTheme.text());
        JsTechTheme.textRight(g, font, GameText.resolve(legacy ? SpeakerTexts.MODEL_LEGACY
                : SpeakerTexts.MODEL_STANDARD), WIDTH - 12, 10, JsTechTheme.accent());
        JsTechTheme.textS(g, font, GameText.resolve(SpeakerTexts.NAME), 10, NAME_LABEL_Y, JsTechTheme.dim());
        renderName(g);

        final boolean linked = !menu.opening().computerKind().isEmpty();
        final String computer = linked ? computerName() : GameText.resolve(SpeakerTexts.NOT_LINKED);
        final boolean clash = menu.nameClashes();
        JsTechTheme.textS(g, font, fit(GameText.resolve(clash ? SpeakerTexts.CLASH : SpeakerTexts.HINT),
                WIDTH - 20), 10, NOTE_Y, clash ? JsTechTheme.red() : JsTechTheme.dim());
        JsTechTheme.tileTextS(g, font, COMPUTER_X, TILE_Y, GameText.resolve(SpeakerTexts.COMPUTER),
                fit(computer, TILE_W - 6), linked ? JsTechTheme.accent2() : JsTechTheme.dim());
        final int channel = menu.channel();
        JsTechTheme.tileTextS(g, font, CHANNEL_X, TILE_Y, GameText.resolve(SpeakerTexts.CHANNEL),
                fit(GameText.resolve(channelText(channel)), TILE_W - 6),
                channel == SpeakerBlockEntity.CHANNEL_NONE ? JsTechTheme.dim() : JsTechTheme.green());
        JsTechTheme.tileTextS(g, font, NAME_X, PLAYS_Y, GameText.resolve(SpeakerTexts.PLAYS),
                fit(GameText.resolve(legacy ? SpeakerTexts.PLAYS_LEGACY : SpeakerTexts.PLAYS_WHOLE), PLAYS_W - 6),
                JsTechTheme.text());
    }

    /*
     * The name being typed, or the default name dimmed while there is none, with a caret where the next letter goes.
     * A name wider than the field shows the part around the caret.
     */
    private void renderName(final GuiGraphics g) {
        final int room = WIDTH - 2 * NAME_X - 4;
        final int x = NAME_X + 2;
        final int y = NAME_Y + 3;
        final String value = nameBox.getValue();
        final int cursor = Math.min(nameBox.getCursorPosition(), value.length());
        final String before = Texts.tail(font, value.substring(0, cursor), room);
        String after = value.substring(cursor);
        while (!after.isEmpty() && font.width(before + after) > room) {
            after = after.substring(0, after.length() - 1);
        }
        if (value.isEmpty()) {
            Draw.text(g, font, GameText.resolve(SpeakerTexts.DEFAULT_NAME), x, y, JsTechTheme.dim(),
                    JsTechTheme.slotBg());
        } else {
            Draw.text(g, font, before + after, x, y, JsTechTheme.text(), JsTechTheme.slotBg());
        }
        if (nameBox.isFocused() && Util.getMillis() / CARET_BLINK_MILLIS % 2 == 0) {
            final int caretX = x + font.width(before);
            g.fill(caretX, y - 1, caretX + 1, y + 9, JsTechTheme.text());
        }
    }

    /* As much of a small line as fits in {@code pixels}: a computer's own name can be as long as a speaker's. */
    private String fit(final String text, final int pixels) {
        return Texts.clip(font, text, (int) (pixels / JsTechTheme.small()));
    }

    /* The computer it plays for, by the name a player gave it, or by what it is. */
    private String computerName() {
        final SpeakerMenu.Opening opening = menu.opening();
        return opening.computerName().isEmpty()
                ? Component.translatable(opening.computerKind()).getString() : opening.computerName();
    }

    private static TextKey channelText(final int channel) {
        return switch (channel) {
            case SpeakerBlockEntity.CHANNEL_ALONE -> SpeakerTexts.CHANNEL_ALONE;
            case SpeakerBlockEntity.CHANNEL_BOTH -> SpeakerTexts.CHANNEL_BOTH;
            case SpeakerBlockEntity.CHANNEL_LEFT -> SpeakerTexts.CHANNEL_LEFT;
            case SpeakerBlockEntity.CHANNEL_RIGHT -> SpeakerTexts.CHANNEL_RIGHT;
            default -> SpeakerTexts.CHANNEL_NONE;
        };
    }
}
