/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Big blocks: one block bigger than a block, placed whole. The player places it at its origin and it raises its parts
 * round it at once; breaking any cell takes the whole of it down and gives back what it held. Nothing is built in it
 * block by block and it forms nothing, which is what tells it from a multiblock (a structure of its own blocks with a
 * terminal, in {@code dev.jstech.core.multiblock}).
 *
 * <p>A big block extends {@link AbstractBigBlock} and hands it its {@link IBigBlockGeometry}, usually a
 * {@link BigBlockFootprint} drawn in letters; its parts' block entities extend {@link BigBlockPartBlockEntity}, which
 * remembers the origin. J's Computers' Mainframe and Server Rack are big blocks
 * ({@code computers/src/main/java/dev/jstech/computers/block/MainframeBlock.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.bigblock;

import org.jetbrains.annotations.ApiStatus;
