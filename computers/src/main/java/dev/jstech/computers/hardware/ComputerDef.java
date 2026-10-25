/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.os.HostScope;
import dev.jstech.core.jstechseries.tier.Era;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.ApiStatus;

/**
 * What a kind of computer is, whichever mod brings it: the entry the computers registry keeps for it, beside the block
 * or the item it is. Its id is that block's or that item's, and its case is the block itself, or the chassis of a
 * server; what the entry says is what each computer reads instead of writing it into its class.
 *
 * @param era         the era of the case: a case takes a board of its own era only
 * @param formFactors the form factors of the boards the case takes
 * @param machine     the machine it is, which decides the programs made for one machine that install on it
 * @param parts       how many parts of each kind the case counts
 */
@ApiStatus.Experimental
public record ComputerDef(Era era, Set<FormFactor> formFactors, HostScope machine, CaseParts parts) {

    public ComputerDef {
        Objects.requireNonNull(era, "era");
        formFactors = Set.copyOf(formFactors);
        if (formFactors.isEmpty()) {
            throw new IllegalArgumentException("a computer takes a board of at least one form factor");
        }
        Objects.requireNonNull(machine, "machine");
        Objects.requireNonNull(parts, "parts");
    }

    /** Whether {@code board} goes in this computer's case: of a form it takes, and of the case's own era. */
    public boolean takesBoard(final MotherboardSpec board) {
        return PartFitting.boardFits(board, formFactors, era);
    }
}
