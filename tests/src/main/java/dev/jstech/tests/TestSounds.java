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
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.content.ModContent;
import net.minecraft.resources.ResourceLocation;

/**
 * The sounds the tests play through the series' sound system: one of the interface, one of the world, a running one,
 * the room many running ones make, and two made as they play. They play files the game already has, or none, so the
 * test mod ships no sound.
 */
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

    private TestSounds() {
    }
}
