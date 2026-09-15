/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

/** A member of one of the system's types: which call it is, where it is called, who answers it and what it costs. */
public sealed interface IMemberSpec permits MethodSpec, PropertySpec {

    /** Which call it is; reading a value is a call that takes nothing. */
    MemberId id();

    /** Whether it is on the type rather than on an object of it. */
    boolean isStatic();

    /** Who answers it. */
    MemberKind kind();

    /** What it costs beyond the instruction that makes it. */
    CallCost cost();
}
