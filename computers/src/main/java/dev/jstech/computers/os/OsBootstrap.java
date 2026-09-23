/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.os.install.InstallerStyle;
import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;

import static dev.jstech.core.tier.HardwareEra.LEGACY;
import static dev.jstech.core.tier.HardwareEra.STANDARD;
import static dev.jstech.core.tier.HardwareEra.VINTAGE;
import dev.jstech.computers.api.ComputersRegisterEvent;
import dev.jstech.computers.api.JsComputersApi;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Optional;

/**
 * The kernels, operating systems, desktops and programs the mod brings of its own.
 *
 * <p>Every one of them is added the way an addon adds one, by listening for the same event, so the way in
 * is the one that is tried every time the game starts rather than a path only addons take.
 */
@EventBusSubscriber(modid = "jsc", bus = EventBusSubscriber.Bus.MOD)
public final class OsBootstrap {

    private OsBootstrap() {}

    @SubscribeEvent
    public static void onRegister(final ComputersRegisterEvent event) {
        registerBuiltins();
    }

    // Built-in registrations

    static void registerBuiltins() {
        registerKernels();
        registerOses();
        registerDesktops();
        registerSpaces();
        registerPrograms();
    }

    /**
     * The built-in kernels. A static list (built eagerly) so both the runtime registration and datagen read
     * from one source. dos = MS-DOS 2.0+ (real directories); win9x = cooperative; nt = preemptive (XP + 11);
     * net_min = the minimal network kernel with no scheduler or filesystem.
     */
    private static final List<KernelDef> BUILTIN_KERNELS = List.of(
            new KernelDef(rl("dos"), SchedulerKind.NONE, FilesystemKind.HIERARCHICAL, ShellFamily.DOS),
            new KernelDef(rl("win9x"), SchedulerKind.COOPERATIVE, FilesystemKind.HIERARCHICAL, ShellFamily.DOS),
            new KernelDef(rl("nt"), SchedulerKind.PREEMPTIVE, FilesystemKind.HIERARCHICAL, ShellFamily.DOS),
            /*
             * The network kernel: one task at a time, and a flat store of files with no folders in it. Flat
             * rather than none, because a network appliance that cannot keep a file cannot keep the pattern
             * that teaches its network a recipe, nor the file that starts itself. Flat rather than a tree,
             * because a bag of files beside a GUI that is the whole network is what this machine is, where
             * MC-DOS of the same age is a personal computer with folders.
             */
            new KernelDef(rl("net_min"), SchedulerKind.NONE, FilesystemKind.FLAT, ShellFamily.NET),
            /*
             * The Linux kernel: preemptive, a single rooted hierarchical filesystem, and POSIX shell syntax.
             * Public through the addon API, so any add-on distribution built on it speaks bash for free.
             */
            new KernelDef(rl("linux"), SchedulerKind.PREEMPTIVE, FilesystemKind.HIERARCHICAL, ShellFamily.POSIX),
            /*
             * FreeBSD's own kernel. To a player at a prompt it reads the same as the one above, and that is
             * the whole of what they share: nothing built for one runs on the other.
             */
            new KernelDef(rl("freebsd"), SchedulerKind.PREEMPTIVE, FilesystemKind.HIERARCHICAL, ShellFamily.POSIX),
            /*
             * System V's kernel: the one here that ran several programs at once on a machine of the first age,
             * and the oldest thing in this list that a Unix prompt is met on.
             */
            new KernelDef(rl("unix"), SchedulerKind.PREEMPTIVE, FilesystemKind.HIERARCHICAL, ShellFamily.POSIX));

