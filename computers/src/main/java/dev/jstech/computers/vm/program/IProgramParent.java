/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.UUID;

/**
 * Which program started another, if any.
 *
 * <p>A program started at a prompt has no parent. One started by another program on the same machine names that
 * program by its number there. One started from another machine cannot, since that number means nothing here, so it
 * names the machine by where it stands and which node of the network it is, and the program by its number there.
 */
public sealed interface IProgramParent {

    /** No parent. */
    None NONE = None.INSTANCE;

    /** No parent: started at a prompt, by a player, or by something that is not a program. */
    enum None implements IProgramParent {
        INSTANCE
    }

    /** A program on the same machine, by its number. */
    record Local(int program) implements IProgramParent {
    }

    /**
     * A program on another machine: where that machine stands (its position packed into one number), the network node
     * it is, and the program's number there.
     */
    record Remote(long machine, UUID node, int program) implements IProgramParent {
    }
}
