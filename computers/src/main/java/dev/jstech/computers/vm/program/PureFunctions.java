/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.system.IntrinsicRegistry;

/**
 * The calls the language answers in Java with nothing but what they hand over: text, the two collections, numbers and
 * reading them out of text, joining handlers, and ticks.
 *
 * <p>A program reaches them through the call it loaded with, never by their names while it runs.
 */
public final class PureFunctions {

    /** Every pure function of the language. */
    public static final IntrinsicRegistry REGISTRY = build();

    private PureFunctions() {
    }

    private static IntrinsicRegistry build() {
        final IntrinsicRegistry.Builder registry = IntrinsicRegistry.builder();
        TextFunctions.register(registry);
        CollectionFunctions.register(registry);
        NumberFunctions.register(registry);
        DelegateFunctions.register(registry);
        return registry.build();
    }
}
