/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Every audio device the series' mods and their addons declared, by id. The Core declares only {@link #NONE}, the
 * device of a machine with no sound hardware; the devices themselves (a PC speaker, a sound card) belong to the mods
 * that build them.
 */
@TextHolder
public final class AudioDevices {

    private static final TextKey NONE_NAME = TextKey.of("jscore.audio_device.none", "No sound");
    private static final Map<String, AudioDevice> BY_ID = new LinkedHashMap<>();

    /** A machine with no sound hardware: it plays nothing. */
    public static final AudioDevice NONE = register(new AudioDevice("jscore:none", NONE_NAME, Set.of(), false, 0));

    private AudioDevices() {
    }

    /**
     * Registers a device.
     *
     * @throws IllegalStateException when a device with that id is already registered
     */
    public static synchronized AudioDevice register(final AudioDevice device) {
        if (BY_ID.putIfAbsent(device.id(), device) != null) {
            throw new IllegalStateException("an audio device " + device.id() + " is already registered");
        }
        return device;
    }

    /** The device with that id, or null when none is registered with it. */
    @Nullable
    public static synchronized AudioDevice find(final String id) {
        return BY_ID.get(id);
    }

    /** Every registered device, in the order they were registered. */
    public static synchronized List<AudioDevice> all() {
        return new ArrayList<>(BY_ID.values());
    }
}
