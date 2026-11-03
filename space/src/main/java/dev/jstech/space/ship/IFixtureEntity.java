/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.IFormedPlaced;
import org.jetbrains.annotations.Nullable;

/**
 * The block entity of a starship's fixture, a locker, a console, a bunk: it keeps the block of its part's formed model
 * it stands for, told as its part forms, so the fixture stands, is aimed at and is outlined as the model draws it
 * there, while its click still works it.
 */
public interface IFixtureEntity extends IFormedPlaced {

    /** Keeps {@code place}, the block of its part's formed model it stands for, or none once its part comes apart. */
    void place(@Nullable FormedPlace place);
}
