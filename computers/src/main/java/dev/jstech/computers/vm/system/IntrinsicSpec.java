/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * One call the system answers instead of the program: which call it is, the type it gives, whether it is made on an
 * object, who answers it and what it costs, and for a call the language answers with nothing but what it hands over,
 * the Java that does.
 *
 * <p>A parameter written as a lone capital letter ({@code T}, {@code K}, {@code V}) is a type parameter and takes a
 * value of any type, because a call on a generic collection is written with the types it was used with.
 *
 * @param id       which call it is
 * @param returns  the type it gives back
 * @param onTarget whether it is made on an object, which is handed over ahead of the arguments
 * @param kind     who answers it
 * @param cost     what it costs beyond the instruction that makes it
 * @param function the Java that answers a pure call; null for a call its process or the machine answers
 */
public record IntrinsicSpec(MemberId id, String returns, boolean onTarget, MemberKind kind, CallCost cost,
                            IPureFunction function) {

    private static final String OUT = "out ";

    public IntrinsicSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(returns, "returns");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(cost, "cost");
        if (kind == MemberKind.PURE && function == null) {
            throw new IllegalArgumentException(id.describe() + " is pure and has no function to answer it");
        }
        if (kind != MemberKind.PURE && function != null) {
            throw new IllegalArgumentException(
                    id.describe() + " is answered by its " + kind.name().toLowerCase(Locale.ROOT)
                            + ", not by a function of the language");
        }
    }

    /** A call the language answers with {@code function}, costing nothing beyond its instruction. */
    public static IntrinsicSpec pure(final String owner, final String name, final List<String> parameters,
                                     final String returns, final boolean onTarget, final IPureFunction function) {
        return new IntrinsicSpec(new MemberId(owner, name, parameters), returns, onTarget, MemberKind.PURE,
                CallCost.FREE, function);
    }

    /** The type the call is on. */
    public String owner() {
        return this.id.owner();
    }

    /** What the call is called. */
    public String name() {
        return this.id.name();
    }

    /** The types it takes, with {@code out} in front of one it fills in. */
    public List<String> parameters() {
        return this.id.parameters();
    }

    /** Whether a call written with these parameter types is this one. */
    public boolean accepts(final List<String> written) {
        final List<String> parameters = this.parameters();
        if (written.size() != parameters.size()) {
            return false;
        }
        for (int i = 0; i < written.size(); i++) {
            if (!matches(parameters.get(i), written.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** How the call is written: {@code Map.TryGet(K, out V)}. */
    public String describe() {
        return this.id.describe();
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
