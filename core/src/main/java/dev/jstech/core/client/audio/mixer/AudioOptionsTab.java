/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.mixer;

import dev.jstech.core.audio.AudioPrefs;
import dev.jstech.core.audio.AudioTexts;
import dev.jstech.core.audio.SoundMixerTexts;
import dev.jstech.core.client.audio.AudioKeys;
import dev.jstech.core.client.audio.AudioPrefsStore;
import dev.jstech.core.gui.layout.SoundMixerLayout;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The Options tab: whether walls muffle sounds, whether alerts are also shown on the screen, whether the other
 * channels are lowered under an alert, and the key that turns off the last sound, which opens the game's controls to
 * choose it.
 */
final class AudioOptionsTab implements Tab {

    private final CycleButton<Boolean> muffle;
    private final CycleButton<Boolean> alertsOnScreen;
    private final CycleButton<Boolean> lowerUnderAlerts;
    private final Button key;
    private final MultiLineTextWidget note;

    /** The Options tab's place among the tabs, which the mixer comes back to from the game's controls. */
    private static final int INDEX = 2;

    AudioOptionsTab(final SoundMixerScreen screen, final Font font) {
        final AudioPrefs prefs = AudioPrefsStore.prefs();
        muffle = toggle(SoundMixerTexts.MUFFLE, SoundMixerTexts.MUFFLE_TIP, prefs.occlusion(),
                AudioPrefs::setOcclusion);
        alertsOnScreen = toggle(SoundMixerTexts.ALERTS_ON_SCREEN, SoundMixerTexts.ALERTS_ON_SCREEN_TIP,
                prefs.visualCues(), AudioPrefs::setVisualCues);
        lowerUnderAlerts = toggle(SoundMixerTexts.LOWER_UNDER_ALERTS, SoundMixerTexts.LOWER_UNDER_ALERTS_TIP,
                prefs.ducking(), AudioPrefs::setDucking);
        key = Button.builder(CommonComponents.optionNameValue(GameText.component(AudioTexts.TURN_OFF_LAST_SOUND),
                AudioKeys.TURN_OFF_LAST_SOUND.getTranslatedKeyMessage()), button -> {
                    final Minecraft minecraft = Minecraft.getInstance();
                    screen.rememberTab(INDEX);
                    minecraft.setScreen(new KeyBindsScreen(screen, minecraft.options));
                }).size(SoundMixerLayout.FULL, SoundMixerLayout.CONTROL_HEIGHT).build();
        note = new MultiLineTextWidget(GameText.component(SoundMixerTexts.KEY_NOTE), font)
                .setMaxWidth(SoundMixerLayout.FULL).setCentered(true).setColor(SoundMixerPalette.get().note());
    }

    CycleButton<Boolean> muffle() {
        return muffle;
    }

    CycleButton<Boolean> alertsOnScreen() {
        return alertsOnScreen;
    }

    CycleButton<Boolean> lowerUnderAlerts() {
        return lowerUnderAlerts;
    }

    Button key() {
        return key;
    }

    @Override
    public Component getTabTitle() {
        return GameText.component(SoundMixerTexts.TAB_OPTIONS);
    }

    @Override
    public void visitChildren(final Consumer<AbstractWidget> visitor) {
        visitor.accept(muffle);
        visitor.accept(alertsOnScreen);
        visitor.accept(lowerUnderAlerts);
        visitor.accept(key);
        visitor.accept(note);
    }

    @Override
    public void doLayout(final ScreenRectangle area) {
        final int width = area.width();
        final int left = SoundMixerLayout.left(width);
        muffle.setPosition(left, SoundMixerLayout.CONTENT_TOP);
        alertsOnScreen.setPosition(left + SoundMixerLayout.COLUMN + SoundMixerLayout.GAP, SoundMixerLayout.CONTENT_TOP);
        lowerUnderAlerts.setPosition(left, SoundMixerLayout.CONTENT_TOP + SoundMixerLayout.PITCH);
        key.setPosition(left, SoundMixerLayout.CONTENT_TOP + 2 * SoundMixerLayout.PITCH);
        note.setPosition(width / 2 - note.getWidth() / 2, SoundMixerLayout.OPTIONS_NOTE_TOP);
    }

    /* An ON/OFF option that is kept in the player's file the moment it changes. */
    private static CycleButton<Boolean> toggle(final TextKey name, final TextKey tip, final boolean now,
                                               final BiConsumer<AudioPrefs, Boolean> set) {
        return CycleButton.onOffBuilder(now)
                .withTooltip(value -> Tooltip.create(GameText.component(tip)))
                .create(0, 0, SoundMixerLayout.COLUMN, SoundMixerLayout.CONTROL_HEIGHT, GameText.component(name),
                        (button, value) -> {
                            set.accept(AudioPrefsStore.prefs(), value);
                            AudioPrefsStore.save();
                        });
    }
}
