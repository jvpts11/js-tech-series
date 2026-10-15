/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

/**
 * What joins lines of two voltage classes, the only way energy crosses from one class to another. The Core knows what
 * each joins; J's Industrial builds them. Pure: no game types.
 */
public enum GridEquipment {

    /** Joins two neighbouring classes of alternating current, stepping the voltage up or down. */
    TRANSFORMER,
    /** Joins the extra-high class to the ultra-high one, turning alternating current into direct and back. */
    HVDC_STATION
}
