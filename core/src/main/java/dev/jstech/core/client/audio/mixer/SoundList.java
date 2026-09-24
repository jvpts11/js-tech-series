/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.mixer;

import dev.jstech.core.audio.SoundMixerTexts;
import dev.jstech.core.client.audio.AudioEngine;
import dev.jstech.core.client.audio.AudioPrefsStore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.gui.layout.SoundMixerLayout;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.CommonComponents;

/**
 * The list of sounds, two lines a row (the name, the id under it) with a button to hear the sound once and one to
 * turn it off or back on. A sound turned off is written in grey.
 */
final class SoundList extends ContainerObjectSelectionList<SoundList.Row> {

    private final Font font;
    private final Runnable changed;

    /** From the right edge of the rows to the scrollbar. */
    private static final int SCROLLBAR_GAP = 10;

    SoundList(final Minecraft minecraft, final Font font, final Runnable changed) {
        super(minecraft, 0, 0, SoundMixerLayout.LIST_TOP, SoundMixerLayout.ROW_HEIGHT);
        this.font = font;
        this.changed = changed;
    }

    /** Shows those sounds, from the top. */
    void show(final List<SoundEntry> sounds) {
        final List<Row> rows = new ArrayList<>(sounds.size());
        for (final SoundEntry sound : sounds) {
            rows.add(new Row(sound));
        }
        replaceEntries(rows);
        setScrollAmount(0);
    }

    /** The rows showing, in order. */
    List<Row> rows() {
        return List.copyOf(children());
    }

    @Override
    public int getRowWidth() {
        return SoundMixerLayout.FULL;
    }

    @Override
    protected int getScrollbarPosition() {
        return getRowLeft() + getRowWidth() + SCROLLBAR_GAP;
    }

    /** One sound: its name and id, Play, and ON or OFF. */
    final class Row extends ContainerObjectSelectionList.Entry<Row> {

        private final SoundEntry sound;
        private final Button play;
        private final Button toggle;

        Row(final SoundEntry sound) {
            this.sound = sound;
            this.play = Button.builder(GameText.component(SoundMixerTexts.PLAY),
                    button -> AudioEngine.preview(sound.id()))
                    .size(SoundMixerLayout.PLAY_WIDTH, SoundMixerLayout.CONTROL_HEIGHT).build();
            if (sound.made()) {
                play.active = false;
                play.setTooltip(Tooltip.create(GameText.component(SoundMixerTexts.NO_PREVIEW)));
            }
            this.toggle = Button.builder(CommonComponents.optionStatus(!off()), button -> flip())
                    .size(SoundMixerLayout.TOGGLE_WIDTH, SoundMixerLayout.CONTROL_HEIGHT).build();
        }

        /** The sound it lists. */
        SoundEntry sound() {
            return sound;
        }

        /** Turns the sound off or back on, as the button does. */
        void flip() {
            final boolean mute = !off();
            AudioPrefsStore.prefs().setMuted(sound.id().toString(), mute);
            AudioPrefsStore.save();
            if (mute) {
                minecraft.getSoundManager().stop(sound.id(), null);
            }
            toggle.setMessage(CommonComponents.optionStatus(!mute));
            changed.run();
        }

        @Override
        public void render(final GuiGraphics g, final int index, final int top, final int left, final int width,
                           final int height, final int mouseX, final int mouseY, final boolean hovered,
                           final float partialTick) {
            final SoundMixerPalette.Colours colours = SoundMixerPalette.get();
            final boolean off = off();
            final int room = width - SoundMixerLayout.PLAY_WIDTH - SoundMixerLayout.TOGGLE_WIDTH - 8;
            Draw.text(g, font, font.plainSubstrByWidth(sound.shown().getString(), room), left, top + 1,
                    off ? colours.nameOff() : colours.name(), colours.ground());
            Draw.text(g, font, font.plainSubstrByWidth(sound.id().toString(), room), left, top + 11,
                    off ? colours.idOff() : colours.id(), colours.ground());
            play.setPosition(left + width - SoundMixerLayout.PLAY_WIDTH - SoundMixerLayout.TOGGLE_WIDTH - 2, top);
            toggle.setPosition(left + width - SoundMixerLayout.TOGGLE_WIDTH, top);
            play.render(g, mouseX, mouseY, partialTick);
            toggle.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(play, toggle);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(play, toggle);
        }

        private boolean off() {
            return AudioPrefsStore.prefs().isMuted(sound.id().toString());
        }
    }
}
