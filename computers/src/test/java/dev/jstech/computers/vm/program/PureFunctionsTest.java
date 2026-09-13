/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.cannon.sem.BuiltIns;
import dev.jstech.computers.cannon.sem.IMemberSymbol;
import dev.jstech.computers.cannon.sem.NamedType;
import dev.jstech.computers.vm.system.IntrinsicSpec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * What the compiler lets a program call and what the runtime answers have to be the same list. These hold the pure
 * functions to the methods the language declares on its pure types, both ways, with the same types taken and given.
 */
class PureFunctionsTest {

    /** The types of the language whose methods need nothing but their arguments. */
    private static final Set<String> PURE_TYPES = Set.of("string", "List", "Map", "Math", "Convert", "Time");

    /** The calls the compiler writes itself for an operator, which no type of the language declares. */
    private static final Set<String> WRITTEN_BY_THE_COMPILER =
            Set.of("string.Concat(T, U)", "Delegate.Combine(T, T)", "Delegate.Remove(T, T)");

    @Test
    void registry_answersEveryMethodThePureTypesDeclare() {
        final List<String> wrong = new ArrayList<>();
        for (final IMemberSymbol.MethodSymbol method : declared()) {
            final List<String> written = written(method);
            final IntrinsicSpec entry = PureFunctions.REGISTRY.find(method.owner().qualifiedName(), method.name(),
                    written);
            if (entry == null || !entry.parameters().equals(written)
                    || !entry.returns().equals(method.returnType().describe()) || entry.onTarget() == method.isStatic()) {
                wrong.add(describe(method) + " -> " + (entry == null ? "nothing" : entry.describe() + " giving "
                        + entry.returns() + (entry.onTarget() ? " on an object" : " on the type")));
            }
        }
        assertTrue(wrong.isEmpty(), () -> "not registered as the language declares them:\n" + String.join("\n", wrong));
    }

    @Test
    void registry_holdsOnlyWhatTheLanguageDeclaresOrTheCompilerWrites() {
        final Set<String> declared = new HashSet<>();
        for (final IMemberSymbol.MethodSymbol method : declared()) {
            declared.add(describe(method));
        }
        final List<String> extra = new ArrayList<>();
        for (final IntrinsicSpec entry : PureFunctions.REGISTRY.all()) {
            if (!declared.contains(entry.describe()) && !WRITTEN_BY_THE_COMPILER.contains(entry.describe())) {
                extra.add(entry.describe());
            }
        }
        assertTrue(extra.isEmpty(), () -> "registered but not in the language: " + extra);
    }

    private static List<IMemberSymbol.MethodSymbol> declared() {
        final List<IMemberSymbol.MethodSymbol> methods = new ArrayList<>();
        for (final NamedType type : new BuiltIns().all()) {
            if (!PURE_TYPES.contains(type.name())) {
                continue;
            }
            for (final IMemberSymbol member : type.members()) {
                if (member instanceof IMemberSymbol.MethodSymbol method) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    private static List<String> written(final IMemberSymbol.MethodSymbol method) {
        final List<String> types = new ArrayList<>();
        for (final IMemberSymbol.ParameterSymbol parameter : method.parameters()) {
            types.add((parameter.outward() ? "out " : "") + parameter.type().describe());
        }
        return types;
    }

    private static String describe(final IMemberSymbol.MethodSymbol method) {
        return method.owner().qualifiedName() + "." + method.name() + "(" + String.join(", ", written(method)) + ")";
    }
}
