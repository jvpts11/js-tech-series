/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.Map;

/**
 * What two values being the same means to a program, and what counts as true where a branch asks.
 *
 * <p>Two values are the same when they say the same thing, which for a bool and the number that stands for it means
 * comparing what they both mean rather than what they are. Two structs or two records are the same when everything
 * they hold is, field by field, however far down that goes. The fields are read where they are, never copied, so
 * comparing two values makes nothing.
 */
final class ValueSemantics {

    private final ProgramImage program;

    ValueSemantics(final ProgramImage program) {
        this.program = program;
    }

    /**
     * Whether a value counts as true where a branch asks.
     *
     * <p>The assembly has no constant for a bool: true and false are written as a one and a zero, so a number stands
     * for a bool wherever one was meant, and zero is the false one.
     */
    static boolean truth(final Object value) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof Number number) {
            return number.longValue() != 0;
        }
        return value != null;
    }

    /** Whether two values are the same, as an equality test or a branch on one asks. */
    boolean same(final Object left, final Object right) {
        if (left == null || right == null) {
            return left == right;
        }
        if (left instanceof Boolean || right instanceof Boolean) {
            return truth(left) == truth(right);
        }
        if (left instanceof String || right instanceof String) {
            return left.equals(right);
        }
        if (left instanceof Values.Obj one && right instanceof Values.Obj other && one.type().equals(other.type())) {
            final TypeImage kind = this.program.type(one.type());
            if (kind != null && kind.kind().byValue()) {
                final Map<String, Object> mine = one.fields();
                final Map<String, Object> theirs = other.fields();
                if (!mine.keySet().equals(theirs.keySet())) {
                    return false;
                }
                for (final Map.Entry<String, Object> field : mine.entrySet()) {
                    if (!this.same(field.getValue(), theirs.get(field.getKey()))) {
                        return false;
                    }
                }
                return true;
            }
        }
        if (left instanceof Number && right instanceof Number) {
            return Numbers.compare(left, right) == 0;
        }
        return left.equals(right);
    }
}
