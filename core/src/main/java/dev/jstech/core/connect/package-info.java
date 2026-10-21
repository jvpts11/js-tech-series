/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Blocks that know their neighbours: what a block takes on each face and through which ports, the faces named from
 * the block's own point of view, the six around a block told when it changes, and which neighbours a face's texture
 * runs into.
 *
 * <p>What reaches a face is a {@link Connection}: a line, the job a cable does, in one generation of it. Lines
 * are open ids, so a mod brings its own without the Core knowing them, and a port takes its own generation of a
 * line and every earlier one, never a later one. A block declares its ports once, as a constant {@link FacePorts},
 * each port on the faces a {@link FaceRule} names: every face, or some of its {@link RelativeFace}s, which turn
 * with the block. It implements {@link IFaceConnector} to answer them, so the cable that shows a connection and
 * the device that joins through it ask the same question and never disagree, and tells the six around it through
 * {@link Neighbours} when what it takes changes. A block whose faces join their neighbours' into one picture says
 * which with an {@link IJoinRule}; {@link ConnectedFaces} works out which of the eight around each face it joins, and
 * {@link ConnectedQuadrants} draws the face from five tiles, a quarter at a time.
 *
 * <p>Lines and ports are explained from zero in {@code core/docs/CABLES.md} and {@code core/docs/NETWORKS.md}, and
 * connected textures in {@code core/docs/CONNECTED_TEXTURES.md}. Examples that compile are J's Example's energy line
 * ({@code example/src/main/java/dev/jstech/example/ExampleContent.java}) and J's Computers' router, whose ports take
 * the data lines up to its era on every face
 * ({@code computers/src/main/java/dev/jstech/computers/block/RouterBlock.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.connect;

import org.jetbrains.annotations.ApiStatus;
