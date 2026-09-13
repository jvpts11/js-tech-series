/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

import java.util.List;
import java.util.Objects;

/**
 * One call the system answers instead of the program: the type it is on, what it is called, the types it takes and
 * gives as the language declares them, and the Java that answers it.
 *
 * <p>A parameter written as a lone capital letter ({@code T}, {@code K}, {@code V}) is a type parameter and takes a
 * value of any type, because a call on a generic collection is written with the types it was used with.
 *
 * @param owner      the type the call is on, as a listing names it
 * @param name       what the call is called
 * @param parameters the types it takes, with {@code out} in front of one it fills in
 * @param returns    the type it gives back
 * @param onTarget   whether it is made on an object, which is handed over ahead of the arguments
 * @param function   the Java that answers it
 */
public record IntrinsicSpec(String owner, String name, List<String> parameters, String returns, boolean onTarget,
                            IPureFunction function) {

    private static final String OUT = "out ";

    public IntrinsicSpec {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(returns, "returns");
        Objects.requireNonNull(function, "function");
        parameters = List.copyOf(parameters);
    }

    /** Whether a call written with these parameter types is this one. */
    public boolean accepts(final List<String> written) {
        if (written.size() != this.parameters.size()) {
            return false;
        }
        for (int i = 0; i < written.size(); i++) {
            if (!matches(this.parameters.get(i), written.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** How the call is written: {@code Map.TryGet(K, out V)}. */
    public String describe() {
        return this.owner + "." + this.name + "(" + String.join(", ", this.parameters) + ")";
    }

    /** Whether a declared type stands for any type. */
    public static boolean isTypeParameter(final String type) {
        return type.length() == 1 && Character.isUpperCase(type.charAt(0));
    }

    private static boolean matches(final String declared, final String written) {
        final boolean outward = declared.startsWith(OUT);
        if (outward != written.startsWith(OUT)) {
            return false;
        }
        final String type = outward ? declared.substring(OUT.length()) : declared;
        return isTypeParameter(type) || type.equals(outward ? written.substring(OUT.length()) : written);
    }
}
