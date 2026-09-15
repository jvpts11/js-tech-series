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
 * Something that happens to one of the system's objects, which a program hears about by joining a handler of its own,
 * as the compiler and the editors know it.
 *
 * @param id       which event it is, taking nothing
 * @param handler  the delegate a handler is written as, a plain one such as {@code Action}
 * @param isStatic whether it happens to the type rather than to an object of it
 * @param kind     who answers it
 * @param cost     what joining or parting a handler costs beyond the instruction that does it
 */
public record EventSpec(MemberId id, String handler, boolean isStatic, MemberKind kind, CallCost cost)
        implements IMemberSpec {

    public EventSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(handler, "handler");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(cost, "cost");
        if (!id.parameters().isEmpty()) {
            throw new IllegalArgumentException(id.describe() + " is an event, so it takes nothing");
        }
        if (handler.indexOf('<') >= 0) {
            throw new IllegalArgumentException(id.describe() + " is heard by a plain delegate, not " + handler);
        }
    }
}