    /**
     * The built-in operating systems, in install order. A static list (built eagerly) so datagen (lang and the
     * OS install media) reads the same source as the runtime registration.
     */
    /*
     * A system's size is in megabytes, the way its box would print it; what it costs in items depends on the
     * disk it lands on (1 MB an item at 16 bits, 16 MB at 32, 256 MB at 64). MC-DOS fills a fifth of a
     * 20 MB vintage drive; Frames 11 is 80 items of a standard disk and will not fit a vintage one at all.
     */
    private static final List<OsDef> BUILTIN_OSES = List.of(
            /*
             * MC-DOS: terminal-only CLI shell from the Vintage era.
             * Each system also says the RAM it holds for itself while running (withRam): a fifth of a
             * vintage machine's few megabytes for the DOS family, most of a small Legacy machine for XP,
             * a good part of a gigabyte for Frames 11. Its bundled programs weigh a quarter of that each.
             */
            OsDef.mediaInstalled(rl("mc_dos"), OsCapability.TERMINAL_ONLY, HardwareEra.VINTAGE, rl("dos"), 4,
                    Platform.MC_DOS, "MC-DOS", Optional.empty(), SoftwareHouse.MIDSOFT).withRam(1)
                    .withInstaller(InstallerStyle.MC_DOS),
            // MC-NET: full-screen network GUI (the rewrapped network interactor), from the Vintage era.
            OsDef.mediaInstalled(rl("mc_net"), OsCapability.NETWORK_GUI, HardwareEra.VINTAGE, rl("net_min"), 8,
                    Platform.MC_NET, "MC-NET", Optional.empty(), SoftwareHouse.NOUVELL).withRam(2)
                    .withInstaller(InstallerStyle.MC_NET),
            /*
             * The Frames editions bundle their own desktop environment (the id doubles as the DE id), and they
             * are the one family here with an order to it: each says where it sits (withRank), so a program can
             * ask for XP or newer without anything but these three lines knowing which is newer than which.
             */
            OsDef.mediaInstalled(rl("frames_95"), OsCapability.FULL_DESKTOP, HardwareEra.LEGACY, rl("win9x"), 48,
                    Platform.FRAMES, "Frames 95", Optional.of(rl("frames_95")), SoftwareHouse.MIDSOFT).withRam(16)
                    .withInstaller(InstallerStyle.FRAMES_95).withRank(1),
            OsDef.mediaInstalled(rl("frames_xp"), OsCapability.FULL_DESKTOP, HardwareEra.LEGACY, rl("nt"), 1_536,
                    Platform.FRAMES, "Frames XP", Optional.of(rl("frames_xp")), SoftwareHouse.MIDSOFT).withRam(64)
                    .withInstaller(InstallerStyle.FRAMES_XP).withRank(2),
            OsDef.mediaInstalled(rl("frames_11"), OsCapability.FULL_DESKTOP, HardwareEra.STANDARD, rl("nt"), 20_480,
                    Platform.FRAMES, "Frames 11", Optional.of(rl("frames_11")), SoftwareHouse.MIDSOFT).withRam(768)
                    .withInstaller(InstallerStyle.FRAMES_11).withRank(3),

            /*
             * Linux distributions: all on the Linux kernel, all boot to a bash TTY until a desktop environment
             * is installed from the network mirror. Footprints are balancing estimates. The shells and package
             * managers are each distribution's real ones; Arch and Gentoo keep their manual installs. Each is
             * credited to its own house, the way the machines are credited to their makers.
             * A distribution's own share is its base system at the TTY; the desktop package it installs
             * weighs on top of it (the desktop environments below say how much).
             */
            OsDef.linuxDistro(rl("ubuntu"), 8_192, "Ubuntu", ShellKind.BASH, PackageManagerKind.APT,
                    InstallMode.GUIDED, SoftwareHouse.AXIOMATIC).withRam(48).withInstaller(InstallerStyle.UBUNTU),
            OsDef.linuxDistro(rl("debian"), 4_096, "Debian", ShellKind.BASH, PackageManagerKind.APT,
                    InstallMode.GUIDED, SoftwareHouse.DEBIAN_CIRCLE).withRam(24).withInstaller(InstallerStyle.DEBIAN),
            OsDef.linuxDistro(rl("fedora"), 8_192, "Fedora", ShellKind.BASH, PackageManagerKind.DNF,
                    InstallMode.GUIDED, SoftwareHouse.RED_CAP).withRam(48).withInstaller(InstallerStyle.FEDORA),
            OsDef.linuxDistro(rl("arch"), 2_048, "Arch Linux", ShellKind.ZSH, PackageManagerKind.PACMAN,
                    InstallMode.LIVE_MANUAL, SoftwareHouse.ARCH_COLLECTIVE).withRam(12),
            OsDef.linuxDistro(rl("gentoo"), 4_096, "Gentoo", ShellKind.BASH, PackageManagerKind.EMERGE,
                    InstallMode.SOURCE, SoftwareHouse.GENTOO_FOUNDRY).withRam(12),
            /*
             * FreeBSD: the solid server and the lean daily driver. It asks for a machine of the Legacy age at
             * least and runs on every one after, it takes less memory than any distribution so the same
             * machine keeps more for its programs, and it comes up at a terminal until a desktop is installed.
             */
            OsDef.terminalSystem(rl("freebsd"), rl("freebsd"), Platform.FREEBSD, HardwareEra.LEGACY, 2_048,
                    "FreeBSD", ShellKind.SH, PackageManagerKind.PKG, InstallMode.GUIDED,
                    SoftwareHouse.DAEMON_FOUNDATION)
                    .withRam(16),
            /*
             * UNIX System V: the one system of the first age that runs several programs at once, in ten
             * megabytes of disk and two of memory. It has no Mirror to install from, only media, which is the
             * price of what it can do on a machine that small.
             */
            OsDef.terminalSystem(rl("unix"), rl("unix"), Platform.UNIX, HardwareEra.VINTAGE, 10,
                    "UNIX System V", ShellKind.SH, PackageManagerKind.NONE, InstallMode.GUIDED,
                    SoftwareHouse.BELLWETHER_LABS)
                    .withRam(2)
            /*
             * OS case (c): PDA/Tablet/Smartphone portables ship with a factory mobile OS. Those item/block
             * types do not exist yet; register the mobile OS here once they do.
             */
    );

    /** The built-in kernels, so datagen and tooling read them from one source. */
    public static List<KernelDef> builtinKernels() {
        return BUILTIN_KERNELS;
    }

    /** The built-in operating systems, so datagen (lang, install media) reads them from one source. */
    public static List<OsDef> builtinOses() {
        return BUILTIN_OSES;
    }

    private static void registerKernels() {
        for (final KernelDef kernel : BUILTIN_KERNELS) {
            JsComputersApi.registerKernel(kernel);
        }
    }

    private static void registerOses() {
        for (final OsDef os : BUILTIN_OSES) {
            JsComputersApi.registerOperatingSystem(os);
        }
    }

