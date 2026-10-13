/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.os.media.LiveMedium;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.tests.testkit.EraKit;
import dev.jstech.tests.testkit.EraNetwork;
import dev.jstech.tests.testkit.EraParts;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lwjgl.glfw.GLFW;

/**
 * A sample of the extremes typed by a player at the monitor, line for line, waiting for the prompt between them. Arch
 * and Gentoo installed by hand from their live media in the hardest layout the game has: the Advanced machine with two
 * disks and the system on the second, the firmware's partition at /boot/efi, the world asking for the whole handbook,
 * and on Gentoo the tree synced, the kernel built by hand with every core and the build options set. Then packages from
 * the Mirror installed on Arch and on Gentoo and used at the prompt, and FreeBSD's ports fetched, a port built and
 * used, taken off and the package of the same program installed in its place.
 */
public final class ExtremesClientTests {

    private static final int SETTLE = EraJourney.SETTLE;
    /** What one line of an installation may take on the machine's own clock: a base system, a kernel, a tree. */
    private static final int STEP_WAIT = 4_000;
    private static final int TIMEOUT = 36_000;
    private static final String NAME = "workshop";
    /** The drive north of the network's computer, for the live medium. */
    private static final BlockPos DRIVE = new BlockPos(9, 2, 1);
    private static final String SCREENFETCH = "jsc:screenfetch";
    private static final String VIM = "jsc:vim";
    private static final int GLASS_TAIL = 3_000;

