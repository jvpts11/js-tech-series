/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.villager;

import dev.jstech.computers.abandoned.AbandonedBuilding;
import dev.jstech.computers.abandoned.AbandonedSoftware;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Where a player gets a system or a program without a disk of their own (computers/docs/design/survival.md,
 * "Villagers"): the Programmer of the level of its era sells it, and the abandoned buildings of its era hold it, a
 * house's office only its house's; a graphical system is in every building's main chest. What a recipe viewer shows
 * on the installer: "Sold by the Programmer (level 3)", "Found in a Midsoft office".
 */
@TextHolder
public final class SoftwareSources {

    public static final TextKey SOLD_BY = TextKey.of("jsc.software_sources.sold_by",
            "Sold by the Programmer (level %s)");
    public static final TextKey FOUND_IN = TextKey.of("jsc.software_sources.found_in", "Found in:");
    public static final TextKey MAIN_CHESTS = TextKey.of("jsc.software_sources.main_chests",
            "Found in the main chest of every abandoned building of its era");
    public static final TextKey ABANDONED_HOUSE = TextKey.of("jsc.software_sources.building.abandoned_house",
            "an abandoned house");
    public static final TextKey EWASTE_DUMP = TextKey.of("jsc.software_sources.building.ewaste_dump",
            "an electronic waste dump");
    public static final TextKey DELIVERY_VAN = TextKey.of("jsc.software_sources.building.delivery_van",
            "an overturned delivery van");
    public static final TextKey COMPUTER_SHOP = TextKey.of("jsc.software_sources.building.computer_shop",
            "a computer shop");
    public static final TextKey LAN_HOUSE = TextKey.of("jsc.software_sources.building.lan_house", "a LAN house");
    public static final TextKey SCHOOL = TextKey.of("jsc.software_sources.building.school", "a school computer room");
    public static final TextKey LIBRARY = TextKey.of("jsc.software_sources.building.library", "a public library");
    public static final TextKey RADIO_TV = TextKey.of("jsc.software_sources.building.radio_tv",
            "a radio and television station");
    public static final TextKey TELEPHONE_EXCHANGE = TextKey.of("jsc.software_sources.building.telephone_exchange",
            "a telephone exchange");
    public static final TextKey UNIVERSITY_LAB = TextKey.of("jsc.software_sources.building.university_lab",
            "a university lab");
    public static final TextKey BANK = TextKey.of("jsc.software_sources.building.bank", "a bank");
    public static final TextKey MIDSOFT_OFFICE = TextKey.of("jsc.software_sources.building.midsoft_office",
            "a Midsoft office");
    public static final TextKey SOFTWARE_HOUSE = TextKey.of("jsc.software_sources.building.software_house",
            "a software house office");
    public static final TextKey STARTUP = TextKey.of("jsc.software_sources.building.startup", "a startup building");
    public static final TextKey GAME_STUDIO = TextKey.of("jsc.software_sources.building.game_studio", "a game studio");
    public static final TextKey HACKER_DEN = TextKey.of("jsc.software_sources.building.hacker_den", "a hacker's den");
    public static final TextKey ELECTRONICS_FACTORY = TextKey.of("jsc.software_sources.building.electronics_factory",
            "an electronics factory");
    public static final TextKey MINING_FARM = TextKey.of("jsc.software_sources.building.mining_farm",
            "a crypto mining farm");
    public static final TextKey DATACENTER = TextKey.of("jsc.software_sources.building.datacenter", "a datacenter");
    public static final TextKey SUPERCOMPUTING_CENTRE = TextKey.of(
            "jsc.software_sources.building.supercomputing_centre", "a supercomputing centre");
    private static final Map<AbandonedBuilding, TextKey> BUILDINGS = names();

    private SoftwareSources() {
    }

    /** Where {@code program} is sold and found, one line each. */
    public static List<Text> of(final ProgramSpec program) {
        final List<Text> lines = new ArrayList<>(soldBy(program.era()));
        final List<Text> found = new ArrayList<>();
        for (final AbandonedBuilding building : AbandonedBuilding.values()) {
            if (building.eras().contains(program.era())
                    && AbandonedSoftware.programs(building, program.era()).contains(program)) {
                found.add(BUILDINGS.get(building).text());
            }
        }
        if (!found.isEmpty()) {
            lines.add(FOUND_IN.text());
            lines.addAll(found);
        }
        return lines;
    }

    /** Where {@code system} is sold and found, one line each. */
    public static List<Text> of(final OsDef system) {
        final Era era = HardwareEra.withParts(system.minEra());
        final List<Text> lines = new ArrayList<>(soldBy(era));
        if (HardwareEra.WITH_PARTS.contains(era) && AbandonedSoftware.graphicalSystem(era).stream()
                .anyMatch(installer -> system.id().equals(installer.get(ComputingComponents.MEDIA_PAYLOAD.get())))) {
            lines.add(MAIN_CHESTS.text());
        }
        return lines;
    }

    /** The name of {@code building}, as a player reads it. */
    public static TextKey name(final AbandonedBuilding building) {
        return BUILDINGS.get(building);
    }

    /* The Programmer of the level of {@code era} sells what is of it: Novice the Vintage to Master the Advanced. */
    private static List<Text> soldBy(final Era era) {
        final int level = HardwareEra.WITH_PARTS.indexOf(era) + 1;
        return level > 0 ? List.of(SOLD_BY.with(level)) : List.of();
    }

    private static Map<AbandonedBuilding, TextKey> names() {
        final Map<AbandonedBuilding, TextKey> out = new EnumMap<>(AbandonedBuilding.class);
        out.put(AbandonedBuilding.ABANDONED_HOUSE, ABANDONED_HOUSE);
        out.put(AbandonedBuilding.EWASTE_DUMP, EWASTE_DUMP);
        out.put(AbandonedBuilding.DELIVERY_VAN, DELIVERY_VAN);
        out.put(AbandonedBuilding.COMPUTER_SHOP, COMPUTER_SHOP);
        out.put(AbandonedBuilding.LAN_HOUSE, LAN_HOUSE);
        out.put(AbandonedBuilding.SCHOOL, SCHOOL);
        out.put(AbandonedBuilding.LIBRARY, LIBRARY);
        out.put(AbandonedBuilding.RADIO_TV, RADIO_TV);
        out.put(AbandonedBuilding.TELEPHONE_EXCHANGE, TELEPHONE_EXCHANGE);
        out.put(AbandonedBuilding.UNIVERSITY_LAB, UNIVERSITY_LAB);
        out.put(AbandonedBuilding.BANK, BANK);
        out.put(AbandonedBuilding.MIDSOFT_OFFICE, MIDSOFT_OFFICE);
        out.put(AbandonedBuilding.SOFTWARE_HOUSE, SOFTWARE_HOUSE);
        out.put(AbandonedBuilding.STARTUP, STARTUP);
        out.put(AbandonedBuilding.GAME_STUDIO, GAME_STUDIO);
        out.put(AbandonedBuilding.HACKER_DEN, HACKER_DEN);
        out.put(AbandonedBuilding.ELECTRONICS_FACTORY, ELECTRONICS_FACTORY);
        out.put(AbandonedBuilding.MINING_FARM, MINING_FARM);
        out.put(AbandonedBuilding.DATACENTER, DATACENTER);
        out.put(AbandonedBuilding.SUPERCOMPUTING_CENTRE, SUPERCOMPUTING_CENTRE);
        return out;
    }
}
