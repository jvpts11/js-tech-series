/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.emit;

import dev.jstech.computers.sigma.sem.IBinding;
import java.util.Map;

/**
 * The object a method and its lambdas share, so that a variable one of them writes is the one the others read.
 *
 * @param fields    the variables it holds, under the names its fields carry
 * @param holdsThis whether it also holds the object the method belonged to, for a lambda that reaches for it
 */
record Closure(String type, Map<IBinding.Variable, String> fields, boolean holdsThis) {

    /** The name of the field a closure keeps the object the method belonged to in. */
    static final String OUTER = "0this";
}
