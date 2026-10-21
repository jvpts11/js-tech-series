/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * The typed ids the Core gives what it keeps an identity for: a whole data network, each of its members, and a moving
 * structure; and the primitives a network's id is assigned, propagated along cable graphs and checked for collisions
 * with: the id itself, made at random or read back from text, and the state it is in.
 *
 * <p>Each id is a record around a {@code UUID}, of a type of its own, so one kind of id can never be handed where
 * another is wanted: {@link NetworkUuid}, which every member of a data network carries; {@link NodeUuid}, each member's
 * own; and {@link StructureUuid}, which a moving structure keeps for its whole life, through a restart, a change of
 * dimension and every pose it takes. A network's id is in one of the states of {@link NetworkUuidState}: active, the
 * only one that runs, or orphaned or in conflict, both of which can be recovered from.
 *
 * <p>How a data network carries these ids, from zero, is in {@code core/docs/NETWORKS.md}. An example that compiles is
 * the test mod's network member tests
 * ({@code tests/src/main/java/dev/jstech/tests/gametest/core/NetworkMemberGameTests.java}), which put a machine of the
 * test mod on a network and keep a member's node id in a save.
 */
@ApiStatus.Experimental
package dev.jstech.core.uuid;

import org.jetbrains.annotations.ApiStatus;
