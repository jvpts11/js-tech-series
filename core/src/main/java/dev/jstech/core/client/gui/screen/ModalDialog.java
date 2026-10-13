/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.screen;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Grounds;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * A modal confirm/cancel dialog ("Confirm launch?", "Really delete?").
 */
public final class ModalDialog extends Screen {

    private final Screen parent;
    private final Component message;
    private final Consumer<Boolean> onResult;

    public ModalDialog(
            final Screen parent,
            final Component title,
            final Component message,
            final Consumer<Boolean> onResult) {
        super(title);
        this.parent = parent;
        this.message = message;
        this.onResult = onResult;
    }

    @Override
    protected void init() {
        final int centerX = this.width / 2;
        final int buttonsY = this.height / 2 + 20;

        addRenderableWidget(Button.builder(
                        GameText.component(DialogTexts.CONFIRM),
                        b -> resolve(true))
                .bounds(centerX - 105, buttonsY, 100, 20)
                .build());

        addRenderableWidget(Button.builder(
                        GameText.component(DialogTexts.CANCEL),
                        b -> resolve(false))
                .bounds(centerX + 5, buttonsY, 100, 20)
                .build());
    }

    private void resolve(final boolean confirmed) {
        // The parent goes back first so a screen the callback opens is not replaced by it.
        this.minecraft.setScreen(parent);
        onResult.accept(confirmed);
    }

    @Override
    public void render(
            final GuiGraphics graphics,
            final int mouseX,
            final int mouseY,
            final float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        // Dim the parent further with a translucent overlay.
        final ScreenPalette.Colours colours = ScreenPalette.get();
        Grounds.fill(graphics, 0, 0, this.width, this.height, colours.dialogDim());
        Draw.textCentered(graphics, this.font, this.title, this.width / 2, this.height / 2 - 30,
                colours.dialogTitle());
        Draw.textCentered(graphics, this.font, this.message, this.width / 2, this.height / 2 - 10,
                colours.dialogMessage());
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        // Treat closing (Esc) as cancel.
        resolve(false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}