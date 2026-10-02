/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.bus;

/** Something a bus can be set to do, which a bus of an era has or has not. */
public enum BusFeature {

    /** Listing the items it moves, only these or all but these. */
    FILTER,
    /** How many to leave where they are taken from, and how many to move at a time. */
    QUANTITIES,
    /** A keep and a max for each item it lists, rather than one for all of them. */
    ITEM_QUANTITIES,
    /** Which of the network's buses moves first when they want the same thing. */
    PRIORITY,
    /** Moving only while a condition holds: the network's stock, the hours of the day, another bus finished. */
    CONDITIONS,
    /** Listing items by their tags as well as one by one. */
    TAGS,
    /** Taking an item whatever its damage and its components, as the item it is. */
    FUZZY,
    /** Which way the network may use an external inventory: to read and write, to read only, or to write only. */
    ACCESS
}
