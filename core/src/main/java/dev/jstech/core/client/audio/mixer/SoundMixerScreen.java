/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.mixer;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.jstech.core.audio.SoundMixerTexts;
import dev.jstech.core.client.audio.AudioPrefsStore;
import dev.jstech.core.gui.layout.SoundMixerLayout;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.Nullable;

/**
 * The Sound Mixer: the screen, opened from the game's own Music and Sound Options, where a player sets each channel's
 * volume, turns any sound of the game off, and chooses how the series' sounds behave. It is built the way the game
 * builds its tabbed screens, with the game's own widgets, so it sits among the game's options as one of them.
 */
public final class SoundMixerScreen extends Screen {

    private final Screen parent;
    private final HeaderAndFooterLayout layout =
            new HeaderAndFooterLayout(this, SoundMixerLayout.TAB_BAR_HEIGHT, SoundMixerLayout.FOOTER);
    private final TabManager tabs = new TabManager(this::addRenderableWidget, this::removeWidget);
    private @Nullable TabNavigationBar bar;
    private int selected;

    public SoundMixerScreen(final Screen parent) {
        super(GameText.component(SoundMixerTexts.TITLE));
        this.parent = parent;
    }

    /** The tab showing now. */
    @Nullable
    public Tab currentTab() {
        return tabs.getCurrentTab();
    }

    /** Shows the tab at that place: 0 channels, 1 sounds, 2 options. */
    public void selectTab(final int index) {
        selected = index;
        if (bar != null) {
            bar.selectTab(index, false);
        }
    }

    @Override
    protected void init() {
        final Tab[] all = {new ChannelsTab(font), new SoundsTab(minecraft, font), new AudioOptionsTab(this, font)};
        bar = TabNavigationBar.builder(tabs, width).addTabs(all).build();
        addRenderableWidget(bar);
        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .width(SoundMixerLayout.DONE_WIDTH).build());
        layout.visitWidgets(this::addRenderableWidget);
        bar.selectTab(selected, false);
        repositionElements();
    }

    @Override
    public void repositionElements() {
        if (bar == null) {
            return;
        }
        bar.setWidth(width);
        bar.arrangeElements();
        final int top = bar.getRectangle().bottom();
        tabs.setTabArea(new ScreenRectangle(0, top, width, height - layout.getFooterHeight() - top));
        layout.setHeaderHeight(top);
        layout.arrangeElements();
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        return bar != null && bar.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        RenderSystem.enableBlend();
        g.blit(minecraft != null && minecraft.level != null ? INWORLD_FOOTER_SEPARATOR : FOOTER_SEPARATOR, 0,
                height - layout.getFooterHeight() - 2, 0.0F, 0.0F, width, 2, 32, 2);
        RenderSystem.disableBlend();
    }

    /* Leaving keeps what was chosen in the player's file. */
    @Override
    public void onClose() {
        AudioPrefsStore.save();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    /** Remembers which tab to come back to when a screen this one opens returns to it. */
    void rememberTab(final int index) {
        selected = index;
    }
}
