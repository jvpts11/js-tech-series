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
        return new LiveInstallState.Env(List.of("sda", "sdb"), mirror, now, 100L);
    }

    private static LiveInstallState.Result run(final LiveInstallState st, final String line, final boolean mirror,
                                               final long now) {
        return st.run(line, env(mirror, now));
    }

    @Test
    void arch_fullSequence_completesOnReboot() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        assertTrue(run(st, "mkfs.ext4 /dev/sda", true, 0).ok());
        assertTrue(run(st, "mount /dev/sda /mnt", true, 0).ok());
        assertTrue(run(st, "pacstrap /mnt base linux", true, 0).ok());
        assertTrue(run(st, "genfstab -U /mnt >> /mnt/etc/fstab", true, 0).ok());
        assertTrue(run(st, "arch-chroot /mnt", true, 0).ok());
        assertEquals("[root@archiso /]#", st.prompt());
        assertTrue(run(st, "grub-install /dev/sda", true, 0).ok());
        assertTrue(run(st, "passwd", true, 0).ok());
        assertTrue(run(st, "exit", true, 0).ok());
        final LiveInstallState.Result reboot = run(st, "reboot", true, 0);
        assertTrue(reboot.ok());
        assertTrue(reboot.complete());
        assertEquals(0, st.targetIndex());
    }

    @Test
    void arch_pacstrapWithoutMount_isNotAMountpoint() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        final LiveInstallState.Result r = run(st, "pacstrap /mnt base linux", true, 0);
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
        final LiveInstallState.Result r = run(st, "mount /dev/sdb /mnt", true, 0);
        assertFalse(r.ok());
        assertTrue(r.lines().get(0).contains("wrong fs type"));
    }

    @Test
    void reboot_beforeBootloader_listsWhatIsMissing() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        run(st, "mkfs.ext4 /dev/sda", true, 0);
        run(st, "mount /dev/sda /mnt", true, 0);
        run(st, "pacstrap /mnt base linux", true, 0);
        final LiveInstallState.Result r = run(st, "reboot", true, 0);
        assertFalse(r.ok());
        assertFalse(r.complete());
        assertTrue(String.join("\n", r.lines()).contains("no bootloader"));
    }

    @Test
    void reboot_insideChroot_isRefused() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        run(st, "mkfs.ext4 /dev/sda", true, 0);
        run(st, "mount /dev/sda /mnt", true, 0);
        run(st, "pacstrap /mnt base linux", true, 0);
        run(st, "arch-chroot /mnt", true, 0);
        assertFalse(run(st, "reboot", true, 0).ok());
    }

    @Test
    void gentoo_kernelBuildWaitsForCompileTicks() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        run(st, "mkfs.ext4 /dev/sdb", true, 0);
        run(st, "mount /dev/sdb /mnt", true, 0);
        assertTrue(run(st, "tar xpf stage3-amd64.tar.xz -C /mnt", true, 0).ok());
        assertTrue(run(st, "chroot /mnt", true, 0).ok());
        assertFalse(run(st, "emerge sys-kernel/gentoo-sources", true, 0).ok()); // not synced yet
        assertTrue(run(st, "emerge --sync", true, 0).ok());
        assertTrue(run(st, "emerge sys-kernel/gentoo-sources", true, 10).ok());
        assertFalse(run(st, "genkernel all", true, 50).ok());   // still compiling (ready at 110)
        assertTrue(run(st, "genkernel all", true, 120).ok());
        assertTrue(run(st, "grub-install /dev/sdb", true, 120).ok());
        assertTrue(run(st, "passwd", true, 120).ok());
        run(st, "exit", true, 120);
        assertTrue(run(st, "reboot", true, 120).complete());
        assertEquals(1, st.targetIndex());
    }

    @Test
    void serialize_roundTripsTheProgress() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        run(st, "mkfs.ext4 /dev/sda", true, 0);
        run(st, "mount /dev/sda /mnt", true, 0);
        run(st, "tar xpf stage3-amd64.tar.xz -C /mnt", true, 0);
        run(st, "chroot /mnt", true, 0);
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
        final LiveInstallState.Result read = run(st, "cat /root/install.txt", true, 0);
        assertTrue(read.ok());
        assertTrue(String.join("\n", read.lines()).contains("pacstrap /mnt base linux"),
                "the guide names the step that fetches the base system");
    }

    /** What a step wrote is what reading it back shows, rather than a line written in advance. */
    @Test
    void cat_theFilesystemTable_isWhatTheStepReallyWrote() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        run(st, "mkfs.ext4 /dev/sdb", true, 0);
        run(st, "mount /dev/sdb /mnt", true, 0);
        run(st, "pacstrap /mnt base linux", true, 0);
        assertFalse(run(st, "cat /mnt/etc/fstab", true, 0).ok(), "nothing has written it yet");

        run(st, "genfstab -U /mnt >> /mnt/etc/fstab", true, 0);
        final LiveInstallState.Result read = run(st, "cat /mnt/etc/fstab", true, 0);
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
        run(st, "mkfs.ext4 /dev/sda", true, 0);
        run(st, "mount /dev/sda /mnt", true, 0);
        run(st, "pacstrap /mnt base linux", true, 0);
        run(st, "genfstab -U /mnt >> /mnt/etc/fstab", true, 0);
        run(st, "arch-chroot /mnt", true, 0);

        assertEquals("[root@archiso /]#", st.prompt());
        assertTrue(run(st, "cat /etc/fstab", true, 0).ok(),
                "the table written to /mnt/etc/fstab outside is /etc/fstab in here");
    }

    @Test
    void cd_movesAndThePromptSaysWhere() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        assertEquals("livecd ~ #", st.prompt());
        assertTrue(run(st, "cd /mnt", true, 0).ok());
        assertEquals("livecd /mnt #", st.prompt());
        assertFalse(run(st, "cd /nowhere", true, 0).ok());
        assertEquals("livecd /mnt #", st.prompt(), "a move that failed moved nothing");
        assertTrue(run(st, "cd", true, 0).ok());
        assertEquals("livecd ~ #", st.prompt(), "and with nowhere named it goes home");
    }

    @Test
    void ls_listsWhatIsThereAndSaysSoWhenThereIsNot() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        assertTrue(String.join(" ", run(st, "ls /root", true, 0).lines()).contains("install.txt"));
        assertFalse(run(st, "ls /mnt/etc", true, 0).ok(), "nothing is installed yet");
        run(st, "mkfs.ext4 /dev/sda", true, 0);
        run(st, "mount /dev/sda /mnt", true, 0);
        run(st, "pacstrap /mnt base linux", true, 0);
        assertTrue(run(st, "ls /mnt", true, 0).ok(), "the base system laid its directories down");
        assertTrue(String.join(" ", run(st, "ls /mnt", true, 0).lines()).contains("etc"));
    }

    /** What the session wrote and where it was standing survive the world going away. */
    @Test
    void serialize_roundTripsTheFilesAndWhereTheSessionStands() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.ARCH);
        run(st, "mkfs.ext4 /dev/sda", true, 0);
        run(st, "mount /dev/sda /mnt", true, 0);
        run(st, "pacstrap /mnt base linux", true, 0);
        run(st, "genfstab -U /mnt >> /mnt/etc/fstab", true, 0);
        run(st, "cd /mnt/etc", true, 0);

        final LiveInstallState back = LiveInstallState.deserialize(st.serialize());
        assertEquals("root@archiso /mnt/etc #", back.prompt());
        assertTrue(String.join("\n", back.run("cat fstab", env(true, 0)).lines()).contains("UUID=jsc-sda"));
        assertTrue(back.run("cat /root/install.txt", env(true, 0)).ok(),
                "and the medium's own guide is back with it");
    }

    @Test
    void wrongDistroVerb_isCommandNotFound() {
        final LiveInstallState st = new LiveInstallState(LiveInstallState.Distro.GENTOO);
        assertTrue(run(st, "pacstrap /mnt base", true, 0).lines().get(0).contains("command not found"));
    }
}
