/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import dev.jstech.core.audio.acoustics.EarRingPayload;
import dev.jstech.core.audio.acoustics.ListenerAirPayload;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.input.CoreKeys;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/**
 * How sound travels on a player's game: the place's echo, the walls, the air and the medium, the speed of sound and
 * the Doppler effect ({@link SoundShaper}); what blocks sound like by their material; the ambience and the music that
 * follow where the player is; ringing ears; and the debugging of it all. What the server tells of the air round the
 * player and of a blast close by arrives here.
 */
public final class AcousticsClient {

    private static final int TICKS_A_SECOND = 20;

    private static long ticks;

    private AcousticsClient() {
    }

    /** Hands the buses what this answers, once, from the mod's constructor on a player's game. */
    public static void listen(final IEventBus modBus) {
        SoundShaper.listen(modBus);
        modBus.addListener(RegisterClientReloadListenersEvent.class, event -> {
            event.registerReloadListener(BlockAcoustics.listener());
            event.registerReloadListener(Soundscapes.listener());
            event.registerReloadListener(LayeredMusic.listener());
        });
        modBus.addListener(FMLClientSetupEvent.class,
                event -> KeyActionsClient.onPress(CoreKeys.SHOW_SOUND_DEBUG, AcousticsDebug::toggle));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> second());
        NeoForge.EVENT_BUS.addListener(SelectMusicEvent.class, LayeredMusic::onSelectMusic);
        NeoForge.EVENT_BUS.addListener(TagsUpdatedEvent.class, event -> BlockAcoustics.forgetBlocks());
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, AcousticsDebug::render);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> {
            Soundscapes.stop();
            LayeredMusic.stop();
        });
    }

    /** What the server told of the air round the player's head. */
    public static void told(final ListenerAirPayload payload) {
        ListenerAir.told(payload);
    }

    /** A blast close by rang the player's ears. */
    public static void ring(final EarRingPayload payload) {
        RingingEars.ring(payload.ring());
    }

    /** The lines the debug screen shows of how sound travels where the player is. */
    public static List<String> debugLines() {
        return AcousticsDebug.lines();
    }

    /** The soundscapes heard now, by id, for a test. */
    public static Set<String> soundscapes() {
        return Soundscapes.playing();
    }

    /** The music set playing, or empty, for a test. */
    public static String musicSet() {
        return LayeredMusic.set();
    }

    /** Each layer of the music set playing, by sound, and how loud it is asked to play, for a test. */
    public static Map<String, Float> musicLayers() {
        return LayeredMusic.layers();
    }

    /** The sound card's sources of every sound playing now, for a test. */
    public static List<Integer> sources() {
        return SoundShaper.playing().values().stream().map(SoundShaper.Playing::source).toList();
    }

    /** The place's echo as the sound card has it now: how long it rings, in seconds, and how loud, for a test. */
    public static double[] echo() {
        return new double[] {SoundShaper.echo().decaySeconds(), SoundShaper.echo().wet()};
    }

    /** Whether the player's ears ring now, for a test. */
    public static boolean ringing() {
        return RingingEars.ringing();
    }

    /** Whether the overlay of sound debugging is on, for a test. */
    public static boolean debugShown() {
        return AcousticsDebug.shown();
    }

    /** Turns the overlay of sound debugging on or off, as its key does. */
    public static void toggleDebug() {
        AcousticsDebug.toggle();
    }

    /* Once a second, where a world is: the ambience and the music follow where the player is. */
    private static void second() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (++ticks % TICKS_A_SECOND != 0 || minecraft.level == null || minecraft.player == null
                || minecraft.gameRenderer == null) {
            return;
        }
        Soundscapes.second(minecraft.level, minecraft.gameRenderer.getMainCamera().getPosition());
        LayeredMusic.second(minecraft.level, minecraft.gameRenderer.getMainCamera().getPosition());
    }
}
