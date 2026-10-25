/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.JsComputers;

/**
 * A small computer drawn as its case: the age of the case, which of the age's cases it is, and which machine it
 * holds. Its block entity draws it, from the model this names: J's Computers' own, or, for a machine another mod
 * brings on the cases J's Computers ships, the models that mod draws for it in its own files.
 */
public interface IComputerCase extends IEraChassisBlock {

    /** Which of its age's cases the computer comes in. */
    CaseStyle caseStyle();

    /** Which small computer it is, as its models name it, such as {@code personal_computer}. */
    String machineName();

    /** The mod whose files its models are in: J's Computers', unless a mod draws its machine in its own. */
    default String modelsOf() {
        return JsComputers.MODID;
    }

    /**
     * The model the case is drawn with, such as {@code computer_standard_neutral_personal_computer}; named with its mod
     * when its files are another mod's ({@code othermod:computer_standard_neutral_reader}).
     */
    default String caseModel() {
        final String name = "computer_" + caseStyle().caseName(chassisEra()) + "_" + machineName();
        return modelsOf().equals(JsComputers.MODID) ? name : modelsOf() + ":" + name;
    }
}
