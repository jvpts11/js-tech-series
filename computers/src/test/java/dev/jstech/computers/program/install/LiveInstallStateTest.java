/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import dev.jstech.computers.gui.term.TermBuffer;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.computers.program.tty.ITtySink;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An installation by hand, a line at a time, the way a player types it.
 *
 * <p>A step that takes time comes back as a tool left running, and nothing it is for has happened until that
 * tool has been played to its end, so every sequence here plays each tool out before typing the next line,
 * which is what a player waiting at the terminal does.
 */
class LiveInstallStateTest {

    private static final String STAGE3_URL = "mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz";
    private static final String ROOT_LINE = "UUID=3a492b72-77a7-483c-b412-d1f4c7d62a48  /  ext4  defaults  0 1";

    private long clock;

    /** What the last tool played printed, which is what the player watched go by. */
    private final List<String> watched = new ArrayList<>();

    private LiveInstallState.Env env(final boolean mirror, final boolean uefi, final boolean everyStep) {
        return new LiveInstallState.Env(
                List.of(new LiveInstallState.Device("sda", 20_480), new LiveInstallState.Device("sdb", 512_000)),
                mirror, this.clock, 4, 2000, 4, uefi, 6_000L, everyStep);
    }

    /** One line on a machine of the older firmware with the Mirror up, its tool played out with no answers. */
    private LiveTurn step(final LiveInstallState st, final String line) {
        return this.step(st, line, this.env(true, false, false));
    }

    /** One line, and whatever it leaves running played to its end, answering what it asks in order. */
    private LiveTurn step(final LiveInstallState st, final String line, final LiveInstallState.Env env,
                          final String... answers) {
        this.clock += 10;
        final LiveTurn turn = st.run(line, env);
        this.watched.clear();
        if (turn.tool() != null) {
            this.play(turn.tool(), answers);
        }
        return turn;
    }

    private void play(final ITtyProcess tool, final String... answers) {
        final Deque<String> told = new ArrayDeque<>(List.of(answers));
        final ITtySink glass = new ITtySink() {
            @Override
            public void line(final CliLine line) {
                LiveInstallStateTest.this.watched.add(line.text());
            }

            @Override
            public void redraw(final CliLine line) {
                LiveInstallStateTest.this.watched.set(LiveInstallStateTest.this.watched.size() - 1, line.text());
            }
        };
        tool.begin(this.clock);
        for (int guard = 0; !tool.over() && guard < 400_000; guard++) {
            tool.advance(this.clock, glass);
            if (tool.asking() != null) {
                tool.answer(told.isEmpty() ? "" : told.pollFirst(), this.clock, glass);
            }
            this.clock++;
        }
        assertTrue(tool.over(), "the tool came to an end");
    }

    private String seen() {
        return String.join("\n", this.watched);
    }