    /**
     * Desktop apps that run under any desktop environment: the Frames editions and a Linux desktop alike. On
     * Frames they arrive on install media; on Linux the same programs are packages the Mirror serves.
     */
    private static final Set<Platform> DESKTOPS =
            Set.of(Platform.FRAMES, Platform.LINUX, Platform.FREEBSD, Platform.UNIX);
    /**
     * The same without UNIX, for the few desktop programs written for machines later than any UNIX desktop
     * reaches: the automation front-end, which asks for the newest Frames, and the two later code editors.
     */
    private static final Set<Platform> LATER_DESKTOPS = Set.of(Platform.FRAMES, Platform.LINUX, Platform.FREEBSD);
    private static final Set<Platform> LINUX_ONLY = Set.of(Platform.LINUX);
    /**
     * Where CDE runs: on UNIX, which has no other desktop, and on FreeBSD and the Linux distributions beside the
     * ones they already take. It was written for the Unix workstations and has been built for the others since.
     */
    private static final Set<Platform> CDE_SYSTEMS = Set.of(Platform.UNIX, Platform.FREEBSD, Platform.LINUX);
    /**
     * What is made for the systems met at a Unix prompt, whichever of them it is: the desktop environments and
     * the small tools the Mirror serves. FreeBSD takes all of it from its own packages and ports.
     */
    private static final Set<Platform> LINUX_AND_FREEBSD = Set.of(Platform.LINUX, Platform.FREEBSD);
    private static final Set<Platform> FRAMES_ONLY = Set.of(Platform.FRAMES);
    /** Where screenfetch installs: the systems at a Unix prompt that take packages, and Frames. */
    private static final Set<Platform> SCREENFETCH_SYSTEMS = Set.of(Platform.LINUX, Platform.FREEBSD, Platform.FRAMES);

    /** The nine built-in desktop apps every desktop environment can bundle, in rail order. */
    private static final List<ResourceLocation> BUILTIN_APPS = List.of(
            rl("network"), rl("this_pc"), rl("settings"), rl("files"), rl("editor"), rl("command_prompt"),
            rl("system_monitor"), rl("calculator"), rl("network_manager"));

    /** What CDE bundles: the same, and its Workstation Info, which no other desktop has. */
    private static final List<ResourceLocation> CDE_APPS = List.of(
            rl("network"), rl("settings"), rl("files"), rl("editor"), rl("command_prompt"),
            rl("system_monitor"), rl("calculator"), rl("network_manager"), rl("workstation_info"));

    /**
     * The built-in desktop environments. The Frames editions bundle their own (the id equals the OS id, so the
     * existing skins and icon sets keep their keys); KDE Plasma, GNOME and Cinnamon are Linux packages, each
     * with its chrome and the native names its bundled apps show.
     */
    private static final List<DesktopEnvironmentDef> BUILTIN_DESKTOPS = List.of(
            new DesktopEnvironmentDef(rl("frames_95"), "Frames 95", PanelStyle.FRAMES_95, BUILTIN_APPS, Map.of(),
                    SoftwareHouse.MIDSOFT),
            new DesktopEnvironmentDef(rl("frames_xp"), "Frames XP", PanelStyle.FRAMES_XP, BUILTIN_APPS, Map.of(),
                    SoftwareHouse.MIDSOFT),
            new DesktopEnvironmentDef(rl("frames_11"), "Frames 11", PanelStyle.FRAMES_11, BUILTIN_APPS, Map.of(),
                    SoftwareHouse.MIDSOFT),
            new DesktopEnvironmentDef(rl("kde_plasma"), "KDE Plasma", PanelStyle.KDE, BUILTIN_APPS, Map.of(
                    rl("files"), "Dolphin", rl("editor"), "Kate", rl("command_prompt"), "Konsole",
                    rl("calculator"), "KCalc", rl("system_monitor"), "System Monitor",
                    rl("settings"), "System Settings", rl("this_pc"), "Info Center"), SoftwareHouse.KDE_GUILD),
            new DesktopEnvironmentDef(rl("gnome"), "GNOME", PanelStyle.GNOME, BUILTIN_APPS, Map.of(
                    rl("files"), "Files", rl("editor"), "Text Editor", rl("command_prompt"), "Terminal",
                    rl("calculator"), "Calculator", rl("system_monitor"), "System Monitor",
                    rl("settings"), "Settings", rl("this_pc"), "About"), SoftwareHouse.GNOME_TRUST),
            new DesktopEnvironmentDef(rl("cinnamon"), "Cinnamon", PanelStyle.CINNAMON, BUILTIN_APPS, Map.of(
                    rl("files"), "Nemo", rl("editor"), "xed", rl("command_prompt"), "Terminal",
                    rl("calculator"), "Calculator", rl("system_monitor"), "System Monitor",
                    rl("settings"), "System Settings", rl("this_pc"), "System Info"), SoftwareHouse.SPEARMINT),
            /*
             * CDE keeps its own names for what it bundles, which are plainer than anybody else's: it called a
             * file manager the File Manager. Its settings are the Style Manager, as they were.
             */
            new DesktopEnvironmentDef(rl("cde"), "CDE", PanelStyle.CDE, CDE_APPS, Map.of(
                    rl("files"), "File Manager", rl("editor"), "Text Editor", rl("command_prompt"), "Terminal",
                    rl("calculator"), "Calculator", rl("system_monitor"), "Performance Meter",
                    rl("settings"), "Style Manager"),
                    SoftwareHouse.OPEN_DESK_CONSORTIUM)
    );

    /** The built-in desktop environments, so tooling reads them from one source. */
    public static List<DesktopEnvironmentDef> builtinDesktops() {
        return BUILTIN_DESKTOPS;
    }

    private static void registerDesktops() {
        for (final DesktopEnvironmentDef desktop : BUILTIN_DESKTOPS) {
            JsComputersApi.registerDesktop(desktop);
        }
    }

    /**
     * The built-in operating spaces: the one MC-NET ships with, and the only one the mod has.
     *
     * <p>Its name is the Network Interactor's, because it is the same thing that runs as a window on a desktop
     * and at the prompt: one way of working a network, drawn wherever the machine can draw it.
     */
    private static final List<OperatingSpaceDef> BUILTIN_SPACES = List.of(
            new OperatingSpaceDef(rl("interactor"), "Interactor", SoftwareHouse.NOUVELL)
    );

    /** The built-in operating spaces, so tooling reads them from one source. */
    public static List<OperatingSpaceDef> builtinSpaces() {
        return BUILTIN_SPACES;
    }

