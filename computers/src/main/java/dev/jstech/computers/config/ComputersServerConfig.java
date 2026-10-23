/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.config;

import dev.jstech.computers.api.language.ExecutionBalance;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLink;
import dev.jstech.core.jstechseries.operation.OperationBalance;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import org.jetbrains.annotations.Nullable;

/**
 * The settings of the computers themselves, {@code jscomputers-server.toml} beside the world: how long their disks
 * take to answer, the share a Subframe lends its Mainframe, and how long their programs run, besides how they boot,
 * install, prompt and play music.
 *
 * <p>Apart from the Core's balance file and the series' Operations engine's: what is in here is about how the
 * machines of this mod behave, and the Core has no business knowing that a boot menu exists.
 */
public final class ComputersServerConfig {

    /* How fast a song comes over the Ethernet when nothing says otherwise, in kilobytes a second. */
    private static final int ETHERNET_SPEED = 512;
    private static final int MOST_SPEED = 1_048_576;
    /* The cable the Ethernet speed is set for; every other line and era goes in proportion to what it carries. */
    private static final DataLink ETHERNET = new DataLink(DataLine.ACCESS, HardwareEra.STANDARD);
    /** What the machines lend the Operations engine and how long their programs may run. */
    public static final String BALANCE = "balance";
    private static final int MAX_LATENCY_TICKS = 200;
    private static final int MIN_MACHINE_MICROS = 50;
    private static final int MAX_MACHINE_MICROS = 1_000_000;
    private static final int MIN_SERVER_MICROS = 100;
    private static final int MAX_SERVER_MICROS = 10_000_000;

    /** The seek latency of a hard disk drive, in ticks. */
    public static final ConfigKey<Integer> HDD_LATENCY_TICKS = ConfigKey.whole(BALANCE + ".hdd_latency_ticks",
                    OperationBalance.DEFAULT_HDD_LATENCY_TICKS).range(0, MAX_LATENCY_TICKS)
            .comment("How long a hard disk drive takes to find what it is asked for, in ticks.")
            .named("Hard disk latency").unit("ticks");
    /** The seek latency of a solid-state drive, in ticks. */
    public static final ConfigKey<Integer> SSD_LATENCY_TICKS = ConfigKey.whole(BALANCE + ".ssd_latency_ticks",
                    OperationBalance.DEFAULT_SSD_LATENCY_TICKS).range(0, MAX_LATENCY_TICKS)
            .comment("How long a solid-state drive takes to find what it is asked for, in ticks.")
            .named("SSD latency").unit("ticks");
    /** The seek latency of an NVMe drive, in ticks. */
    public static final ConfigKey<Integer> NVME_LATENCY_TICKS = ConfigKey.whole(BALANCE + ".nvme_latency_ticks",
                    OperationBalance.DEFAULT_NVME_LATENCY_TICKS).range(0, MAX_LATENCY_TICKS)
            .comment("How long an NVMe drive takes to find what it is asked for, in ticks.")
            .named("NVMe latency").unit("ticks");
    /** The share of a Subframe's own capacity it lends to the Mainframe orchestrating it. */
    public static final ConfigKey<Double> SUBFRAME_EFFICIENCY_FACTOR = ConfigKey.number(
                    BALANCE + ".subframe_efficiency_factor", OperationBalance.DEFAULT_SUBFRAME_EFFICIENCY_FACTOR)
            .range(0.0, 1.0)
            .comment("The share of its own capacity a Subframe lends the Mainframe it works for.")
            .named("Subframe share");
    /** Real time one machine may spend running its programs in a tick, in microseconds. */
    public static final ConfigKey<Integer> PROGRAM_MACHINE_MICROS = ConfigKey.whole(
                    BALANCE + ".program_machine_micros", ExecutionBalance.DEFAULT_MACHINE_MICROS)
            .range(MIN_MACHINE_MICROS, MAX_MACHINE_MICROS)
            .comment("The real time one machine may spend running its programs in a tick, in microseconds.")
            .named("Program time per machine").unit("µs");
    /** Real time every machine together may spend running programs in a tick, in microseconds. */
    public static final ConfigKey<Integer> PROGRAM_SERVER_MICROS = ConfigKey.whole(
                    BALANCE + ".program_server_micros", ExecutionBalance.DEFAULT_SERVER_MICROS)
            .range(MIN_SERVER_MICROS, MAX_SERVER_MICROS)
            .comment("The real time every machine together may spend running programs in a tick, in microseconds.")
            .named("Program time per server").unit("µs");

