/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

import java.util.Objects;

/**
 * A method of one of the system's types, as the compiler and the editors know it: which call it is, what it gives back,
 * whether it is called on the type or on an object of it, who answers it and what it costs.
 *
 * @param id       which call it is
 * @param returns  the type it gives back, written as a listing writes it
 * @param isStatic whether it is called on the type rather than on an object of it
 * @param kind     who answers it
 * @param cost     what it costs beyond the instruction that makes it
 */
public record MethodSpec(MemberId id, String returns, boolean isStatic, MemberKind kind, CallCost cost)
        implements IMemberSpec {

    public MethodSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(returns, "returns");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(cost, "cost");
    }
}
