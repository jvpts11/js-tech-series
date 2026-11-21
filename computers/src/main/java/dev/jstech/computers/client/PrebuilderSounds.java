/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.chain.machine.PrebuilderBlockEntity;
import dev.jstech.computers.chain.machine.PrebuilderCues;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.client.audio.AudioEngine;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

/**
 * The Prebuilder heard as it is drawn at work, on the player's game: each tick, the sounds of its job whose moments
 * its drawn work passed since the last tick ({@link PrebuilderCues}), its press, its arms and its parts, from its
 * terminal; so what is heard keeps time with what is seen, which only the player's game works out.
 */
public final class PrebuilderSounds {

    /* The Prebuilders on the player's game, and the moment of its job's loop each was at last tick. */
    private static final Map<PrebuilderBlockEntity, Float> HEARD = new WeakHashMap<>();

    private PrebuilderSounds() {
    }

    /** Hands the game's bus what this listens to, once, from the client's setup. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
    }

    /** A Prebuilder loaded on the player's game, to be heard while it works. */
    public static void track(final PrebuilderBlockEntity prebuilder) {
        HEARD.put(prebuilder, PrebuilderCues.notYet());
    }

    public static void untrack(final PrebuilderBlockEntity prebuilder) {
        HEARD.remove(prebuilder);
    }

    private static void tick() {
        for (final Map.Entry<PrebuilderBlockEntity, Float> each : HEARD.entrySet()) {
            final Optional<PrebuilderRenderer.Heard> now = PrebuilderRenderer.heard(each.getKey());
            if (now.isEmpty()) {
                each.setValue(PrebuilderCues.notYet());
                continue;
            }
            final BlockPos at = each.getKey().getBlockPos();
            for (final PrebuilderCues.Sound sound : now.get().cues().crossed(each.getValue(), now.get().a())) {
                AudioEngine.playAt(soundOf(sound), at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 1F, 1F);
            }
            each.setValue(now.get().a());
        }
    }

    private static SoundKey soundOf(final PrebuilderCues.Sound sound) {
        return switch (sound) {
            case PRESS -> ComputingSounds.PREBUILDER_PRESS;
            case ARM -> ComputingSounds.PREBUILDER_ARM;
            case PART -> ComputingSounds.PREBUILDER_PART;
        };
    }
}