    private static void registerSpaces() {
        for (final OperatingSpaceDef space : BUILTIN_SPACES) {
            JsComputersApi.registerSpace(space);
        }
    }

    /** Where an operating space runs: on a network system, which is the only kind that has one. */
    private static final Set<Platform> NET_ONLY = Set.of(Platform.MC_NET);

    /* What runs on every system, at a terminal as well as on a desktop. */
    private static final Set<Platform> ALL_PLATFORMS = Set.of(Platform.MC_DOS, Platform.MC_NET, Platform.FRAMES,
            Platform.LINUX, Platform.FREEBSD, Platform.UNIX);

    /** Every system whose prompt is a thing that can be opened, which is every one but the network's. */
    private static final Set<Platform> PROMPT_PLATFORMS = Set.of(Platform.MC_DOS, Platform.FRAMES,
            Platform.LINUX, Platform.FREEBSD, Platform.UNIX);

    /**
     * The built-in program descriptors, in desktop launcher order (the built-in apps first, then the
     * installables). Built eagerly as a static list so both the runtime registration and datagen (which does
     * not run common setup) read from the same single source. Hardware minimums are conservative balancing
     * estimates; {@code hostScope} replaces the old per-program install special cases.
     */
    private static final List<ProgramSpec> BUILTIN_PROGRAMS = List.of(
            /*
             * Built-in Frames apps: pre-installed, no install disc, always present on a Frames desktop (subject
             * to host scope and OS rank). These used to be a hardcoded launcher list.
             * The built-in apps carry no house of their own: Files is Midsoft's on Frames and the KDE Guild's on
             * Plasma. The network tools are the exception: they are the hardware house's wherever they run.
             */
            ProgramSpec.of(rl("network"), "network", true, DESKTOPS, 0, ProgramKind.APP, 0, HostScope.ANY)
                    .named("Network").described("Browse the storage and machines on this computer's network.")
                    .withHouse(SoftwareHouse.JSC),
            /*
             * "This PC" is a Frames idea and stays one. Linux has no single such place: its volumes
             * live in the file manager's device list and the detail in a disks utility, which is what
             * the Disks program below is.
             */
            ProgramSpec.of(rl("this_pc"), "thispc", true, FRAMES_ONLY, 0, ProgramKind.APP, 0, HostScope.ANY)
                    .named("This PC").described("The machine itself: its hardware, its disks and what fills them."),
            ProgramSpec.of(rl("disks"), "disks", true, LINUX_ONLY, 0, ProgramKind.APP, 0, HostScope.ANY)
                    .named("Disks").described("The volumes attached to this machine, and what occupies each one."),
            /*
             * CDE's own answer to "what is this machine": who is at it, the system and the hardware. Only CDE
             * bundles it, on every system CDE stands on; the command is named the way CDE named its tools.
             */
            ProgramSpec.of(rl("workstation_info"), "dtwsinfo", true, CDE_SYSTEMS, 0, ProgramKind.APP, 0, HostScope.ANY)
                    .named("Workstation Info")
                    .described("Who is at this workstation, the system it runs and the hardware it runs on.")
                    .withHouse(SoftwareHouse.OPEN_DESK_CONSORTIUM),
            /*
             * The Help Viewer, which is CDE's own and is named the way CDE named it. What it shows is the
             * machine's manual pages, so it ships with the desktop rather than being installed.
             */
            ProgramSpec.of(rl("help_viewer"), "dthelpview", true, CDE_SYSTEMS, 0, ProgramKind.APP, 0, HostScope.ANY)
                    .named("Help Viewer").described("The machine's manual pages, read in a window.")
                    .withHouse(SoftwareHouse.OPEN_DESK_CONSORTIUM),
            ProgramSpec.of(rl("settings"), "settings", true, DESKTOPS, 0, ProgramKind.APP, 0, HostScope.ANY)
                    .named("Settings").described("Change how this computer looks and behaves."),
            ProgramSpec.of(rl("files"), "files", true, DESKTOPS, 0, ProgramKind.APP, 0, HostScope.ANY)
                    .named("Files").described("Browse, open and organise the files on this computer's disks."),
            ProgramSpec.of(rl("editor"), "editor", true, DESKTOPS, 0, ProgramKind.APP, 0, HostScope.ANY)
                    .named("Editor").described("Write and edit text files."),
            /*
             * The Command Prompt ships with every computer that has somewhere to put it: a terminal on MC-DOS,
             * a window on a desktop. Not on a network system, whose interface is the whole screen and whose
             * prompt is a heading inside it: a window opened there would be the machine drawing a window onto
             * itself, which is the one thing that interface does not do.
             */
            ProgramSpec.of(rl("command_prompt"), "cmd", true, PROMPT_PLATFORMS, 0, ProgramKind.APP, 0, HostScope.ANY)
                    .named("Command Prompt").described("A shell: everything the machine can do, typed."),
            ProgramSpec.of(rl("system_monitor"), "sysmon", true, DESKTOPS, 0, ProgramKind.APP, 0, HostScope.ANY)
                    .named("System Monitor").described("Live load, memory and running work on this machine."),
            ProgramSpec.of(rl("calculator"), "calc", true, DESKTOPS, 0, ProgramKind.APP, 0, HostScope.ANY)
                    .named("Calculator").described("A calculator."),
            // The Network Manager is pre-installed but exclusive to the Mainframe, and needs Frames XP or newer.
            ProgramSpec.of(rl("network_manager"), "netmgr", true, DESKTOPS, 0, ProgramKind.APP, 2, HostScope.MAINFRAME)
                    .named("Network Manager")
                    .described("The Mainframe's control room: nodes, storage and operations across the network.")
                    .withHouse(SoftwareHouse.JSC),
            /*
             * The Task Manager ships with every desktop but keeps off the desktop and the Start menu: it is
             * reached by right-clicking the panel, the way it always was, so it is not in the bundled list.
             */
            ProgramSpec.of(rl("task_manager"), "taskmgr", true, DESKTOPS, 0, ProgramKind.APP, 0, HostScope.ANY)
                    .named("Task Manager")
                    .described("What this machine is running, what it is spending, and how to end it."),

            /*
             * Installables. The OS rank only gates the Frames editions (a Linux distribution ranks 0, so any
             * desktop program the Mirror serves installs on it once a desktop environment is present).
             * Network Management Studio: a professional network tool -> Frames XP or newer (rank 2).
             * Every installable also says the generation it was WRITTEN in (withEra): that decides the
             * medium it ships on and the year on its banner, and never where it may install. The OS rank
             * stays the gate. A modern tool that still runs on XP is Standard-era software on a DVD.
             * Each installable also names its house (withHouse): the maker on its disc and its banner.
             * And each says the RAM it holds while it runs (withRam): the balancing estimates follow the
             * generation the tool was written in, so a modern tool is the heavier one.
             */
            ProgramSpec.of(rl("nms"), "nms", false, DESKTOPS, 128, ProgramKind.APP, 2, HostScope.ANY)
                    .named("Network Management Studio")
                    .described("Query the network in IQL, inspect the index and run maintenance from one console.")
                    .withEra(STANDARD).withHouse(SoftwareHouse.MIDSOFT).withRam(128),
            /*
             * The IQL Engine is a headless Mainframe service (the NMS is its client): every platform, lives on
             * the Mainframe, follows the NMS OS version (Frames XP or newer).
             */
            ProgramSpec.of(rl("iqlengine"), "iqlengine", false, ALL_PLATFORMS, 32, ProgramKind.SERVICE, 2,
                            HostScope.MAINFRAME)
                    .named("IQL Engine")
                    .described("The service that compiles and runs IQL on the Mainframe. The NMS is its front end.")
                    .withEra(LEGACY).withHouse(SoftwareHouse.MIDSOFT).withRam(24),
            // The Crafting Manager installs only on a Crafting Computer -> Frames XP or newer.
            ProgramSpec.of(rl("crafting_manager"), "craftmgr", false, DESKTOPS, 128, ProgramKind.APP, 2,
                            HostScope.CRAFTING_COMPUTER)
                    .named("Crafting Manager")
                    .described("Load crafting patterns and watch the jobs the network is working through.")
                    .withEra(LEGACY).withHouse(SoftwareHouse.AUTODECK).withRam(32),
            // The Pattern Studio authors recipe files on any desktop and hands them to a linked encoder.
            ProgramSpec.of(rl("pattern_studio"), "studio", false, DESKTOPS, 96, ProgramKind.APP, 2, HostScope.ANY)
                    .named("Pattern Studio")
                    .described("Author bench, machine and multi-stage recipes, then burn them onto media at a"
                            + " linked encoder.")
                    .withEra(LEGACY).withHouse(SoftwareHouse.AUTODECK).withRam(24),
            // The Cluster Manager installs only on a Cluster Management Computer -> Frames XP or newer.
            ProgramSpec.of(rl("cluster_manager"), "clustermgr", false, DESKTOPS, 96, ProgramKind.APP, 2,
                            HostScope.CLUSTER_MANAGEMENT_COMPUTER)
                    .named("Cluster Manager")
                    .described("Run every supercomputer and datacenter section on the network as one machine.")
                    .withEra(STANDARD).withHouse(SoftwareHouse.JSC).withRam(96),
            // The Gateway Manager runs on whichever computer has Network Gateways on its ports -> Frames XP or newer.
            ProgramSpec.of(rl("gateway_manager"), "gatewaymgr", false, DESKTOPS, 96, ProgramKind.APP, 2, HostScope.ANY)
                    .named("Gateway Manager")
                    .described("Manage the Network Gateways on this computer's ports, and what ComputerCraft may"
                            + " do through them.")
                    .withEra(LEGACY).withHouse(SoftwareHouse.JSC).withRam(32),
            // Minesweeper: a small game available on any desktop (rank 0 = Frames 95 and newer).
            ProgramSpec.of(rl("minesweeper"), "mines", false, DESKTOPS, 16, ProgramKind.APP, 0, HostScope.ANY)
                    .named("Minesweeper").described("Minesweeper.")
                    .withEra(VINTAGE).withHouse(SoftwareHouse.MIDSOFT).withRam(1),
            // Solitaire: the other game every one of these desktops shipped with, and as light as that one.
            ProgramSpec.of(rl("solitaire"), "solitaire", false, DESKTOPS, 16, ProgramKind.APP, 0, HostScope.ANY)
                    .named("Solitaire").described("Klondike solitaire, drawing one card at a time.")
                    .withEra(VINTAGE).withHouse(SoftwareHouse.MIDSOFT).withRam(1),
            // Snake: the game a machine with almost nothing in it could still run.
            ProgramSpec.of(rl("snake"), "snake", false, DESKTOPS, 8, ProgramKind.APP, 0, HostScope.ANY)
                    .named("Snake").described("Snake. Eat, grow, and do not run into yourself.")
                    .withEra(VINTAGE).withHouse(SoftwareHouse.MIDSOFT).withRam(1),
            // 67ark: packs files into one that weighs less, which is how a small disk is made to stretch.
            ProgramSpec.of(rl("ark"), "ark", false, DESKTOPS, 24, ProgramKind.APP, 0, HostScope.ANY)
                    .named("67ark").described("Pack many files into one that weighs less, and take them back out.")
                    .withEra(LEGACY).withHouse(SoftwareHouse.VAULTIS).withRam(16),
            // Paint: a real picture in an indexed palette, which also becomes the desktop's wallpaper.
            ProgramSpec.of(rl("paint"), "paint", false, DESKTOPS, 48, ProgramKind.APP, 0, HostScope.ANY)
                    .named("Paint").described("Draw a picture, and hang it on the desktop.")
                    .withEra(LEGACY).withHouse(SoftwareHouse.BELLWETHER_LABS).withRam(32),
            // Exceed: a sheet whose cells can ask the network what it is holding.
            ProgramSpec.of(rl("exceed"), "exceed", false, DESKTOPS, 64, ProgramKind.APP, 2, HostScope.ANY)
                    .named("Exceed").described("A sheet of cells that can ask the network what it is holding.")
                    .withEra(STANDARD).withHouse(SoftwareHouse.MIDSOFT).withRam(64),
            /*
             * The Messenger: a service on a server in a rack and a client on every computer. Its declared
             * memory is only its floor; what it really costs grows with the conversations it keeps and with
             * how many people have the messenger open, which is the whole point of it. It belongs on a
             * server rather than on the Mainframe because that is what a server is for: the Mainframe
             * orchestrates the network, the servers run the things it serves.
             */
            ProgramSpec.of(rl("messenger_service"), "msgsvc", false, ALL_PLATFORMS, 48, ProgramKind.SERVICE, 2,
                            HostScope.SERVER)
                    .named("Messenger Service")
                    .described("Keeps the network's conversations. It grows on the disk as it keeps them, and in"
                            + " memory as more people have the messenger open.")
                    .withEra(STANDARD).withHouse(SoftwareHouse.MIDSOFT).withRam(8),
            ProgramSpec.of(rl("messenger"), "messenger", false, DESKTOPS, 48, ProgramKind.APP, 2, HostScope.ANY)
                    .named("Midsoft Messenger").described("Talk to whoever else is on this network.")
                    .withEra(STANDARD).withHouse(SoftwareHouse.MIDSOFT).withRam(32),
            // KnotHub keeps the source a network is still arguing over; Knot is what a computer reads it with.
            ProgramSpec.of(rl("knothub"), "knothub", false, ALL_PLATFORMS, 64, ProgramKind.SERVICE, 2, HostScope.SERVER)
                    .named("KnotHub")
                    .described("Keeps the source this network is still working on, revision by revision.")
                    .withEra(STANDARD).withHouse(SoftwareHouse.DAYLIGHT_FOUNDATION).withRam(16),
            ProgramSpec.of(rl("knot"), "knot", false, DESKTOPS, 48, ProgramKind.APP, 2, HostScope.ANY)
                    .named("Knot").described("Push a file to the network's repository, and see who changed what.")
                    .withEra(STANDARD).withHouse(SoftwareHouse.DAYLIGHT_FOUNDATION).withRam(32),
            // Storage Insights: a network dashboard -> Frames XP or newer.
            ProgramSpec.of(rl("storage_insights"), "insights", false, DESKTOPS, 64, ProgramKind.APP, 2, HostScope.ANY)
                    .named("Storage Insights")
                    .described("Where the network's storage went: biggest types, what is running low, how full"
                            + " each server is.")
                    .withEra(STANDARD).withHouse(SoftwareHouse.VAULTIS).withRam(64),
            // Craft Planner: a network planning tool -> Frames XP or newer.
            ProgramSpec.of(rl("craft_planner"), "planner", false, DESKTOPS, 64, ProgramKind.APP, 2, HostScope.ANY)
                    .named("Craft Planner")
                    .described("Plan a craft before committing it: what it needs, what is missing and what it will"
                            + " cost.")
                    .withEra(STANDARD).withHouse(SoftwareHouse.AUTODECK).withRam(64),
            // The Automation Engine is a headless Mainframe service; it needs the modern OS (Frames 11, rank 3).
            ProgramSpec.of(rl("automation_engine"), "autoeng", false, ALL_PLATFORMS, 32, ProgramKind.SERVICE, 3,
                            HostScope.MAINFRAME)
                    .named("Automation Engine")
                    .described("The service that runs standing automation rules on the Mainframe.")
                    .withEra(STANDARD).withHouse(SoftwareHouse.RED_CAP).withRam(64),
            // The Automation Manager is a modern automation front-end -> Frames 11 (rank 3).
            ProgramSpec.of(rl("automation_manager"), "automgr", false, LATER_DESKTOPS, 64, ProgramKind.APP, 3,
                            HostScope.ANY)
                    .named("Automation Manager").described("Write and supervise the rules the Automation Engine runs.")
                    .withEra(STANDARD).withHouse(SoftwareHouse.RED_CAP).withRam(96),
            /*
             * Server services: headless daemons that only make sense on a machine mounted in a rack,
             * which is what gives a server its ROLE: hardware decides capacity, software decides job.
             * Predictive Cache keeps the hot items staged, so queries this bay serves come back sooner.
             */
            ProgramSpec.of(rl("predictive_cache"), "predcache", false, ALL_PLATFORMS, 32, ProgramKind.SERVICE, 0,
                            HostScope.SERVER)
                    .named("Predictive Cache")
                    .described("Keeps this bay's most-wanted items staged in memory, cutting read latency by 15%."
                            + " Stacks with a Cache Card.")
                    .withEra(STANDARD).withHouse(SoftwareHouse.VAULTIS).withRam(128),
            // Load Balancer spreads writes across the bay's drives instead of filling them in order.
            ProgramSpec.of(rl("load_balancer"), "loadbal", false, ALL_PLATFORMS, 32, ProgramKind.SERVICE, 0,
                            HostScope.SERVER)
                    .named("Load Balancer")
                    .described("Spreads writes across the bay's drives instead of filling them one after another.")
                    .withEra(LEGACY).withHouse(SoftwareHouse.JSC).withRam(16),
            /*
             * Integrity Monitor re-reads what a hot event left in doubt, so light index maintenance
             * stops being a chore (a fragmented index still wants a vacuum by hand).
             */
            ProgramSpec.of(rl("integrity_monitor"), "integrity", false, ALL_PLATFORMS, 32, ProgramKind.SERVICE, 0,
                            HostScope.SERVER)
                    .named("Integrity Monitor")
                    .described("Re-reads this bay after a hot swap, so the network index never has to doubt it.")
                    .withEra(STANDARD).withHouse(SoftwareHouse.VAULTIS).withRam(32),
            /*
             * Remote Control: the graphical way into the network's other machines (a headless rack server
             * above all). Any desktop, Frames XP or newer, the point-and-click twin of ssh.
             */
            ProgramSpec.of(rl("remote_control"), "remotectl", false, DESKTOPS, 48, ProgramKind.APP, 2, HostScope.ANY)
                    .named("Remote Control")
                    .described("Take over another machine on the network and use it on this screen.")
                    .withEra(STANDARD).withHouse(SoftwareHouse.MIDSOFT).withRam(48),
            /*
             * The Mirror: the package repository service every Linux computer on the network installs from
             * (apt/dnf/pacman/emerge resolve against it). A headless Mainframe service on any platform.
             */
            ProgramSpec.of(rl("mirror"), "mirror", false, ALL_PLATFORMS, 64, ProgramKind.SERVICE, 0,
                            HostScope.MAINFRAME)
                    .named("Mirror")
                    .described("The network's package repository. Every package manager installs from it.")
                    .withEra(STANDARD).withHouse(SoftwareHouse.JSC).withRam(32),
            /*
             * screenfetch: the little system-identity tool, a package the Mirror serves to any Linux, to FreeBSD
             * and to Frames, where it runs at the Command Prompt (its absence teaching the package manager:
             * 'command not found' until it is installed).
             */
            ProgramSpec.of(rl("screenfetch"), "screenfetch", false, SCREENFETCH_SYSTEMS, 4, ProgramKind.APP, 0,
                            HostScope.ANY)
                    .named("screenfetch").described("Prints the system's identity, with its distribution's logo.")
                    .withEra(LEGACY).withHouse(SoftwareHouse.ARCH_COLLECTIVE).withRam(1),
            /*
             * The Σ# toolchain: the compiler and the runtime, two packages the Mirror serves to any
             * machine of the Legacy generation or later running Frames XP or a Linux. Neither has a window
             * of its own; both are verbs at the prompt, which is where a program is written and run from.
             */
            ProgramSpec.of(rl("sgsc"), "sgsc", false, ALL_PLATFORMS, 8, ProgramKind.APP, 2, HostScope.ANY)
                    .named("Σ# Compiler").described("Compiles a Σ# program into the assembly the runtime reads.")
                    .withMinEra(LEGACY).withEra(LEGACY).withHouse(SoftwareHouse.SIGMA_FOUNDATION).withRam(16),
            /*
             * The Sigma Compiler Collection, which is the whole toolchain of the earliest machines: it writes the
             * assembly the machine already runs, so there is no runtime to install beside it. Small enough to sit
             * on a Vintage disk, and useful long past that, since a program built with it runs everywhere.
             */
            ProgramSpec.of(rl("scc"), "scc", false, ALL_PLATFORMS, 4, ProgramKind.APP, 1, HostScope.ANY)
                    .named("Σ Compiler")
                    .described("Compiles a Σ program into the assembly a machine runs. Small enough for the oldest"
                            + " of them, and the whole toolchain there, since nothing else has to be installed to"
                            + " run what it writes.")
                    .withMinEra(VINTAGE).withEra(VINTAGE).withHouse(SoftwareHouse.SIGMA_FOUNDATION).withRam(2),
            ProgramSpec.of(rl("sigma"), "sigma", false, ALL_PLATFORMS, 12, ProgramKind.SERVICE, 2, HostScope.ANY)
                    .named("Sigma Runtime")
                    .described("Runs compiled Σ# programs, and brings the 'sigma' command to the prompt.")
                    .withMinEra(LEGACY).withEra(LEGACY).withHouse(SoftwareHouse.SIGMA_FOUNDATION).withRam(24),
            /*
             * Virtual Studio: the whole workshop in one window, and the only editor that says what a call
             * will cost the program before the line is written. Frames only, and it asks the machine to
             * prove it: a quarter of a gigabyte held while it is open, which is what a program of the
             * generation after this one weighs.
             */
            ProgramSpec.of(rl("virtual_studio"), "virtualstudio", false, FRAMES_ONLY, 512, ProgramKind.APP, 2,
                            HostScope.ANY)
                    .named("Virtual Studio")
                    .described("The whole Σ# workshop in one window, and the one editor that says what a call will"
                            + " cost before it is written.")
                    .withMinEra(LEGACY).withEra(STANDARD).withHouse(SoftwareHouse.MIDSOFT).withRam(256),
            /*
             * Virtual Studio Code: the light editor for Σ#, with the machine's programs down the side
             * and its console welded into the bottom of the window, so a program is written, compiled and
             * run without leaving it. Frames XP or newer, and any Linux desktop.
             */
            ProgramSpec.of(rl("virtual_studio_code"), "virtualcode", false, LATER_DESKTOPS, 128, ProgramKind.APP, 2,
                            HostScope.ANY)
                    .named("Virtual Studio Code")
                    .described("A light editor for Σ# with the console built in: write, compile and run without"
                            + " leaving it.")
                    .withMinEra(LEGACY).withEra(STANDARD).withHouse(SoftwareHouse.MIDSOFT).withRam(48),
            /*
             * Exposure: the editor that compiles every program on the disk instead of the one in front
             * of you, so changing something shared shows which of the others stopped building. It offers
             * nothing as you type, which is the trade.
             */
            ProgramSpec.of(rl("exposure"), "exposure", false, LATER_DESKTOPS, 192, ProgramKind.APP, 2, HostScope.ANY)
                    .named("Exposure")
                    .described("An editor that compiles every program on the disk at once, so a change shows what"
                            + " else it broke.")
                    .withMinEra(LEGACY).withEra(LEGACY).withHouse(SoftwareHouse.DAYLIGHT_FOUNDATION).withRam(64),
            /*
             * Vim takes over the terminal it was started from instead of opening a window of its own,
             * which is the only reason a rack server with no graphics can be programmed at all, and why
             * it runs on every platform there is a prompt on. Four megabytes, and it shows.
             */
            ProgramSpec.of(rl("vim"), "vim", false, ALL_PLATFORMS, 8, ProgramKind.APP, 0, HostScope.ANY)
                    .named("Vim")
                    .described("A text editor that takes over the terminal it was started from, so it works where"
                            + " there is no desktop.")
                    .withMinEra(VINTAGE).withEra(VINTAGE).withHouse(SoftwareHouse.BUNDLED).withRam(4),
            /*
             * Emacs is also a terminal editor and also the answer to a different question: it splits the
             * glass, so the source and what the compiler said about it are readable at once. Three times
             * the memory of Vim, which is the trade.
             */
            ProgramSpec.of(rl("emacs"), "emacs", false, ALL_PLATFORMS, 24, ProgramKind.APP, 0, HostScope.ANY)
                    .named("Emacs")
                    .described("A text editor that splits the terminal, so the source and what the compiler said"
                            + " about it are read together.")
                    .withMinEra(VINTAGE).withEra(VINTAGE).withHouse(SoftwareHouse.BUNDLED).withRam(12),
            /*
             * The Linux desktop environments: packages that turn a TTY distribution into a graphical desktop.
             * Footprints are balancing estimates (Plasma is the heaviest, Cinnamon the lightest), and so is
             * the RAM each holds once it is up, on top of the distribution's own share.
             * Each one requires the hardware generation it belongs to. KDE and GNOME are old enough to
             * run on Legacy machines; Cinnamon is a much later desktop and needs Standard hardware. A
             * Vintage computer therefore has no graphical desktop at all and lives at the TTY.
             */
            ProgramSpec.of(rl("kde_plasma"), "kde-plasma", false, LINUX_AND_FREEBSD, 256,
                            ProgramKind.DESKTOP_ENVIRONMENT, 0, HostScope.ANY)
                    .named("KDE Plasma").described("The KDE Plasma desktop environment.")
                    .withMinEra(LEGACY).withEra(LEGACY).withHouse(SoftwareHouse.KDE_GUILD).withRam(224),
            ProgramSpec.of(rl("gnome"), "gnome", false, LINUX_AND_FREEBSD, 192, ProgramKind.DESKTOP_ENVIRONMENT, 0,
                            HostScope.ANY)
                    .named("GNOME").described("The GNOME desktop environment.")
                    .withMinEra(LEGACY).withEra(LEGACY).withHouse(SoftwareHouse.GNOME_TRUST).withRam(256),
            ProgramSpec.of(rl("cinnamon"), "cinnamon", false, LINUX_AND_FREEBSD, 160, ProgramKind.DESKTOP_ENVIRONMENT,
                            0, HostScope.ANY)
                    .named("Cinnamon").described("The Cinnamon desktop environment.")
                    .withMinEra(STANDARD).withEra(STANDARD).withHouse(SoftwareHouse.SPEARMINT).withRam(160),
            /*
             * CDE: the desktop of the Unix workstations, and the only one UNIX has. A fraction of what the
             * later desktops weigh, which is how a Legacy machine with a few megabytes to spare runs one. UNIX
             * takes it from a medium like everything else it installs; FreeBSD takes it from its packages, and
             * a Linux distribution from the Mirror by its own package manager, as one still can.
             */
            ProgramSpec.of(rl("cde"), "cde", false, CDE_SYSTEMS, 32, ProgramKind.DESKTOP_ENVIRONMENT, 0, HostScope.ANY)
                    .named("CDE").described("The Common Desktop Environment.")
                    .withMinEra(LEGACY).withEra(LEGACY).withHouse(SoftwareHouse.OPEN_DESK_CONSORTIUM).withRam(16),
            /*
             * The operating space MC-NET ships with, and what makes the machine more than a prompt. It goes on
             * with the system rather than being bought from the Mirror, but it is a package like any other, so
             * a player who wants nothing but the prompt takes it off and a machine that has lost it puts it
             * back. Two megabytes and a Vintage minimum, because the system it belongs to is a Vintage system.
             */
            ProgramSpec.of(rl("interactor"), "interactor", false, NET_ONLY, 2, ProgramKind.OPERATING_SPACE, 0,
                            HostScope.ANY)
                    .named("Interactor")
                    .described("The operating space a network system draws: its store, its work and its prompt, on"
                            + " the whole screen. Take it off and the machine is a prompt and nothing else.")
                    .withMinEra(VINTAGE).withEra(VINTAGE).withHouse(SoftwareHouse.NOUVELL).withRam(1)
    );

    /** The built-in program descriptors, so datagen (lang, install media) reads them from one source. */
    public static List<ProgramSpec> builtinPrograms() {
        return BUILTIN_PROGRAMS;
    }

    private static void registerPrograms() {
        for (final ProgramSpec program : BUILTIN_PROGRAMS) {
            JsComputersApi.registerProgram(program);
        }
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath("jsc", path);
    }
}
