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
import dev.jstech.computers.vm.system.IntrinsicTypes;
import java.util.ArrayList;
import java.util.List;

/**
 * Joining handlers and parting them, which the compiler writes for {@code +=} and {@code -=} on an event or a delegate.
 *
 * <p>Joining two handlers makes a third that calls both. Parting takes the last one that matches, which is how a
 * listener removes only what it added.
 */
final class DelegateFunctions {

    private static final String DELEGATE = IntrinsicTypes.DELEGATE;

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
            /*
             * As C# parts them: the last run of handlers that is the whole of what is taken off goes, so taking off
             * a and b together from a, b, c leaves c; a run that is not there leaves the handlers as they were.
             */
            final List<Values.Bound> taken = added.chain();
            for (int start = chain.size() - taken.size(); start >= 0; start--) {
                final List<Values.Bound> run = chain.subList(start, start + taken.size());
                if (run.equals(taken)) {
                    run.clear();
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
