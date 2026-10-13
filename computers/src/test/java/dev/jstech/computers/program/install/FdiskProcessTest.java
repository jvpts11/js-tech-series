/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FdiskProcessTest {

    private LiveDisks disks;
    private FdiskProcess fdisk;

    @BeforeEach
    void setUp() {
        this.disks = new LiveDisks("root");
        this.fdisk = new FdiskProcess(this.disks, "sda", 20_480L);
    }

    @Test
    void newPartition_afterOneThatTookTheRestMakesNoSecondOne() {
        type("n", "", "", "");
        type("n", "", "", "+100M");
        type("w");

        assertEquals(1, this.disks.table("sda").size());
        assertEquals(0, this.disks.table("sda").get(0).sizeMb());
    }

    @Test
    void newPartition_aPlainLastAnswerIsASectorNotMegabytes() {
        // Starts at sector 2048; 2099199 is the last sector of the first 1 GiB.
        type("n", "", "", "2099199");
        type("w");

        assertEquals(1024, this.disks.table("sda").get(0).sizeMb());
    }

    @Test
    void newPartition_aLastSectorBeforeTheStartIsRefused() {
        type("n", "", "", "100");
        type("w");

        assertEquals(0, this.disks.table("sda").size());
    }

    @Test
    void newPartition_aPlusSizeStillMeansThatMuch() {
        type("n", "", "", "+512M");
        type("w");

        assertEquals(512, this.disks.table("sda").get(0).sizeMb());
    }

    private void type(final String... lines) {
        for (final String line : lines) {
            this.fdisk.answer(line, 0L, null);
        }
    }
}
