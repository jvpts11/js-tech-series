/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.mixer;

import dev.jstech.core.audio.AudioChannel;
import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.SoundMixerTexts;
import dev.jstech.core.client.audio.AudioMixer;
import dev.jstech.core.gui.layout.SoundMixerLayout;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

/** The Channels tab: a slider for every channel, the series' and any an addon declared, and a reset for them all. */
final class ChannelsTab implements Tab {

    private final List<ChannelSlider> sliders = new ArrayList<>();
    private final Button reset;
    private final MultiLineTextWidget note;

    ChannelsTab(final Font font) {
        for (final AudioChannel channel : AudioChannels.all()) {
            sliders.add(new ChannelSlider(channel));
        }
        reset = Button.builder(GameText.component(SoundMixerTexts.RESET_CHANNELS), button -> {
            sliders.forEach(ChannelSlider::reset);
            AudioMixer.refreshVolumes();
        }).size(SoundMixerLayout.COLUMN, SoundMixerLayout.CONTROL_HEIGHT).build();
        note = new MultiLineTextWidget(GameText.component(SoundMixerTexts.CHANNELS_NOTE), font)
                .setMaxWidth(SoundMixerLayout.FULL).setCentered(true).setColor(SoundMixerPalette.get().note());
    }

    /** The sliders, in the order the channels were declared. */
    List<ChannelSlider> sliders() {
        return List.copyOf(sliders);
    }

    Button reset() {
        return reset;
    }

    @Override
    public Component getTabTitle() {
        return GameText.component(SoundMixerTexts.TAB_CHANNELS);
    }

    @Override
    public void visitChildren(final Consumer<AbstractWidget> visitor) {
        sliders.forEach(visitor);
        visitor.accept(reset);
        visitor.accept(note);
    }

    @Override
    public void doLayout(final ScreenRectangle area) {
        final int width = area.width();
        for (int i = 0; i < sliders.size(); i++) {
            sliders.get(i).setPosition(SoundMixerLayout.channelX(width, i), SoundMixerLayout.channelY(i));
        }
        reset.setPosition(SoundMixerLayout.left(width) + (SoundMixerLayout.FULL - SoundMixerLayout.COLUMN) / 2,
                SoundMixerLayout.resetY(sliders.size()));
        note.setPosition(width / 2 - note.getWidth() / 2, SoundMixerLayout.channelsNoteY(sliders.size()));
    }
}
