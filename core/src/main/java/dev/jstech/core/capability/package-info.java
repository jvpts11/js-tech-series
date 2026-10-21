/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Capabilities for any mod: one of its own, declared in one line for blocks, items and entities at once; a lookup of
 * what a neighbour offers, kept between ticks; and what a block entity implements to say what it is on the network,
 * and what may be asked of it.
 *
 * <p>A mod declares a capability of its own as a {@link CapabilityKind} constant, with {@link CapabilityKinds}: a
 * block or an entity answers it by the face it is asked on, an item as a whole, each offered while the capabilities
 * are registered and asked for wherever it is needed. Something that asks the same neighbour every tick keeps a
 * {@link CapabilityCache} instead, for any capability, the game's own included: asking it costs a field read, and
 * the game tells it when the block there is placed, broken or changes what it offers, so the answer is worked out
 * again only then. A block entity that is a node of the data network implements {@link INetworkNodeCapability}, its
 * identity, the network it is on and its category; one the network may inspect implements {@link IMonitorable}, and
 * reports the same readings a sensor speaks in redstone.
 *
 * <p>How to declare a capability and keep a lookup, from zero, is in {@code core/docs/TOOLKIT.md}. An example that
 * compiles is the test mod's probe, a capability of its own offered by a kiln and by a part on a cable
 * ({@code tests/src/main/java/dev/jstech/tests/TestToolkit.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.capability;

import org.jetbrains.annotations.ApiStatus;
