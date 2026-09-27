/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.config;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.network.DataTier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The settings of the computers themselves, {@code jscomputers-server.toml} next to the world save.
 *
 * <p>Apart from the series' balance file, which the Core owns: what is in here is about how the machines of this
 * mod behave, and the Core has no business knowing that a boot menu exists.
 *
 * <p>Every value is copied into a plain field as the file is read, so the things that ask for them, which are
 * machines in the middle of starting, read a field rather than going through the config library each time.
 */
public final class ComputersServerConfig {

    public static final String FILE_NAME = "jscomputers-server.toml";

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue SHOW_BOOT_MENU_VALUE;
    private static final ModConfigSpec.BooleanValue GENTOO_EVERY_STEP_VALUE;
    private static final ModConfigSpec.BooleanValue ARCH_EVERY_STEP_VALUE;
    private static final ModConfigSpec.BooleanValue LIST_COMMANDS_VALUE;
    private static final ModConfigSpec.BooleanValue SOUNDFOUNDRY_CATALOG_VALUE;
    private static final ModConfigSpec.IntValue ETHERNET_SPEED_VALUE;
    private static final ModConfigSpec.IntValue HBW_SPEED_VALUE;
    private static final ModConfigSpec.IntValue HPC_SPEED_VALUE;

    /* How fast a song comes over each cable when nothing says otherwise, in kilobytes a second. */
    private static final int ETHERNET_SPEED = 512;
    private static final int HBW_SPEED = 2_048;
    private static final int HPC_SPEED = 8_192;
    private static final int MOST_SPEED = 1_048_576;

