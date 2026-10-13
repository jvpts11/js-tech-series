/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import com.mojang.serialization.Codec;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A player's sound preferences as their settings file keeps them, {@code jstech-audio.json} beside the game's own
 * options: each channel's volume, the sounds never to be heard, and the three switches of the sound options. Reading
 * the file fills the preferences in force; {@link #save} writes them back, all at once.
 */
public final class AudioSettings {

    public static final ConfigKey<Map<String, Float>> VOLUMES = ConfigKey.of("volumes",
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT), Map.of());
    public static final ConfigKey<List<String>> MUTED = ConfigKey.of("muted", Codec.STRING.listOf(), List.of());
    public static final ConfigKey<Boolean> VISUAL_CUES = ConfigKey.flag("visual_cues", false);
    public static final ConfigKey<Boolean> OCCLUSION = ConfigKey.flag("occlusion", true);
    public static final ConfigKey<Boolean> DUCKING = ConfigKey.flag("duck_under_alerts", true);

    /** The preferences in force on this game, which the file fills and the sound options change. */
    private static final AudioPrefs PREFS = new AudioPrefs();

    /** The player's file, read into the preferences in force. */
    public static final ConfigFile FILE = file(PREFS);

    private AudioSettings() {
    }

    /** The preferences in force on this game. */
    public static AudioPrefs prefs() {
        return PREFS;
    }

    /**
     * A settings file whose readings fill {@code prefs}. The game has one, {@link #FILE}; a test makes its own so it
     * can read and write one without touching the player's.
     */
    public static ConfigFile file(final AudioPrefs prefs) {
        return ConfigFile.builder("jstech-audio", ConfigSide.CLIENT, ConfigFormats.JSON)
                .key(VOLUMES, prefs::setVolumes)
                .key(MUTED, prefs::setMutedSounds)
                .key(VISUAL_CUES, prefs::setVisualCues)
                .key(OCCLUSION, prefs::setOcclusion)
                .key(DUCKING, prefs::setDucking)
                .build();
    }

    /** Puts {@code prefs} into {@code file} and writes it once. */
    public static void save(final AudioPrefs prefs, final ConfigFile file) {
        file.stage(VOLUMES, prefs.volumes());
        file.stage(MUTED, new ArrayList<>(prefs.mutedSounds()));
        file.stage(VISUAL_CUES, prefs.visualCues());
        file.stage(OCCLUSION, prefs.occlusion());
        file.stage(DUCKING, prefs.ducking());
        file.save();
    }
}
