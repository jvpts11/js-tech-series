/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

/**
 * What a pure function may ask of the program that called it: room on its heap for what it makes, and nothing else.
 *
 * <p>A pure function reads its arguments and gives back an answer. The one thing it may do beyond that is make
 * something (a piece of text, a list), which has to go on the calling program's heap and weigh there what it weighs.
 */
public interface IPureContext {

    /** A fresh piece of text on the program's heap. */
    String text(String value, int line);

    /** Puts something the call made on the program's heap, weighing {@code bytes}, and gives it back. */
    <T> T allocate(T value, long bytes, int line);

    /** Changes what something already on the heap weighs, as a collection does when it grows or shrinks. */
    void resize(Object value, long bytes, int line);
}
