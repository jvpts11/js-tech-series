/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

/** The Java that makes one of the objects the language's core brings, on the heap of the program making it. */
@FunctionalInterface
interface IObjectMaker {

    /**
     * Makes one.
     *
     * @param heap the heap of the program making it, which what is made is counted against
     * @param line the line making it, for what the program is told when it goes wrong
     * @return what was made
     */
    Object make(Heap heap, int line);
}
