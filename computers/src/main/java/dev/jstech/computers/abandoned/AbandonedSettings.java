/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.abandoned;

import com.mojang.serialization.Codec;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * How the server lays the abandoned buildings, in {@code jscomputers-server.toml} (computers/docs/design/survival.md,
 * "Settings"): the share that still work, the distances from the world's spawn where each era begins, and for each
 * building whether it is laid, how far apart and of which eras.
 */
public final class AbandonedSettings {

    /** The section of the buildings' own tables. */
    public static final String STRUCTURES = "structures";
    /** The share of the abandoned buildings that still work. */
    public static final ConfigKey<Double> WORKING_CHANCE = ConfigKey.number("survival.working_ruin_chance", 0.2)
            .range(0.0, 1.0)
            .comment("The share of the abandoned buildings that still work: their machines whole, on their network and "
                    + "switched on. The rest are ruins.")
            .named("Working buildings");
    /** Where each era after the Vintage begins, in blocks from the world's spawn. */
    public static final ConfigKey<List<Integer>> ERA_DISTANCES = ConfigKey.of("survival.era_distances",
                    Codec.INT.listOf(), List.of(750, 1500, 3000, 5000))
            .comment("How far from the world's spawn, in blocks, the abandoned buildings of the Legacy, the "
                    + "Transition, the Standard and the Advanced begin: nearer than the first, they are of the "
                    + "Vintage.")
            .named("Era distances");
    private static final Map<AbandonedBuilding, Keys> KEYS = new EnumMap<>(AbandonedBuilding.class);
    private static final int MOST_SPACING = 1024;

    static {
        for (final AbandonedBuilding building : AbandonedBuilding.values()) {
            final String at = at(building);
            KEYS.put(building, new Keys(
                    ConfigKey.flag(at + ".enabled", true)
                            .comment("Whether this building is laid in the worlds made from now on.")
                            .named("Laid"),
                    ConfigKey.whole(at + ".spacing", building.spacing()).range(2, MOST_SPACING)
                            .comment("How many chunks apart two of them are, on average.")
                            .named("Spacing").unit("chunks"),
                    ConfigKey.of(at + ".eras", Codec.STRING.listOf(), building.eras().stream().map(Era::path)
                                    .toList())
                            .comment("The eras it may be of, among those it has: " + String.join(", ",
                                    building.eras().stream().map(Era::path).toList()) + ".")
                            .named("Eras")));
        }
    }

    private AbandonedSettings() {
    }

    /** Adds the settings to the server's file, with their sections. */
    public static ConfigFile.Builder addTo(final ConfigFile.Builder file) {
        file.key(WORKING_CHANCE).key(ERA_DISTANCES)
                .sectionNamed(STRUCTURES, "Abandoned buildings")
                .sectionComment(STRUCTURES,
                        "Each abandoned building: whether it is laid, how far apart, of which eras.")
                .sectionNamed(at(AbandonedBuilding.ABANDONED_HOUSE), "Abandoned house")
                .sectionNamed(at(AbandonedBuilding.EWASTE_DUMP), "Electronic waste dump")
                .sectionNamed(at(AbandonedBuilding.DELIVERY_VAN), "Overturned delivery van")
                .sectionNamed(at(AbandonedBuilding.COMPUTER_SHOP), "Computer shop")
                .sectionNamed(at(AbandonedBuilding.LAN_HOUSE), "LAN house")
                .sectionNamed(at(AbandonedBuilding.SCHOOL), "School computer room")
                .sectionNamed(at(AbandonedBuilding.LIBRARY), "Public library")
                .sectionNamed(at(AbandonedBuilding.RADIO_TV), "Radio and television station")
                .sectionNamed(at(AbandonedBuilding.TELEPHONE_EXCHANGE), "Telephone exchange")
                .sectionNamed(at(AbandonedBuilding.UNIVERSITY_LAB), "University lab")
                .sectionNamed(at(AbandonedBuilding.BANK), "Bank")
                .sectionNamed(at(AbandonedBuilding.MIDSOFT_OFFICE), "Midsoft office")
                .sectionNamed(at(AbandonedBuilding.SOFTWARE_HOUSE), "Software house office")
                .sectionNamed(at(AbandonedBuilding.STARTUP), "Startup building")
                .sectionNamed(at(AbandonedBuilding.GAME_STUDIO), "Game studio")
                .sectionNamed(at(AbandonedBuilding.HACKER_DEN), "Hacker's den")
                .sectionNamed(at(AbandonedBuilding.ELECTRONICS_FACTORY), "Electronics factory")
                .sectionNamed(at(AbandonedBuilding.MINING_FARM), "Crypto mining farm")
                .sectionNamed(at(AbandonedBuilding.DATACENTER), "Datacenter")
                .sectionNamed(at(AbandonedBuilding.SUPERCOMPUTING_CENTRE), "Supercomputing centre");
        for (final Map.Entry<AbandonedBuilding, Keys> entry : KEYS.entrySet()) {
            final Keys keys = entry.getValue();
            file.key(keys.enabled()).key(keys.spacing()).key(keys.eras());
        }
        return file;
    }

    /** Whether {@code building} is laid. */
    public static boolean laid(final AbandonedBuilding building) {
        return read(KEYS.get(building).enabled());
    }

    /** How far apart two of {@code building} are, in chunks. */
    public static int spacing(final AbandonedBuilding building) {
        return read(KEYS.get(building).spacing());
    }

    /** The eras {@code building} may be of, as the server allows among its own; all of its own when none is left. */
    public static List<Era> eras(final AbandonedBuilding building) {
        final List<String> allowed = read(KEYS.get(building).eras());
        final List<Era> out = new ArrayList<>();
        for (final Era era : building.eras()) {
            if (allowed.contains(era.path())) {
                out.add(era);
            }
        }
        return out.isEmpty() ? building.eras() : out;
    }

    /** The era a building {@code distance} blocks from the world's spawn is of, before its own eras are asked. */
    public static Era eraAt(final double distance) {
        final List<Integer> bands = read(ERA_DISTANCES);
        int era = 0;
        while (era < bands.size() && era + 1 < HardwareEra.WITH_PARTS.size() && distance >= bands.get(era)) {
            era++;
        }
        return HardwareEra.WITH_PARTS.get(era);
    }

    /** The share of the buildings that still work. */
    public static double workingChance() {
        return read(WORKING_CHANCE);
    }

    private static <T> T read(final ConfigKey<T> key) {
        return ConfigFiles.forData(ComputersServerConfig.FILE).get(key);
    }

    /* Where {@code building}'s settings stand in the file. */
    private static String at(final AbandonedBuilding building) {
        return STRUCTURES + "." + building.serializedName();
    }

    /* A building's three settings. */
    private record Keys(ConfigKey<Boolean> enabled, ConfigKey<Integer> spacing, ConfigKey<List<String>> eras) {
    }
}
