/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.jstechseries.tier.Era;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * What fits with what, in one place for every computer, a server's chassis among them, and for every part, whichever
 * mod brings it: a board in a case, a processor and memory on a board, a card in a slot, a disk in a machine. A part
 * that physically cannot go in should not go in the slot either, since letting it in only to refuse to boot tells the
 * player nothing about why. What a kind of computer takes beyond this is its own to say.
 *
 * <p>With no board yet, a processor, memory or a card is staged: it waits for a board, and the slot stays idle until
 * one comes. Plain logic over the sheets, so the rules are tested without the game.
 */
public final class PartFitting {

    private PartFitting() {
    }

    /**
     * Whether {@code board} goes in a case that takes {@code formFactors}: of one of those forms, and of the case's
     * own era when the case asks for one ({@code requiredEra}), as every computer's case does.
     */
    public static boolean boardFits(final MotherboardSpec board, final Set<FormFactor> formFactors,
                                    final @Nullable Era requiredEra) {
        return formFactors.contains(board.formFactor()) && (requiredEra == null || board.era() == requiredEra);
    }

    /** Whether {@code cpu} seats on {@code board}: the socket has to match, and so does the era. */
    public static boolean cpuFits(final CpuSpec cpu, final @Nullable MotherboardSpec board) {
        return board == null || cpu.socket().equals(board.socket()) && cpu.era() == board.era();
    }

    /** Whether {@code ram} seats on {@code board}: a generation its slots are keyed for, of the board's era. */
    public static boolean ramFits(final RamSpec ram, final @Nullable MotherboardSpec board) {
        return board == null || board.acceptedRam().contains(ram.generation()) && ram.era() == board.era();
    }

    /**
     * Whether {@code card} seats on {@code board}: a slot of its bus's family, and, for a sound card, a board of its
     * own age, since no Standard card exists and the boards have it built in.
     */
    public static boolean cardFits(final IExpansionCardSpec card, final @Nullable MotherboardSpec board) {
        if (board == null) {
            return true;
        }
        if (card instanceof SoundCardSpec sound && sound.era() != board.era()) {
            return false;
        }
        return board.takes(card);
    }

    /** Whether a sound card is kept off {@code board} for being of another age. */
    public static boolean soundCardOfAnotherAge(final IExpansionCardSpec card, final @Nullable MotherboardSpec board) {
        return board != null && card instanceof SoundCardSpec sound && sound.era() != board.era();
    }

    /**
     * Whether {@code disk} goes in a machine of {@code machineEra}: a disk of its era or of an older one. A disk made
     * for a later machine has no way into an earlier one (an NVMe made for the Standard age has nothing to plug into
     * in an AT tower), while an old disk still goes into a newer machine, as old disks did. A machine whose era is not
     * known yet takes any.
     */
    public static boolean diskFits(final DiskSpec disk, final @Nullable Era machineEra) {
        return machineEra == null || disk.era().isAtMost(machineEra);
    }
}
