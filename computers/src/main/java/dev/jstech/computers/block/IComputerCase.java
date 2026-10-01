/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

/**
 * A small computer drawn as its case: the age of the case, which of the age's cases it is, and which of the three
 * machines it holds. Its block entity draws it, from the model this names.
 */
public interface IComputerCase extends IEraChassisBlock {

    /** Which of its age's cases the computer comes in. */
    CaseStyle caseStyle();

    /** Which of the three small computers it is, as its models name it, such as {@code personal_computer}. */
    String machineName();

    /** The model the case is drawn with, such as {@code computer_standard_neutral_personal_computer}. */
    default String caseModel() {
        return "computer_" + caseStyle().caseName(chassisEra()) + "_" + machineName();
    }
}
