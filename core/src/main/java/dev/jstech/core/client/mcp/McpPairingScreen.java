/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.developer.mcp.McpOnServer;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.gui.layout.McpPairLayout;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

/**
 * The question a client of the MCP server is let in by, over whatever was open, in the game's confirm look: who asks
 * and from where, what reading and acting mean, and three answers. With no answer in a minute it refuses by itself;
 * closed any other way it refuses too, and the screen it opened over comes back.
 */
public final class McpPairingScreen extends Screen {

    private final Panel panel = new Panel();
    private final Button read;
    private final Button act;
    private final Button refuse;
    private final String name;
    private final String version;
    private final boolean onServer;
    private final @Nullable Screen before;
    private final Consumer<Optional<McpPermission>> answered;
    private McpPairLayout.Regions regions = McpPairLayout.of(McpPairLayout.MIN_WIDTH, McpPairLayout.MIN_HEIGHT, 1,
            1, 1);
    private List<FormattedCharSequence> who = List.of();
    private List<FormattedCharSequence> reading = List.of();
    private List<FormattedCharSequence> acting = List.of();
    private int label = McpPairLayout.LABEL;
    private int ticks;
    private boolean done;

    /** How long the player is given, in ticks. */
    private static final int GIVEN = McpOnServer.ANSWER_SECONDS * 20;

    public McpPairingScreen(final String name, final String version, final boolean onServer,
                            @Nullable final Screen before, final Consumer<Optional<McpPermission>> answered) {
        super(GameText.component(McpTexts.PAIR_TITLE));
        this.name = name;
        this.version = version;
        this.onServer = onServer;
        this.before = before;
        this.answered = answered;
        this.read = this.panel.add(new Button(() -> GameText.resolve(McpTexts.LET_READ),
                () -> this.answer(Optional.of(McpPermission.READ))));
        this.act = this.panel.add(new Button(() -> GameText.resolve(McpTexts.LET_ACT),
                () -> this.answer(Optional.of(McpPermission.ACT))));
        this.refuse = this.panel.add(new Button(() -> GameText.resolve(McpTexts.REFUSE),
                () -> this.answer(Optional.empty())));
    }

    /** Answers as a click on {@code permission}'s button does, or on Refuse for none; for a test as for a click. */
    public void answer(final Optional<McpPermission> permission) {
        if (this.done) {
            return;
        }
        this.done = true;
        this.answered.accept(permission);
        if (this.minecraft != null && this.minecraft.screen == this) {
            this.minecraft.setScreen(this.before);
        }
    }

    /** Who asks, as the question names them. */
    public String asker() {
        return this.name + " " + this.version;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final McpPalette.Colours colours = McpPalette.get();
        final int middle = this.regions.width() / 2;
        Draw.textCentered(g, font, GameText.resolve(McpTexts.PAIR_TITLE), middle, this.regions.titleY(),
                colours.title());
        int y = this.regions.whoY();
        for (final FormattedCharSequence line : this.who) {
            Draw.text(g, font, line, middle - font.width(line) / 2, y, colours.text());
            y += McpPairLayout.LINE;
        }
        this.row(g, GameText.resolve(McpTexts.TO_READ), this.reading, this.regions.readY(), colours);
        this.row(g, GameText.resolve(McpTexts.TO_ACT), this.acting, this.regions.actY(), colours);
        Draw.textCentered(g, font, GameText.resolve(McpTexts.NOTHING_ELSE), middle, this.regions.noteY(),
                colours.dim());
        final int left = Math.max(0, GIVEN - this.ticks) / 20;
        Draw.textCentered(g, font, GameText.resolve(McpTexts.TIMEOUT.with(left)), middle, this.regions.timeoutY(),
                colours.dim());
        this.panel.render(g, new UiContext(Styles.skin(Styles.game()), font, mouseX, mouseY, partialTick));
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (this.panel.mouseClicked(mouseX, mouseY, button)) {
            this.panel.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        if (++this.ticks >= GIVEN) {
            this.answer(Optional.empty());
        }
    }

    @Override
    public void onClose() {
        this.answer(Optional.empty());
    }

    @Override
    public void removed() {
        // Put away by anything else, the question is refused rather than left without an answer.
        if (!this.done) {
            this.done = true;
            this.answered.accept(Optional.empty());
        }
        this.panel.dispose();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        final int wrap = Math.min(McpPairLayout.MOST_TEXT, Math.max(McpPairLayout.MIN_WIDTH, width)
                - 2 * McpPairLayout.SIDE);
        this.who = font.split(GameText.component((this.onServer ? McpTexts.PAIR_WHO_SERVER : McpTexts.PAIR_WHO)
                .with(this.name, this.version)), wrap);
        // The explanations stand clear of the longer of their two names, in whatever language.
        this.label = Math.max(McpPairLayout.LABEL, Math.max(font.width(GameText.resolve(McpTexts.TO_READ)),
                font.width(GameText.resolve(McpTexts.TO_ACT))) + 8);
        this.reading = font.split(GameText.component(McpTexts.READ_WHAT), wrap - this.label);
        this.acting = font.split(GameText.component(McpTexts.ACT_WHAT), wrap - this.label);
        this.regions = McpPairLayout.of(width, height, this.who.size(), this.reading.size(), this.acting.size());
        final List<StudioLayout.Rect> buttons = this.regions.buttons();
        place(this.read, buttons.get(0));
        place(this.act, buttons.get(1));
        place(this.refuse, buttons.get(2));
    }

    private void row(final GuiGraphics g, final String label, final List<FormattedCharSequence> lines, final int top,
                     final McpPalette.Colours colours) {
        Draw.text(g, font, label, this.regions.textX(), top, colours.title());
        int y = top;
        for (final FormattedCharSequence line : lines) {
            Draw.text(g, font, line, this.regions.textX() + this.label, y, colours.text());
            y += McpPairLayout.LINE;
        }
    }

    private static void place(final Button button, final StudioLayout.Rect rect) {
        button.setBounds(rect.x(), rect.y(), rect.width(), rect.height());
    }
}
