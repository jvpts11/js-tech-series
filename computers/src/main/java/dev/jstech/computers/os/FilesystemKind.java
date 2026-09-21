/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.core.id.IStableName;

/**
 * The filesystem model provided by a kernel.
 *
 * <p>This enum is pure and carries no Minecraft dependency, so it can be used freely in JUnit tests
 * and in kernel definitions without pulling in the binding layer.
 */
public enum FilesystemKind implements IStableName {
    /** No filesystem support, so data lives in memory only. */
    NONE("none"),
    /** A flat key-value store with no directory hierarchy. */
    FLAT("flat"),
    /** A full hierarchical directory tree (folders within folders). */
    HIERARCHICAL("hierarchical");

    private final String serializedName;

    FilesystemKind(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