    private ExtremesClientTests() {
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_archIsInstalledByHandInItsHardestLayout(final ClientTestContext ctx) {
        live(ctx, LiveInstallState.Distro.ARCH);
        partition(ctx);
        lines(ctx, "mkfs.fat -F 32 /dev/sdb1", "mkfs.ext4 /dev/sdb2", "mount /dev/sdb2 /mnt",
                "mount --mkdir /dev/sdb1 /mnt/boot/efi", "pacstrap -K /mnt base linux linux-firmware nano");
        // The base system has to be on the disk before anything is done inside it.
        ctx.thenServer(SETTLE, level -> ctx.assertTrue(computer(ctx, level).console().liveInstall() != null
                && !glass(ctx, level).contains("ERROR"), "pacstrap lays the base system out: " + glass(ctx, level)));
        lines(ctx, "genfstab -U /mnt >> /mnt/etc/fstab", "arch-chroot /mnt", "hwclock --systohc",
                "echo " + NAME + " > /etc/hostname", "mkinitcpio -P", "pacman -S screenfetch", "y",
                "pacman -S grub efibootmgr", "y",
                "grub-install --target=x86_64-efi --efi-directory=/boot/efi --bootloader-id=GRUB",
                "grub-mkconfig -o /boot/grub/grub.cfg", "exit", "reboot");
        cameUp(ctx, LiveInstallState.Distro.ARCH);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_gentooIsInstalledByHandInItsHardestLayout(final ClientTestContext ctx) {
        final int[] cores = new int[1];
        live(ctx, LiveInstallState.Distro.GENTOO);
        ctx.thenServer(0, level -> cores[0] = Math.max(1, computer(ctx, level).cpuCores()));
        partition(ctx);
        lines(ctx, "mkfs.fat -F 32 /dev/sdb1", "mkfs.ext4 /dev/sdb2", "mount /dev/sdb2 /mnt/gentoo", "cd /mnt/gentoo",
                "wget mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz",
                "tar xpvf stage3-*.tar.xz --xattrs-include='*.*' --numeric-owner", "mkdir -p /mnt/gentoo/boot/efi",
                "mount /dev/sdb1 /mnt/gentoo/boot/efi", "mount --types proc /proc /mnt/gentoo/proc",
                "mount --rbind /sys /mnt/gentoo/sys", "mount --rbind /dev /mnt/gentoo/dev",
                "mount --bind /run /mnt/gentoo/run", "chroot /mnt/gentoo /bin/bash");
        // Every core the machine has, which is only known once it is up.
        ctx.then(SETTLE, () -> {
            ctx.type("echo 'MAKEOPTS=\"-j" + cores[0] + "\"' >> /etc/portage/make.conf");
            ctx.key(GLFW.GLFW_KEY_ENTER);
        });
        settled(ctx, "MAKEOPTS");
        lines(ctx, "emerge --sync", "eselect profile set 1", "emerge --ask --verbose --update --deep --newuse @world",
                "y", "emerge sys-kernel/gentoo-sources", "eselect kernel set 1", "cd /usr/src/linux",
                "make && make modules_install && make install", "cd /",
                "echo 'UUID=0000 / ext4 defaults,noatime 0 1' >> /etc/fstab", "echo " + NAME + " > /etc/hostname",
                "emerge app-misc/screenfetch", "emerge --ask sys-boot/grub", "y",
                "grub-install --efi-directory=/boot/efi", "grub-mkconfig -o /boot/grub/grub.cfg", "exit", "reboot");
        cameUp(ctx, LiveInstallState.Distro.GENTOO);
    }

    /** Packages from the Mirror installed with Arch's pacman and Gentoo's emerge, and run at the prompt. */
    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_mirrorPackagesAreTypedInAndUsed(final ClientTestContext ctx) {
        EraJourney.lay(ctx, EraKit.STANDARD)
                .thenServer(SETTLE, level -> mainframe(ctx, level).installMirror());
        onSystem(ctx, "arch");
        lines(ctx, "pacman -S screenfetch", "y");
        installed(ctx, SCREENFETCH);
        ran(ctx, "screenfetch");
        lines(ctx, "pacman -S vim", "y");
        installed(ctx, VIM);
        ctx.thenScreenshot(SETTLE, "extremes-arch-packages");
        onSystem(ctx, "gentoo");
        lines(ctx, "emerge screenfetch");
        installed(ctx, SCREENFETCH);
        ran(ctx, "screenfetch");
        ctx.thenScreenshot(SETTLE, "extremes-gentoo-packages");
    }

    /**
     * FreeBSD's ports at the prompt: the tree fetched and laid out, a port built, installed and used, then taken off,
     * and the package of the same program installed in its place.
     */
    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_portsAreTypedInBuiltAndUsed(final ClientTestContext ctx) {
        EraJourney.lay(ctx, EraKit.STANDARD)
                .thenServer(SETTLE, level -> mainframe(ctx, level).installMirror());
        onSystem(ctx, "freebsd");
        lines(ctx, "portsnap fetch extract");
        ctx.thenWaitUntilServer(level -> DiskFilesystem.exists(computer(ctx, level).systemDisk(),
                "usr/ports/INDEX-14"), SETTLE * 10, "the ports tree to be on the disk", level -> glass(ctx, level));
        lines(ctx, "cd /usr/ports/sysutils/screenfetch", "make install clean");
        ctx.thenWaitUntilServer(level -> computer(ctx, level).console().isInstalled(SCREENFETCH)
                        && computer(ctx, level).console().builtFromSource(SCREENFETCH), SETTLE * 10,
                "the port to be installed, built here", level -> glass(ctx, level));
        ran(ctx, "screenfetch");
        ctx.thenScreenshot(SETTLE, "extremes-port-built");
        lines(ctx, "make deinstall", "cd /", "pkg install screenfetch", "y");
        ctx.thenWaitUntilServer(level -> computer(ctx, level).console().isInstalled(SCREENFETCH)
                        && !computer(ctx, level).console().builtFromSource(SCREENFETCH), SETTLE * 10,
                "the package to take the port's place", level -> glass(ctx, level));
    }

    /*
     * The Advanced network's computer with two blank disks, the distribution's live medium in the drive north of it,
     * the world asking for the whole handbook, and the player at the live session's prompt.
     */
    private static void live(final ClientTestContext ctx, final LiveInstallState.Distro distro) {
        final EraKit kit = EraKit.ADVANCED;
        EraJourney.lay(ctx, kit)
                .thenBuild(0, world -> world.setBlock(DRIVE, ComputingModule.CD_DRIVE.get()))
                .thenServer(SETTLE, level -> {
                    mainframe(ctx, level).installMirror();
                    final PersonalComputerBlockEntity computer = computer(ctx, level);
                    computer.togglePower();
                    final ItemStackHandler hardware = computer.getHardware();
                    for (int slot = 0; slot < 2; slot++) {
                        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START + slot,
                                new ItemStack(EraParts.diskFor(kit.era())));
                    }
                    TestWorldBuilder.at(level, ctx.origin()).blockEntity(DRIVE, MediaReaderBlockEntity.class)
                            .mediaSlot().setStackInSlot(0, InstallerStacks.forSystem(id(distro)));
                    handbook(distro, true);
                    computer.togglePower();
                })
                /*
                 * The session is begun once the computer sees the medium in its drive, as a boot from it would have:
                 * begun before, the machine takes the medium for gone and ends the session on its next tick.
                 */
                .thenWaitUntilServer(level -> LiveMedium.holding(level, computer(ctx, level), distro)
                        == LiveMedium.Answer.PRESENT, EraJourney.BOOT_WAIT, "the computer to see the live medium",
                        level -> "not seen")
                .thenServer(1, level -> computer(ctx, level).console().startLiveInstall(distro));
        seated(ctx);
    }

    /* The second disk laid out for the modern firmware: the partition it reads, and the rest for the system. */
    private static void partition(final ClientTestContext ctx) {
        lines(ctx, "fdisk /dev/sdb", "g", "n 512M", "t 1 uefi", "n", "", "", "", "w");
    }

    /* The machine up on the system it was given, with its name, from the second disk; the handbook let go again. */
    private static void cameUp(final ClientTestContext ctx, final LiveInstallState.Distro distro) {
        final ResourceLocation id = id(distro);
        final String system = id.getPath();
        ctx.thenWaitUntilServer(level -> id.equals(computer(ctx, level).installedOsId())
                                && computer(ctx, level).isRunning(), EraJourney.BOOT_WAIT,
                        system + " to come up after the restart", level -> "up: "
                                + computer(ctx, level).installedOsId() + "; " + glass(ctx, level))
                .thenServer(0, level -> {
                    final PersonalComputerBlockEntity computer = computer(ctx, level);
                    ctx.assertTrue(NAME.equals(computer.console().computerName()),
                            system + " comes up named " + NAME + ": " + computer.console().computerName());
                    ctx.assertTrue(computer.bootDiskSlot() == 1, system + " boots from the second disk");
                    handbook(distro, false);
                });
        seated(ctx);
        ctx.thenScreenshot(SETTLE * 3, "extremes-" + system + "-by-hand");
    }

    /*
     * The network's computer switched off, its disk wiped and the system put on it, switched on again, and the player
     * at its prompt.
     */
    private static void onSystem(final ClientTestContext ctx, final String system) {
        ctx.thenWaitUntil(EraJourney.away(ctx), EraJourney.SCREEN_WAIT, "the screen to be left")
                .thenServer(SETTLE, level -> {
                    final PersonalComputerBlockEntity computer = computer(ctx, level);
                    computer.togglePower();
                    computer.formatDisk(0);
                    ctx.assertTrue(computer.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                            system)), system + " goes on the computer");
                    computer.togglePower();
                });
        seated(ctx);
    }

    private static void seated(final ClientTestContext ctx) {
        ctx.thenWaitUntil(EraJourney.away(ctx), EraJourney.SCREEN_WAIT, "the screen to be left")
                .thenTeleport(SETTLE, EraNetwork.AT_THE_DESK, Direction.WEST)
                .thenRightClick(SETTLE, EraNetwork.MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, EraJourney.BOOT_WAIT);
    }

    /* Each line typed and sent, the next only once the terminal is the player's again or asks something. */
    private static void lines(final ClientTestContext ctx, final String... typed) {
        for (final String line : List.of(typed)) {
            ctx.then(SETTLE, () -> {
                ctx.type(line);
                ctx.key(GLFW.GLFW_KEY_ENTER);
            });
            settled(ctx, line);
        }
    }

    /*
     * Waits until no tool holds the terminal and the machine is setting nothing up, or a tool has stopped to ask
     * something.
     */
    private static void settled(final ClientTestContext ctx, final String after) {
        ctx.thenWaitUntilServer(level -> {
            final ComputerConsoleState console = computer(ctx, level).console();
            if (console == null) {
                return false;
            }
            final var tool = console.foreground().tool();
            return (tool != null && tool.asking() != null)
                    || (!console.foreground().running() && console.setup() == null);
        }, STEP_WAIT, "the terminal to be back after " + after, level -> glass(ctx, level));
    }

    private static void installed(final ClientTestContext ctx, final String program) {
        ctx.thenWaitUntilServer(level -> computer(ctx, level).console().isInstalled(program), STEP_WAIT,
                program + " to be installed", level -> glass(ctx, level));
    }

    /* The program typed at the prompt answers, rather than being a command the shell has never heard of. */
    private static void ran(final ClientTestContext ctx, final String command) {
        lines(ctx, "clear", command);
        ctx.thenServer(SETTLE, level -> ctx.assertTrue(!glass(ctx, level).contains("not found: " + command),
                command + " answers at the prompt: " + glass(ctx, level)));
    }

    /* Whether the world asks the distribution's live install for the whole handbook. */
    private static void handbook(final LiveInstallState.Distro distro, final boolean whole) {
        ComputersServerConfig.FILE.set(distro == LiveInstallState.Distro.ARCH ? ComputersServerConfig.ARCH_EVERY_STEP
                : ComputersServerConfig.GENTOO_EVERY_STEP, whole);
    }

    private static ResourceLocation id(final LiveInstallState.Distro distro) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, distro.name().toLowerCase(Locale.ROOT));
    }

    private static String glass(final ClientTestContext ctx, final ServerLevel level) {
        final StringBuilder out = new StringBuilder();
        for (final WireLine line : computer(ctx, level).console().glass()) {
            line.spans().forEach(span -> out.append(span.text().english()));
            out.append('\n');
        }
        return out.substring(Math.max(0, out.length() - GLASS_TAIL));
    }

    private static PersonalComputerBlockEntity computer(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.COMPUTER,
                PersonalComputerBlockEntity.class);
    }

    private static MainframeBlockEntity mainframe(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.MAINFRAME, MainframeBlockEntity.class);
    }
}
