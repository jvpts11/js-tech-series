/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.operation;

/**
 * The family a kind of Operation belongs to, by what part of the world it works on.
 *
 * <p>The family is for people: the screens that list a network's Operations group and filter by it. It changes
 * nothing about how an Operation is queued or run. Most families belong to a mod of the series, some of them still
 * to come, so an addon picks the one its kind of work is closest to.
 */
public enum OperationCategory {
    /** Items, fluids and other things kept on the network's disks: searching, storing, moving, taking out. */
    STORAGE,

    /** The network itself: its index of what is stored, and its upkeep. */
    NETWORK,

    /** Making things: recipes at a crafting computer and machine work. */
    CRAFTING,

    /** Defence and weapons. */
    MILITARY,

    /** Factories and their machines. */
    INDUSTRIAL,

    /** Satellites, rockets and what is beyond the sky. */
    SPACE,

    /** Moving things and people from place to place. */
    TRANSPORT,

    /** Crops and animals. */
    AGRICULTURE,

    /** The ground: surveys, deposits and what lies under them. */
    GEOLOGICAL,

    /** The sea. */
    OCEANIC,

    /** Building: structures raised, changed or taken down. */
    CONSTRUCTION,

    /** Robots and what they are told to do. */
    ROBOTICS,

    /** Who may use what, and when: reserving items and letting them go. */
    CONCURRENCY,

    /** Access and protection. */
    SECURITY;
}
