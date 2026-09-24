/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.JsCore;
import dev.jstech.core.audio.AudioTexts;
import dev.jstech.core.audio.LastSoundToggle;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The sound system's key: it turns off the last sound the player heard around them and says which on the action bar,
 * and a second press soon after brings it back. It has no key of its own until the player gives it one, since the
 * game and the mods beside it already take nearly every key.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class AudioKeys {

    public static final KeyMapping TURN_OFF_LAST_SOUND = new KeyMapping(AudioTexts.TURN_OFF_LAST_SOUND.key(),
            InputConstants.UNKNOWN.getValue(), AudioTexts.KEY_CATEGORY.key());

    private static final LastSoundToggle TOGGLE = new LastSoundToggle(LastSoundToggle.DEFAULT_UNDO_MILLIS);

    private AudioKeys() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(final RegisterKeyMappingsEvent event) {
        event.register(TURN_OFF_LAST_SOUND);
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        while (TURN_OFF_LAST_SOUND.consumeClick()) {
            turnOffLastSound();
        }
    }

    /**
     * Does what the key does: turns off the last sound heard, or brings back the one the press before turned off, and
     * keeps the choice in the player's file.
     *
     * @return what it told the player
     */
    public static Component turnOffLastSound() {
        final ResourceLocation last = AudioMixer.lastHeard();
        final LastSoundToggle.Outcome outcome = TOGGLE.press(last == null ? null : last.toString(), Util.getMillis(),
                AudioPrefsStore.prefs());
        final Component message = switch (outcome.kind()) {
            case NOTHING -> GameText.component(AudioTexts.NOTHING_HEARD);
            case TURNED_OFF -> GameText.component(AudioTexts.TURNED_OFF.with(nameOf(outcome.sound()),
                    GameText.of(TURN_OFF_LAST_SOUND.getTranslatedKeyMessage())));
            case BACK_ON -> GameText.component(AudioTexts.BACK_ON.with(nameOf(outcome.sound())));
        };
        if (outcome.kind() == LastSoundToggle.Kind.TURNED_OFF) {
            Minecraft.getInstance().getSoundManager().stop(last, null);
        }
        if (outcome.kind() != LastSoundToggle.Kind.NOTHING) {
            AudioPrefsStore.save();
        }
        Minecraft.getInstance().gui.setOverlayMessage(message, false);
        return message;
    }

    /* A sound by its subtitle, which is how a player knows it; by its id when it has none. */
    private static Text nameOf(@Nullable final String sound) {
        final ResourceLocation id = sound == null ? null : ResourceLocation.tryParse(sound);
        final WeighedSoundEvents events =
                id == null ? null : Minecraft.getInstance().getSoundManager().getSoundEvent(id);
        final Component subtitle = events == null ? null : events.getSubtitle();
        return subtitle != null ? GameText.of(subtitle) : Text.literal(String.valueOf(sound));
    }
}