    /** An Arch installation on the older firmware, as far as a base system on a mounted disk. */
    private LiveInstallState archWithABase() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "mkfs.ext4 /dev/sdb");
        step(st, "mount /dev/sdb /mnt");
        step(st, "pacstrap -K /mnt base linux linux-firmware");
        return st;
    }

    /** A Gentoo installation on the older firmware, as far as inside a freshly unpacked base system. */
    private LiveInstallState gentooInsideTheNewSystem() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        step(st, "mkfs.ext4 /dev/sdb");
        step(st, "mount /dev/sdb /mnt/gentoo");
        step(st, "cd /mnt/gentoo");
        step(st, "wget " + STAGE3_URL);
        step(st, "tar xpvf stage3-*.tar.xz --xattrs-include='*.*' --numeric-owner");
        step(st, "chroot /mnt/gentoo /bin/bash");
        return st;
    }

    @Test
    void arch_theWholeSequence_completesOnReboot() {
        final LiveInstallState st = archWithABase();
        assertTrue(step(st, "genfstab -U /mnt >> /mnt/etc/fstab").ok());
        assertTrue(step(st, "arch-chroot /mnt").ok());
        assertEquals("[root@archiso /]#", st.prompt());
        step(st, "passwd", env(true, false, false), "hunter2", "hunter2");
        step(st, "pacman -S grub", env(true, false, false), "y");
        assertTrue(step(st, "grub-install /dev/sdb").ok());
        assertTrue(step(st, "grub-mkconfig -o /boot/grub/grub.cfg").ok());
        step(st, "exit");
        final LiveTurn last = step(st, "reboot");
        assertTrue(last.complete(), last.text());
        assertEquals(1, st.targetIndex());
    }

    @Test
    void gentoo_theWholeSequence_completesOnReboot() {
        final LiveInstallState st = gentooInsideTheNewSystem();
        assertEquals("(chroot) livecd / #", st.prompt());
        step(st, "emerge-webrsync");
        step(st, "emerge sys-kernel/gentoo-sources");
        step(st, "genkernel all");
        assertTrue(seen().contains("Kernel compiled successfully!"), seen());
        step(st, "echo '" + ROOT_LINE + "' >> /etc/fstab");
        step(st, "passwd", env(true, false, false), "hunter2", "hunter2");
        step(st, "emerge --ask sys-boot/grub", env(true, false, false), "y");
        assertTrue(seen().contains(">>> sys-boot/grub-2.12-r5 merged."), seen());
        assertTrue(step(st, "grub-install /dev/sdb").ok());
        assertTrue(step(st, "grub-mkconfig -o /boot/grub/grub.cfg").ok());
        step(st, "exit");
        final LiveTurn last = step(st, "reboot");
        assertTrue(last.complete(), last.text());
        assertEquals(1, st.targetIndex());
    }

    /** What a tool is for happens when it ends, so until it has been played out the step has not happened. */
    @Test
    void aStepThatTakesTime_hasNotHappenedUntilItsToolEnds() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "mkfs.ext4 /dev/sdb");
        step(st, "mount /dev/sdb /mnt");
        final LiveTurn fetching = st.run("pacstrap -K /mnt base linux linux-firmware", env(true, false, false));
        assertNotNull(fetching.tool(), "the base system is left arriving");
        assertFalse(st.run("arch-chroot /mnt", env(true, false, false)).ok(), "and there is nothing to step into");
        play(fetching.tool());
        assertTrue(st.run("arch-chroot /mnt", env(true, false, false)).ok(), "until it has all arrived");
    }

    /** Ctrl+C half way through leaves the step not done, however much of it had gone by. */
    @Test
    void aStepInterruptedHalfWay_isNotDone() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        final LiveTurn making = st.run("mkfs.ext4 /dev/sdb", env(true, false, false));
        making.tool().begin(0);
        making.tool().advance(10, null);
        making.tool().interrupt(10, null);
        assertTrue(making.tool().over());
        assertFalse(step(st, "mount /dev/sdb /mnt").ok(), "no filesystem was made, so there is nothing to mount");
    }

    @Test
    void arch_pacstrapWithoutAMountedRoot_isNotAMountpoint() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        final LiveTurn r = step(st, "pacstrap -K /mnt base linux");
        assertFalse(r.ok());
        assertTrue(r.text().contains("is not a mountpoint"), r.text());
    }

    @Test
    void arch_pacstrapWithoutTheMirror_cannotResolveHost() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "mkfs.ext4 /dev/sda");
        step(st, "mount /dev/sda /mnt");
        final LiveTurn r = step(st, "pacstrap -K /mnt base linux", env(false, false, false));
        assertFalse(r.ok());
        assertTrue(r.text().contains("Could not resolve host"), r.text());
    }

    @Test
    void mount_aDeviceWithNoFilesystem_failsWithWrongFsType() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        final LiveTurn r = step(st, "mount /dev/sda /mnt");
        assertFalse(r.ok());
        assertTrue(r.text().contains("wrong fs type"), r.text());
    }

    /** Mounting says nothing when it works, which is how the real one says yes. */
    @Test
    void mount_whenItWorks_saysNothing() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "mkfs.ext4 /dev/sda");
        final LiveTurn r = step(st, "mount /dev/sda /mnt");
        assertTrue(r.ok());
        assertTrue(r.lines().isEmpty(), r.text());
    }

    /** Gentoo mounts where its handbook mounts, and Arch where its guide does. */
    @Test
    void mount_eachDistributionHasItsOwnPlace() {
        final LiveInstallState gentoo = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        step(gentoo, "mkfs.ext4 /dev/sda");
        assertFalse(step(gentoo, "mount /dev/sda /mnt").ok());
        assertTrue(step(gentoo, "mount /dev/sda /mnt/gentoo").ok());
    }

    @Test
    void reboot_beforeTheBootloader_listsWhatIsMissing() {
        final LiveInstallState st = archWithABase();
        final LiveTurn r = step(st, "reboot");
        assertFalse(r.ok());
        assertFalse(r.complete());
        assertTrue(r.text().contains("no bootloader"), r.text());
        assertTrue(r.text().contains("no root password"), r.text());
    }

    @Test
    void reboot_insideTheNewSystem_isRefused() {
        final LiveInstallState st = archWithABase();
        step(st, "arch-chroot /mnt");
        assertFalse(step(st, "reboot").ok());
    }

    /** A world that asks for the whole handbook is refused a restart for the steps a boot does not need. */
    @Test
    void reboot_inAWorldThatAsksForEveryStep_wantsTheRestOfTheHandbook() {
        final LiveInstallState st = archWithABase();
        final LiveTurn r = step(st, "reboot", env(true, false, true));
        assertTrue(r.text().contains("no time zone"), r.text());
        assertTrue(r.text().contains("no locale generated"), r.text());
        assertTrue(r.text().contains("the machine has no name"), r.text());
        assertFalse(step(st, "reboot").text().contains("no time zone"), "and a world that does not is not");
    }

    /** The table this distribution has written by hand is asked for, and a line naming a root is what it is. */
    @Test
    void gentoo_aTableWithNoRootInIt_stopsTheRestart() {
        final LiveInstallState st = gentooInsideTheNewSystem();
        step(st, "exit");
        assertTrue(step(st, "reboot").text().contains("no root filesystem in /etc/fstab"));
        step(st, "echo '" + ROOT_LINE + "' >> /mnt/gentoo/etc/fstab");
        assertFalse(step(st, "reboot").text().contains("no root filesystem in /etc/fstab"));
    }

    @Test
    void gentoo_theArchiveIsFetchedThenUnpackedOverTheDisk() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        step(st, "mkfs.ext4 /dev/sdb");
        step(st, "mount /dev/sdb /mnt/gentoo");
        step(st, "cd /mnt/gentoo");
        assertTrue(step(st, "tar xpvf stage3-*.tar.xz").text().contains("Cannot open: No such file"),
                "there is nothing to unpack until it has been fetched");
        step(st, "wget " + STAGE3_URL);
        assertTrue(seen().contains("mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz"), seen());
        assertTrue(seen().contains("100%["), seen());
        assertTrue(step(st, "ls").text().contains("stage3-vel64-openrc.tar.xz"), "and then it is there to see");
        step(st, "tar xpvf stage3-*.tar.xz --xattrs-include='*.*' --numeric-owner");
        assertTrue(this.watched.size() > 900, "every path of a base system went by: " + this.watched.size());
        assertTrue(step(st, "cat /mnt/gentoo/etc/portage/make.conf").text().contains("COMMON_FLAGS"),
                "and the files a base system brings are there to read");
    }

    /** Asked to work quietly the archiver works quietly, which is why the handbook asks it not to. */
    @Test
    void gentoo_tarWithoutTheV_saysNothingAndStillUnpacks() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        step(st, "mkfs.ext4 /dev/sdb");
        step(st, "mount /dev/sdb /mnt/gentoo");
        step(st, "cd /mnt/gentoo");
        step(st, "wget " + STAGE3_URL);
        step(st, "tar xpf stage3-*.tar.xz");
        assertTrue(this.watched.isEmpty(), seen());
        assertTrue(step(st, "chroot /mnt/gentoo /bin/bash").ok());
    }

    /** Unpacked anywhere but over the disk it would land on the medium, which is gone at the next restart. */
    @Test
    void gentoo_tarAwayFromTheMountedDisk_isRefused() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        step(st, "mkfs.ext4 /dev/sdb");
        step(st, "mount /dev/sdb /mnt/gentoo");
        step(st, "wget " + STAGE3_URL);
        assertFalse(step(st, "tar xpvf stage3-*.tar.xz").ok());
    }

    /** There is one network in this world and one thing on it to fetch from: a web address resolves nowhere. */
    @Test
    void gentoo_wgetOfAWebAddress_cannotBeResolved() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        step(st, "wget https://distfiles.gentoo.org/releases/stage3-vel64-openrc.tar.xz");
        assertTrue(seen().contains("unable to resolve host address 'distfiles.gentoo.org'"), seen());
        assertFalse(step(st, "ls").text().contains("stage3"), "and nothing was fetched");
    }

    @Test
    void gentoo_emergeBeforeTheTree_isRefused() {
        final LiveInstallState st = gentooInsideTheNewSystem();
        assertTrue(step(st, "emerge sys-kernel/gentoo-sources").text().contains("portage tree is empty"));
    }

    /** Bringing the system up to date lists what it would merge, asks, and compiles each in front of you. */
    @Test
    void gentoo_theWorldUpdate_listsAsksAndCompiles() {
        final LiveInstallState st = gentooInsideTheNewSystem();
        step(st, "emerge-webrsync");
        step(st, "emerge --ask --verbose --update --deep --newuse @world", env(true, false, false), "y");
        assertTrue(seen().contains("Total: 3 packages (2 upgrades, 1 new)"), seen());
        assertTrue(seen().contains("Would you like to merge these packages? [Yes/No] y"), seen());
        assertTrue(seen().contains(">>> Emerging (3 of 3) app-editors/vim-9.1.0794::gentoo"), seen());
        assertTrue(seen().contains("x86_64-pc-linux-gnu-scc"), seen());
    }

    @Test
    void gentoo_theWorldUpdateAnsweredNo_mergesNothing() {
        final LiveInstallState st = gentooInsideTheNewSystem();
        step(st, "emerge-webrsync");
        step(st, "emerge --ask @world", env(true, false, false), "n");
        assertTrue(seen().contains("Quitting."), seen());
        assertFalse(seen().contains(">>> Emerging"), seen());
    }

    /** The build by hand, from where the sources are: the long step, and no kernel until the install. */
    @Test
    void gentoo_theKernelBuiltByHand_needsItsSourcesItsPlaceAndItsInstall() {
        final LiveInstallState st = gentooInsideTheNewSystem();
        step(st, "emerge-webrsync");
        assertFalse(step(st, "make -j4").ok(), "there are no sources yet");
        step(st, "emerge sys-kernel/gentoo-sources");
        step(st, "eselect kernel set 1");
        assertFalse(step(st, "make -j4").ok(), "and it is built from where they are");
        step(st, "cd /usr/src/linux");
        step(st, "make -j4 && make modules_install && make install");
        assertTrue(seen().contains("Kernel: arch/x86/boot/bzImage is ready  (#1)"), seen());
        assertTrue(this.watched.size() > 1_000, "every object went by: " + this.watched.size());
        step(st, "cd /");
        step(st, "emerge sys-boot/grub");
        assertTrue(step(st, "grub-install /dev/sdb").ok(), "and with a kernel installed the bootloader goes on");
    }

    /** Two things decide a compile and the player owns one of them: the jobs written in the build options. */
    @Test
    void compile_isAQuarterAsLongWithFourJobsAsWithOne() {
        final LiveInstallState st = gentooInsideTheNewSystem();
        assertEquals(1, st.makeJobs(), "left alone it builds one thing at a time");
        final long oneJob = st.compileTicks(env(true, false, false));
        step(st, "echo 'MAKEOPTS=\"-j4\"' >> /etc/portage/make.conf");
        assertEquals(4, st.makeJobs(), "what was written is what it reads back");
        assertEquals(oneJob / 4, st.compileTicks(env(true, false, false)));
    }

    /** The cores the machine has are the ceiling: asking for more than it has buys nothing. */
    @Test
    void compile_askedForMoreJobsThanCores_isCappedAtTheCores() {
        final LiveInstallState st = gentooInsideTheNewSystem();
        step(st, "echo 'MAKEOPTS=\"-j64\"' >> /etc/portage/make.conf");
        assertEquals(64, st.makeJobs(), "the file says what it says");
        assertEquals(new LiveInstallState(LiveInstallState.Distro.GENTOO).compileTicks(env(true, false, false)) / 4,
                st.compileTicks(env(true, false, false)), "and four cores is all the machine can bring");
    }

    @Test
    void serialize_roundTripsHowFarItHasGotAndWhereItStands() {
        final LiveInstallState st = gentooInsideTheNewSystem();
        step(st, "cd /etc");
        final LiveInstallState back = LiveInstallState.deserialize(st.serialize());
        assertEquals(st.prompt(), back.prompt());
        assertEquals("(chroot) livecd /etc #", back.prompt());
        assertTrue(back.inChroot());
        assertEquals(1, back.targetIndex());
        assertTrue(back.run("cat /etc/portage/make.conf", env(true, false, false)).text().contains("COMMON_FLAGS"),
                "and the files it made came back with it");
        back.run("exit", env(true, false, false));
        assertTrue(back.run("cat /root/install.txt", env(true, false, false)).ok(),
                "and the medium's own guide, which is never written down, is back with the medium");
    }

    @Test
    void deserialize_aStateThatSaysLess_comesBackAsFarAsItGoes() {
        final LiveInstallState back = LiveInstallState.deserialize("distro=gentoo\nmounted=1");
        assertNotNull(back);
        assertEquals("livecd ~ #", back.prompt());
        assertFalse(back.inChroot());
    }

    @Test
    void deserialize_nothingAtAll_isNoInstallRatherThanACrash() {
        assertNull(LiveInstallState.deserialize(""));
        assertNull(LiveInstallState.deserialize("distro=slackware"));
    }

    @Test
    void cat_theGuideOnTheMedium_namesTheStepsAndMarksTheOnesABootNeeds() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        final String guide = step(st, "cat /root/install.txt").text();
        assertTrue(guide.contains(" * wget mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz"), guide);
        assertTrue(guide.contains("   eselect profile list"), guide);
        assertFalse(guide.contains("http"), guide);
    }

    /**
     * The walkthrough is read on the medium's own terminal, so none of it may run past that terminal's edge: a
     * line that does is broken wherever the edge happens to be, and a command broken in two is not one a
     * player can copy.
     */
    @Test
    void theGuideAndTheHelp_fitTheTerminalTheyAreReadOn() {
        for (final LiveInstallState.Distro distro : LiveInstallState.Distro.values()) {
            final LiveInstallState st = new LiveInstallState(distro);
            for (final String shown : List.of(step(st, "cat /root/install.txt").text(), step(st, "help").text())) {
                for (final String line : shown.split("\n")) {
                    assertTrue(line.length() <= TermBuffer.MONITOR_COLUMNS,
                            distro + " runs to " + line.length() + " columns: " + line);
                }
            }
        }
    }

    /** Sent into the file the table writer says nothing; run without the redirection it prints and writes nothing. */
    @Test
    void arch_genfstab_saysNothingIntoAFileAndPrintsWithoutOne() {
        final LiveInstallState st = archWithABase();
        final LiveTurn printed = step(st, "genfstab -U /mnt");
        assertTrue(printed.text().contains("# /dev/sdb"), printed.text());
        assertTrue(printed.text().matches("(?s).*UUID=[0-9a-f]{8}-[0-9a-f]{4}-.*"), printed.text());
        assertFalse(step(st, "cat /mnt/etc/fstab").ok(), "and nothing was written");
        final LiveTurn written = step(st, "genfstab -U /mnt >> /mnt/etc/fstab");
        assertTrue(written.ok() && written.lines().isEmpty(), written.text());
        assertTrue(step(st, "cat /mnt/etc/fstab").text().contains("# /dev/sdb"));
        assertTrue(st.filesystemTable().contains("ext4"));
    }

    /** Inside the new system a path is that system's own, which is the one thing a chroot has to get right. */
    @Test
    void cat_insideTheNewSystem_readsItsOwnPaths() {
        final LiveInstallState st = archWithABase();
        step(st, "genfstab -U /mnt >> /mnt/etc/fstab");
        step(st, "arch-chroot /mnt");
        assertTrue(step(st, "cat /etc/fstab").text().contains("# /dev/sdb"));
    }

    @Test
    void cd_movesAndThePromptSaysWhere() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        assertEquals("livecd ~ #", st.prompt());
        step(st, "cd /mnt/gentoo");
        assertEquals("livecd /mnt/gentoo #", st.prompt());
        assertFalse(step(st, "cd /nowhere").ok());
        assertEquals("livecd /mnt/gentoo #", st.prompt(), "a move that failed moved nothing");
        step(st, "cd");
        assertEquals("livecd ~ #", st.prompt(), "and with nowhere named it goes home");
    }

    @Test
    void ls_listsWhatIsThereAndSaysSoWhenThereIsNot() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        assertTrue(step(st, "ls").text().contains("install.txt"));
        assertFalse(step(st, "ls /nowhere").ok());
    }

    /** The real dialogue: a new partition is three questions, each with a default that Enter takes. */
    @Test
    void fdisk_asksItsWayThroughANewPartition() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "fdisk /dev/sda", env(true, true, false), "g", "n", "", "", "+512M", "t", "1", "n", "", "", "",
                "p", "w");
        assertTrue(seen().contains("Welcome to fdisk (util-linux 2.40.2)."), seen());
        assertTrue(seen().contains("Partition number (1-128, default 1): "), seen());
        assertTrue(seen().contains("First sector (2048-"), seen());
        assertTrue(seen().contains("Last sector, +/-sectors or +/-size{K,M,G,T,P}"), seen());
        assertTrue(seen().contains("Created a new partition 1 of type 'Linux filesystem' and of size 512 MiB."),
                seen());
        assertTrue(seen().contains("Selected partition 1"), seen());
        assertTrue(seen().contains("Changed type of partition 'Linux filesystem' to 'EFI System'."), seen());
        assertTrue(seen().contains("/dev/sda1"), seen());
        assertTrue(seen().contains("The partition table has been altered."), seen());
        final String listing = step(st, "lsblk").text();
        assertTrue(listing.contains("|-sda1") && listing.contains("`-sda2"), listing);
    }

    @Test
    void fdisk_aDiskTheMachineDoesNotHave_isNoSuchFile() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        assertTrue(step(st, "fdisk /dev/sdz").text().contains("No such file or directory"));
    }

    /** Nothing reaches the disk until it is told to write, and leaving throws away all of it. */
    @Test
    void fdisk_leftWithoutWriting_throwsAwayEverythingTypedSinceItOpened() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "fdisk /dev/sda", env(true, true, false), "g", "n 512M", "q");
        assertFalse(step(st, "lsblk").text().contains("sda1"));
    }

    @Test
    void mkfs_onAPartitionedDisk_refusesRatherThanWipingTheTable() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "fdisk /dev/sda", env(true, true, false), "g", "n 512M", "n", "", "", "", "w");
        final LiveTurn r = step(st, "mkfs.ext4 /dev/sda");
        assertFalse(r.ok());
        assertTrue(r.text().contains("contains a gpt partition table"), r.text());
    }

    @Test
    void mkfsFat_onlyOnThePartitionTheFirmwareReads() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "fdisk /dev/sda", env(true, true, false), "g", "n 512M", "t 1 uefi", "n", "", "", "", "w");
        assertFalse(step(st, "mkfs.fat -F 32 /dev/sda2").ok());
        assertFalse(step(st, "mkfs.ext4 /dev/sda1").ok(), "and that one is not given the other filesystem");
        assertTrue(step(st, "mkfs.fat -F 32 /dev/sda1").ok());
        assertEquals("mkfs.fat 4.2 (2021-01-31)", seen());
    }

    /** The place the bootloader goes hangs inside the new system, so it is mounted after the root and under it. */
    @Test
    void mount_thePartitionTheFirmwareReads_goesUnderTheRootAndOnlyAfterIt() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        step(st, "fdisk /dev/sda", env(true, true, false), "g", "n 512M", "t 1 uefi", "n", "", "", "", "w");
        step(st, "mkfs.fat -F 32 /dev/sda1");
        step(st, "mkfs.ext4 /dev/sda2");
        assertFalse(step(st, "mount --mkdir /dev/sda1 /mnt/boot").ok(), "not before the root is mounted");
        step(st, "mount /dev/sda2 /mnt");
        assertTrue(step(st, "mount --mkdir /dev/sda1 /mnt/boot").ok());
        assertTrue(step(st, "lsblk").text().contains("/mnt/boot"));
    }

    /** The modern firmware takes the bootloader in the partition it reads, and has to be told where that is. */
    @Test
    void grubInstall_onTheModernFirmware_isToldWhereThePartitionIs() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        final LiveInstallState.Env modern = env(true, true, false);
        step(st, "fdisk /dev/sda", modern, "g", "n 512M", "t 1 uefi", "n", "", "", "", "w");
        step(st, "mkfs.fat -F 32 /dev/sda1");
        step(st, "mkfs.ext4 /dev/sda2");
        step(st, "mount /dev/sda2 /mnt");
        step(st, "mount --mkdir /dev/sda1 /mnt/boot");
        step(st, "pacstrap -K /mnt base linux linux-firmware");
        step(st, "arch-chroot /mnt");
        assertFalse(step(st, "grub-install --target=x86_64-efi --efi-directory=/boot", modern).ok(),
                "the bootloader is a package, and it is not installed yet");
        step(st, "pacman -S grub efibootmgr", modern, "y");
        assertFalse(step(st, "grub-install", modern).ok(), "left to guess, it looks where the partition is not");
        step(st, "grub-install --target=x86_64-efi --efi-directory=/boot --bootloader-id=GRUB", modern);
        assertTrue(seen().contains("Installing for x86_64-efi platform."), seen());
        assertTrue(seen().contains("Installation finished. No error reported."), seen());
    }

    @Test
    void grubInstall_onTheOlderFirmware_wantsTheDiskAndNotThePartition() {
        final LiveInstallState st = archWithABase();
        step(st, "arch-chroot /mnt");
        step(st, "pacman -S grub", env(true, false, false), "y");
        assertFalse(step(st, "grub-install").ok());
        assertFalse(step(st, "grub-install /dev/sdb1").ok());
        step(st, "grub-install /dev/sdb");
        assertTrue(seen().contains("Installing for i386-pc platform."), seen());
    }

    @Test
    void grubMkconfig_writesAFileNamingThisMachinesSystem() {
        final LiveInstallState st = archWithABase();
        step(st, "arch-chroot /mnt");
        assertFalse(step(st, "grub-mkconfig -o /boot/grub/grub.cfg").ok(), "there is no bootloader to list for yet");
        step(st, "pacman -S grub", env(true, false, false), "y");
        step(st, "grub-install /dev/sdb");
        step(st, "grub-mkconfig -o /boot/grub/grub.cfg");
        assertTrue(seen().contains("Found linux image: /boot/vmlinuz-linux"), seen());
        final String written = step(st, "cat /boot/grub/grub.cfg").text();
        assertTrue(written.contains("Arch Linux") && written.contains("root=UUID="), written);
    }

    /** The name is the one thing here the player chooses, given with the tool for it or written into its file. */
    @Test
    void hostname_isRememberedHoweverItWasGiven() {
        final LiveInstallState byTool = archWithABase();
        assertFalse(step(byTool, "hostname library").ok(), "not from outside the new system");
        step(byTool, "arch-chroot /mnt");
        assertTrue(step(byTool, "hostname library").ok());
        assertEquals("library", byTool.chosenName());
        assertEquals("library", step(byTool, "cat /etc/hostname").text());
        final LiveInstallState byFile = archWithABase();
        step(byFile, "arch-chroot /mnt");
        step(byFile, "echo workshop > /etc/hostname");
        assertEquals("workshop", byFile.chosenName());
    }

    @Test
    void hostname_somethingThatIsNotAName_isRefused() {
        final LiveInstallState st = archWithABase();
        step(st, "arch-chroot /mnt");
        assertFalse(step(st, "hostname two words").ok());
        assertFalse(step(st, "hostname -starts-with-a-dash").ok());
        assertEquals("", st.chosenName());
    }

    /** A password is asked for twice and neither answer shown; two that differ set nothing. */
    @Test
    void passwd_twoThatDoNotMatch_setNothing() {
        final LiveInstallState st = archWithABase();
        step(st, "arch-chroot /mnt");
        step(st, "passwd", env(true, false, false), "hunter2", "hunter3");
        assertTrue(seen().contains("Sorry, passwords do not match."), seen());
        assertFalse(seen().contains("hunter"), seen());
        step(st, "exit");
        assertTrue(step(st, "reboot").text().contains("no root password"));
    }

    @Test
    void echo_writesAndAppendsToAFile() {
        final LiveInstallState st = gentooInsideTheNewSystem();
        step(st, "echo 'CFLAGS=\"-O2\"' > /etc/portage/make.conf");
        step(st, "echo 'MAKEOPTS=\"-j2\"' >> /etc/portage/make.conf");
        final String written = step(st, "cat /etc/portage/make.conf").text();
        assertTrue(written.contains("CFLAGS=\"-O2\"") && written.contains("MAKEOPTS=\"-j2\""), written);
        assertEquals("said", step(st, "echo said").text(), "and with nowhere to put it, it just says it");
    }

    @Test
    void editable_namesTheFileAnEditorWouldOpenAndPassesOverItsOptions() {
        final LiveInstallState st = gentooInsideTheNewSystem();
        assertEquals("/etc/fstab", st.editable(List.of("-w", "/etc/fstab")));
        assertEquals("notes", st.editable(List.of("+12", "notes")), "a file that is not there yet is a new one");
        assertNull(st.editable(List.of("/etc")), "a directory is not something an editor opens");
        assertNull(st.editable(List.of("-w")), "and neither is nothing at all");
        assertTrue(step(st, "nano /etc").text().contains("is a directory"));
        assertTrue(step(st, "nano").text().startsWith("Usage: nano"));
        assertEquals("", step(st, "nano /etc/fstab").text(), "opening one says nothing: the editor has the glass");
    }

    @Test
    void writeFileAt_whatAnEditorSavesIsWhatTheLaterStepsRead() {
        final LiveInstallState st = gentooInsideTheNewSystem();
        assertEquals(1, st.makeJobs(), "a build left alone uses one core");
        st.writeFileAt("/etc/portage/make.conf", "COMMON_FLAGS=\"-O2 -pipe\"\nMAKEOPTS=\"-j4\"\n");
        assertEquals(4, st.makeJobs(), "the build options are read from the file, however it was written");
        assertTrue(st.fileAt("/etc/portage/make.conf").contains("-j4"));
        st.writeFileAt("/etc/fstab", st.fileAt("/etc/fstab") + "\n" + ROOT_LINE + "\n");
        assertTrue(st.filesystemTable().contains(ROOT_LINE), "the table is the new system's, by its own path there");
    }

    @Test
    void fileAt_aNameTypedFromWhereTheSessionStands_isThatFolders() {
        final LiveInstallState st = gentooInsideTheNewSystem();
        step(st, "cd /etc/portage");
        st.writeFileAt("make.conf", "MAKEOPTS=\"-j3\"");
        assertEquals(3, st.makeJobs());
        assertNull(st.fileAt("nothing-here"), "a file that is not there is not there");
    }

    @Test
    void aVerbOfTheOtherDistribution_isCommandNotFound() {
        assertTrue(step(new LiveInstallState(LiveInstallState.Distro.GENTOO), "pacstrap /mnt base").text()
                .contains("command not found"));
        assertTrue(step(new LiveInstallState(LiveInstallState.Distro.ARCH), "emerge --sync").text()
                .contains("command not found"));
    }
}