    public static final ConfigKey<Boolean> SHOW_BOOT_MENU = ConfigKey.flag("boot.show_boot_menu", true)
            .comment("Whether a machine whose system brings a boot manager stops at it every time it starts: GRUB "
                            + "on a Linux, the loader on FreeBSD, the Midsoft Boot Manager with two systems.",
                    "Turning this off boots the chosen system at once, the way a machine with the menu hidden does.")
            .named("Show the boot menu");

    public static final ConfigKey<Boolean> GENTOO_EVERY_STEP = ConfigKey.flag("install_by_hand.gentoo_every_step",
            false)
            .comment("Whether installing Gentoo by hand asks for every step of the handbook.",
                    "Off, only the steps a system cannot boot without are asked for: the disk, the stage 3, the "
                            + "package tree, a kernel, the filesystem table and the bootloader.",
                    "On, the rest are asked for as well: the bind mounts, a profile, the @world update, the kernel "
                            + "link and a name for the machine.",
                    "Every step answers the way the real tool does either way; this only decides which of them a "
                            + "restart refuses to go on without.")
            .named("Every step of Gentoo");

    public static final ConfigKey<Boolean> ARCH_EVERY_STEP = ConfigKey.flag("install_by_hand.arch_every_step", false)
            .comment("Whether installing Arch by hand asks for every step of the installation guide.",
                    "Off, only the steps a system cannot boot without are asked for. On, the hardware clock and a "
                            + "name for the machine are asked for as well.")
            .named("Every step of Arch");

    public static final ConfigKey<Boolean> LIST_COMMANDS = ConfigKey.flag("prompt.list_commands", false)
            .comment("Whether every computer has the 'listcmd' command, which lists absolutely everything that "
                            + "computer can run right now: its commands, whatever family they belong to, and the "
                            + "programs installed on it.",
                    "Off, it is nowhere at all: not in help, not in a manual, not in what a half-typed name "
                            + "completes to, and typing it is an unknown command. Each system then teaches what it "
                            + "has in its own way, which is the experience those systems really gave.",
                    "On, it is on every computer and shows up everywhere like any other command, for whoever would "
                            + "rather read one list than learn each system's own habits.")
            .named("The listcmd command");

    public static final ConfigKey<Boolean> SOUNDFOUNDRY_CATALOG = ConfigKey.flag("soundfoundry.catalog", true)
            .comment("Whether the server offers its music catalogue: the albums put in "
                            + "config/jstech/soundfoundry/catalog/, a folder each, and those the data packs carry in "
                            + "soundfoundry/catalog/.",
                    "Off, neither is read and the catalogue is empty. A change is taken up by '/soundfoundry "
                            + "catalog reload' or the next start.")
            .named("Music catalogue");

    public static final ConfigKey<Integer> ETHERNET_KILOBYTES_PER_SECOND = ConfigKey.whole(
            "soundfoundry.ethernet_kilobytes_per_second", ETHERNET_SPEED).range(1, MOST_SPEED)
            .comment("How fast a song comes over the network into a computer, in kilobytes a second, by the "
                            + "slowest cable on its way. A song from the catalogue comes at the speed of the cable the "
                            + "computer itself is plugged into; the songs coming in at once share it.",
                    "Every other cable, in every era, carries songs in proportion to what it carries "
                            + "in items a tick, so the faster the cable the faster the song.")
            .named("Songs over Ethernet")
            .unit("KB/s");

    public static final ConfigKey<Boolean> OUTSIDE_COMPONENTS = ConfigKey.flag("programs.outside_components", false)
            .comment("Whether a program may put in its windows a component of a kind that reaches outside the game, "
                            + "such as one showing a web address or a file on the player's own computer.",
                    "Such kinds come only from other mods, which say so when they add them. Off, a program making "
                            + "one is stopped and told; on, each player still sees them only if their own settings "
                            + "turn them on as well.")
            .named("Outside components");

