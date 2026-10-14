/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.item.CpuItem;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.operation.payload.program.TerminalTools;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.computers.program.install.MakeOpts;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.EraParts;
import dev.jstech.tests.testkit.computers.TerminalHand;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Arch and Gentoo installed by hand in many configurations, each typed line for line at a machine of the modern
 * firmware, with whatever a line leaves running moved along until it gives the prompt back. Every layout: the era of
 * the machine (the Standard and the Advanced), a Personal Computer on the era's network or a Mainframe of its own, the
 * firmware's partition mounted at /boot, /boot/efi or /efi, the system on the one disk or on the second of two, and
 * the world asking only for what a system boots by or for the whole handbook. Each comes up after the restart with
 * its name, its filesystem table on its disk, the program installed on the way and, on Gentoo, its build options; and
 * it comes up again after a second restart. Each step a restart cannot go on without, left out, is named by the
 * restart and then done, and the restart after goes through. Gentoo takes each of its profiles by its number and by its
 * name, builds its kernel with genkernel or by hand, with no build options, one job or every core, and fetches its
 * tree either way. A disk too small for the layout takes no base system, and nor does a root partition too small
 * for the base system, which the tool laying it out says.
 *
 * <p>The machines of the older firmware, whose disks this fdisk lays out only as GPT, and the systemd profile over
 * the OpenRC stage 3 are combinations the game does not define yet, and are left out.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LiveInstallMatrixGameTests {

    private static final String BENCH = "bench";
    private static final int SETTLE = 8;
    private static final int TIMEOUT = 2_000;
    private static final String NAME = "workshop";
    /** How much of a failed session's typing a failure shows. */
    private static final int TRANSCRIPT = 1_500;
    /** A root partition the partitions fit around and no base system fits in. */
    private static final String SMALL_ROOT = "600M";
    /** Where the Mainframe of its own stands, well away from the era's network, and its drive beside it. */
    private static final BlockPos LONE_MAINFRAME = new BlockPos(30, 2, 30);
    /** The drive north of the network's computer, holding the distribution's medium. */
    private static final BlockPos PC_DRIVE = new BlockPos(9, 2, 1);
    private static final ResourceLocation UBUNTU = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "ubuntu");
    private static final List<String> ESPS = List.of("/boot", "/boot/efi", "/efi");
    /** The Gentoo profiles the stage 3 here takes: every one but the systemd one, which wants another stage 3. */
    private static final List<Integer> PROFILES = List.of(1, 3, 4, 5, 6, 7);
    private static final List<String> PROFILE_NAMES = List.of("default/linux/vel64/23.0",
            "default/linux/vel64/23.0/desktop", "default/linux/vel64/23.0/desktop/gnome",
            "default/linux/vel64/23.0/desktop/plasma", "default/linux/vel64/23.0/no-multilib",
            "default/linux/vel64/23.0/hardened");

    private LiveInstallMatrixGameTests() {
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void standard_archBootsInEveryLayout(final GameTestHelper helper) {
        layouts(helper, LiveInstallState.Distro.ARCH, EraKit.STANDARD);
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void advanced_archBootsInEveryLayout(final GameTestHelper helper) {
        layouts(helper, LiveInstallState.Distro.ARCH, EraKit.ADVANCED);
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void standard_gentooBootsInEveryLayout(final GameTestHelper helper) {
        layouts(helper, LiveInstallState.Distro.GENTOO, EraKit.STANDARD);
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void advanced_gentooBootsInEveryLayout(final GameTestHelper helper) {
        layouts(helper, LiveInstallState.Distro.GENTOO, EraKit.ADVANCED);
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void arch_eachLeftOutStepIsNamedAtTheRestartThenDone(final GameTestHelper helper) {
        leftOut(helper, LiveInstallState.Distro.ARCH, List.of(Step.FSTAB, Step.GRUB_INSTALL, Step.GRUB_CONFIG,
                Step.CLOCK, Step.HOSTNAME));
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void gentoo_eachLeftOutStepIsNamedAtTheRestartThenDone(final GameTestHelper helper) {
        leftOut(helper, LiveInstallState.Distro.GENTOO, List.of(Step.FSTAB, Step.KERNEL, Step.GRUB_INSTALL,
                Step.GRUB_CONFIG, Step.BIND, Step.PROFILE, Step.WORLD, Step.KERNEL_LINK, Step.HOSTNAME));
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void gentoo_takesEveryProfileByItsNumberAndByItsName(final GameTestHelper helper) {
        final EraNetwork network = network(helper, EraKit.STANDARD);
        helper.startSequence().thenExecuteAfter(SETTLE, () -> {
            final List<String> failures = new ArrayList<>();
            for (int i = 0; i < PROFILES.size(); i++) {
                final Config number = Config.gentoo(EraKit.STANDARD).profile(String.valueOf(PROFILES.get(i)));
                failures.addAll(run(helper, network, null, number));
                final Config name = Config.gentoo(EraKit.STANDARD).profile(PROFILE_NAMES.get(i));
                failures.addAll(run(helper, network, null, name));
            }
            helper.assertTrue(failures.isEmpty(), failures.size() + " failed: " + String.join(" | ", failures));
        }).thenSucceed();
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void gentoo_buildsItsKernelAndFetchesItsTreeEveryWay(final GameTestHelper helper) {
        final EraNetwork network = network(helper, EraKit.ADVANCED);
        helper.startSequence().thenExecuteAfter(SETTLE, () -> {
            final List<String> failures = new ArrayList<>();
            final int cores = coresOf(network.computer());
            for (final boolean byHand : new boolean[] {false, true}) {
                for (final String jobs : new String[] {"", "-j1", "-j" + cores}) {
                    for (final boolean webrsync : new boolean[] {true, false}) {
                        failures.addAll(run(helper, network, null, Config.gentoo(EraKit.ADVANCED)
                                .kernelByHand(byHand).makeOpts(jobs).webrsync(webrsync)));
                    }
                }
            }
            helper.assertTrue(failures.isEmpty(), failures.size() + " failed: " + String.join(" | ", failures));
        }).thenSucceed();
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void aDiskTooSmall_takesNoBaseSystem(final GameTestHelper helper) {
        final EraNetwork network = network(helper, EraKit.STANDARD);
        helper.startSequence().thenExecuteAfter(SETTLE, () -> {
            for (final LiveInstallState.Distro distro : LiveInstallState.Distro.values()) {
                final TerminalHand hand = fresh(helper, network, distro, Disks.SMALL);
                partition(hand, "sda");
                if (distro == LiveInstallState.Distro.ARCH) {
                    archBase(hand, "sda", "/boot");
                } else {
                    gentooBase(hand, "sda", "/efi");
                }
                final String refused = hand.type("reboot");
                helper.assertTrue(refused.toLowerCase(Locale.ROOT).contains("no base system"),
                        distro + ": the restart says there is no base system; the session went\n"
                                + hand.transcript(TRANSCRIPT));
            }
        }).thenSucceed();
    }

    /**
     * A root partition the base system does not fit in takes none of it, and the tool that lays it says so in its own
     * words: pacman counts the room first, tar runs out of it.
     */
    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void aRootTooSmall_takesNoBaseSystemAndSaysWhy(final GameTestHelper helper) {
        final EraNetwork network = network(helper, EraKit.STANDARD);
        helper.startSequence().thenExecuteAfter(SETTLE, () -> {
            for (final LiveInstallState.Distro distro : LiveInstallState.Distro.values()) {
                final TerminalHand hand = fresh(helper, network, distro, Disks.ONE);
                hand.type("fdisk /dev/sda");
                hand.type("g");
                hand.type("n 512M");
                hand.type("t 1 uefi");
                hand.type("n " + SMALL_ROOT);
                hand.type("w");
                if (distro == LiveInstallState.Distro.ARCH) {
                    archBase(hand, "sda", "/boot");
                } else {
                    gentooBase(hand, "sda", "/efi");
                }
                final String said = hand.transcript(TRANSCRIPT);
                helper.assertTrue(said.contains(distro == LiveInstallState.Distro.ARCH
                        ? "not enough free disk space" : "No space left on device"),
                        distro + ": the base system does not fit and the tool says so; the session went\n" + said);
                helper.assertTrue(hand.type("reboot").toLowerCase(Locale.ROOT).contains("no base system"),
                        distro + ": the restart says there is no base system");
            }
        }).thenSucceed();
    }

    /* Every layout of the distribution in the era: on the era's network's computer, and on a Mainframe alone. */
    private static void layouts(final GameTestHelper helper, final LiveInstallState.Distro distro, final EraKit kit) {
        final EraNetwork network = network(helper, kit);
        final MainframeBlockEntity lone = lone(helper, kit);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(lone.networkUuid() != null && lone.isMirrorActive(),
                        "the lone Mainframe to serve its own Mirror"))
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> failures = new ArrayList<>();
                    for (final String esp : ESPS) {
                        for (final boolean handbook : new boolean[] {false, true}) {
                            for (final Disks disks : List.of(Disks.ONE, Disks.TWO)) {
                                failures.addAll(run(helper, network, null, Config.of(distro, kit).esp(esp)
                                        .handbook(handbook).disks(disks)));
                            }
                            failures.addAll(run(helper, null, lone, Config.of(distro, kit).esp(esp)
                                    .handbook(handbook).onAMainframe()));
                        }
                    }
                    helper.assertTrue(failures.isEmpty(), distro + ": " + failures.size() + " failed: "
                            + String.join(" | ", failures));
                }).thenSucceed();
    }

    /*
     * Each step left out of an otherwise whole install, in the whole handbook's world: the restart names it and goes
     * no further, and once it is done the restart after goes through.
     */
    private static void leftOut(final GameTestHelper helper, final LiveInstallState.Distro distro,
                                final List<Step> steps) {
        final EraNetwork network = network(helper, EraKit.STANDARD);
        helper.startSequence().thenExecuteAfter(SETTLE, () -> {
            final List<String> failures = new ArrayList<>();
            for (final Step step : steps) {
                failures.addAll(run(helper, network, null, Config.of(distro, EraKit.STANDARD).handbook(true)
                        .leaving(step)));
            }
            helper.assertTrue(failures.isEmpty(), distro + ": " + failures.size() + " failed: "
                    + String.join(" | ", failures));
        }).thenSucceed();
    }

    /* The era's network with the Mirror on its Mainframe, and a drive north of its computer for the medium. */
    private static EraNetwork network(final GameTestHelper helper, final EraKit kit) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final EraNetwork network = EraNetwork.lay(world, kit);
        network.mainframe().installMirror();
        world.setBlock(PC_DRIVE, ComputingModule.CD_DRIVE.get());
        return network;
    }

    /*
     * One install in one configuration, on the era's network's computer or on the lone Mainframe, with what it found
     * wrong, each with the configuration it was in.
     */
    private static List<String> run(final GameTestHelper helper, final EraNetwork network,
                                    final MainframeBlockEntity lone, final Config config) {
        final List<String> failures = new ArrayList<>();
        final boolean everyStep = config.handbook;
        final boolean wasArch = ComputersServerConfig.archEveryStep();
        final boolean wasGentoo = ComputersServerConfig.gentooEveryStep();
        ComputersServerConfig.FILE.set(ComputersServerConfig.ARCH_EVERY_STEP, everyStep);
        ComputersServerConfig.FILE.set(ComputersServerConfig.GENTOO_EVERY_STEP, everyStep);
        try {
            final TerminalHand hand = config.mainframe ? again(helper, lone, config.kit, config.distro)
                    : fresh(helper, network, config.distro, config.disks);
            final String dev = config.disks == Disks.TWO ? "sdb" : "sda";
            if (config.distro == LiveInstallState.Distro.ARCH) {
                arch(hand, config, dev);
            } else {
                gentoo(hand, config, dev);
            }
            hand.type("exit");
            final String restart = hand.type("reboot");
            if (config.left != null) {
                if (!restart.contains(config.left.says)) {
                    failures.add(config + ": the restart does not name " + config.left + ": " + restart);
                }
                config.left.doIt(hand, config, dev);
                hand.type("exit");
                final String again = hand.type("reboot");
                if (!again.contains("Installation complete")) {
                    failures.add(config + ": with " + config.left + " done the restart still refuses: " + again);
                }
            } else if (!restart.contains("Installation complete")) {
                failures.add(config + ": the restart refuses: " + restart);
            }
            failures.addAll(came(hand.machine(), config, dev));
            hand.machine().togglePower();
            hand.machine().togglePower();
            if (!config.distroId().equals(hand.machine().installedOsId())) {
                failures.add(config + ": a second restart does not bring the system up: "
                        + hand.machine().installedOsId());
            }
            if (!failures.isEmpty()) {
                failures.add(config + ": the session ended with\n" + hand.transcript(TRANSCRIPT));
            }
        } catch (final RuntimeException broken) {
            failures.add(config + ": " + broken);
        } finally {
            ComputersServerConfig.FILE.set(ComputersServerConfig.ARCH_EVERY_STEP, wasArch);
            ComputersServerConfig.FILE.set(ComputersServerConfig.GENTOO_EVERY_STEP, wasGentoo);
        }
        return failures;
    }

    /* What the machine that came up should have: its system, its name, its table, its program, its build options. */
    private static List<String> came(final AbstractComputerBlockEntity machine, final Config config, final String dev) {
        final List<String> failures = new ArrayList<>();
        if (!config.distroId().equals(machine.installedOsId())) {
            failures.add(config + ": the system that came up is " + machine.installedOsId());
            return failures;
        }
        if (machine.console() == null || !NAME.equals(machine.console().computerName())) {
            failures.add(config + ": the machine is called "
                    + (machine.console() == null ? "nothing" : machine.console().computerName()));
        }
        final FilesystemContents disk = machine.diskInSlot(config.disks == Disks.TWO ? 1 : 0)
                .get(ComputingComponents.FILESYSTEM.get());
        final String table = disk == null || disk.files().get("/etc/fstab") == null ? ""
                : disk.files().get("/etc/fstab").content();
        if (!table.contains(config.distro == LiveInstallState.Distro.ARCH ? "# /dev/" + dev + "2" : "UUID=")) {
            failures.add(config + ": the filesystem table on the disk is " + table);
        }
        if (!machine.console().isInstalled("jsc:screenfetch")) {
            failures.add(config + ": the program installed on the way is not there");
        }
        if (config.distro == LiveInstallState.Distro.GENTOO && !config.makeOpts.isEmpty()) {
            final String options = disk == null || disk.files().get(MakeOpts.PATH) == null ? ""
                    : disk.files().get(MakeOpts.PATH).content();
            if (!options.contains(config.makeOpts)) {
                failures.add(config + ": the build options on the disk are " + options);
            }
        }
        return failures;
    }

    /* Arch, line for line, leaving out the step the configuration leaves out. */
    private static void arch(final TerminalHand hand, final Config config, final String dev) {
        partition(hand, dev);
        archBase(hand, dev, config.esp);
        if (config.left != Step.FSTAB) {
            hand.type("genfstab -U /mnt >> /mnt/etc/fstab");
        }
        hand.type("arch-chroot /mnt");
        if (config.handbook && config.left != Step.CLOCK) {
            hand.type("hwclock --systohc");
        }
        if (config.left != Step.HOSTNAME) {
            hand.type("echo " + NAME + " > /etc/hostname");
        }
        hand.type("mkinitcpio -P");
        hand.yes("pacman -S screenfetch");
        hand.yes("pacman -S grub efibootmgr");
        if (config.left != Step.GRUB_INSTALL) {
            hand.type("grub-install --target=x86_64-efi --efi-directory=" + config.esp + " --bootloader-id=GRUB");
        }
        if (config.left != Step.GRUB_CONFIG) {
            hand.type("grub-mkconfig -o /boot/grub/grub.cfg");
        }
    }

    /*
     * The disks made, the root mounted with the firmware's partition beneath it, and the base system laid down with
     * a package more than the guide's three.
     */
    private static void archBase(final TerminalHand hand, final String dev, final String esp) {
        hand.type("mkfs.fat -F 32 /dev/" + dev + "1");
        hand.type("mkfs.ext4 /dev/" + dev + "2");
        hand.type("mount /dev/" + dev + "2 /mnt");
        hand.type("mount --mkdir /dev/" + dev + "1 /mnt" + esp);
        hand.type("pacstrap -K /mnt base linux linux-firmware nano");
    }

    /* Gentoo, line for line, leaving out the step the configuration leaves out. */
    private static void gentoo(final TerminalHand hand, final Config config, final String dev) {
        partition(hand, dev);
        gentooBase(hand, dev, config.esp);
        if (config.handbook && config.left != Step.BIND) {
            hand.type("mount --types proc /proc /mnt/gentoo/proc");
            hand.type("mount --rbind /sys /mnt/gentoo/sys");
            hand.type("mount --rbind /dev /mnt/gentoo/dev");
            hand.type("mount --bind /run /mnt/gentoo/run");
        }
        hand.type("chroot /mnt/gentoo /bin/bash");
        if (!config.makeOpts.isEmpty()) {
            hand.type("echo 'MAKEOPTS=\"" + config.makeOpts + "\"' >> /etc/portage/make.conf");
        }
        hand.type(config.webrsync ? "emerge-webrsync" : "emerge --sync");
        // The whole handbook asks for a profile, and the first is what a player who reads no further takes.
        final String profile = config.profile.isEmpty() && config.handbook ? "1" : config.profile;
        if (!profile.isEmpty() && config.left != Step.PROFILE) {
            hand.type("eselect profile set " + profile);
        }
        if (config.handbook && config.left != Step.WORLD) {
            hand.yes("emerge --ask --verbose --update --deep --newuse @world");
        }
        hand.type("emerge sys-kernel/gentoo-sources");
        if ((config.handbook || config.kernelByHand) && config.left != Step.KERNEL_LINK) {
            hand.type("eselect kernel set 1");
        }
        if (config.left != Step.KERNEL) {
            kernel(hand, config.kernelByHand);
        }
        if (config.left != Step.FSTAB) {
            hand.type("echo 'UUID=0000 / ext4 defaults,noatime 0 1' >> /etc/fstab");
        }
        if (config.left != Step.HOSTNAME) {
            hand.type("echo " + NAME + " > /etc/hostname");
        }
        hand.type("emerge app-misc/screenfetch");
        hand.yes("emerge --ask sys-boot/grub");
        if (config.left != Step.GRUB_INSTALL) {
            hand.type("grub-install --efi-directory=" + config.esp);
        }
        if (config.left != Step.GRUB_CONFIG) {
            hand.type("grub-mkconfig -o /boot/grub/grub.cfg");
        }
    }

    /* The disks made, the root mounted, the stage 3 fetched and unpacked, the firmware's partition mounted in it. */
    private static void gentooBase(final TerminalHand hand, final String dev, final String esp) {
        hand.type("mkfs.fat -F 32 /dev/" + dev + "1");
        hand.type("mkfs.ext4 /dev/" + dev + "2");
        hand.type("mount /dev/" + dev + "2 /mnt/gentoo");
        hand.type("cd /mnt/gentoo");
        hand.type("wget mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz");
        hand.type("tar xpvf stage3-*.tar.xz --xattrs-include='*.*' --numeric-owner");
        hand.type("mkdir -p /mnt/gentoo" + esp);
        hand.type("mount /dev/" + dev + "1 /mnt/gentoo" + esp);
    }

    /* The kernel, built by genkernel or by hand from its sources with the handbook's three steps on one line. */
    private static void kernel(final TerminalHand hand, final boolean byHand) {
        if (!byHand) {
            hand.type("genkernel all");
            return;
        }
        hand.type("cd /usr/src/linux");
        hand.type("make && make modules_install && make install");
        hand.type("cd /");
    }

    /* The disk laid out for the modern firmware: the partition it reads, and the rest for the system. */
    private static void partition(final TerminalHand hand, final String dev) {
        hand.type("fdisk /dev/" + dev);
        hand.type("g");
        hand.type("n 512M");
        hand.type("t 1 uefi");
        hand.type("n");
        hand.type(TerminalTools.ENTER);
        hand.type(TerminalTools.ENTER);
        hand.type(TerminalTools.ENTER);
        hand.type("w");
    }

    /*
     * The network's computer, switched off, given blank disks (one, two, or one too small for any base system) and
     * switched on again, its drive holding the distribution's medium, and the live session begun on it.
     */
    private static TerminalHand fresh(final GameTestHelper helper, final EraNetwork network,
                              final LiveInstallState.Distro distro, final Disks disks) {
        final PersonalComputerBlockEntity computer = network.computer();
        if (computer.isRunning()) {
            computer.togglePower();
        }
        final ItemStackHandler hardware = computer.getHardware();
        final Item disk = disks == Disks.SMALL ? EraParts.smallestDiskFor(EraKit.VINTAGE.era())
                : EraParts.diskFor(network.kit().era());
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START, new ItemStack(disk));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START + 1,
                disks == Disks.TWO ? new ItemStack(disk) : ItemStack.EMPTY);
        medium(helper, PC_DRIVE, distro);
        computer.togglePower();
        computer.console().startLiveInstall(distro);
        return new TerminalHand(computer, helper.getLevel());
    }

    /*
     * A Mainframe of the era standing alone, well away from the era's network: switched on running Ubuntu, serving
     * its own Mirror, a drive beside it for the medium.
     */
    private static MainframeBlockEntity lone(final GameTestHelper helper, final EraKit kit) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.setBlock(LONE_MAINFRAME, kit.mainframe());
        final MainframeBlockEntity mainframe = world.blockEntity(LONE_MAINFRAME, MainframeBlockEntity.class);
        final ItemStackHandler parts = mainframe.getInventory();
        final Item board = kit.mainframeBoard();
        parts.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
        parts.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(EraParts.cpuFor(board)));
        parts.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START, new ItemStack(EraParts.ramFor(board)));
        parts.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(EraParts.psuFor(kit.era())));
        parts.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                new ItemStack(EraParts.cardFor(board, GpuItem.class)));
        parts.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START, new ItemStack(EraParts.diskFor(kit.era())));
        mainframe.togglePower();
        if (!mainframe.installOs(UBUNTU)) {
            throw new IllegalStateException("the lone " + kit + " Mainframe took no system");
        }
        mainframe.installMirror();
        world.setBlock(LONE_MAINFRAME.east(), ComputingModule.CD_DRIVE.get());
        return mainframe;
    }

    /*
     * The lone Mainframe switched off, given a blank disk, switched on again with the distribution's medium in its
     * drive, and the live session begun on it.
     */
    private static TerminalHand again(final GameTestHelper helper, final MainframeBlockEntity mainframe,
                                      final EraKit kit, final LiveInstallState.Distro distro) {
        if (mainframe.isRunning()) {
            mainframe.togglePower();
        }
        mainframe.getInventory().setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(EraParts.diskFor(kit.era())));
        medium(helper, LONE_MAINFRAME.east(), distro);
        mainframe.togglePower();
        mainframe.console().startLiveInstall(distro);
        return new TerminalHand(mainframe, helper.getLevel());
    }

    private static void medium(final GameTestHelper helper, final BlockPos drive,
                               final LiveInstallState.Distro distro) {
        if (!(helper.getBlockEntity(drive) instanceof MediaReaderBlockEntity reader)) {
            throw new IllegalStateException("no drive at " + drive);
        }
        reader.mediaSlot().setStackInSlot(0, InstallerStacks.forSystem(ResourceLocation.fromNamespaceAndPath(
                JsComputers.MODID, distro.name().toLowerCase(Locale.ROOT))));
    }

    private static int coresOf(final PersonalComputerBlockEntity computer) {
        return computer.getHardware().getStackInSlot(PersonalComputerBlockEntity.CPU_SLOT).getItem()
                instanceof CpuItem cpu ? cpu.spec().cores() : 1;
    }

    /** How many disks the network's computer is given, or a disk too small. */
    private enum Disks {
        ONE, TWO, SMALL
    }

    /**
     * A step a restart cannot go on without, by what the restart says when it is left out, and how it is done
     * afterwards from where the session stands then: at the medium's own prompt, outside the new system.
     */
    private enum Step {
        FSTAB("/etc/fstab") {
            @Override
            void doIt(final TerminalHand hand, final Config config, final String dev) {
                if (config.distro == LiveInstallState.Distro.ARCH) {
                    hand.type("genfstab -U /mnt >> /mnt/etc/fstab");
                    return;
                }
                inside(hand, config);
                hand.type("echo 'UUID=0000 / ext4 defaults,noatime 0 1' >> /etc/fstab");
            }
        },
        KERNEL("no kernel") {
            /* With no kernel the bootloader would not go on either, so it follows the kernel. */
            @Override
            void doIt(final TerminalHand hand, final Config config, final String dev) {
                inside(hand, config);
                kernel(hand, false);
                hand.type("grub-install --efi-directory=" + config.esp);
                hand.type("grub-mkconfig -o /boot/grub/grub.cfg");
            }
        },
        GRUB_INSTALL("no bootloader") {
            @Override
            void doIt(final TerminalHand hand, final Config config, final String dev) {
                inside(hand, config);
                hand.type(config.distro == LiveInstallState.Distro.ARCH
                        ? "grub-install --target=x86_64-efi --efi-directory=" + config.esp + " --bootloader-id=GRUB"
                        : "grub-install --efi-directory=" + config.esp);
                hand.type("grub-mkconfig -o /boot/grub/grub.cfg");
            }
        },
        GRUB_CONFIG("nothing for the bootloader to start") {
            @Override
            void doIt(final TerminalHand hand, final Config config, final String dev) {
                inside(hand, config);
                hand.type("grub-mkconfig -o /boot/grub/grub.cfg");
            }
        },
        CLOCK("hardware clock") {
            @Override
            void doIt(final TerminalHand hand, final Config config, final String dev) {
                inside(hand, config);
                hand.type("hwclock --systohc");
            }
        },
        HOSTNAME("has no name") {
            @Override
            void doIt(final TerminalHand hand, final Config config, final String dev) {
                inside(hand, config);
                hand.type("echo " + NAME + " > /etc/hostname");
            }
        },
        BIND("were not bound") {
            @Override
            void doIt(final TerminalHand hand, final Config config, final String dev) {
                hand.type("mount --types proc /proc /mnt/gentoo/proc");
                hand.type("mount --rbind /sys /mnt/gentoo/sys");
                hand.type("mount --rbind /dev /mnt/gentoo/dev");
                hand.type("mount --bind /run /mnt/gentoo/run");
            }
        },
        PROFILE("no profile") {
            @Override
            void doIt(final TerminalHand hand, final Config config, final String dev) {
                inside(hand, config);
                hand.type("eselect profile set 1");
            }
        },
        WORLD("brought up to date") {
            @Override
            void doIt(final TerminalHand hand, final Config config, final String dev) {
                inside(hand, config);
                hand.yes("emerge --ask --verbose --update --deep --newuse @world");
            }
        },
        KERNEL_LINK("points nowhere") {
            @Override
            void doIt(final TerminalHand hand, final Config config, final String dev) {
                inside(hand, config);
                hand.type("eselect kernel set 1");
            }
        };

        private final String says;

        Step(final String says) {
            this.says = says;
        }

        abstract void doIt(TerminalHand hand, Config config, String dev);

        /* Back into the new system, as each distribution steps into it. */
        private static void inside(final TerminalHand hand, final Config config) {
            hand.type(config.distro == LiveInstallState.Distro.ARCH ? "arch-chroot /mnt"
                    : "chroot /mnt/gentoo /bin/bash");
        }
    }

    /** One configuration of an install, built up a choice at a time from the defaults. */
    private static final class Config {

        private final LiveInstallState.Distro distro;
        private final EraKit kit;
        private String esp = "/boot";
        private boolean handbook;
        private Disks disks = Disks.ONE;
        private boolean mainframe;
        private Step left;
        private String profile = "";
        private boolean kernelByHand;
        private String makeOpts = "-j4";
        private boolean webrsync = true;

        private Config(final LiveInstallState.Distro distro, final EraKit kit) {
            this.distro = distro;
            this.kit = kit;
            this.esp = distro == LiveInstallState.Distro.GENTOO ? "/efi" : "/boot";
        }

        static Config of(final LiveInstallState.Distro distro, final EraKit kit) {
            return new Config(distro, kit);
        }

        static Config gentoo(final EraKit kit) {
            return new Config(LiveInstallState.Distro.GENTOO, kit);
        }

        Config esp(final String where) {
            this.esp = where;
            return this;
        }

        Config handbook(final boolean whole) {
            this.handbook = whole;
            return this;
        }

        Config disks(final Disks how) {
            this.disks = how;
            return this;
        }

        Config onAMainframe() {
            this.mainframe = true;
            return this;
        }

        Config leaving(final Step step) {
            this.left = step;
            return this;
        }

        Config profile(final String typed) {
            this.profile = typed;
            return this;
        }

        Config kernelByHand(final boolean byHand) {
            this.kernelByHand = byHand;
            return this;
        }

        Config makeOpts(final String jobs) {
            this.makeOpts = jobs;
            return this;
        }

        Config webrsync(final boolean archive) {
            this.webrsync = archive;
            return this;
        }

        ResourceLocation distroId() {
            return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, distro.name().toLowerCase(Locale.ROOT));
        }

        @Override
        public String toString() {
            return distro.name().toLowerCase(Locale.ROOT) + "[" + kit.name().toLowerCase(Locale.ROOT)
                    + (mainframe ? " mainframe" : " pc " + disks.name().toLowerCase(Locale.ROOT)) + " esp=" + esp
                    + (handbook ? " handbook" : "") + (left != null ? " without " + left : "")
                    + (profile.isEmpty() ? "" : " profile=" + profile) + (kernelByHand ? " make" : "")
                    + " makeopts=" + (makeOpts.isEmpty() ? "none" : makeOpts) + (webrsync ? "" : " sync") + "]";
        }
    }
}
