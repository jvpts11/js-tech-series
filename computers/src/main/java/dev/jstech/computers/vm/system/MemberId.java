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
 * Which call a member of the system is: the type it is on, what it is called, and the types it takes as a listing
 * writes them, with {@code out} in front of one it fills in.
 *
 * <p>Two members may share a type and a name as long as the types they take differ, so a call is only ever one of them.
 *
 * @param owner      the type the call is on, as a listing names it
 * @param name       what the call is called
 * @param parameters the types it takes
 */
public record MemberId(String owner, String name, List<String> parameters) {

    public MemberId {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(name, "name");
        parameters = List.copyOf(parameters);
    }

    /** How the call is written: {@code Map.TryGet(K, out V)}. */
    public String describe() {
        return this.owner + "." + this.name + "(" + String.join(", ", this.parameters) + ")";
    }

    /** What calls are first sorted by, before their types are compared: the type, the name and how many they take. */
    static String shape(final String owner, final String name, final int parameters) {
        return owner + "." + name + "/" + parameters;
    }
}