    public static final ConfigFile FILE = ConfigFile.builder("jscomputers-server", ConfigSide.SERVER,
                    ConfigFormats.TOML)
            .comment("How the computers of J's Computers behave, and what they lend the Operations engine, whose own "
                    + "settings live in the series' file beside this one.")
            .sectionNamed(BALANCE, "Disks and programs")
            .sectionNamed("boot", "Starting up")
            .sectionNamed("install_by_hand", "Installing by hand")
            .sectionNamed("prompt", "The prompt")
            .sectionNamed("soundfoundry", "Soundfoundry")
            .sectionNamed("programs", "Programs")
            .sectionComment("boot", "How the machines come up: whether they stop at their system's boot manager.")
            .sectionComment("install_by_hand", "Installing a system by hand: how much of its guide the machine "
                    + "insists on.")
            .sectionComment("prompt", "What every computer's command line has, whatever system it runs.")
            .sectionComment("soundfoundry", "The music service: the catalogue the server offers, and how fast a song "
                    + "travels over each kind of cable into a computer.")
            .sectionComment("programs", "What the programs players write may put in their windows.")
            .sectionComment(BALANCE, "How long the disks take to answer the network, the share a Subframe lends its "
                    + "Mainframe, and how long the machines' programs may run each tick.")
            .key(HDD_LATENCY_TICKS, OperationBalance::setHddLatencyTicks)
            .key(SSD_LATENCY_TICKS, OperationBalance::setSsdLatencyTicks)
            .key(NVME_LATENCY_TICKS, OperationBalance::setNvmeLatencyTicks)
            .key(SUBFRAME_EFFICIENCY_FACTOR, OperationBalance::setSubframeEfficiencyFactor)
            .key(PROGRAM_MACHINE_MICROS, ExecutionBalance::setMachineMicros)
            .key(PROGRAM_SERVER_MICROS, ExecutionBalance::setServerMicros)
            .key(SHOW_BOOT_MENU)
            .key(GENTOO_EVERY_STEP)
            .key(ARCH_EVERY_STEP)
            .key(LIST_COMMANDS)
            .key(SOUNDFOUNDRY_CATALOG)
            .key(ETHERNET_KILOBYTES_PER_SECOND)
            .key(OUTSIDE_COMPONENTS)
            .build();

    private ComputersServerConfig() {
    }

    /** Puts the file beside the world, where its side reads it. */
    public static void register(final IEventBus modEventBus, final ModContainer modContainer) {
        ConfigFiles.register(FILE, modEventBus, modContainer);
    }

    /** Whether a machine whose system brings a boot manager stops at it on the way up. */
    public static boolean showBootMenu() {
        return FILE.get(SHOW_BOOT_MENU);
    }

    /** Whether installing Gentoo by hand asks for the whole handbook rather than only what a system boots by. */
    public static boolean gentooEveryStep() {
        return FILE.get(GENTOO_EVERY_STEP);
    }

    /** Whether installing Arch by hand asks for the whole guide rather than only what a system boots by. */
    public static boolean archEveryStep() {
        return FILE.get(ARCH_EVERY_STEP);
    }

    /** Whether every computer has {@code listcmd}, the one word that lists all it can run. */
    public static boolean listCommands() {
        return FILE.get(LIST_COMMANDS);
    }

    /** Whether a program may use a component of a kind that reaches outside the game. */
    public static boolean outsideComponents() {
        return FILE.get(OUTSIDE_COMPONENTS);
    }

    /** Whether the server offers its music catalogue. */
    public static boolean soundfoundryCatalog() {
        return FILE.get(SOUNDFOUNDRY_CATALOG);
    }

    /**
     * How fast a song comes over a way whose slowest cable is {@code link}, in bytes a second: the Ethernet's speed
     * scaled by how much the cable carries a tick against the Ethernet's, in every line and era, and the Ethernet's
     * own for a way not yet known.
     */
    public static long songBytesPerSecond(@Nullable final DataLink link) {
        return songBytesPerSecond(link, FILE.get(ETHERNET_KILOBYTES_PER_SECOND));
    }

    /** The same for a given Ethernet speed in kilobytes a second; a link that carries nothing goes at one byte. */
    public static long songBytesPerSecond(@Nullable final DataLink link, final int ethernetKilobytes) {
        final long base = ethernetKilobytes * 1_024L;
        if (link == null) {
            return base;
        }
        return Math.max(1L, base * link.throughput() / ETHERNET.throughput());
    }
}
