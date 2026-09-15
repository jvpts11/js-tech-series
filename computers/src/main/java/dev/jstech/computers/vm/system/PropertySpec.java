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
 * A value one of the system's types holds, which a program reads and, where it may, writes, as the compiler and the
 * editors know it.
 *
 * <p>To the machine, being asked for a value is a call that takes nothing, so a value is named the way that call is.
 *
 * @param id       which value it is, taking nothing
 * @param type     the type it holds, written as a listing writes it
 * @param isStatic whether it is read from the type rather than from an object of it
 * @param writable whether a program may write it as well as read it
 * @param kind     who answers it
 * @param cost     what reading or writing it costs beyond the instruction that makes it
 */
public record PropertySpec(MemberId id, String type, boolean isStatic, boolean writable, MemberKind kind,
                           CallCost cost) implements IMemberSpec {

    public PropertySpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(cost, "cost");
        if (!id.parameters().isEmpty()) {
            throw new IllegalArgumentException(id.describe() + " is a value, so it takes nothing");
        }
    }
}
