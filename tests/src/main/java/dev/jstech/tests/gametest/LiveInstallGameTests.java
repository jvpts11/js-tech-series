/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.payload.files.FilePayloads;
import dev.jstech.computers.operation.payload.files.LiveSessionFiles;
import dev.jstech.computers.operation.payload.program.TerminalTools;
import dev.jstech.computers.os.boot.BootController;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.computers.program.install.MakeOpts;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TerminalAt;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Installing Arch and Gentoo by hand, typed at a real machine's terminal the way a player types it.
 *
 * <p>The sequences are held to account a line at a time where the state machine lives. What these are for is
 * the whole thing on a machine ticking in a world: a step that takes time holds the terminal while the
 * machine moves it along, the next line is typed when the prompt comes back, and at the end of it the machine
 * restarts into the system that was built, carrying what the player chose on the way.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LiveInstallGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    /** Long enough for the drive beside the machine to have linked to it. */
    private static final int SETTLE = 8;

    /** Long enough for a kernel to compile on the test machine, which is the longest wait there is. */
    private static final int A_WHOLE_INSTALL = 12_000;

    private LiveInstallGameTests() {
    }

    /**
     * Arch, end to end, on a machine of the modern firmware: the disk laid out with a partition the firmware
     * reads, the base system over the network, the table written for it, the bootloader as a package, and a
     * restart into a system that answers to the name it was given.
     */
    @GameTest(template = ARENA, timeoutTicks = A_WHOLE_INSTALL)
    public static void arch_byHand_bootsTheSystemItBuilt(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = modernMachineWithMedium(helper, "arch");
        final TerminalAt term = new TerminalAt(mainframe, helper.getLevel());
        final GameTestSequence steps = helper.startSequence().thenExecuteAfter(SETTLE, () -> {
            mainframe.installMirror();
            mainframe.console().startLiveInstall(LiveInstallState.Distro.ARCH);
            helper.assertTrue(
                    BootController.targetForComputer(mainframe) == BootController.BootTarget.TERMINAL_ONLY,
                    "a booted live medium runs in the terminal");
            helper.assertTrue("root@archiso ~ #".equals(term.prompt()), "a root prompt: " + term.prompt());
            helper.assertTrue(term.type("cat /root/install.txt").contains("pacstrap"),
                    "the medium's own guide is there to read");
            helper.assertTrue(term.type("nmap").contains("command not found"),
                    "and the live shell knows only what a live medium carries");
            helper.assertTrue(term.type("pacstrap -K /mnt base linux linux-firmware").contains("not a mountpoint"),
                    "the base system is refused before anything is mounted, in the real words");
        });
        partitioned(helper, steps, term);
        typed(helper, steps, term, "mkfs.fat -F 32 /dev/sda1");
        typed(helper, steps, term, "mkfs.ext4 /dev/sda2");
        typed(helper, steps, term, "mount /dev/sda2 /mnt");
        typed(helper, steps, term, "mount --mkdir /dev/sda1 /mnt/boot");
        steps.thenExecute(() -> {
            helper.assertTrue(term.type("pacstrap -K /mnt base linux linux-firmware")
                    .contains("==> Creating install root at /mnt"), "the base system starts arriving");
            helper.assertTrue(term.busy(), "and holds the terminal while it does");
            term.type("arch-chroot /mnt");
            helper.assertTrue("root@archiso ~ #".equals(term.prompt()),
                    "what is typed at a tool that asked nothing goes nowhere: " + term.prompt());
        });
        idle(helper, steps, term);
        typed(helper, steps, term, "genfstab -U /mnt >> /mnt/etc/fstab");
        steps.thenExecute(() -> {
            term.type("arch-chroot /mnt");
            helper.assertTrue("[root@archiso /]#".equals(term.prompt()), "inside the new system: " + term.prompt());
            final String table = term.type("cat /etc/fstab");
            helper.assertTrue(table.contains("# /dev/sda2") && table.contains("# /dev/sda1"), table);
            helper.assertTrue(table.matches("(?s).*UUID=[0-9a-f]{8}-[0-9a-f]{4}-.*"), "the root by its identifier");
            helper.assertTrue(table.matches("(?s).*UUID=[0-9A-F]{4}-[0-9A-F]{4}\\s.*"), "the other by its serial");
            term.type("echo workshop > /etc/hostname");
            term.type("passwd");
            helper.assertTrue(term.asking().startsWith("New password"), "it asks: " + term.asking());
            term.type("hunter2");
            helper.assertTrue(term.asking().startsWith("Retype"), "and asks again: " + term.asking());
            term.type("hunter2");
            helper.assertFalse(term.busy(), "and is done once it has been told twice");
            term.type("pacman -S grub efibootmgr");
        });
        steps.thenWaitUntil(() -> helper.assertTrue(term.asking().contains("Proceed with installation?"),
                "it lists what it would install and then really asks: " + term.asking()));
        steps.thenExecute(() -> term.type(TerminalTools.ENTER));
        idle(helper, steps, term);
        typed(helper, steps, term, "grub-install --target=x86_64-efi --efi-directory=/boot --bootloader-id=GRUB");
        typed(helper, steps, term, "grub-mkconfig -o /boot/grub/grub.cfg");
        steps.thenExecute(() -> {
            term.type("exit");
            helper.assertTrue(term.type("reboot").contains("Installation complete"), "the restart completes it");
            helper.assertTrue(mainframe.console().liveInstall() == null, "the live session ends");
            helper.assertTrue(osId("arch").equals(mainframe.installedOsId()),
                    "and the machine boots what was built: " + mainframe.installedOsId());
            /*
             * What the player decided on the way survives the restart. Without this the whole sequence is
             * theatre: the distribution lands and every choice made getting it there is thrown away.
             */
            helper.assertTrue("workshop".equals(mainframe.console().computerName()),
                    "the machine keeps the name it was given: " + mainframe.console().computerName());
            helper.assertTrue("player@workshop ~ %".equals(term.prompt()),
                    "which is the name its own prompt now shows: " + term.prompt());
            final FilesystemContents disk = mainframe.diskInSlot(0).get(ComputingModule.FILESYSTEM.get());
            helper.assertTrue(disk != null && disk.files().get("/etc/fstab") != null
                            && disk.files().get("/etc/fstab").content().contains("# /dev/sda2"),
                    "and the table it wrote is on the disk it describes");
        }).thenSucceed();
    }

    /**
     * Gentoo, end to end: the archive fetched and unpacked over the disk, the package tree, the sources, a
     * kernel compiled for as long as this processor takes over one, the table written by hand, the bootloader
     * merged from source, and a restart into the system that came of it.
     */
    @GameTest(template = ARENA, timeoutTicks = A_WHOLE_INSTALL)
    public static void gentoo_byHand_bootsTheSystemItBuilt(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = modernMachineWithMedium(helper, "gentoo");
        final TerminalAt term = new TerminalAt(mainframe, helper.getLevel());
        final GameTestSequence steps = helper.startSequence().thenExecuteAfter(SETTLE, () -> {
            mainframe.installMirror();
            mainframe.console().startLiveInstall(LiveInstallState.Distro.GENTOO);
            helper.assertTrue("livecd ~ #".equals(term.prompt()), "the medium's root prompt: " + term.prompt());
        });
        partitioned(helper, steps, term);
        typed(helper, steps, term, "mkfs.fat -F 32 /dev/sda1");
        typed(helper, steps, term, "mkfs.ext4 /dev/sda2");
        steps.thenExecute(() -> {
            helper.assertTrue(term.type("mount /dev/sda2 /mnt").contains("mount point does not exist"),
                    "this distribution mounts where its handbook mounts");
            term.type("mount /dev/sda2 /mnt/gentoo");
            term.type("cd /mnt/gentoo");
            helper.assertTrue(term.type("tar xpvf stage3-*.tar.xz").contains("Cannot open: No such file"),
                    "there is nothing to unpack until it has been fetched");
            helper.assertTrue(term.type("wget mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz")
                    .contains("Resolving mainframe... done."), "the archive starts arriving");
            helper.assertTrue(term.busy(), "and holds the terminal until it has all arrived");
        });
        idle(helper, steps, term);
        steps.thenExecute(() -> {
            helper.assertTrue(term.type("ls").contains("stage3-vel64-openrc.tar.xz"), "it is where it was fetched to");
            term.type("tar xpvf stage3-*.tar.xz --xattrs-include='*.*' --numeric-owner");
            helper.assertTrue(term.busy(), "unpacking a whole system takes the terminal for a while");
        });
        idle(helper, steps, term);
        steps.thenExecute(() -> {
            term.type("mount /dev/sda1 /mnt/gentoo/efi");
            term.type("chroot /mnt/gentoo /bin/bash");
            helper.assertTrue("(chroot) livecd / #".equals(term.prompt()), "inside the new system: " + term.prompt());
            term.type("echo 'MAKEOPTS=\"-j64\"' >> /etc/portage/make.conf");
            helper.assertTrue(term.type("emerge sys-kernel/gentoo-sources").contains("portage tree is empty"),
                    "nothing merges before there is a tree to merge from");
        });
        typed(helper, steps, term, "emerge-webrsync");
        typed(helper, steps, term, "emerge sys-kernel/gentoo-sources");
        steps.thenExecute(() -> {
            helper.assertTrue(term.type("genkernel all").contains("Gentoo Linux Genkernel"),
                    "the kernel starts building");
            helper.assertTrue(term.busy(), "and holds the terminal for as long as this processor takes over it");
        });
        idle(helper, steps, term);
        steps.thenExecute(() -> {
            helper.assertTrue(term.type("blkid").contains("TYPE=\"ext4\""), "the identifiers to copy the table from");
            term.type("echo 'UUID=0000 / ext4 defaults,noatime 0 1' >> /etc/fstab");
            term.type("passwd");
            term.type("hunter2");
            term.type("hunter2");
            helper.assertTrue(term.type("grub-install --efi-directory=/efi").contains("command not found"),
                    "the bootloader is a package, and it is not merged yet");
            term.type("emerge --ask sys-boot/grub");
        });
        steps.thenWaitUntil(() -> helper.assertTrue(term.asking().contains("Would you like to merge"),
                "it lists what it would merge and then asks: " + term.asking()));
        steps.thenExecute(() -> term.type("y"));
        idle(helper, steps, term);
        typed(helper, steps, term, "grub-install --efi-directory=/efi");
        typed(helper, steps, term, "grub-mkconfig -o /boot/grub/grub.cfg");
        steps.thenExecute(() -> {
            term.type("exit");
            helper.assertTrue(term.type("reboot").contains("Installation complete"), "the restart completes it");
            helper.assertTrue(mainframe.console().liveInstall() == null, "the live session ends");
            helper.assertTrue(osId("gentoo").equals(mainframe.installedOsId()),
                    "and the machine boots what was built: " + mainframe.installedOsId());
            // The build options are the system's and not the install's: every later build is built by them.
            final FilesystemContents disk = mainframe.diskInSlot(0).get(ComputingModule.FILESYSTEM.get());
            helper.assertTrue(disk != null && disk.files().get(MakeOpts.PATH) != null
                            && disk.files().get(MakeOpts.PATH).content().contains("-j64"),
                    "and the build options written on the way are on the disk it installed to");
        }).thenSucceed();
    }

    /**
     * The partition editor is talked to through the terminal: its question stands where the prompt would, Enter
     * by itself takes a default, and nothing reaches the disk until it is told to write.
     */
    @GameTest(template = ARENA)
    public static void fdisk_isTalkedToThroughTheTerminal(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacyWithDisk(helper);
        computer.console().startLiveInstall(LiveInstallState.Distro.ARCH);
        final TerminalAt term = new TerminalAt(computer, helper.getLevel());
        helper.assertTrue(term.type("fdisk /dev/sda").contains("Welcome to fdisk"), "the editor opens from the shell");
        helper.assertTrue(term.asking().equals("Command (m for help): "), "and asks for a command: " + term.asking());
        helper.assertTrue(term.type("g").contains("Created a new GPT disklabel"), "a single letter reaches it");
        term.type("n");
        helper.assertTrue(term.asking().startsWith("Partition number (1-128, default 1)"), term.asking());
        term.type(TerminalTools.ENTER);
        helper.assertTrue(term.asking().startsWith("First sector (2048-"), term.asking());
        term.type(TerminalTools.ENTER);
        helper.assertTrue(term.type("+512M").contains("of size 512 MiB"), "a plus and a size, as it has always been");
        helper.assertFalse(term.type("lsblk").contains("sda1"), "the shell does not hear a line meant for the editor");
        helper.assertTrue(term.busy(), "which still has the terminal");
        helper.assertTrue(term.type("w").contains("The partition table has been altered."), "until it is written");
        helper.assertFalse(term.busy(), "and then the prompt comes back");
        helper.assertTrue(term.type("lsblk").contains("sda1"), "with the table on the disk");
        helper.succeed();
    }

    /** Leaving the editor without writing leaves the disk as it was, which is the promise it opens with. */
    @GameTest(template = ARENA)
    public static void fdisk_leftWithoutWritingChangesNothing(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacyWithDisk(helper);
        computer.console().startLiveInstall(LiveInstallState.Distro.GENTOO);
        final TerminalAt term = new TerminalAt(computer, helper.getLevel());
        term.type("fdisk /dev/sda");
        term.type("g");
        term.type("n 512M");
        term.type("q");
        helper.assertFalse(term.busy(), "the prompt comes back");
        helper.assertFalse(term.type("lsblk").contains("sda1"), "and nothing typed in there reached the disk");
        helper.succeed();
    }

    /**
     * The medium's editor gives the terminal away on a file of the session, which is on no disk, and what it
     * saves there is what reading the file back shows and what the steps after it read.
     */
    @GameTest(template = ARENA)
    public static void nano_opensAndSavesTheSessionsOwnFiles(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacyWithDisk(helper);
        computer.console().startLiveInstall(LiveInstallState.Distro.GENTOO);
        final TerminalAt term = new TerminalAt(computer, helper.getLevel());
        helper.assertTrue(term.type("nano -w /root/notes.txt").isEmpty(), "the editor opens without a word");
        helper.assertTrue("live:/root/notes.txt".equals(term.givenAwayOn()),
                "on the file as it was typed, marked as the session's: " + term.givenAwayOn());
        helper.assertTrue(FilePayloads.readDiskFile(helper.getLevel(), computer, "live:/root/notes.txt").isEmpty(),
                "which is not there yet, so it opens as a new one");
        helper.assertTrue(FilePayloads.readDiskFile(helper.getLevel(), computer, "live:/root/install.txt")
                .orElse("").contains("emerge"), "while the medium's own guide opens with its text");
        helper.assertTrue(LiveSessionFiles.write(computer, term.givenAwayOn(), "the table wants the root's UUID"),
                "saving writes into the session");
        helper.assertTrue(term.type("cat /root/notes.txt").contains("the table wants the root's UUID"),
                "and the shell reads back what the editor wrote");
        term.type("cd /root");
        term.type("nano notes.txt");
        helper.assertTrue(LiveSessionFiles.read(computer, term.givenAwayOn()).orElse("").contains("UUID"),
                "a name typed from inside a folder is that folder's");
        helper.assertTrue(term.type("nano /root").contains("is a directory"), "a directory is refused in words");
        helper.assertTrue(term.givenAwayOn().isEmpty(), "and the terminal stays with the shell");
        helper.succeed();
    }

    /** A machine with no Mirror on its network is told so by the fetcher, in the fetcher's own words. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void wget_withNoMirrorOnTheNetworkFetchesNothing(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = modernMachineWithMedium(helper, "gentoo");
        final TerminalAt term = new TerminalAt(mainframe, helper.getLevel());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    mainframe.console().startLiveInstall(LiveInstallState.Distro.GENTOO);
                    final String disks = term.type("lsblk");
                    helper.assertTrue(disks.contains("sda") && !disks.contains("sdb"),
                            "the one disk that is really in it: " + disks);
                    term.type("wget mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz");
                    helper.assertTrue(term.busy(), "the fetcher tries for a moment");
                })
                .thenWaitUntil(() -> helper.assertFalse(term.busy(), "before it gives up"))
                .thenExecute(() -> helper.assertFalse(term.type("ls").contains("stage3"), "having fetched nothing"))
                .thenSucceed();
    }

    /**
     * Asking whether the medium is still in the drive never takes the session away.
     *
     * <p>This is the bug that made a whole installation vanish without a word. The question is asked by the
     * gate on every line typed and by the container every tick, and it used to end the session the moment it
     * did not like the answer: a drive one tick late to load read as a drive with nothing in it, the
     * installation was thrown away, and the terminal stayed on the screen answering nothing at all.
     */
    @GameTest(template = ARENA)
    public static void validateOsSession_neverThrowsTheSessionAwayByItself(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacyWithDisk(helper);
        computer.console().startLiveInstall(LiveInstallState.Distro.GENTOO);
        computer.validateOsSession();
        computer.validateOsSession();
        helper.assertTrue(computer.console().liveInstall() != null,
                "asking where the medium is must never be what takes it away");
        helper.succeed();
    }

    /** Ending a session is the machine's own business, and it does end one whose medium is really gone. */
    @GameTest(template = ARENA)
    public static void settleLiveInstall_endsTheSessionWhenNoDriveHoldsTheMedium(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacyWithDisk(helper);
        computer.console().startLiveInstall(LiveInstallState.Distro.GENTOO);
        helper.assertTrue(computer.settleLiveInstall(), "no drive holds it, so the session is over");
        helper.assertTrue(computer.console().liveInstall() == null, "and it is gone");
        helper.assertFalse(computer.settleLiveInstall(), "and there is nothing left to end a second time");
        helper.succeed();
    }

    /** Types a line, and waits for whatever it left running to give the prompt back. */
    private static void typed(final GameTestHelper helper, final GameTestSequence steps, final TerminalAt term,
                              final String line) {
        steps.thenExecute(() -> term.type(line));
        idle(helper, steps, term);
    }

    /** Waits for the tool in front to end, the way a player waits for the prompt. */
    private static void idle(final GameTestHelper helper, final GameTestSequence steps, final TerminalAt term) {
        steps.thenWaitUntil(() -> helper.assertFalse(term.busy(), "a tool is still in front of the terminal"));
    }

    /** Lays the disk out for the modern firmware: a partition it reads, and the rest for the system. */
    private static void partitioned(final GameTestHelper helper, final GameTestSequence steps,
                                    final TerminalAt term) {
        steps.thenExecute(() -> {
            term.type("fdisk /dev/sda");
            term.type("g");
            term.type("n 512M");
            term.type("t 1 uefi");
            term.type("n");
            term.type(TerminalTools.ENTER);
            term.type(TerminalTools.ENTER);
            term.type(TerminalTools.ENTER);
            helper.assertTrue(term.type("p").contains("EFI System"), "the editor prints what it was told");
            term.type("w");
            helper.assertFalse(term.busy(), "and hands the terminal back once it is written");
        });
    }

    private static ResourceLocation osId(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    /**
     * A running machine of the modern firmware with a system on it, and beside it a drive holding that
     * distribution's live medium, which is what a machine running one really has.
     */
    private static MainframeBlockEntity modernMachineWithMedium(final GameTestHelper helper, final String distro) {
        helper.setBlock(WHERE, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(WHERE) instanceof MainframeBlockEntity mainframe)) {
            throw new IllegalStateException("no mainframe at " + WHERE);
        }
        final ItemStackHandler parts = mainframe.getInventory();
        parts.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_MTX_P.get()));
        parts.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START,
                new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        parts.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        parts.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        // A graphics card gives the machine peripheral ports, which is what the drive beside it links to.
        parts.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                new ItemStack(ComputingModule.GPU_HD_7970.get()));
        parts.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        mainframe.togglePower();
        if (!mainframe.installOs(osId("ubuntu"))) {
            throw new IllegalStateException("the test machine took no system");
        }
        final BlockPos beside = WHERE.east();
        helper.setBlock(beside, ComputingModule.CD_DRIVE.get());
        if (!(helper.getBlockEntity(beside) instanceof MediaReaderBlockEntity reader)) {
            throw new IllegalStateException("no drive at " + beside);
        }
        final ItemStack medium = new ItemStack(ComputingModule.CD_ROM.get());
        MediaItem.setKind(medium, MediaKind.OS_INSTALL);
        MediaItem.setPayload(medium, osId(distro));
        reader.mediaSlot().setStackInSlot(0, medium);
        return mainframe;
    }

    /** A Legacy machine with one disk in it, which is what a live medium is usually put into. */
    private static PersonalComputerBlockEntity legacyWithDisk(final GameTestHelper helper) {
        helper.setBlock(WHERE, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(WHERE) instanceof PersonalComputerBlockEntity computer)) {
            throw new IllegalStateException("no legacy personal computer at " + WHERE);
        }
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_DUO_E4300.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_DDR2_2048.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                new ItemStack(HardwareItems.PSU_500B.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        computer.togglePower();
        return computer;
    }
}
