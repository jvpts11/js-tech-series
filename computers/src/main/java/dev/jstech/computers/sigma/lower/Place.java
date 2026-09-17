/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.lower;

import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.sem.IBinding;
import dev.jstech.computers.sigma.sem.IMemberSymbol;

/**
 * Somewhere a value can be both read from and written back to.
 *
 * <p>There are only so many of those in the language, and every way of writing to one reads the same list:
 * plain assignment, assignment that combines, and one more or one less. Each used to work the list out again
 * for itself, in its own order, which is how the three came to disagree about how many times the place gets
 * named. Worked out once, here, they cannot.
 *
 * <p>This is only what a place IS. What the assembly says to reach one belongs to whoever is writing it, which
 * keeps the two apart: a place has no opinion about stacks, and the stack has no opinion about how the player
 * wrote the place down. A local names the variable and not a slot, because which slot that turns out to be is
 * the writing stage's business.
 *
 * <p>The one distinction that matters to whoever emits is whether reaching the place leaves something on the
 * stack underneath the value. A local, a captured variable and a static field are reached from nothing, so the
 * value on top can be answered with a duplicate; a field of an object and an element of a collection sit on
 * top of what they belong to, so the answer has to be kept somewhere of its own.
 */
public sealed interface Place {

    /** Whether reaching this place leaves what it belongs to on the stack, under the value. */
    boolean overSomething();

    /** A local or a parameter, which the method keeps in a slot of its own. */
    record Local(IBinding.Variable variable) implements Place {

        @Override
        public boolean overSomething() {
            return false;
        }
    }

    /** A variable a lambda shares with the method it was written in, which lives as a field of one object. */
    record Captured(IBinding.Variable variable) implements Place {

        @Override
        public boolean overSomething() {
            return false;
        }
    }

    /** A field or a property belonging to a type rather than to any one of its objects. */
    record Shared(IMemberSymbol member) implements Place {

        @Override
        public boolean overSomething() {
            return false;
        }
    }

    /**
     * A field or a property of an object.
     *
     * <p>Fields and properties are one thing here because they are one thing in the assembly: a named place on
     * an object. Whether a program is allowed to write to it was settled long before this.
     *
     * @param target what it belongs to as the program wrote it, or null for one of this object's own
     */
    record Held(IExpr target, IMemberSymbol member) implements Place {

        @Override
        public boolean overSomething() {
            return true;
        }
    }

    /** A place inside an array, a list or a map, named by the thing and the place in it. */
    record Element(IExpr.Index index) implements Place {

        @Override
        public boolean overSomething() {
            return true;
        }
    }
}
