/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.data;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.client.gui.component.KeyboardFocus;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A screen with no menu declared in a file, centred over the game in the style the player picked: an instrument's, a
 * chooser's, a block's settings. Opened with {@link DataScreens#open}.
 */
public class DataScreen extends Screen implements IDataScreen {

    private final ResourceLocation file;
    private final DataScreenView view;
    @Nullable
    private final Screen parent;
    private int left;
    private int top;
    @Nullable
    private KeyboardFocus keyboard;

    public DataScreen(final ResourceLocation file, final Component title, final IScreenLogic<Void> logic,
                      @Nullable final Screen parent) {
        super(title);
        this.file = Objects.requireNonNull(file, "file");
        this.parent = parent;
        final ScreenWiring<Void> wiring = new ScreenWiring<>(null);
        logic.wire(wiring);
        this.view = new DataScreenView(file, wiring, null, this::getTitle);
    }

    @Override
    public ResourceLocation file() {
        return file;
    }

    @Override
    public DataScreenView view() {
        return view;
    }

    @Override
    public void rebuild() {
        if (minecraft != null) {
            init(minecraft, width, height);
        }
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        if (view.reshape()) {
            centre();
        }
        super.render(g, mouseX, mouseY, partialTick);
        view.render(g, left, top, mouseX, mouseY, partialTick);
        final List<Component> tip = view.tooltipAt(mouseX, mouseY);
        if (!tip.isEmpty()) {
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        return view.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        return view.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                final double dragY) {
        return view.mouseDragged(mouseX, mouseY, button) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        return view.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (DataKeys.toView(view, key)) {
            return view.keyPressed(key, scanCode, modifiers) || key != InputConstants.KEY_ESCAPE;
        }
        // From the component a click gave the keyboard, Tab and the arrows go on to the next.
        setFocused(keyboard == null ? null : keyboard.focused());
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c, final int modifiers) {
        return view.charTyped(c) || super.charTyped(c, modifiers);
    }

    @Override
    public void removed() {
        view.dispose();
        super.removed();
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        view.build(font);
        centre();
        keyboard = view.keyboard();
        keyboard.entries().forEach(this::addWidget);
    }

    /* The window in the middle of the game's screen, as tall as what it shows needs, laid out there. */
    private void centre() {
        left = (width - view.width()) / 2;
        top = Math.max(0, (height - view.height()) / 2);
        view.layout(left, top, height);
    }
}
