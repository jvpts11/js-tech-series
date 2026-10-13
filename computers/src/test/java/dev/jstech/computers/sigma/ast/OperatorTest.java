/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OperatorTest {

    @Test
    void assignmentText_plainAssignIsOneEqualsSign() {
        assertEquals("=", Operator.ASSIGN.assignmentText());
    }

    @Test
    void assignmentText_compoundFormsAppendTheEqualsSign() {
        assertEquals("+=", Operator.ADD.assignmentText());
        assertEquals("<<=", Operator.SHIFT_LEFT.assignmentText());
    }
}
