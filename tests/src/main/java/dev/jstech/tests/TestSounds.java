/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.audio.AmbientField;
import dev.jstech.core.audio.AmbientFields;
import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.AudioDevice;
import dev.jstech.core.audio.AudioDevices;
import dev.jstech.core.audio.SoundContext;
import dev.jstech.core.audio.SoundCue;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.pcm.Waveform;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * The sounds the tests play through the series' sound system: one of the interface, one of the world, a running one,
 * the room many running ones make, two made as they play, an alarm cue and the buzzer it sounds different through.
 * They play files the game already has, or none, so the test mod ships no sound.
 */
@TextHolder
public final class TestSounds {

    public static final ModContent CONTENT = new ModContent(JsTests.MODID);

    public static final SoundKey CLICK = CONTENT.sound("test/click").onScreen()
            .file(ResourceLocation.withDefaultNamespace("random/click"))
            .subtitle("A test click").register();

    public static final SoundKey HUM = CONTENT.sound("test/hum").world().range(8)
            .file(ResourceLocation.withDefaultNamespace("block/beacon/ambient"))
            .subtitle("A test hum").register();

    /** A running sound, the way a machine's fan is one. */
    public static final SoundKey WHIR = CONTENT.sound("test/whir").world().loop().range(16)
            .file(ResourceLocation.withDefaultNamespace("block/beacon/ambient"))
            .subtitle("A test whir").register();

    /** The sound of a room full of whirring things. */
    public static final SoundKey ROOM = CONTENT.sound("test/room").world().loop().range(24)
            .file(ResourceLocation.withDefaultNamespace("block/conduit/ambient"))
            .subtitle("A test room").register();

    /** A room of whirring things: four close together are heard as one. */
    public static final AmbientField ROOM_FIELD = AmbientFields.register(
            new AmbientField(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_room"), ROOM, 4, 6.0));

    /** A speaker's beep, made as it plays from the notes it is handed. */
    public static final SoundKey BEEP = CONTENT.sound("test/beep").world().made().channel(AudioChannels.DEVICES)
            .range(12).subtitle("A test beep").register();

    /** A recording played on the player's own screen, made as it plays from the samples it is handed. */
    public static final SoundKey TUNE = CONTENT.sound("test/tune").onScreen().made()
            .subtitle("A test tune").register();

    private static final TextKey BUZZER_NAME = TextKey.of("jstests.audio_device.buzzer", "Test buzzer");

    /** A speaker that beeps square waves, one at a time, and plays no recording. */
    public static final AudioDevice BUZZER = AudioDevices.register(
            new AudioDevice("jstests:buzzer", BUZZER_NAME, Set.of(Waveform.SQUARE), false, 1));

    /** A test machine's alarm: the game's bass note through the buzzer, the hum through anything else. */
    public static final SoundCue ALARM = CONTENT.cue("test/alarm").world().channel(AudioChannels.ALERTS).range(12)
            .when(Map.of(SoundContext.DEVICE, BUZZER.id()),
                    ResourceLocation.withDefaultNamespace("block.note_block.bass"))
            .otherwise(HUM).register();

    private TestSounds() {
    }
}
