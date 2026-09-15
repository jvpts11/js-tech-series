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
 * A way of making an object of one of the system's types, as the compiler and the editors know it.
 *
 * <p>A listing names a constructor by its type and the types it takes, {@code Window(string, int, int)}, so every
 * constructor has the same name, {@link #NAME}, and they are told apart by what they take.
 *
 * @param id   which constructor it is, named {@link #NAME}
 * @param kind who answers it
 * @param cost what making one costs beyond the instruction that makes it
 */
public record ConstructorSpec(MemberId id, MemberKind kind, CallCost cost) implements IMemberSpec {

    /** The name every constructor goes by. */
    public static final String NAME = "new";

    public ConstructorSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(cost, "cost");
        if (!NAME.equals(id.name())) {
            throw new IllegalArgumentException(id.describe() + " is a constructor, so it is called " + NAME);
        }
    }

    /** A constructor is asked of the type, since there is no object yet to ask. */
    @Override
    public boolean isStatic() {
        return true;
    }
}
