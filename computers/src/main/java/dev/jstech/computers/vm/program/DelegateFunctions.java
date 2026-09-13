/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.system.IPureContext;
import dev.jstech.computers.vm.system.IntrinsicRegistry;
import java.util.ArrayList;
import java.util.List;

/**
 * Joining handlers and parting them, which the compiler writes for {@code +=} and {@code -=} on an event or a delegate.
 *
 * <p>Joining two handlers makes a third that calls both. Parting takes the last one that matches, which is how a
 * listener removes only what it added.
 */
final class DelegateFunctions {

    private static final String DELEGATE = "Delegate";

    private DelegateFunctions() {
    }

    static void register(final IntrinsicRegistry.Builder registry) {
        registry.onType(DELEGATE, "Combine", "T", (context, target, arguments, line) ->
                join(context, arguments, true, line), "T", "T");
        registry.onType(DELEGATE, "Remove", "T", (context, target, arguments, line) ->
                join(context, arguments, false, line), "T", "T");
    }

    private static Object join(final IPureContext context, final Object[] arguments, final boolean combine,
                               final int line) {
        final Object left = arguments[0];
        if (!(arguments[1] instanceof Values.DelegateValue added)) {
            return left;
        }
        final List<Values.Bound> chain = new ArrayList<>();
        if (left instanceof Values.DelegateValue held) {
            chain.addAll(held.chain());
        }
        if (combine) {
            chain.addAll(added.chain());
        } else {
            for (int i = chain.size() - 1; i >= 0; i--) {
                if (chain.get(i).equals(added.chain().getFirst())) {
                    chain.remove(i);
                    break;
                }
            }
        }
        if (chain.isEmpty()) {
            return null;
        }
        final Values.DelegateValue made = new Values.DelegateValue(added.type(), chain);
        return context.allocate(made, made.bytes(), line);
    }
}
