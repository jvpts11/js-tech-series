/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.life;

import dev.jstech.core.text.GameText;
import dev.jstech.space.life.SuitControls;
import dev.jstech.space.network.SuitLightPayload;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The suit's controls looked at up close, full size and live, as the hazard devices are held up: the light's button
 * pressed with the mouse lights the face, kept on the suit. The game goes on behind it, dimmed, with the HUD and the
 * hand put away while the controls are looked at.
 */
public final class SuitControlsScreen extends Screen {

    private int left;
    private int top;
    /* Whether the player had the HUD hidden already, to leave it so when the controls are put down. */
    private boolean hudHidden;

    /** Up close, the controls are drawn at twice their HUD size. */
    public static final int UNIT = 2;
    /* How far right of the button its moulded word reaches, so the word is pressed with it. */
    private static final int WORD = 26;

    public SuitControlsScreen() {
        super(GameText.component(SuitControls.KEY));
    }

    /** Where the controls' case starts on the screen, its left then its top. */
    public int left() {
        return this.left;
    }

    public int top() {
        return this.top;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void added() {
        // Held up, the controls are all the player looks at: the HUD and the hand go until they are put down.
        final Options options = Minecraft.getInstance().options;
        this.hudHidden = options.hideGui;
        options.hideGui = true;
    }

    @Override
    public void removed() {
        Minecraft.getInstance().options.hideGui = this.hudHidden;
    }

    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        // The world dimmed by the game's gradient, as behind an open container, and not blurred.
        this.renderTransparentBackground(g);
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        reading().ifPresent(reading -> SuitControlsArt.draw(g, this.font, this.left, this.top, UNIT, reading));
    }

    @Override
    public void tick() {
        if (reading().isEmpty()) {
            this.onClose();
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final Optional<SuitControlsHud.Reading> reading = reading();
        if (button == 0 && reading.isPresent() && this.minecraft != null && this.minecraft.player != null) {
            final int bx = this.left + SuitControlsArt.LIGHT_X * UNIT;
            final int by = this.top + SuitControlsArt.LIGHT_Y * UNIT;
            if (mouseX >= bx && mouseX < bx + WORD * UNIT && mouseY >= by - UNIT
                    && mouseY < by + (SuitControlsArt.BUTTON_HEIGHT + 2) * UNIT) {
                final boolean on = !reading.get().light();
                SuitControls.light(this.minecraft.player, on);
                PacketDistributor.sendToServer(new SuitLightPayload(on));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void init() {
        this.left = (this.width - SuitControlsArt.WIDTH * UNIT) / 2;
        this.top = (this.height - SuitControlsArt.HEIGHT * UNIT) / 2;
    }

    /* What the controls read now; none once the suit's chest piece is off. */
    private Optional<SuitControlsHud.Reading> reading() {
        return this.minecraft == null || this.minecraft.player == null ? Optional.empty()
                : SuitControlsHud.reading(this.minecraft.player);
    }
}
