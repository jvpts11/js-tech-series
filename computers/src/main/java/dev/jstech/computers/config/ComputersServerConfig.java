/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.config;

import dev.jstech.computers.api.language.ExecutionBalance;
import dev.jstech.computers.hardware.ThermalModel;
import dev.jstech.computers.workshop.UpdateCost;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.id.StableNames;
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
    private static final StableNames<UpdateCost> COSTS = StableNames.of(UpdateCost.class);
    private static final String UPDATE_COST_PATH = "update.cost";
    private static final double MIN_PSU_LIFE = 0.1;
    private static final double MAX_PSU_LIFE = 10.0;

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

    public static final ConfigKey<Boolean> DRAW_ENERGY = ConfigKey.flag("power.draw_energy", true)
            .comment("Whether the computers draw energy for what their parts take, through their power supplies: "
                            + "J's Energy in the low voltage class, or FE where J's Core lets the series take it. "
                            + "A computer without it does not start, and one that loses it goes dark.",
                    "Off, every computer runs free, and so do the chain's benches and machines, for a world with no"
                            + " energy source at all.")
            .named("Computers draw energy");

    public static final ConfigKey<String> UPDATE_COST = ConfigKey.text(UPDATE_COST_PATH,
                    UpdateCost.ENERGY.serializedName())
            .allowing(UpdateCost.ENERGY.serializedName(), UpdateCost.XP.serializedName(),
                    UpdateCost.BOTH.serializedName(), UpdateCost.NONE.serializedName())
            .comment("What the personal-use cards' work is paid with, an UPDATE's and the Workshop's alike.",
                    "energy: the computer that has the cards draws it: smelting 2 kJ an item, enchanting 10 kJ for "
                            + "every level the enchanting table would take, repairing 2 kJ for every percent of "
                            + "durability given back, combining 10 kJ for every level the anvil would ask, naming "
                            + "5 kJ.",
                    "xp: whoever asked pays half the levels the game's own block would take, at least one, and no "
                            + "lapis lazuli. both: the energy and those levels. none: nothing.")
            .named("What the cards cost");

    public static final ConfigKey<Boolean> PSU_FAILURES = ConfigKey.flag("hardware.psu_failures", true)
            .comment("Whether power supplies wear out while they are on and in the end fail, faster the nearer their "
                            + "rating they run: degraded from 80% worn, with warnings at every boot, and failed at "
                            + "100%, when the computer shuts down and does not start until the supply is replaced.",
                    "Off, no supply wears, warns or fails.")
            .named("Power supplies wear out");
    public static final ConfigKey<Double> PSU_LIFE_MULTIPLIER = ConfigKey.number("hardware.psu_life_multiplier",
                    1.0).range(MIN_PSU_LIFE, MAX_PSU_LIFE)
            .comment("How much longer than their lines' lives every power supply lasts: 2 is twice as long, 0.5 half.")
            .named("Power supply life")
            .activeWhen(PSU_FAILURES);

    public static final ConfigKey<Integer> AMBIENT_BASE = ConfigKey.whole("temperature.ambient_base", 20)
            .range(-40, 60)
            .comment("The surroundings a computer stands in where the biome's own temperature is 0.8, as in the "
                    + "plains, in °C: a computer runs that much plus what its load raises it by.")
            .named("Surroundings")
            .unit("°C");
    public static final ConfigKey<Integer> BIOME_FACTOR = ConfigKey.whole("temperature.biome_factor", 15)
            .range(0, 40)
            .comment("How many °C the surroundings rise for every point of the biome's own temperature: about 38 °C "
                    + "in the desert and 8 °C in the snow at the defaults.")
            .named("Biome warmth")
            .unit("°C");
    public static final ConfigKey<Integer> NETHER_EXTRA = ConfigKey.whole("temperature.nether_extra", 10)
            .range(0, 60)
            .comment("How many °C the Nether adds to the surroundings, over its biomes' own warmth.")
            .named("Nether warmth")
            .unit("°C");
    public static final ConfigKey<Integer> RESTART_BELOW_C = ConfigKey.whole("temperature.restart_below_c", 60)
            .range(30, 95)
            .comment("How cool a computer that tripped from its heat has to be before it starts again, in °C.")
            .named("Restart below")
            .unit("°C");

    public static final ConfigKey<Boolean> LOSE_ON_POWER_LOSS = ConfigKey.flag("item_loss.power_loss", true)
            .comment("Whether what the Operations were carrying is lost when the machine running them goes dark "
                            + "suddenly: a power loss, unstable power or a power supply failing. The log says how "
                            + "much and of what.",
                    "Off, a sudden shutdown gives everything back, as switching the machine off does.")
            .named("Lost on a power loss");
    public static final ConfigKey<Boolean> LOSE_ON_NETWORK_SPLIT = ConfigKey.flag("item_loss.network_split", true)
            .comment("Whether what an Operation was carrying is lost when the network splits under it: a cable pulled "
                            + "between the Mainframe and a server it was reading or writing, a router that disappears.",
                    "Off, the Operation stops and gives everything back.")
            .named("Lost when the network splits");
    public static final ConfigKey<Boolean> LOSE_ON_NETWORK_CONFLICT = ConfigKey.flag("item_loss.network_conflict",
            true)
            .comment("Whether a network conflict, two Mainframes on one network, costs: what the discarded Operations "
                            + "were carrying is lost, and a tenth of what both networks store is corrupted, waiting "
                            + "for the Defrag.",
                    "Off, a conflict gives everything back and corrupts nothing.")
            .named("A network conflict costs");
    public static final ConfigKey<Boolean> CORRUPTION = ConfigKey.flag("item_loss.corruption", true)
            .comment("Whether storage can be corrupted at all: by a network conflict, or by a thermal trip, which may "
                            + "corrupt the disks of the machine that shut down. Corrupted items leave the network's "
                            + "index until the Defrag gives them back or loses them.",
                    "Off, nothing is ever corrupted.")
            .named("Storage corruption");

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

    public static final ConfigKey<Integer> LOST_DEVICE_HOURS = ConfigKey.whole("wireless.lost_device_hours", 72)
            .range(1, 8_760)
            .comment("How long a portable device may be out of every player's inventory and every charging dock "
                            + "before the server lets go of what was on it, in hours of the server running: its apps, "
                            + "its alerts, its storage and its pictures.",
                    "A device whose item is destroyed (lava, fire, the void, despawning) is let go at once.")
            .named("Lost devices")
            .unit("h");

    /** Whether J's Computers' own chain is the path to its parts: its machines' recipes and its veins. */
    public static final ConfigKey<Boolean> OWN_CHAIN = ConfigKey.flag("survival.own_chain", true)
            .comment("Whether J's Computers' own chain is laid: the recipes of its chain's machines, from the ore to "
                            + "the chip, and the veins of its ores. Off, its parts are made by J's Industrial's chain "
                            + "where J's Industrial is installed, or found and bought only.",
                    "The veins of a world are laid as it was made: a change shows in chunks made afterwards.")
            .named("Own chain");

    public static final ConfigFile FILE = ConfigFile.builder("jscomputers-server", ConfigSide.SERVER,
                    ConfigFormats.TOML)
            .comment("How the computers of J's Computers behave, and what they lend the Operations engine, whose own "
                    + "settings live in the series' file beside this one.")
            .sectionNamed(BALANCE, "Disks and programs")
            .sectionNamed("power", "Power")
            .sectionNamed("update", "Changing items")
            .sectionNamed("hardware", "Hardware")
            .sectionNamed("temperature", "Temperature")
            .sectionNamed("item_loss", "Items lost")
            .sectionNamed("boot", "Starting up")
            .sectionNamed("install_by_hand", "Installing by hand")
            .sectionNamed("prompt", "The prompt")
            .sectionNamed("soundfoundry", "Soundfoundry")
            .sectionNamed("programs", "Programs")
            .sectionNamed("wireless", "Wireless")
            .sectionNamed("survival", "Survival")
            .sectionComment("survival", "How a player in survival comes by the computers: the mod's own chain.")
            .sectionComment("wireless", "The portable devices: how long a lost one keeps what was on it.")
            .sectionComment("power", "What the computers run on: the energy their power supplies draw.")
            .sectionComment("update", "What the personal-use cards charge for changing an item: smelting, "
                    + "enchanting, repairing, combining and naming.")
            .sectionComment("hardware", "How the parts age: the power supplies' wear and their failure.")
            .sectionComment("temperature", "How warm the surroundings a computer stands in are, by the biome and the "
                    + "Nether, and how cool one that tripped from its heat has to be to start again. Temperature "
                    + "itself has no switch: a computer cooled well never feels it.")
            .sectionComment("item_loss", "What a player who lets the network fail pays for it: the items in "
                    + "transit, lost in each situation turned on here.")
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
            .key(DRAW_ENERGY)
            .key(UPDATE_COST)
            .key(PSU_FAILURES)
            .key(PSU_LIFE_MULTIPLIER)
            .key(AMBIENT_BASE)
            .key(BIOME_FACTOR)
            .key(NETHER_EXTRA)
            .key(RESTART_BELOW_C)
            .key(LOSE_ON_POWER_LOSS)
            .key(LOSE_ON_NETWORK_SPLIT)
            .key(LOSE_ON_NETWORK_CONFLICT)
            .key(CORRUPTION)
            .key(SHOW_BOOT_MENU)
            .key(GENTOO_EVERY_STEP)
            .key(ARCH_EVERY_STEP)
            .key(LIST_COMMANDS)
            .key(SOUNDFOUNDRY_CATALOG)
            .key(ETHERNET_KILOBYTES_PER_SECOND)
            .key(OUTSIDE_COMPONENTS)
            .key(LOST_DEVICE_HOURS)
            .key(OWN_CHAIN)
            .build();

    private ComputersServerConfig() {
    }

    /** Puts the file beside the world, where its side reads it. */
    public static void register(final IEventBus modEventBus, final ModContainer modContainer) {
        ConfigFiles.register(FILE, modEventBus, modContainer);
    }

    /** Whether the computers draw energy for what their parts take, and the chain's benches and machines too. */
    public static boolean drawEnergy() {
        return FILE.get(DRAW_ENERGY);
    }

    /** Whether the mod's own chain is laid, as a world's data reads it while it loads and once it runs. */
    public static boolean ownChain() {
        return ConfigFiles.forData(FILE).get(OWN_CHAIN);
    }

    /** How long a portable may be out of every inventory and dock before what was on it is let go, in hours. */
    public static int lostDeviceHours() {
        return FILE.get(LOST_DEVICE_HOURS);
    }

    /** What the personal-use cards' work is paid with. */
    public static UpdateCost updateCost() {
        return COSTS.byName(FILE.get(UPDATE_COST), UpdateCost.ENERGY);
    }

    /** Whether power supplies wear out and fail. */
    public static boolean psuFailures() {
        return FILE.get(PSU_FAILURES);
    }

    /** How much longer than their lines' lives every power supply lasts. */
    public static double psuLifeMultiplier() {
        return FILE.get(PSU_LIFE_MULTIPLIER);
    }

    /**
     * The surroundings in °C at a place of the biome temperature {@code biome}, in the Nether when {@code nether}: the
     * settings' base, warmth for every point of the biome and the Nether's own.
     */
    public static double ambient(final double biome, final boolean nether) {
        return ThermalModel.ambient(FILE.get(AMBIENT_BASE), FILE.get(BIOME_FACTOR), biome, FILE.get(NETHER_EXTRA),
                nether);
    }

    /** How cool a computer that tripped from its heat has to be before it starts again, in °C. */
    public static int restartBelow() {
        return FILE.get(RESTART_BELOW_C);
    }

    /** Whether what the Operations carry is lost when the machine running them goes dark suddenly. */
    public static boolean loseOnPowerLoss() {
        return FILE.get(LOSE_ON_POWER_LOSS);
    }

    /** Whether what an Operation carries is lost when the network splits under it. */
    public static boolean loseOnNetworkSplit() {
        return FILE.get(LOSE_ON_NETWORK_SPLIT);
    }

    /** Whether a network conflict loses what its Operations carried and corrupts both networks' storage. */
    public static boolean conflictCosts() {
        return FILE.get(LOSE_ON_NETWORK_CONFLICT);
    }

    /** Whether storage can be corrupted at all. */
    public static boolean corruption() {
        return FILE.get(CORRUPTION);
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

    /** How fast a song comes over a way that carries {@code itemsPerTick}, a Wi-Fi's, scaled as a cable's is. */
    public static long songBytesPerSecond(final long itemsPerTick) {
        final long base = FILE.get(ETHERNET_KILOBYTES_PER_SECOND) * 1_024L;
        return Math.max(1L, base * itemsPerTick / ETHERNET.throughput());
    }
}
