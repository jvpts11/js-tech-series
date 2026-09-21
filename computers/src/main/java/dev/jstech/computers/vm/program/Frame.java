/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.ArrayList;
import java.util.List;

/**
 * One call in progress.
 *
 * <p>The stack holds nulls, because null is a value a program can have and hand around, so it is
 * kept in something that allows one rather than in something that treats it as an absence.
 */
final class Frame {
    final MethodImage method;
    final Object[] slots;
    final List<Object> stack = new ArrayList<>();
    final Object self;
    int at;
    /** Set on all but the last handler of a run, whose answers nobody is waiting for. */
    boolean discard;

    Frame(final MethodImage method, final Object self) {
        this.method = method;
        this.slots = new Object[Math.max(method.slots(), method.parameters().size())];
        this.self = self;
    }

    void push(final Object value) {
        this.stack.add(value);
    }

    Object pop() {
        return this.stack.isEmpty() ? null : this.stack.remove(this.stack.size() - 1);
    }

    Object peek() {
        return this.stack.isEmpty() ? null : this.stack.getLast();
    }
}
