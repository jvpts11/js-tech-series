/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import java.util.List;

/**
 * A block entity whose block stands as blocks of formed models: a merged block, or one that keeps itself in its
 * structure, as a locker a ship's model draws keeps its stores and its click. Such a block is aimed at by the faces of
 * the blocks it stands as and outlined by them, or by the whole piece aimed at ({@link FormedPiece}), on a player's
 * game.
 */
public interface IFormedPlaced {

    /** Every block of a formed model it stands as, its own first; none for none. */
    List<FormedPlace> places();
}
