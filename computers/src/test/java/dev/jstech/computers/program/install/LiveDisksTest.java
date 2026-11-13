/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LiveDisksTest {

    private LiveDisks disks;
    private LiveFiles files;

    @BeforeEach
    void setUp() {
        this.disks = new LiveDisks("/mnt");
        this.files = new LiveFiles("/mnt", "");
    }

    @Test
    void mount_aTypeFlagTakesItsArgumentAndMountsTheDevice() {
        final LiveTurn turn = this.disks.mount(words("mount -t ext4 /dev/sda2 /mnt"), this.files);

        // Read as a device mount, so it is refused for the missing filesystem and not for a missing mount point.
        assertFalse(turn.ok());
        assertTrue(turn.text().contains("wrong fs type"), turn.text());
    }

    @Test
    void mount_anOptionsFlagTakesItsArgumentToo() {
        final LiveTurn turn = this.disks.mount(words("mount -o rw /dev/sda2 /mnt"), this.files);

        assertTrue(turn.text().contains("wrong fs type"), turn.text());
    }

    @Test
    void mount_aVirtualTypeIsABindOfTheMediumsOwnPlace() {
        final LiveTurn turn = this.disks.mount(words("mount --types proc /proc /mnt/proc"), this.files);

        assertFalse(turn.ok());
        assertTrue(turn.text().contains("mount point does not exist"), turn.text());
    }

    @Test
    void mkfsExt4_refusesTheRootWhileItIsMounted() {
        this.disks.load(saved("sda", true));

        final LiveTurn turn = this.disks.mkfsExt4("/dev/sda", env());

        assertFalse(turn.ok());
        assertTrue(turn.text().contains("is mounted; will not make a filesystem here!"), turn.text());
    }

    @Test
    void mkfsExt4_refusesADiskGivenALabelThoughNoPartitionIsOnItYet() {
        final FdiskProcess fdisk = new FdiskProcess(this.disks, "sda", 20_480L);
        fdisk.answer("g", 0L, null);
        fdisk.answer("w", 0L, null);

        assertTrue(this.disks.labelled("sda"), "the label is kept with no partition on it");
        final LiveTurn turn = this.disks.mkfsExt4("/dev/sda", env());
        assertFalse(turn.ok());
        assertTrue(turn.text().contains("contains a gpt partition table"), turn.text());
    }

    @Test
    void mount_aPlaceBoundAnywhereButUnderTheNewRootIsNotBoundIn() {
        this.disks.load(saved("sda", true));

        for (final String place : new String[] {"/proc", "/sys", "/dev", "/run"}) {
            this.disks.mount(words("mount --rbind " + place + " /tmp/x"), this.files);
        }
        assertFalse(this.disks.boundIn(), "none of them is under the new system");
        for (final String place : new String[] {"/proc", "/sys", "/dev", "/run"}) {
            this.disks.mount(words("mount --rbind " + place + " /mnt" + place), this.files);
        }
        assertTrue(this.disks.boundIn(), "each bound to its own place under it is");
    }

    private static LiveSaved saved(final String device, final boolean mounted) {
        final LiveSaved saved = new LiveSaved();
        saved.put("device", device);
        saved.put("formatted", true);
        saved.put("mounted", mounted);
        return saved;
    }

    private static LiveInstallState.Env env() {
        return new LiveInstallState.Env(List.of(new LiveInstallState.Device("sda", 20_480)), true, 0L, 4, 2000, 4,
                false, 6_000L, false, (typed, byCategory) -> null);
    }

    private static String[] words(final String line) {
        return line.split(" ");
    }
}
