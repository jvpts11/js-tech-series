/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.lower;

import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.sem.IBinding;
import java.util.List;

/**
 * A tuple taken apart, as what it comes down to: the tuple worked out once and kept in a local of its own, then one
 * plain assignment for each place, from the field that holds it. Every value is read before any place is written, so
 * {@code (a, b) = (b, a)} swaps the two.
 *
 * @param kept   the local the tuple is kept in while it is taken apart, which no source can name
 * @param value  the tuple being taken apart
 * @param writes the assignments, one for each place, in order
 */
public record TakenApart(IBinding.Variable kept, IExpr value, List<IExpr.Assign> writes) {

    public TakenApart {
        writes = List.copyOf(writes);
    }
}
