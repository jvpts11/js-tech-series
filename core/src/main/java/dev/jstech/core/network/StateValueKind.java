/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

/** What a column of a state table holds: a number, in a unit it names; a piece of text; or a yes or a no. */
public enum StateValueKind {
    NUMBER,
    TEXT,
    FLAG
}
