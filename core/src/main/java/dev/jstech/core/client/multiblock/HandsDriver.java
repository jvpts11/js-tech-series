/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.multiblock;

import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.model.ModelAnimation;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import dev.jstech.core.multiblock.hands.HandGeometry;

/**
 * Drives a formed structure's model as its own driver does, and then turns its doors on their hinges as far as they
 * are open this frame.
 *
 * @param <T> what the model is drawn for
 */
public final class HandsDriver<T> implements IKitDriver<T> {

    private final IKitDriver<? super T> own;
    private final HandGeometry geometry;
    private final float[] open;

    /** Drives as {@code own} does, the doors of {@code geometry} as far open as {@code open} says by their number. */
    public HandsDriver(final IKitDriver<? super T> own, final HandGeometry geometry, final float[] open) {
        this.own = own;
        this.geometry = geometry;
        this.open = open;
    }

    @Override
    public void drive(final T thing, final ModelAnimator animator, final double seconds) {
        this.own.drive(thing, animator, seconds);
    }

    @Override
    public void move(final T thing, final ModelPose pose, final double seconds) {
        this.own.move(thing, pose, seconds);
        this.geometry.poseDoors(pose, this.open);
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    @Override
    public void instruction(final T thing, final ModelAnimation animation, final String word) {
        this.own.instruction(thing, animation, word);
    }
}
