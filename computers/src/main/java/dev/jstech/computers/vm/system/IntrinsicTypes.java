/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

/**
 * The names of the types the machine answers for itself, rather than because a program declared them.
 *
 * <p>These are the two ends of one contract. The compiler writes a call to one of them into a listing and the
 * machine looks that call up by the same name, so the two have to agree exactly, and until now each wrote the
 * name out for itself: text was spelled in four places and a delegate in two. A name written twice is a name
 * that can come to differ, and the listing would still be written, still be loaded, and the call would be the
 * one thing nothing answers.
 */
public final class IntrinsicTypes {

    /** Text, whose joining and reading the machine does rather than any program. */
    public static final String TEXT = "string";

    /** A handler, or several joined into one. */
    public static final String DELEGATE = "Delegate";

    /** The growable sequence. */
    public static final String LIST = "List";

    /** The keyed collection. */
    public static final String MAP = "Map";

    private IntrinsicTypes() {
    }
}
