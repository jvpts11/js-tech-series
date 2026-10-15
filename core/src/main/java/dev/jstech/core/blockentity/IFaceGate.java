/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

/**
 * A block entity whose faces each let through only part of what it offers: a machine whose back only takes in items,
 * whose top is closed to fluids. The fields it declares {@code exposed()} are offered through it ({@link
 * FieldCapabilities}), face by face; asked with no face, from inside the block, it gives the whole.
 */
public interface IFaceGate {

    /**
     * What it offers for {@code capability} on {@code side}, out of {@code whole}, everything it would offer: the
     * whole, a part that only takes in or only gives out, or nothing.
     */
    @Nullable
    <T> T throughFace(BlockCapability<T, ?> capability, @Nullable Direction side, @Nullable T whole);
}
