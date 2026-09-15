/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.List;

/** The Java that makes one of the objects the runtime brings: a list, a map, a window or a widget. */
@FunctionalInterface
interface IObjectMaker {

    /**
     * Makes one.
     *
     * @param process   the process making it, whose heap what is made is counted against
     * @param arguments what its constructor is handed, in order
     * @param line      the line making it, for what the program is told when it goes wrong
     * @return what was made
     */
    Object make(Process process, List<Object> arguments, int line);
}