    /** Held apart from the file so a machine can ask while the world is still coming up. */
    private static boolean showBootMenu = true;
    private static boolean gentooEveryStep;
    private static boolean archEveryStep;
    private static boolean listCommands;
    private static boolean soundfoundryCatalog = true;
    private static int ethernetSpeed = ETHERNET_SPEED;
    private static int hbwSpeed = HBW_SPEED;
    private static int hpcSpeed = HPC_SPEED;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("How the computers of J's Computers behave. Balance of the Operations engine lives in the "
                + "series' own file beside this one.");
        builder.push("boot");
        SHOW_BOOT_MENU_VALUE = builder
                .comment("Whether a machine whose system brings a boot manager stops at it every time it starts: "
                                + "GRUB on a Linux, the loader on FreeBSD, the Midsoft Boot Manager with two systems.",
                        "Turning this off boots the chosen system at once, the way a machine with the menu hidden "
                                + "does.")
                .define("show_boot_menu", true);
        builder.pop();
        builder.push("install_by_hand");
        GENTOO_EVERY_STEP_VALUE = builder
                .comment("Whether installing Gentoo by hand asks for every step of the handbook.",
                        "Off, only the steps a system cannot boot without are asked for: the disk, the stage 3, the "
                                + "package tree, a kernel, the filesystem table and the bootloader.",
                        "On, the rest are asked for as well: the bind mounts, a profile, the @world update, the "
                                + "kernel link and a name for the machine.",
                        "Every step answers the way the real tool does either way; this only decides which of "
                                + "them a restart refuses to go on without.")
                .define("gentoo_every_step", false);
        ARCH_EVERY_STEP_VALUE = builder
                .comment("Whether installing Arch by hand asks for every step of the installation guide.",
                        "Off, only the steps a system cannot boot without are asked for. On, the hardware clock "
                                + "and a name for the machine are asked for as well.")
                .define("arch_every_step", false);
        builder.pop();
        builder.push("prompt");
        LIST_COMMANDS_VALUE = builder
                .comment("Whether every computer has the 'listcmd' command, which lists absolutely everything that "
                                + "computer can run right now: its commands, whatever family they belong to, and "
                                + "the programs installed on it.",
                        "Off, it is nowhere at all: not in help, not in a manual, not in what a half-typed name "
                                + "completes to, and typing it is an unknown command. Each system then teaches what "
                                + "it has in its own way, which is the experience those systems really gave.",
                        "On, it is on every computer and shows up everywhere like any other command, for whoever "
                                + "would rather read one list than learn each system's own habits.")
                .define("list_commands", false);
        builder.pop();
        builder.push("soundfoundry");
        SOUNDFOUNDRY_CATALOG_VALUE = builder
                .comment("Whether the server offers its music catalogue: the albums put in "
                                + "config/jstech/soundfoundry/catalog/, a folder each, and those the data packs carry in "
                                + "soundfoundry/catalog/.",
                        "Off, neither is read and the catalogue is empty. A change is taken up by "
                                + "'/soundfoundry catalog reload' or the next start.")
                .define("catalog", true);
        builder.comment("How fast a song comes over the network into a computer, in kilobytes a second, by the "
                        + "slowest cable on its way. A song from the catalogue comes at the speed of the cable the "
                        + "computer itself is plugged into; the songs coming in at once share it.",
                "A cable with no speed of its own below carries songs at Ethernet's.");
        ETHERNET_SPEED_VALUE = builder.defineInRange("ethernet_kilobytes_per_second", ETHERNET_SPEED, 1, MOST_SPEED);
        HBW_SPEED_VALUE = builder.defineInRange("hbw_kilobytes_per_second", HBW_SPEED, 1, MOST_SPEED);
        HPC_SPEED_VALUE = builder.defineInRange("hpc_kilobytes_per_second", HPC_SPEED, 1, MOST_SPEED);
        builder.pop();
        SPEC = builder.build();
    }

    private ComputersServerConfig() {
    }

    /** Puts the file beside the world save and keeps the fields in step with it. */
    public static void register(final IEventBus modEventBus, final ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SPEC, FILE_NAME);
        modEventBus.addListener(ComputersServerConfig::onLoad);
        modEventBus.addListener(ComputersServerConfig::onReload);
    }

    /** Whether a machine whose system brings a boot manager stops at it on the way up. */
    public static boolean showBootMenu() {
        return showBootMenu;
    }

    /** Whether installing Gentoo by hand asks for the whole handbook rather than only what a system boots by. */
    public static boolean gentooEveryStep() {
        return gentooEveryStep;
    }

    /** Whether installing Arch by hand asks for the whole guide rather than only what a system boots by. */
    public static boolean archEveryStep() {
        return archEveryStep;
    }

    /** Whether every computer has {@code listcmd}, the one word that lists all it can run. */
    public static boolean listCommands() {
        return listCommands;
    }

    /** Whether the server offers its music catalogue. */
    public static boolean soundfoundryCatalog() {
        return soundfoundryCatalog;
    }

    /** How fast a song comes over a way whose slowest cable is {@code tier}, in bytes a second. */
    public static long songBytesPerSecond(final DataTier tier) {
        final int kilobytes = switch (tier) {
            case T2_HBW -> hbwSpeed;
            case HPC -> hpcSpeed;
            default -> ethernetSpeed;
        };
        return kilobytes * 1_024L;
    }

    private static void onLoad(final ModConfigEvent.Loading event) {
        apply(event.getConfig());
    }

    private static void onReload(final ModConfigEvent.Reloading event) {
        apply(event.getConfig());
    }

    private static void apply(final ModConfig config) {
        // Only our own file: every other mod's config raises the same events.
        if (config.getSpec() != SPEC) {
            return;
        }
        showBootMenu = SHOW_BOOT_MENU_VALUE.get();
        gentooEveryStep = GENTOO_EVERY_STEP_VALUE.get();
        archEveryStep = ARCH_EVERY_STEP_VALUE.get();
        listCommands = LIST_COMMANDS_VALUE.get();
        soundfoundryCatalog = SOUNDFOUNDRY_CATALOG_VALUE.get();
        ethernetSpeed = ETHERNET_SPEED_VALUE.get();
        hbwSpeed = HBW_SPEED_VALUE.get();
        hpcSpeed = HPC_SPEED_VALUE.get();
        JsComputers.LOGGER.debug("Boot menu is {}", showBootMenu ? "shown" : "hidden");
    }
}
