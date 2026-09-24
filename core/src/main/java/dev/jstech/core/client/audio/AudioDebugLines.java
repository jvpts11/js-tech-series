/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.JsCore;
import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.AudioDebugTexts;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.SoundKeys;
import dev.jstech.core.audio.SoundMixerTexts;
import dev.jstech.core.client.GameLocale;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

/**
 * The sound system on the game's debug screen (F3), in its right-hand column where the game shows what it has inside:
 * how many running sounds the director keeps out of its budget, the rooms, what is muffled and by how many walls, how
 * far the other channels are lowered under an alert, the last sound heard and how many are turned off.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class AudioDebugLines {

    private static final int TICKS_PER_SECOND = 20;

    private AudioDebugLines() {
    }

    @SubscribeEvent
    public static void onDebugText(final CustomizeGuiOverlayEvent.DebugText event) {
        final List<String> right = event.getRight();
        right.add("");
        right.add(ChatFormatting.UNDERLINE + GameText.resolve(SoundMixerTexts.TITLE));
        right.addAll(lines());
    }

    /** The lines under the header, as they read now. */
    public static List<String> lines() {
        final AudioStats stats = SoundDirector.stats();
        final List<String> out = new ArrayList<>();
        out.add(GameText.resolve(AudioDebugTexts.RUNNING.with(stats.shorts(), stats.shortBudget(), stats.longs(),
                stats.longBudget())));
        if (stats.rooms().isEmpty()) {
            out.add(GameText.resolve(AudioDebugTexts.ROOMS.with(0)));
        } else {
            final AudioStats.Room first = stats.rooms().getFirst();
            out.add(GameText.resolve(AudioDebugTexts.ROOMS_FIRST.with(stats.rooms().size(), first.field(),
                    first.members(), (int) Math.round(first.volume() * 100))));
        }
        if (stats.walls().isEmpty()) {
            out.add(GameText.resolve(AudioDebugTexts.MUFFLED.with(0)));
        } else {
            out.add(GameText.resolve(AudioDebugTexts.MUFFLED_WALLS.with(stats.walls().size(), list(stats.walls()))));
        }
        out.add(GameText.resolve(lowered()));
        final ResourceLocation last = AudioMixer.lastHeard();
        out.add(GameText.resolve(last == null ? AudioDebugTexts.LAST_HEARD_NONE.text()
                : AudioDebugTexts.LAST_HEARD.with(last.toString())));
        out.add(GameText.resolve(AudioDebugTexts.TURNED_OFF.with(
                GameLocale.count(AudioPrefsStore.prefs().mutedSounds().size()), GameLocale.count(known()))));
        return out;
    }

    private static Text lowered() {
        final float share = AudioMixer.duck(AudioChannels.MACHINES.id().toString());
        if (share >= 1.0F) {
            return AudioDebugTexts.LOWERED_NO.text();
        }
        final int percent = Math.round(share * 100);
        if (AudioMixer.alerting()) {
            return AudioDebugTexts.LOWERED.with(percent);
        }
        final double seconds = AudioMixer.ticksToWhole() / (double) TICKS_PER_SECOND;
        return AudioDebugTexts.LOWERED_BACK.with(percent, String.format(GameLocale.locale(), "%.1f", seconds));
    }

    /* A list the way a sentence says it: "1, 1 and 2". */
    private static Text list(final List<Integer> items) {
        if (items.size() == 1) {
            return Text.literal(String.valueOf(items.getFirst()));
        }
        Text out = AudioDebugTexts.LIST_LAST.with(items.get(items.size() - 2), items.getLast());
        for (int i = items.size() - 3; i >= 0; i--) {
            out = AudioDebugTexts.LIST_MORE.with(items.get(i), out);
        }
        return out;
    }

    /* Every sound the game knows, the files the game has read and the series' own made as they play. */
    private static int known() {
        int made = 0;
        for (final SoundKey key : SoundKeys.all()) {
            if (key.spec().made()) {
                made++;
            }
        }
        return Minecraft.getInstance().getSoundManager().getAvailableSounds().size() + made;
    }
}
