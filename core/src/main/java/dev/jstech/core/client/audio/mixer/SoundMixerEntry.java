/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.mixer;

import dev.jstech.core.JsCore;
import dev.jstech.core.audio.SoundMixerTexts;
import dev.jstech.core.gui.layout.SoundMixerLayout;
import dev.jstech.core.text.GameText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Where the Sound Mixer is reached: the footer of the game's own Music and Sound Options, which gains Sound Mixer...
 * beside Done, the two 150 wide the way the game lays out a footer of two. The game's own Done is hidden and a Done
 * that does the same stands beside the new button, both in the screen's own layout, so they keep their places when
 * the window is resized.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class SoundMixerEntry {

    /** Between the two buttons of the footer, as the game spaces them. */
    private static final int SPACING = 8;

    private SoundMixerEntry() {
    }

    @SubscribeEvent
    public static void onInit(final ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof SoundOptionsScreen options)) {
            return;
        }
        for (final GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof Button done && CommonComponents.GUI_DONE.equals(done.getMessage())) {
                done.visible = false;
                done.active = false;
            }
        }
        final LinearLayout footer = LinearLayout.horizontal().spacing(SPACING);
        footer.addChild(Button.builder(GameText.component(SoundMixerTexts.OPEN),
                button -> Minecraft.getInstance().setScreen(new SoundMixerScreen(options)))
                .width(SoundMixerLayout.COLUMN).build());
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> options.onClose())
                .width(SoundMixerLayout.COLUMN).build());
        options.layout.addToFooter(footer);
        footer.visitWidgets(event::addListener);
        options.layout.arrangeElements();
    }
}
