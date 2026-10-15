/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import com.mojang.serialization.Codec;
import dev.jstech.core.config.ConfigApply;
import dev.jstech.core.config.ConfigControl;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigPreset;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Map;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * A player's settings file with every kind of control the Core's settings screen has, in sections and groups that fold,
 * with settings that take hold on a reload or a restart, one active under another and one shown only under another, a
 * rule between two, and a preset: what the screen's client tests open, read and photograph.
 */
@TextHolder
public final class TestShowcaseSettings {

    public static final TextKey GENTLE = TextKey.of("jstests.config.showcase.gentle", "Gentle consequences");
    public static final TextKey FEWER_THAN_MOST = TextKey.of("jstests.config.showcase.fewer_than_most",
            "The fewest loaded chunks cannot be more than the most");

    public static final ConfigKey<Boolean> VEINS = ConfigKey.flag("world.veins.enabled", true)
            .named("The vein system").appliesOn(ConfigApply.RESTART)
            .comment("One vein lies dense in the middle and thin at its edges: you find nothing, or hundreds.")
            .inManual("jstests:test_manual", "veins");
    public static final ConfigKey<Map<String, Double>> RARITY = ConfigKey.of("world.veins.rarity",
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE), Map.of("copper_porphyry", 1.0, "banded_iron", 0.8))
            .named("Each deposit type's rarity").appliesOn(ConfigApply.RELOAD).activeWhen(VEINS)
            .comment("How often each type of deposit is found, against the usual.");
    public static final ConfigKey<List<String>> SKIPPED = ConfigKey.ids("world.veins.skipped",
            List.of("minecraft:iron_ingot", "minecraft:copper_ingot"), ConfigControl.ITEMS)
            .named("Don't generate these items' ores").appliesOn(ConfigApply.RELOAD)
            .comment("The ores of these are left out of every new chunk.");
    public static final ConfigKey<Integer> RESERVOIRS = ConfigKey.whole("world.reservoirs.per_region", 3)
            .range(0, 16).named("Reservoirs per region").unit("reservoirs");
    public static final ConfigKey<Boolean> DRAIN = ConfigKey.flag("world.reservoirs.drain", false)
            .named("Reservoirs run dry").shownWhen(VEINS);
    public static final ConfigKey<Integer> MOST_CHUNKS = ConfigKey.whole("world.chunks.most", 400).range(0, 2000)
            .named("Loaded chunks, whole server").unit("chunks");
    public static final ConfigKey<Integer> FEWEST_CHUNKS = ConfigKey.whole("world.chunks.fewest", 16).range(0, 2000)
            .named("Loaded chunks, each player").unit("chunks");
    public static final ConfigKey<String> ACCENT = ConfigKey.colour("look.accent", "#39D6C4").named("Accent colour");
    public static final ConfigKey<String> MODE = ConfigKey.text("look.mode", "standard")
            .allowing("simple", "standard", "detailed").named("Detail");
    public static final ConfigKey<String> GREETING = ConfigKey.text("look.greeting", "Ready.").named("Greeting");
    public static final ConfigKey<String> MARKER = ConfigKey.text("look.marker", "minecraft:beacon")
            .shownAs(ConfigControl.BLOCK).named("Marker block");
    public static final ConfigKey<List<String>> COOLANTS = ConfigKey.ids("look.coolants",
            List.of("minecraft:water"), ConfigControl.FLUIDS).named("Coolants");
    public static final ConfigKey<List<String>> TAGS = ConfigKey.of("look.tags", Codec.STRING.listOf(),
            List.of("alpha", "beta")).named("Tags");
    public static final ConfigKey<Double> SCALE = ConfigKey.number("look.scale", 1.5).named("Scale");

    public static final ConfigFile FILE = ConfigFile.builder("jstests-showcase", ConfigSide.CLIENT,
                    ConfigFormats.JSON5)
            .version(1)
            .comment("Every kind of setting the Core's settings screen shows.")
            .sectionNamed("world", "World")
            .sectionNamed("world.veins", "Veins")
            .sectionNamed("world.reservoirs", "Reservoirs")
            .sectionNamed("world.chunks", "Chunks and the Space Persistor")
            .sectionNamed("look", "Look")
            .key(VEINS).key(RARITY).key(SKIPPED).key(RESERVOIRS).key(DRAIN).key(MOST_CHUNKS).key(FEWEST_CHUNKS)
            .key(ACCENT).key(MODE).key(GREETING).key(MARKER).key(COOLANTS).key(TAGS).key(SCALE)
            .rule(FEWER_THAN_MOST, values -> values.get(FEWEST_CHUNKS) <= values.get(MOST_CHUNKS))
            .preset(ConfigPreset.of("gentle", GENTLE).with(VEINS, false).with(MOST_CHUNKS, 100))
            .build();

    private TestShowcaseSettings() {
    }

    /** Puts the file in its place, from the test mod's constructor. */
    public static void register(final IEventBus modEventBus, final ModContainer modContainer) {
        ConfigFiles.register(FILE, modEventBus, modContainer);
    }
}
