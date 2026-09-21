/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.term;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tab at a terminal: the command being typed, the drive a command is pointed at, and the round of presses. */
class TermCompletionTest {

    private TermCompletion completion;

    @BeforeEach
    void setUp() {
        completion = new TermCompletion();
        completion.know(List.of("mkdir", "mkfs.ext4", "mkfs.fat", "mount", "lsblk"), List.of("sda", "sdb"));
    }

    @Test
    void next_finishesTheOnlyCommandThatStartsThatWay_withTheSpaceAfterIt() {
        assertEquals(Optional.of("lsblk "), completion.next("ls"));
    }

    @Test
    void next_goesRoundEveryCommandThatStartsWithWhatWasTyped() {
        assertEquals(Optional.of("mkdir"), completion.next("mk"));
        assertEquals(Optional.of("mkfs.ext4"), completion.next("mkdir"));
        assertEquals(Optional.of("mkfs.fat"), completion.next("mkfs.ext4"));
        assertEquals(Optional.of("mkdir"), completion.next("mkfs.fat"));
    }

    @Test
    void typedByHand_endsTheRound_soTheNextPressStartsFromTheLineAsItIs() {
        completion.next("mk");
        completion.typedByHand();
        assertEquals(Optional.of("mkfs.ext4"), completion.next("mkfs"));
        assertEquals(Optional.of("mkfs.fat"), completion.next("mkfs.ext4"));
    }

    @Test
    void next_ignoresTheCaseOfWhatWasTyped() {
        assertEquals(Optional.of("mount "), completion.next("MOU"));
    }

    @Test
    void next_withNothingTypedOrNothingThatStartsThatWayLeavesTheLineAlone() {
        assertEquals(Optional.empty(), completion.next(""));
        assertEquals(Optional.empty(), completion.next("zz"));
    }

    @Test
    void next_offersTheDrivesToACommandThatIsPointedAtOne() {
        assertEquals(Optional.of("mount /dev/sda"), completion.next("mount "));
        assertEquals(Optional.of("mount /dev/sdb"), completion.next("mount /dev/sda"));
        assertEquals(Optional.of("mount /dev/sda"), completion.next("mount /dev/sdb"));
    }

    @Test
    void next_takesADriveNamedWithOrWithoutItsDirectory() {
        assertEquals(Optional.of("fdisk /dev/sdb "), completion.next("fdisk sdb"));
        completion.typedByHand();
        assertEquals(Optional.of("fdisk /dev/sdb "), completion.next("fdisk /dev/sdb"));
    }

    @Test
    void next_completesNoArgumentOfACommandThatTakesNoDrive_andNoSecondArgument() {
        assertEquals(Optional.empty(), completion.next("mkdir sd"));
        assertEquals(Optional.empty(), completion.next("mount /dev/sda /m"));
    }

    @Test
    void know_startsOverWithWhatTheMachineHasNow() {
        completion.next("mk");
        completion.know(List.of("make"), List.of());
        assertEquals(Optional.of("make "), completion.next("ma"));
        completion.typedByHand();
        assertEquals(Optional.empty(), completion.next("mount "));
    }
}
