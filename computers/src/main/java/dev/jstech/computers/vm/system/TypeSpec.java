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
 * One of the types the system brings, as the compiler and the editors know it: the namespace a program brings it in
 * from, what it is called, and its members in the order they were declared.
 *
 * @param namespace where a program brings it in from, {@code System.Utils}
 * @param name      what it is called, which is also how a listing names it
 * @param members   its methods and values, every one of them on this type
 */
public record TypeSpec(String namespace, String name, List<IMemberSpec> members) {

    public TypeSpec {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(name, "name");
        members = List.copyOf(members);
        for (final IMemberSpec member : members) {
            if (!member.id().owner().equals(name)) {
                throw new IllegalArgumentException(member.id().describe() + " is not a member of " + name);
            }
        }
    }
}
