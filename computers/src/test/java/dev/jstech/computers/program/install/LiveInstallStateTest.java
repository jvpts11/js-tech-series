/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveInstallStateTest {

    private static LiveInstallState.Env env(final boolean mirror, final long now) {
        return new LiveInstallState.Env(
                List.of(new LiveInstallState.Device("sda", 20_480), new LiveInstallState.Device("sdb", 512_000)),
                mirror, now, 4, 2000, 4, false);
    }

    private static LiveInstallState.Result run(final LiveInstallState st, final String line, final boolean mirror,
                                               final long now) {
        return st.run(line, env(mirror, now));
    }

    /**
     * How far the clock moves between one command and the next: more than any fetch in these sequences takes,
     * since a step that waits for one is not what most of them are asking about.
     */
    private static final long APART = 4_000L;

    private long clock;

    /**
     * One command, later than the one before it.
     *
     * <p>Steps that fetch something take time now, and the steps after them wait for it, so a sequence written
     * in order has to be a sequence in time as well. The tests that are about the waiting say their own times.
     */
    private LiveInstallState.Result step(final LiveInstallState st, final String line) {
        this.clock += APART;
        return st.run(line, env(true, this.clock));
    }

    @Test
    void arch_fullSequence_completesOnReboot() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        assertTrue(step(st, "mkfs.ext4 /dev/sda").ok());
        assertTrue(step(st, "mount /dev/sda /mnt").ok());
        assertTrue(step(st, "pacstrap /mnt base linux").ok());
        assertTrue(step(st, "genfstab -U /mnt >> /mnt/etc/fstab").ok());
        assertTrue(step(st, "arch-chroot /mnt").ok());
        assertEquals("[root@archiso /]#", st.prompt());
        assertTrue(step(st, "hostname workshop").ok());
        assertTrue(step(st, "mkinitcpio -P").ok());
        assertTrue(step(st, "grub-install /dev/sda").ok());
        assertTrue(step(st, "grub-mkconfig -o /boot/grub/grub.cfg").ok());
        assertTrue(step(st, "passwd").ok());
        assertTrue(step(st, "exit").ok());
        final LiveInstallState.Result reboot = step(st, "reboot");
        assertTrue(reboot.ok());
        assertTrue(reboot.complete());
        assertEquals(0, st.targetIndex());
    }

    @Test
    void arch_pacstrapWithoutMount_isNotAMountpoint() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        final LiveInstallState.Result r = step(st, "pacstrap /mnt base linux");
        assertFalse(r.ok());
        assertTrue(r.lines().get(0).contains("not a mountpoint"));
    }

    @Test
    void arch_pacstrapWithoutMirror_cannotResolveHost() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        run(st, "mkfs.ext4 /dev/sda", false, 0);
        run(st, "mount /dev/sda /mnt", false, 0);
        final LiveInstallState.Result r = run(st, "pacstrap /mnt base linux", false, 0);
        assertFalse(r.ok());
        assertTrue(r.lines().get(0).contains("Could not resolve host"));
    }

    @Test
    void mount_unformattedDevice_failsWithWrongFsType() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        final LiveInstallState.Result r = step(st, "mount /dev/sdb /mnt");
        assertFalse(r.ok());
        assertTrue(r.lines().get(0).contains("wrong fs type"));
    }

    @Test
    void reboot_beforeBootloader_listsWhatIsMissing() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "mkfs.ext4 /dev/sda");
        step(st, "mount /dev/sda /mnt");
        step(st, "pacstrap /mnt base linux");
        final LiveInstallState.Result r = step(st, "reboot");
        assertFalse(r.ok());
        assertFalse(r.complete());
        assertTrue(String.join("\n", r.lines()).contains("no bootloader"));
    }

    @Test
    void reboot_insideChroot_isRefused() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "mkfs.ext4 /dev/sda");
        step(st, "mount /dev/sda /mnt");
        step(st, "pacstrap /mnt base linux");
        step(st, "arch-chroot /mnt");
        assertFalse(step(st, "reboot").ok());
    }

    @Test
    void gentoo_kernelBuildWaitsForCompileTicks() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        step(st, "mkfs.ext4 /dev/sdb");
        step(st, "mount /dev/sdb /mnt");
        assertTrue(step(st, "tar xpf stage3-amd64.tar.xz -C /mnt").ok());
        assertTrue(step(st, "chroot /mnt").ok());
        assertFalse(step(st, "emerge sys-kernel/gentoo-sources").ok()); // not synced yet
        assertTrue(step(st, "emerge --sync").ok());
        /*
         * One job on a 2000 MHz processor is a 32 s build, which is 640 ticks, and genkernel refuses until
         * they have passed. The clock here is the test's own, so the numbers are the real ones.
         */
        final long started = this.clock;
        assertTrue(run(st, "emerge sys-kernel/gentoo-sources", true, started).ok());
        assertFalse(run(st, "genkernel all", true, started + 300).ok(), "still compiling");
        assertTrue(run(st, "genkernel all", true, started + 700).ok());
        assertTrue(run(st, "grub-install /dev/sdb", true, started + 700).ok());
        assertTrue(run(st, "grub-mkconfig -o /boot/grub/grub.cfg", true, started + 700).ok());
        assertTrue(run(st, "passwd", true, started + 700).ok());
        run(st, "exit", true, started + 700);
        assertTrue(run(st, "reboot", true, started + 700).complete());
        assertEquals(1, st.targetIndex());
    }

    @Test
    void serialize_roundTripsTheProgress() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        step(st, "mkfs.ext4 /dev/sda");
        step(st, "mount /dev/sda /mnt");
        step(st, "tar xpf stage3-amd64.tar.xz -C /mnt");
        step(st, "chroot /mnt");
        final LiveInstallState back = LiveInstallState.deserialize(st.serialize());
        assertEquals(st.prompt(), back.prompt());
        assertTrue(back.inChroot());
        assertEquals(0, back.targetIndex());
    }

    /**
     * A state saved by a build that remembered less comes back as far as it goes, rather than not at all.
     *
     * <p>This sequence is going to grow, and a saved state that named nothing would throw away a player's
     * half-finished install every time it did.
     */
    @Test
    void deserialize_aStateThatSaysLess_comesBackAsFarAsItGoes() {
        final LiveInstallState back = LiveInstallState.deserialize(
                "distro=arch\ndevice=sdb\nformatted=1\nmounted=1");
        assertEquals(LiveInstallState.Distro.ARCH, back.distro());
        assertEquals(1, back.targetIndex());
        assertFalse(back.inChroot());
    }

    @Test
    void deserialize_aStateNamingSomethingUnknown_ignoresIt() {
        final LiveInstallState back = LiveInstallState.deserialize(
                "distro=gentoo\ndevice=sda\nsomething_later=yes");
        assertEquals(LiveInstallState.Distro.GENTOO, back.distro());
        assertEquals(0, back.targetIndex());
    }

    @Test
    void deserialize_nothingAtAll_isNoInstallRatherThanACrash() {
        assertEquals(null, LiveInstallState.deserialize(""));
        assertEquals(null, LiveInstallState.deserialize("distro=solaris"));
    }

    /** The medium carries the guide the real one carries, and reading it is how a player knows what to type. */
    @Test
    void cat_theGuideOnTheMedium_namesTheStepsInOrder() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        final LiveInstallState.Result read = step(st, "cat /root/install.txt");
        assertTrue(read.ok());
        assertTrue(String.join("\n", read.lines()).contains("pacstrap /mnt base linux"),
                "the guide names the step that fetches the base system");
    }

    /** What a step wrote is what reading it back shows, rather than a line written in advance. */
    @Test
    void cat_theFilesystemTable_isWhatTheStepReallyWrote() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "mkfs.ext4 /dev/sdb");
        step(st, "mount /dev/sdb /mnt");
        step(st, "pacstrap /mnt base linux");
        assertFalse(step(st, "cat /mnt/etc/fstab").ok(), "nothing has written it yet");

        step(st, "genfstab -U /mnt >> /mnt/etc/fstab");
        final LiveInstallState.Result read = step(st, "cat /mnt/etc/fstab");
        assertTrue(read.ok());
        assertTrue(String.join("\n", read.lines()).contains("UUID=jsc-sdb"),
                "and it names the disk the install really used: " + read.lines());
    }

    /**
     * Inside the new system a path is that system's own, which is the one thing about a chroot that has to be
     * true for the rest of it to make any sense.
     */
    @Test
    void cat_insideTheChroot_readsTheNewSystemsOwnPaths() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "mkfs.ext4 /dev/sda");
        step(st, "mount /dev/sda /mnt");
        step(st, "pacstrap /mnt base linux");
        step(st, "genfstab -U /mnt >> /mnt/etc/fstab");
        step(st, "arch-chroot /mnt");

        assertEquals("[root@archiso /]#", st.prompt());
        assertTrue(step(st, "cat /etc/fstab").ok(),
                "the table written to /mnt/etc/fstab outside is /etc/fstab in here");
    }

    @Test
    void cd_movesAndThePromptSaysWhere() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        assertEquals("livecd ~ #", st.prompt());
        assertTrue(step(st, "cd /mnt").ok());
        assertEquals("livecd /mnt #", st.prompt());
        assertFalse(step(st, "cd /nowhere").ok());
        assertEquals("livecd /mnt #", st.prompt(), "a move that failed moved nothing");
        assertTrue(step(st, "cd").ok());
        assertEquals("livecd ~ #", st.prompt(), "and with nowhere named it goes home");
    }

    @Test
    void ls_listsWhatIsThereAndSaysSoWhenThereIsNot() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        assertTrue(String.join(" ", step(st, "ls /root").lines()).contains("install.txt"));
        assertFalse(step(st, "ls /mnt/etc").ok(), "nothing is installed yet");
        step(st, "mkfs.ext4 /dev/sda");
        step(st, "mount /dev/sda /mnt");
        step(st, "pacstrap /mnt base linux");
        assertTrue(step(st, "ls /mnt").ok(), "the base system laid its directories down");
        assertTrue(String.join(" ", step(st, "ls /mnt").lines()).contains("etc"));
    }

    /** What the session wrote and where it was standing survive the world going away. */
    @Test
    void serialize_roundTripsTheFilesAndWhereTheSessionStands() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "mkfs.ext4 /dev/sda");
        step(st, "mount /dev/sda /mnt");
        step(st, "pacstrap /mnt base linux");
        step(st, "genfstab -U /mnt >> /mnt/etc/fstab");
        step(st, "cd /mnt/etc");

        final LiveInstallState back = LiveInstallState.deserialize(st.serialize());
        assertEquals("root@archiso /mnt/etc #", back.prompt());
        assertTrue(String.join("\n", back.run("cat fstab", env(true, 0)).lines()).contains("UUID=jsc-sda"));
        assertTrue(back.run("cat /root/install.txt", env(true, 0)).ok(),
                "and the medium's own guide is back with it");
    }

    private static LiveInstallState.Env uefi(final boolean mirror, final long now) {
        return new LiveInstallState.Env(
                List.of(new LiveInstallState.Device("sda", 20_480), new LiveInstallState.Device("sdb", 512_000)),
                mirror, now, 4, 2000, 4, true);
    }

    /** The editor takes the shell over while it is open, and says so the way it always has. */
    @Test
    void fdisk_takesTheShellOverWhileItIsOpen() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        assertFalse(st.editingTable());
        assertTrue(step(st, "fdisk /dev/sda").ok());
        assertTrue(st.editingTable());
        assertEquals("Command (m for help):", st.prompt());
        assertTrue(step(st, "w").ok());
        assertFalse(st.editingTable());
        assertEquals("root@archiso ~ #", st.prompt());
    }

    @Test
    void fdisk_aDiskTheMachineDoesNotHave_isNoSuchFile() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        final LiveInstallState.Result r = step(st, "fdisk /dev/sdz");
        assertFalse(r.ok());
        assertTrue(r.lines().get(0).contains("No such file or directory"));
        assertFalse(st.editingTable(), "and it did not open on a disk that is not there");
    }

    /** The editor says nothing reaches the disk until it is told to write, so walking away must mean it. */
    @Test
    void fdisk_quit_throwsAwayEverythingTypedSinceItOpened() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "fdisk /dev/sda");
        step(st, "g");
        step(st, "n 512M");
        step(st, "q");

        assertFalse(st.editingTable());
        final String listed = String.join("\n", step(st, "lsblk").lines());
        assertFalse(listed.contains("sda1"), "nothing was written to the disk: " + listed);
    }

    @Test
    void fdisk_write_putsTheTableOnTheDiskAndTheListingShowsIt() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "fdisk /dev/sda");
        step(st, "g");
        step(st, "n 512M");
        step(st, "t 1 uefi");
        step(st, "n");
        final String printed = String.join("\n", step(st, "p").lines());
        assertTrue(printed.contains("EFI System"), "the editor prints what it was told: " + printed);
        assertTrue(printed.contains("20 GiB"), "and the disk the machine really has: " + printed);
        step(st, "w");

        final String listed = String.join("\n", step(st, "lsblk").lines());
        assertTrue(listed.contains("sda1") && listed.contains("sda2"), "both partitions are there: " + listed);
    }

    @Test
    void fdisk_aPartitionBeforeALabel_isRefused() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "fdisk /dev/sda");
        final LiveInstallState.Result r = step(st, "n 512M");
        assertFalse(r.ok());
        assertTrue(r.lines().get(0).contains("Create a disklabel first"));
    }

    /** A disk somebody partitioned is not a disk to write a filesystem straight onto. */
    @Test
    void mkfs_onAPartitionedDisk_refusesRatherThanWipingTheTable() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "fdisk /dev/sda");
        step(st, "g");
        step(st, "n");
        step(st, "w");

        assertFalse(step(st, "mkfs.ext4 /dev/sda").ok(), "the whole disk is refused");
        assertTrue(step(st, "mkfs.ext4 /dev/sda1").ok(), "its partition is not");
    }

    /** The place the firmware reads a bootloader from is made with its own tool, not with the other one. */
    @Test
    void mkfat_onlyOnTheBootPartition() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "fdisk /dev/sda");
        step(st, "g");
        step(st, "n 512M");
        step(st, "n");
        step(st, "w");

        assertFalse(st.run("mkfs.fat -F32 /dev/sda1", uefi(true, 0)).ok(), "it is not marked as one yet");
        step(st, "fdisk /dev/sda");
        step(st, "t 1 uefi");
        step(st, "w");
        assertTrue(st.run("mkfs.fat -F32 /dev/sda1", uefi(true, 0)).ok(), "now it is");
        assertFalse(st.run("mkfs.ext4 /dev/sda1", uefi(true, 0)).ok(),
                "and the other tool will not take it, since that is not what it holds");
    }

    /** The boot partition hangs inside the new system, so it is mounted after the root and under it. */
    @Test
    void mount_theBootPartition_goesUnderTheRootAndOnlyAfterIt() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "fdisk /dev/sda");
        step(st, "g");
        step(st, "n 512M");
        step(st, "t 1 uefi");
        step(st, "n");
        step(st, "w");
        st.run("mkfs.fat -F32 /dev/sda1", uefi(true, 0));
        st.run("mkfs.ext4 /dev/sda2", uefi(true, 0));

        assertFalse(st.run("mount /dev/sda1 /mnt/boot", uefi(true, 0)).ok(), "the root is not mounted yet");
        assertTrue(st.run("mount /dev/sda2 /mnt", uefi(true, 0)).ok());
        assertTrue(st.run("mount /dev/sda1 /mnt/boot", uefi(true, 0)).ok());
        assertTrue(String.join("\n", st.run("lsblk", uefi(true, 0)).lines()).contains("/mnt/boot"));
    }

    /**
     * The bootloader is installed for the firmware the machine really has, which is the whole reason the
     * partition it may or may not need exists at all.
     */
    @Test
    void grubInstall_namesThePlatformThisMachineBootsBy() {
        final LiveInstallState older = new LiveInstallState(LiveInstallState.Distro.ARCH);
        for (final String line : new String[]{"mkfs.ext4 /dev/sda", "mount /dev/sda /mnt",
                "pacstrap /mnt base linux", "arch-chroot /mnt"}) {
            this.clock += APART;
            older.run(line, env(true, this.clock));
        }
        assertTrue(String.join("\n", older.run("grub-install /dev/sda", env(true, this.clock)).lines())
                .contains("i386-pc"), "an older machine takes it on the disk itself");

        final LiveInstallState newer = new LiveInstallState(LiveInstallState.Distro.ARCH);
        for (final String line : new String[]{"fdisk /dev/sda", "g", "n 512M", "t 1 uefi", "n", "w",
                "mkfs.fat -F32 /dev/sda1", "mkfs.ext4 /dev/sda2", "mount /dev/sda2 /mnt",
                "pacstrap /mnt base linux", "arch-chroot /mnt"}) {
            this.clock += APART;
            newer.run(line, uefi(true, this.clock));
        }
        assertTrue(String.join("\n", newer.run("grub-install", uefi(true, this.clock)).lines())
                        .contains("failed to get canonical path"),
                "a modern machine will not take it with nowhere to put it");

        for (final String line : new String[]{"exit", "mount /dev/sda1 /mnt/boot", "arch-chroot /mnt"}) {
            this.clock += APART;
            newer.run(line, uefi(true, this.clock));
        }
        assertTrue(String.join("\n", newer.run("grub-install", uefi(true, this.clock)).lines())
                .contains("x86_64-efi"), "and with somewhere to put it, it goes there");
    }

    /** On the older firmware it goes on the disk, not into the partition the system lives in. */
    @Test
    void grubInstall_onTheOlderFirmware_wantsTheDiskAndNotThePartition() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "fdisk /dev/sda");
        step(st, "g");
        step(st, "n");
        step(st, "w");
        step(st, "mkfs.ext4 /dev/sda1");
        step(st, "mount /dev/sda1 /mnt");
        step(st, "pacstrap /mnt base linux");
        step(st, "arch-chroot /mnt");

        final LiveInstallState.Result wrong = step(st, "grub-install /dev/sda1");
        assertFalse(wrong.ok());
        assertTrue(String.join("\n", wrong.lines()).contains("grub-install /dev/sda"),
                "and it says which one it wanted: " + wrong.lines());
        assertTrue(step(st, "grub-install /dev/sda").ok());
    }

    /** The list the bootloader is given is generated from the system that is really installed. */
    @Test
    void grubMkconfig_writesAFileNamingThisMachinesSystem() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "mkfs.ext4 /dev/sdb");
        step(st, "mount /dev/sdb /mnt");
        step(st, "pacstrap /mnt base linux");
        step(st, "arch-chroot /mnt");
        assertFalse(step(st, "grub-mkconfig -o /boot/grub/grub.cfg").ok(),
                "there is no bootloader to give a list to yet");

        step(st, "grub-install /dev/sdb");
        assertTrue(step(st, "grub-mkconfig -o /boot/grub/grub.cfg").ok());
        final String written = String.join("\n", step(st, "cat /boot/grub/grub.cfg").lines());
        assertTrue(written.contains("Arch Linux") && written.contains("jsc-sdb"),
                "and it names the system and the disk it is on: " + written);
    }

    /** A bootloader with nothing to start is a bootloader that starts nothing. */
    @Test
    void reboot_withoutTheBootloadersList_saysSo() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "mkfs.ext4 /dev/sda");
        step(st, "mount /dev/sda /mnt");
        step(st, "pacstrap /mnt base linux");
        step(st, "genfstab -U /mnt >> /mnt/etc/fstab");
        step(st, "arch-chroot /mnt");
        step(st, "mkinitcpio -P");
        step(st, "grub-install /dev/sda");
        step(st, "passwd");
        step(st, "exit");

        final LiveInstallState.Result r = step(st, "reboot");
        assertFalse(r.complete());
        assertTrue(String.join("\n", r.lines()).contains("grub-mkconfig"));
    }

    /** The name is the one thing here the player chooses, and it is written where it belongs. */
    @Test
    void hostname_isWrittenIntoTheNewSystemAndRemembered() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        step(st, "mkfs.ext4 /dev/sda");
        step(st, "mount /dev/sda /mnt");
        step(st, "tar xpf stage3-amd64.tar.xz -C /mnt");
        assertFalse(step(st, "hostname library").ok(), "not from outside the new system");

        step(st, "chroot /mnt");
        assertTrue(step(st, "hostname library").ok());
        assertEquals("library", st.chosenName());
        assertEquals("library", String.join("", step(st, "cat /etc/hostname").lines()));
        assertEquals("library", String.join("", step(st, "hostname").lines()),
                "and asking with no name reads back the one that was set");
    }

    @Test
    void hostname_somethingThatIsNotAName_isRefused() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "mkfs.ext4 /dev/sda");
        step(st, "mount /dev/sda /mnt");
        step(st, "pacstrap /mnt base linux");
        step(st, "arch-chroot /mnt");
        assertFalse(step(st, "hostname a machine").ok());
        assertFalse(step(st, "hostname -weird").ok());
        assertEquals("", st.chosenName());
    }

    /**
     * How long the compile takes is the machine's and the player's together, and the player's half is the one
     * they write in the build options.
     */
    @Test
    void compile_takesLongerWithOneJobThanWithFour() {
        final LiveInstallState alone = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        assertEquals(1, alone.makeJobs(), "left alone it builds one thing at a time");
        final long oneJob = alone.compileTicks(env(true, 0));

        step(alone, "mkfs.ext4 /dev/sda");
        step(alone, "mount /dev/sda /mnt");
        step(alone, "tar xpf stage3-amd64.tar.xz -C /mnt");
        step(alone, "chroot /mnt");
        step(alone, "echo 'MAKEOPTS=\"-j4\"' >> /etc/portage/make.conf");
        assertEquals(4, alone.makeJobs(), "and what was written is what it reads back");
        assertEquals(oneJob / 4, alone.compileTicks(env(true, 0)), "four jobs, a quarter of the wait");
    }

    /** The cores the machine has are the ceiling: asking for more than it has buys nothing. */
    @Test
    void compile_asksForMoreJobsThanCores_isCappedAtTheCores() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        step(st, "mkfs.ext4 /dev/sda");
        step(st, "mount /dev/sda /mnt");
        step(st, "tar xpf stage3-amd64.tar.xz -C /mnt");
        step(st, "chroot /mnt");
        step(st, "echo 'MAKEOPTS=\"-j64\"' >> /etc/portage/make.conf");
        assertEquals(64, st.makeJobs(), "the file says what it says");
        // The test machine has four cores, so sixty-four of them is four of them.
        assertEquals(new LiveInstallState(LiveInstallState.Distro.GENTOO).compileTicks(env(true, 0)) / 4,
                st.compileTicks(env(true, 0)), "and four cores is all the machine can bring");
    }

    /** Fetching the system takes time, and what needs it waits rather than pretending it is there. */
    @Test
    void base_takesTimeAndTheNextStepWaitsForIt() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        run(st, "mkfs.ext4 /dev/sda", true, 0);
        run(st, "mount /dev/sda /mnt", true, 0);
        final LiveInstallState.Result fetch = run(st, "pacstrap /mnt base linux", true, 0);
        assertTrue(fetch.ok());
        assertTrue(String.join("\n", fetch.lines()).contains("linux-firmware"),
                "and says what it is fetching, one package at a time: " + fetch.lines());

        final LiveInstallState.Result tooSoon = run(st, "arch-chroot /mnt", true, 10);
        assertFalse(tooSoon.ok());
        assertTrue(tooSoon.lines().get(0).contains("Still fetching"), tooSoon.lines().get(0));
        assertTrue(run(st, "arch-chroot /mnt", true, 100_000).ok(), "and once it is there, it is there");
    }

    /** A line written into a file is the line that is read back, which is how the options get set at all. */
    @Test
    void echo_writesAndAppendsToAFile() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        step(st, "mkfs.ext4 /dev/sda");
        step(st, "mount /dev/sda /mnt");
        step(st, "tar xpf stage3-amd64.tar.xz -C /mnt");
        step(st, "chroot /mnt");
        step(st, "echo 'CFLAGS=\"-O2\"' > /etc/portage/make.conf");
        step(st, "echo 'MAKEOPTS=\"-j2\"' >> /etc/portage/make.conf");

        final String written = String.join("\n", step(st, "cat /etc/portage/make.conf").lines());
        assertTrue(written.contains("CFLAGS=\"-O2\"") && written.contains("MAKEOPTS=\"-j2\""),
                "both lines are there, in the order they were written: " + written);
        assertEquals("said", String.join("", step(st, "echo said").lines()),
                "and with nowhere to put it, it just says it");
    }

    @Test
    void wrongDistroVerb_isCommandNotFound() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        assertTrue(step(st, "pacstrap /mnt base").lines().get(0).contains("command not found"));
    }
}
