/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.villager;

import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;

/**
 * The server's settings of the villagers (computers/docs/design/survival.md, "Settings"): whether the Programmer and
 * the IT Technician take their job sites, and whether the villages have a computer shop.
 */
public final class VillagerSettings {

    public static final ConfigKey<Boolean> PROGRAMMER = ConfigKey.flag("villagers.programmer", true)
            .comment("Whether villagers take the Programmer's Desk and become Programmers.")
            .named("Programmer");
    public static final ConfigKey<Boolean> IT_TECHNICIAN = ConfigKey.flag("villagers.it_technician", true)
            .comment("Whether villagers take the Repair Bench and become IT Technicians.")
            .named("IT Technician");
    public static final ConfigKey<Boolean> COMPUTER_SHOP = ConfigKey.flag("villagers.computer_shop", true)
            .comment("Whether the villages of every kind may have a computer shop, with both job sites.")
            .named("Computer shop");
    private static final String SECTION = "villagers";

    private VillagerSettings() {
    }

    /** The settings added to {@code builder}, their section named. */
    public static ConfigFile.Builder addTo(final ConfigFile.Builder builder) {
        return builder.key(PROGRAMMER).key(IT_TECHNICIAN).key(COMPUTER_SHOP)
                .sectionNamed(SECTION, "Villagers")
                .sectionComment(SECTION, "The two professions of the computers, and the villages' computer shop.");
    }

    /** Whether villagers become Programmers, as the world's settings say. */
    public static boolean programmer() {
        return ConfigFiles.forData(ComputersServerConfig.FILE).get(PROGRAMMER);
    }

    /** Whether villagers become IT Technicians, as the world's settings say. */
    public static boolean itTechnician() {
        return ConfigFiles.forData(ComputersServerConfig.FILE).get(IT_TECHNICIAN);
    }

    /** Whether the villages may have a computer shop, as the world's settings say. */
    public static boolean computerShop() {
        return ConfigFiles.forData(ComputersServerConfig.FILE).get(COMPUTER_SHOP);
    }
}
