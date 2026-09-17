/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.lower;

import dev.jstech.computers.sigma.sem.IBinding;
import java.util.Map;

/**
 * What a method's lambdas keep hold of.
 *
 * <p>This is the answer, not the thing the answer is used to build. Where those variables end up living, and
 * under what name, is settled where the assembly is written; that they have to live somewhere other than the
 * method's own slots is settled here, because it is a fact about the program.
 *
 * <p>The order of the variables is the order they were met in, and it is kept: it decides the order the
 * fields are written down in and the order they are filled, and a program reads the same either way only
 * because both ends walk the same list.
 *
 * @param fields    the variables kept, in the order they were met, under the names they will be known by
 * @param holdsThis whether a lambda also reaches for the object the method belonged to
 */
public record Captures(Map<IBinding.Variable, String> fields, boolean holdsThis) {
}
