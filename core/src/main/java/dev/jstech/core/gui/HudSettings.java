/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Where the player put the HUD's elements, in their own settings, {@code jstech-hud.toml}: each element they moved,
 * resized or hid in the HUD editor, by its id. An element they never touched stays where its mod declared it, stacked
 * in its corner with the others.
 */
public final class HudSettings {

    /** What the HUD editor is registered as, the chooser of this setting. */
    public static final String EDITOR = "jscore:hud_editor";

    private static final String SECTION = "hud";

    /** A placement as the file keeps it: the corner by its name, the rest by theirs. */
    public static final Codec<HudPlacement> PLACEMENT = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.comapFlatMap(HudSettings::cornerNamed, corner -> corner.name().toLowerCase(Locale.ROOT))
                    .fieldOf("corner").forGetter(HudPlacement::corner),
            Codec.INT.optionalFieldOf("x", 0).forGetter(HudPlacement::x),
            Codec.INT.optionalFieldOf("y", 0).forGetter(HudPlacement::y),
            Codec.floatRange(HudPlacement.LEAST_SCALE, HudPlacement.MOST_SCALE).optionalFieldOf("scale", 1.0F)
                    .forGetter(HudPlacement::scale),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(HudPlacement::hidden)
    ).apply(i, HudPlacement::new));
    /** Each element the player placed, by its id. */
    public static final ConfigKey<Map<String, HudPlacement>> PLACED = ConfigKey.of(SECTION + ".placed",
                    Codec.unboundedMap(Codec.STRING, PLACEMENT), Map.<String, HudPlacement>of())
            .comment("Where each HUD element the player moved, resized or hid in the HUD editor is, by its id: the "
                    + "corner it is held from, how far from it, its size and whether it is hidden.")
            .named("Where the HUD's elements are").choosing(EDITOR);
    /** The player's file. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-hud", ConfigSide.CLIENT, ConfigFormats.TOML)
            .comment("Where the HUD's elements of the mods built on J's Core are on this game.")
            .sectionComment(SECTION, "The HUD's elements the player placed in the HUD editor.")
            .sectionNamed(SECTION, "HUD")
            .key(PLACED, value -> placed = Map.copyOf(value))
            .build();

    private static volatile Map<String, HudPlacement> placed = Map.of();

    private HudSettings() {
    }

    /** Where the player put the element {@code id}, if they placed it. */
    public static Optional<HudPlacement> placement(final String id) {
        return Optional.ofNullable(placed.get(id));
    }

    /** Every element the player placed. */
    public static Map<String, HudPlacement> placed() {
        return placed;
    }

    private static DataResult<HudStack.Corner> cornerNamed(final String name) {
        for (final HudStack.Corner corner : HudStack.Corner.values()) {
            if (corner.name().equalsIgnoreCase(name)) {
                return DataResult.success(corner);
            }
        }
        return DataResult.error(() -> "No corner of the screen is called " + name
                + "; it is top_left, top_right, bottom_left or bottom_right");
    }
}
