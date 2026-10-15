/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.TextArea;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.config.ConfigScreenTexts;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.text.GameText;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A file's settings as text: written out to copy and share (Export), or pasted in to take (Import). The text is the
 * file's own format, so what a person shares reads like the file; an import that cannot be read says why and is not
 * taken.
 */
public final class ConfigTextPopup extends Popup {

    private final TextArea text;
    private final Button first;
    private final Button second;
    private final Button close;
    private String message = "";
    private boolean failed;

    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;
    private static final int PAD = 8;
    private static final int TEXT_Y = 16;

    private ConfigTextPopup(final String title) {
        super(title, WIDTH, HEIGHT);
        this.text = add(new TextArea());
        this.first = add(new Button("", () -> { }).setFocusable(true));
        this.second = add(new Button("", () -> { }).setFocusable(true));
        this.close = add(new Button(GameText.resolve(ConfigScreenTexts.CANCEL), this::close).setFocusable(true));
        setLayouter(popup -> place());
    }

    /** The settings written as {@code exported}, to copy. */
    static ConfigTextPopup export(final String exported) {
        final ConfigTextPopup popup = new ConfigTextPopup(GameText.resolve(ConfigScreenTexts.EXPORT_TITLE));
        popup.text.setText(exported);
        popup.first.setLabel(GameText.resolve(ConfigScreenTexts.COPY)).setOnPress(() -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(popup.text.text());
            popup.say(GameText.resolve(ConfigScreenTexts.COPIED), false);
        });
        popup.second.setVisible(false);
        return popup;
    }

    /**
     * An empty page to paste settings into; {@code take} reads what was pasted and says what came of it, or throws
     * with why it could not be read.
     */
    static ConfigTextPopup importing(final Function<String, String> take) {
        final ConfigTextPopup popup = new ConfigTextPopup(GameText.resolve(ConfigScreenTexts.IMPORT_TITLE));
        popup.first.setLabel(GameText.resolve(ConfigScreenTexts.PASTE)).setOnPress(() ->
                popup.text.setText(Minecraft.getInstance().keyboardHandler.getClipboard()));
        popup.second.setLabel(GameText.resolve(ConfigScreenTexts.TAKE)).setOnPress(() -> {
            try {
                popup.say(take.apply(popup.text.text()), false);
                popup.close();
            } catch (final IllegalArgumentException unreadable) {
                popup.say(GameText.resolve(ConfigScreenTexts.IMPORT_FAILED.with(String.valueOf(
                        unreadable.getMessage()))), true);
            }
        });
        popup.focus(popup.text);
        return popup;
    }

    /** The text on the page; for a test to read. */
    public String text() {
        return text.text();
    }

    /** Puts {@code value} on the page, as pasting it does. */
    public void setText(final String value) {
        text.setText(value);
    }

    /** Presses the second button, Take them, as a click does. */
    public void take() {
        second.press();
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        super.render(g, ctx);
        if (!message.isEmpty()) {
            ctx.skin().drawText(g, ctx.font(), message, x() + PAD, bottom() - PAD - 20 - 11,
                    failed ? ConfigWidgets.colour(ctx.skin(), StyleRole.WARNING) : ctx.skin().dim());
        }
    }

    private void say(final String said, final boolean wrong) {
        message = said;
        failed = wrong;
    }

    private void place() {
        final int left = x() + PAD;
        final int inner = width() - 2 * PAD;
        final int buttonsY = bottom() - PAD - 20;
        text.setBounds(left, y() + TEXT_Y, inner, buttonsY - 14 - y() - TEXT_Y);
        final int w = 70;
        first.setBounds(left, buttonsY, w, 20);
        second.setBounds(left + w + 4, buttonsY, w, 20);
        close.setBounds(right() - PAD - w, buttonsY, w, 20);
    }
}
