/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.AbstractSmallComputerBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.BootMenuScreen;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.ComputerTerminalScreen;
import dev.jstech.computers.client.FirmwareScreen;
import dev.jstech.computers.client.InstallerScreen;
import dev.jstech.computers.client.MainframeScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.hardware.FrontArea;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.boot.BootLines;
import dev.jstech.computers.os.boot.BootMenu;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.media.InstallMedia;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

/**
 * Each era's systems put on a computer by a player, from their own media and through the firmware. A computer of the
 * era is built with nothing on its disks; the player puts the system's medium in the drive beside it (a floppy, a CD,
 * a DVD, or from the Standard on a stick in a Dock Station), sits at its monitor, waits out the self-test into the
 * firmware and presses Install, then answers the installer's pages, typing the machine's name where it is asked; the
 * machine comes back up in the system with that name. Every system of every era goes on so: MC-DOS and UNIX on the
 * Vintage, with MC-NET reinstalled on the network's Mainframe after its disk is formatted in its firmware; Frames 95,
 * Frames XP, FreeBSD, Ubuntu, Debian and Fedora on the Legacy; Frames 7, 10 and 11 after it. A disk is formatted in the
 * firmware and the system goes on again; two systems on two disks of one computer are chosen between at the boot
 * manager; and each desktop environment goes onto a distribution with its package manager, from the network's Mirror,
 * and comes up after the restart.
 */
public final class EraSystemSetupClientTests {

    private static final int SETTLE = EraJourney.SETTLE;
    private static final int WAIT = EraJourney.SCREEN_WAIT;
    private static final int BOOT_WAIT = EraJourney.BOOT_WAIT;
    /** A whole install: the self-test into the installer, the copy, held under ninety seconds, and the restart. */
    private static final int INSTALL_WAIT = 8_000;
    /** A desktop environment over the network from the Mirror, as long as the largest of them takes. */
    private static final int DESKTOP_WAIT = 4_000;
    private static final int TIMEOUT = 24_000;
    private static final BlockPos COMPUTER = EraNetwork.COMPUTER;
    private static final BlockPos MONITOR = EraNetwork.MONITOR;
    private static final BlockPos AT_THE_DESK = EraNetwork.AT_THE_DESK;
    /** In front of the computer, which faces west, to reach its power button. */
    private static final BlockPos BEFORE_THE_COMPUTER = new BlockPos(7, 2, 2);
    /** The floppy drive or the Dock Station south of the computer, its back on it, and where the player reaches it. */
    private static final BlockPos SOUTH_DRIVE = new BlockPos(9, 2, 3);
    private static final BlockPos AT_THE_SOUTH_DRIVE = new BlockPos(9, 2, 5);
    /** The optical drive north of it, and where the player reaches it. */
    private static final BlockPos NORTH_DRIVE = new BlockPos(9, 2, 1);
    private static final BlockPos AT_THE_NORTH_DRIVE = new BlockPos(9, 2, 0);
    /* The network's Mainframe, as the era's network lays it: its monitor and floppy drive in front of it. */
    private static final BlockPos MAINFRAME_MONITOR = new BlockPos(3, 2, 1);
    private static final BlockPos AT_THE_MAINFRAME = new BlockPos(5, 2, 1);
    private static final BlockPos MAINFRAME_FLOPPY = new BlockPos(3, 3, 1);
    private static final BlockPos AT_THE_MAINFRAME_FLOPPY = new BlockPos(4, 2, 3);
    private static final BlockPos MAINFRAME_PART = new BlockPos(1, 2, 3);
    private static final BlockPos BY_THE_MAINFRAME_PART = new BlockPos(1, 2, 5);

