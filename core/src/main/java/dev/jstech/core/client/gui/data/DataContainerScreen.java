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
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

/**
 * The screen of a menu declared in a file: the window and its components in the style the player picked, the menu's
 * slots moved under the frames the file placed, and nothing else drawn by hand. Made by {@link DataScreens#register}.
 *
 * @param <M> the menu
 */
public class DataContainerScreen<M extends AbstractContainerMenu> extends CoreContainerScreen<M>
        implements IDataScreen {

    private final ResourceLocation file;
    private final DataScreenView view;
    @Nullable
    private KeyboardFocus keyboard;

    public DataContainerScreen(final M menu, final Inventory inventory, final Component title,
                               final ResourceLocation file, final IScreenLogic<M> logic) {
        super(menu, inventory, title);
        this.file = Objects.requireNonNull(file, "file");
        final ScreenWiring<M> wiring = new ScreenWiring<>(menu);
        logic.wire(wiring);
        this.view = new DataScreenView(file, wiring, menu, this::getTitle);
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
            // A component came or went: the window takes its new height in the middle, its slots moved with it.
            imageHeight = view.height();
            topPos = (height - imageHeight) / 2;
            view.layout(leftPos, topPos, height);
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (menu.getCarried().isEmpty() && hoveredSlot == null) {
            final List<Component> tip = view.tooltipAt(mouseX, mouseY);
            if (!tip.isEmpty()) {
                g.renderComponentTooltip(font, tip, mouseX, mouseY);
            }
        }
    }

    @Override
    public void removed() {
        view.dispose();
        super.removed();
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

    /*
     * While a field has the keyboard every key but Escape and Tab is its own, so typing an E does not close the
     * screen; otherwise Tab and the arrows go on from the component a click gave the keyboard.
     */
    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (DataKeys.toView(view, key)) {
            return view.keyPressed(key, scanCode, modifiers) || key != InputConstants.KEY_ESCAPE;
        }
        setFocused(keyboard == null ? null : keyboard.focused());
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c, final int modifiers) {
        return view.charTyped(c) || super.charTyped(c, modifiers);
    }

    @Override
    protected void init() {
        view.build(font);
        imageWidth = view.width();
        imageHeight = view.height();
        super.init();
        view.layout(leftPos, topPos, height);
        keyboard = view.keyboard();
        keyboard.entries().forEach(this::addWidget);
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        view.render(g, leftPos, topPos, mouseX, mouseY, partialTick);
    }

    /* The title and the inventory's name are components of the file, drawn with the rest. */
    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
    }
}
