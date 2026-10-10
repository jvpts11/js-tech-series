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

    private static String[] words(final String line) {
        return line.split(" ");
    }
}
