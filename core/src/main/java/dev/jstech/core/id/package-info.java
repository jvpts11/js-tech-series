/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Enum constants with a number or a name of their own, which saves, packets, menus and data files carry instead of the
 * constant's position or its Java name, so constants can be renamed, reordered and added anywhere without anything
 * already written coming back as a different constant.
 *
 * <p>An enum that is saved or sent implements {@link IStableId}, a number from 0 to 127 for packets, menus and saves,
 * or {@link IStableName}, a lower case name for data files, item components and what a player types, and finds its
 * constants through a {@link StableIds} or {@link StableNames} table built once, usually in a static field of the enum
 * itself: a number out of range or a name given twice then fails the first time the enum is used, instead of coming
 * back later as the wrong constant. Both tables are pure Java, so the enums of logic tested without the game can hold
 * them. {@link StableCodecs} makes the codecs that write a constant by its name in data files and item components, and
 * by its id on the wire.
 *
 * <p>How to give an enum its ids, from zero, is in {@code core/docs/CONTENT.md}. Examples that compile are the test
 * mod's codec tests ({@code tests/src/main/java/dev/jstech/tests/gametest/core/StableCodecGameTests.java}) and J's
 * Computers' bus conditions ({@code computers/src/main/java/dev/jstech/computers/bus/BusCondition.java}), whose kinds
 * are saved by their ids.
 */
@ApiStatus.Experimental
package dev.jstech.core.id;

import org.jetbrains.annotations.ApiStatus;
