/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

/**
 * What an energy does to a port that meets a live line of another class, by how many classes the line is above the
 * port's (below it when negative). A rule may read the server's settings, so it is asked each time.
 */
@FunctionalInterface
public interface IWrongClassRule {

    /**
     * A class above burns the block, two or more an arc flash; a class below leaves it short of voltage. The rule of
     * real electrical equipment.
     */
    IWrongClassRule STANDARD = steps -> steps < 0 ? WrongClass.UNDERVOLTAGE
            : steps == 0 ? WrongClass.NONE : steps == 1 ? WrongClass.BURNS : WrongClass.ARC_FLASH;
    /** Any other class leaves the block short and harms nothing. */
    IWrongClassRule REFUSES = steps -> steps == 0 ? WrongClass.NONE : WrongClass.UNDERVOLTAGE;
    /** Every port takes every class. */
    IWrongClassRule ANY = steps -> WrongClass.NONE;

    /** What befalls a port when the line is {@code steps} classes above its own. */
    WrongClass at(int steps);
}