    private EraSystemSetupClientTests() {
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void vintage_dosIsInstalledNamedThenFormattedForUnix(final ClientTestContext ctx) {
        bare(ctx, EraKit.VINTAGE, 1);
        install(ctx, EraKit.VINTAGE, os("mc_dos"), "LEDGER", false);
        install(ctx, EraKit.VINTAGE, os("unix"), "SYSV", true);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void vintage_networkSystemIsReinstalledOnTheMainframe(final ClientTestContext ctx) {
        final ResourceLocation mcNet = EraKit.NETWORK_OS;
        EraJourney.lay(ctx, EraKit.VINTAGE).thenBuild(0, world -> {
            world.setBlock(MAINFRAME_FLOPPY, facing(ComputingModule.FLOPPY_DRIVE.get(), Direction.EAST));
            world.placeMonitor(MAINFRAME_MONITOR, Direction.EAST, EraKit.VINTAGE.monitor());
        });
        ctx.thenTeleport(SETTLE, AT_THE_MAINFRAME_FLOPPY, Direction.WEST);
        DiscSteps.swap(ctx, MAINFRAME_FLOPPY, InstallerStacks.forSystem(mcNet));
        // Off and on at its own screen, so the self-test runs and DEL reaches the firmware.
        ctx.thenTeleport(SETTLE, BY_THE_MAINFRAME_PART, Direction.NORTH)
                .thenRightClick(SETTLE, MAINFRAME_PART)
                .thenAwaitScreen(MainframeScreen.class, WAIT)
                .then(SETTLE, () -> ctx.clickGui(MainframeScreen.powerButtonX(), MainframeScreen.powerButtonY()))
                .thenWaitUntilServer(level -> !mainframe(ctx, level).isRunning(), WAIT, "the Mainframe to go off",
                        level -> "running")
                .then(SETTLE, () -> ctx.clickGui(MainframeScreen.powerButtonX(), MainframeScreen.powerButtonY()))
                .thenWaitUntilServer(level -> mainframe(ctx, level).isRunning(), WAIT, "and to come back on",
                        level -> "off")
                .thenWaitUntil(EraJourney.away(ctx), WAIT, "its screen to be left")
                .thenTeleport(SETTLE, AT_THE_MAINFRAME, Direction.WEST)
                .thenRightClick(SETTLE, MAINFRAME_MONITOR)
                .thenWaitUntil(EraJourney.intoSetup(ctx), BOOT_WAIT, "DEL through the self-test to open the setup");
        format(ctx, level -> !mainframe(ctx, level).hasOs(), "the Mainframe's disk");
        ctx.then(SETTLE, () -> {
                    final int[] install = ctx.screen(FirmwareScreen.class).installButtonCenter();
                    ctx.click(install[0], install[1]);
                })
                .thenWaitUntil(new Answer(ctx, null, ComputerTerminalScreen.class), INSTALL_WAIT,
                        "the installer's pages to be answered and MC-NET to come up on the Mainframe")
                .thenWaitUntilServer(level -> mainframe(ctx, level).hasOs()
                                && mcNet.equals(mainframe(ctx, level).installedOs().id())
                                && mainframe(ctx, level).networkUuid() != null, BOOT_WAIT,
                        "the Mainframe to run the network again on the system it was given",
                        level -> "os=" + mainframe(ctx, level).installedOs() + " network="
                                + mainframe(ctx, level).networkUuid())
                .thenScreenshot(SETTLE, "vintage-mc-net-reinstalled");
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_frames95InstallsFromItsCd(final ClientTestContext ctx) {
        single(ctx, EraKit.LEGACY, "frames_95", "WORKBENCH");
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_framesXpInstallsFromItsCd(final ClientTestContext ctx) {
        single(ctx, EraKit.LEGACY, "frames_xp", "STUDY");
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_freeBsdInstallsFromItsCd(final ClientTestContext ctx) {
        single(ctx, EraKit.LEGACY, "freebsd", "harbour");
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_ubuntuInstallsFromItsCd(final ClientTestContext ctx) {
        single(ctx, EraKit.LEGACY, "ubuntu", "orchard");
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_debianInstallsFromItsCd(final ClientTestContext ctx) {
        single(ctx, EraKit.LEGACY, "debian", "quarry");
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_fedoraInstallsFromItsCd(final ClientTestContext ctx) {
        single(ctx, EraKit.LEGACY, "fedora", "foundry");
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void transition_frames7InstallsFromItsDvd(final ClientTestContext ctx) {
        single(ctx, EraKit.TRANSITION, "frames_7", "WORKSHOP");
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_frames10InstallsFromItsStickAndAgainAfterAFormat(final ClientTestContext ctx) {
        bare(ctx, EraKit.STANDARD, 1);
        install(ctx, EraKit.STANDARD, os("frames_10"), "OFFICE", false);
        install(ctx, EraKit.STANDARD, os("frames_10"), "LIBRARY", true);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_frames11InstallsFromItsStick(final ClientTestContext ctx) {
        single(ctx, EraKit.ADVANCED, "frames_11", "STATION");
    }

    /**
     * Frames XP on the first of two disks and Ubuntu on the second: the boot manager lists both, and each boots when it
     * is picked from the list.
     */
    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_twoSystemsOnTwoDisksArePickedAtTheBootManager(final ClientTestContext ctx) {
        final ResourceLocation xp = os("frames_xp");
        final ResourceLocation ubuntu = os("ubuntu");
        bare(ctx, EraKit.LEGACY, 2);
        install(ctx, EraKit.LEGACY, xp, "DUAL", false);
        install(ctx, EraKit.LEGACY, ubuntu, "dual", false);
        ctx.thenServer(SETTLE, level -> {
            final BootMenu menu = BootLines.menuFor(computer(ctx, level), 100);
            ctx.assertTrue(menu.entries().stream().anyMatch(entry -> xp.toString().equals(entry.osId()))
                            && menu.entries().stream().anyMatch(entry -> ubuntu.toString().equals(entry.osId())),
                    "the boot manager lists both systems: " + menu.entries());
        });
        pick(ctx, EraKit.LEGACY, xp, DesktopScreen.class);
        pick(ctx, EraKit.LEGACY, ubuntu, CommandPromptScreen.class);
    }

    /**
     * Each desktop environment put on a distribution with its package manager, from the Mirror on the network's
     * Mainframe: KDE Plasma on Ubuntu, GNOME on Debian, Cinnamon on Fedora and CDE on Arch; each comes up after the
     * restart. Cinnamon wants Standard hardware, so the network is the Standard's.
     */
    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_desktopsGoOnTheDistributionsFromTheMirror(final ClientTestContext ctx) {
        EraJourney.lay(ctx, EraKit.STANDARD)
                .thenServer(SETTLE, level -> mainframe(ctx, level).installMirror());
        desktop(ctx, "ubuntu", "apt install kde_plasma", "kde_plasma");
        desktop(ctx, "debian", "apt install gnome", "gnome");
        desktop(ctx, "fedora", "dnf install cinnamon", "cinnamon");
        desktop(ctx, "arch", "pacman -S cde", "cde");
    }

    /* A bare computer of the era with one system put on it from its medium. */
    private static void single(final ClientTestContext ctx, final EraKit kit, final String system, final String name) {
        bare(ctx, kit, 1);
        install(ctx, kit, os(system), name, false);
    }

    /*
     * The era's computer with {@code disks} empty disks, turned to face west, its monitor east of it and its drives
     * north and south of it: a floppy drive or a Dock Station to the south, the era's optical drive to the north.
     */
    private static void bare(final ClientTestContext ctx, final EraKit kit, final int disks) {
        ctx.thenGive(0).thenBuild(0, world -> {
            EraNetwork.bareComputer(world, kit, COMPUTER, disks);
            world.setBlock(COMPUTER, world.getBlockState(COMPUTER)
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
            world.placeMonitor(MONITOR, Direction.EAST, kit.monitor());
            world.setBlock(SOUTH_DRIVE, facing(kit.era().isAtLeast(EraKit.STANDARD.era())
                    ? ComputingModule.DOCK_STATION.get() : ComputingModule.FLOPPY_DRIVE.get(), Direction.SOUTH));
            final Block optical = opticalDrive(kit);
            if (optical != null) {
                world.setBlock(NORTH_DRIVE, facing(optical, Direction.NORTH));
            }
        });
    }

    /*
     * The system's medium put in its drive, the firmware reached (on a machine with a system already, by restarting it
     * from its power button and pressing DEL through the self-test), the disk formatted there when asked, and Install
     * pressed; the installer's pages are answered, the name typed where it is asked, and the machine comes back up in
     * the system with that name.
     */
    private static void install(final ClientTestContext ctx, final EraKit kit, final ResourceLocation system,
                                final String name, final boolean format) {
        final MediaFormat medium = InstallMedia.forSystem(OsRegistry.getOs(system).minEra());
        final boolean south = medium == MediaFormat.FLOPPY || medium == MediaFormat.USB;
        final AtomicInteger hadSystem = new AtomicInteger();
        ctx.thenWaitUntil(EraJourney.away(ctx), WAIT, "the screen to be left")
                .thenServer(0, level -> hadSystem.set(computer(ctx, level).hasOs() ? 1 : 0))
                .thenTeleport(SETTLE, south ? AT_THE_SOUTH_DRIVE : AT_THE_NORTH_DRIVE,
                        south ? Direction.NORTH : Direction.SOUTH);
        DiscSteps.swap(ctx, south ? SOUTH_DRIVE : NORTH_DRIVE, InstallerStacks.forSystem(system));
        restartIfRunning(ctx, kit, hadSystem);
        ctx.thenTeleport(SETTLE, AT_THE_DESK, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenWaitUntil(EraJourney.intoSetup(ctx), BOOT_WAIT, "the firmware to be reached through the self-test")
                .thenScreenshot(SETTLE, shot(kit, system.getPath() + "-firmware"));
        if (format) {
            format(ctx, level -> !computer(ctx, level).hasOs(), "the computer's disk");
        }
        final Class<? extends Screen> up = systemScreen(system);
        ctx.then(SETTLE, () -> {
                    final int[] install = ctx.screen(FirmwareScreen.class).installButtonCenter();
                    ctx.click(install[0], install[1]);
                })
                .thenWaitUntil(new Answer(ctx, name, up), INSTALL_WAIT,
                        "the installer's pages to be answered and " + system.getPath() + " to come up")
                .thenWaitUntilServer(level -> installed(computer(ctx, level), system), WAIT,
                        system.getPath() + " to be on the computer's disk", level -> "os="
                                + computer(ctx, level).installedOs())
                .thenWaitUntilServer(level -> name.equals(computer(ctx, level).customName()), WAIT,
                        "the computer to have the name typed at the installer",
                        level -> "name=" + computer(ctx, level).customName())
                .thenScreenshot(SETTLE * 3, shot(kit, system.getPath()));
    }

    /* On a machine already running a system, its power button pressed twice, so the self-test runs again. */
    private static void restartIfRunning(final ClientTestContext ctx, final EraKit kit, final AtomicInteger running) {
        final FrontArea power = ((AbstractSmallComputerBlock<?>) kit.computer()).frontLayout().power();
        ctx.thenTeleport(SETTLE, BEFORE_THE_COMPUTER, Direction.EAST)
                .then(SETTLE, () -> ctx.selectHotbar(8))
                .then(1, () -> {
                    if (running.get() == 1) {
                        ctx.rightClickFront(COMPUTER, power.middleX(), power.middleY(), 0);
                    }
                })
                .thenWaitUntilServer(level -> running.get() == 0 || !computer(ctx, level).isRunning(), WAIT,
                        "the power button to switch the computer off", level -> "running")
                .then(SETTLE, () -> {
                    if (running.get() == 1) {
                        ctx.rightClickFront(COMPUTER, power.middleX(), power.middleY(), 0);
                    }
                })
                .thenWaitUntilServer(level -> computer(ctx, level).isRunning(), WAIT,
                        "and back on", level -> "off");
    }

    /* The first disk picked on the firmware's boot page by its key, and F pressed twice to format it. */
    private static void format(final ClientTestContext ctx, final Predicate<ServerLevel> done, final String what) {
        ctx.thenWaitUntil(() -> !ctx.screen(FirmwareScreen.class).diskRows().isEmpty(), WAIT,
                        "the firmware to list the disk")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_1 + ctx.screen(FirmwareScreen.class).diskRows().get(0)))
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_F))
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_F))
                .thenWaitUntilServer(done, WAIT, "F twice to format " + what, level -> "still holds a system")
                .thenScreenshot(SETTLE, "formatted");
    }

    /*
     * The computer restarted from its power button and the system picked at the boot manager by moving down the list
     * to it; it comes up on its own screen.
     */
    private static void pick(final ClientTestContext ctx, final EraKit kit, final ResourceLocation system,
                             final Class<? extends Screen> up) {
        final AtomicInteger presses = new AtomicInteger();
        final AtomicInteger running = new AtomicInteger(1);
        ctx.thenWaitUntil(EraJourney.away(ctx), WAIT, "the screen to be left");
        restartIfRunning(ctx, kit, running);
        ctx.thenServer(SETTLE, level -> {
                    final BootMenu menu = BootLines.menuFor(computer(ctx, level), 100);
                    final List<BootMenu.Entry> entries = menu.entries();
                    for (int i = 0; i < entries.size(); i++) {
                        if (system.toString().equals(entries.get(i).osId())) {
                            presses.set((i - menu.defaultIndex() + entries.size()) % entries.size());
                            return;
                        }
                    }
                    throw new ClientTestFailure("the boot manager does not list " + system + ": " + entries);
                })
                .thenTeleport(SETTLE, AT_THE_DESK, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(BootMenuScreen.class, BOOT_WAIT)
                .thenScreenshot(SETTLE, shot(kit, "boot-manager"))
                .then(SETTLE, () -> {
                    for (int i = 0; i < presses.get(); i++) {
                        ctx.key(GLFW.GLFW_KEY_DOWN);
                    }
                })
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenAwaitScreen(up, BOOT_WAIT)
                .thenScreenshot(SETTLE * 3, shot(kit, "picked-" + system.getPath()));
    }

    /*
     * The distribution put on the network's computer, which is restarted into it; at its prompt the player installs
     * the desktop with the package manager from the Mirror, answers its question when it asks one, and restarts into
     * the desktop.
     */
    private static void desktop(final ClientTestContext ctx, final String distro, final String command,
                                final String environment) {
        final ResourceLocation desktop = os(environment);
        ctx.thenWaitUntil(EraJourney.away(ctx), WAIT, "the screen to be left")
                .thenServer(SETTLE, level -> {
                    final PersonalComputerBlockEntity computer = computer(ctx, level);
                    // The disk is wiped first, so the desktop the system before had goes with it.
                    computer.togglePower();
                    computer.formatDisk(0);
                    ctx.assertTrue(computer.installOs(os(distro)), distro + " goes on the computer");
                    computer.togglePower();
                })
                .thenTeleport(SETTLE, AT_THE_DESK, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenWaitUntil(() -> ctx.openScreen(CommandPromptScreen.class) != null, BOOT_WAIT,
                        distro + " to come up at its prompt", () -> "the monitor shows " + ctx.mc().screen)
                .then(SETTLE, () -> {
                    ctx.type(command);
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                })
                // A manager that asks before it installs takes y; one that asked nothing reads it as a line.
                .then(WAIT, () -> {
                    ctx.type("y");
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                })
                .thenWaitUntilServer(level -> desktop.equals(computer(ctx, level).installedDesktopId()), DESKTOP_WAIT,
                        environment + " to be installed on " + distro + " from the Mirror",
                        level -> "desktop=" + computer(ctx, level).installedDesktopId())
                .then(SETTLE, () -> {
                    ctx.type("reboot");
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                })
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenScreenshot(SETTLE * 4, "standard-" + distro + "-" + environment);
    }

    /* Whether the machine's disk carries the system, as the machine itself reads it. */
    private static boolean installed(final PersonalComputerBlockEntity computer, final ResourceLocation system) {
        final OsDef os = computer.installedOs();
        if (os != null && system.equals(os.id())) {
            return true;
        }
        return BootLines.menuFor(computer, 100).entries().stream()
                .anyMatch(entry -> system.toString().equals(entry.osId()));
    }

    /* The screen a system comes up on: a desktop for Frames, the prompt for every other. */
    private static Class<? extends Screen> systemScreen(final ResourceLocation system) {
        return system.getPath().startsWith("frames") ? DesktopScreen.class : CommandPromptScreen.class;
    }

    /* The drive that reads the era's discs: a CD drive, then DVD, then Blu-ray; the Vintage reads floppies alone. */
    private static Block opticalDrive(final EraKit kit) {
        return switch (kit) {
            case VINTAGE -> null;
            case LEGACY -> ComputingModule.CD_DRIVE.get();
            case TRANSITION, STANDARD -> ComputingModule.DVD_DRIVE.get();
            case ADVANCED -> ComputingModule.BLU_RAY_DRIVE.get();
        };
    }

    private static BlockState facing(final Block block, final Direction front) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, front);
    }

    private static ResourceLocation os(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    private static PersonalComputerBlockEntity computer(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(COMPUTER, PersonalComputerBlockEntity.class);
    }

    private static MainframeBlockEntity mainframe(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.MAINFRAME, MainframeBlockEntity.class);
    }

    private static String shot(final EraKit kit, final String what) {
        return kit.name().toLowerCase(Locale.ROOT) + "-" + what;
    }

    /*
     * A player at an installer: on each page as it comes, a moment to read it, then its answer. The name is typed where
     * a name is asked, a guided list's work is begun with its own letter (and its first question opened when the letter
     * is not enough yet), the copy is waited out, and every other page is taken as it stands with Enter, the last one
     * restarting the machine. Done once the system's own screen is up.
     */
    private static final class Answer implements BooleanSupplier {

        private final ClientTestContext ctx;
        private final String name;
        private final Class<? extends Screen> up;
        private String page = "";
        private int ticks;
        private int tries;
        private int visited;
        private boolean named;

        /** How many questions a gathered list is looked through for the name. */
        private static final int HUB_QUESTIONS = 9;

        Answer(final ClientTestContext ctx, final String name, final Class<? extends Screen> up) {
            this.ctx = ctx;
            this.name = name;
            this.up = up;
        }

        @Override
        public boolean getAsBoolean() {
            if (ctx.openScreen(up) != null) {
                return true;
            }
            final InstallerScreen installer = ctx.openScreen(InstallerScreen.class);
            if (installer == null) {
                return false;
            }
            final String now = installer.pageName();
            if (!now.equals(page)) {
                page = now;
                ticks = 0;
                tries = 0;
                return false;
            }
            if (++ticks % 30 != 10 || "COPY".equals(page)) {
                return false;
            }
            if (("NAME".equals(page) || "SETTINGS".equals(page)) && name != null && !named) {
                // The field comes filled with the installer's own suggestion, which is rubbed out first.
                for (int letter = 0; letter < InstallerFlow.MOST_NAME_LETTERS; letter++) {
                    ctx.key(GLFW.GLFW_KEY_BACKSPACE);
                }
                ctx.type(name);
                named = true;
                return false;
            }
            if ("HUB".equals(page)) {
                /*
                 * The installer that gathers its questions on one list: its questions are visited by their numbers
                 * until the name is asked and typed, then its own letter begins the work.
                 */
                if (name != null && !named && visited < HUB_QUESTIONS) {
                    ctx.key(GLFW.GLFW_KEY_1 + visited++);
                } else {
                    ctx.key(tries++ % 2 == 0 ? GLFW.GLFW_KEY_B : GLFW.GLFW_KEY_ENTER);
                }
                return false;
            }
            ctx.key(GLFW.GLFW_KEY_ENTER);
            return false;
        }
    }
}
