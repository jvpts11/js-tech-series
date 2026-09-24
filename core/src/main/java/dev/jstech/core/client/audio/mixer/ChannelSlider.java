/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.mixer;

import dev.jstech.core.audio.AudioChannel;
import dev.jstech.core.audio.SoundMixerTexts;
import dev.jstech.core.client.audio.AudioMixer;
import dev.jstech.core.client.audio.AudioPrefsStore;
import dev.jstech.core.gui.layout.SoundMixerLayout;
import dev.jstech.core.text.GameText;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * One channel's volume, in the game's own slider: its name and how loud it is, "OFF" at nothing, and a tooltip that
 * says what the channel carries and which of the game's categories it also follows. Moving it reaches the sounds
 * already playing, not only the next ones.
 */
final class ChannelSlider extends AbstractSliderButton {

    private final AudioChannel channel;

    ChannelSlider(final AudioChannel channel) {
        super(0, 0, SoundMixerLayout.COLUMN, SoundMixerLayout.CONTROL_HEIGHT, CommonComponents.EMPTY,
                AudioPrefsStore.prefs().volume(channel.id().toString()));
        this.channel = channel;
        updateMessage();
        final Component category = Component.translatable("soundCategory." + channel.source().getName());
        setTooltip(Tooltip.create(GameText.component(channel.description()).append(CommonComponents.SPACE)
                .append(GameText.component(SoundMixerTexts.ALSO_FOLLOWS.with(GameText.of(category))))));
    }

    /** Sets the channel back to its whole volume. */
    void reset() {
        value = 1.0;
        applyValue();
        updateMessage();
    }

    /** The channel it sets. */
    AudioChannel channel() {
        return channel;
    }

    @Override
    protected void updateMessage() {
        final Component name = GameText.component(channel.name());
        setMessage(value <= 0.0 ? CommonComponents.optionNameValue(name, CommonComponents.OPTION_OFF)
                : Options.genericValueLabel(name,
                        GameText.component(SoundMixerTexts.PERCENT.with((int) Math.round(value * 100)))));
    }

    @Override
    protected void applyValue() {
        AudioPrefsStore.prefs().setVolume(channel.id().toString(), (float) value);
        AudioMixer.refreshVolumes();
    }
}
