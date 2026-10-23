/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import dev.jstech.core.model.AnimationController;
import dev.jstech.core.model.BakedGeometry;
import dev.jstech.core.model.GeometryBaker;
import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.MeshDeformer;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * One thing drawn with a model of the Core's: a block entity, an entity, an item stack. It keeps what moves it, its
 * animator, and the room its frames are worked out in, so a frame makes nothing new; it works its pose out once a
 * frame, however many times it is drawn in it. A thing that plays nothing, and that no code moves, is at rest, and its
 * pose is not worked out again until it moves; every thing drawn with one model shares the model's shape.
 */
public final class KitInstance {

    private final KitModel model;
    private final ModelAnimator animator;
    private final Matrix4f[] world;
    private final List<MeshDeformer> deformers = new ArrayList<>();
    private final GeometryBaker.Corners corners = new GeometryBaker.Corners();
    private final boolean[] hiddenScratch;
    private final Matrix4f branchScratch = new Matrix4f();
    private boolean moved;
    private long animatedFrame = -1;

    KitInstance(final KitModel model) {
        this.model = model;
        this.animator = new ModelAnimator(model.data(), model.animations());
        this.world = new Matrix4f[model.data().bones().size()];
        this.hiddenScratch = new boolean[this.world.length];
        for (int i = 0; i < this.world.length; i++) {
            this.world[i] = new Matrix4f();
        }
        for (final BakedGeometry.Deformed deformed : model.geometry().deformed()) {
            this.deformers.add(new MeshDeformer(deformed, this.animator.pose()));
        }
        this.animator.pose().world(this.world);
    }

    /** The model it is drawn with. */
    public KitModel model() {
        return this.model;
    }

    /** What moves it: its controllers, its code, its expressions' queries. */
    public ModelAnimator animator() {
        return this.animator;
    }

    /** Its pose, as last worked out. */
    public ModelPose pose() {
        return this.animator.pose();
    }

    /** The place of each of its bones in the model's space, as last worked out. */
    public Matrix4f[] world() {
        return this.world;
    }

    /**
     * Works its pose out for frame {@code frame}, at {@code seconds} by the model's clock, unless it has been for that
     * frame already; one at rest is left at rest.
     *
     * @param code whether code moves its bones each frame, so it is never at rest
     */
    public void animate(final long frame, final double seconds, final boolean code, final IAnimationListener listener) {
        animate(frame, seconds, code, listener, null, null);
    }

    /**
     * Works its pose out for frame {@code frame} as {@link #animate(long, double, boolean, IAnimationListener)} does,
     * {@code driver} moving {@code thing}'s bones after the animations ({@link IKitDriver#move}); never at rest while
     * the driver moves bones.
     */
    public <T> void animate(final long frame, final double seconds, final IKitDriver<? super T> driver, final T thing,
                            final IAnimationListener listener) {
        animate(frame, seconds, driver.movesBones(), listener, driver, thing);
    }

    List<MeshDeformer> deformers() {
        return this.deformers;
    }

    GeometryBaker.Corners corners() {
        return this.corners;
    }

    boolean[] hiddenScratch() {
        return this.hiddenScratch;
    }

    Matrix4f branchScratch() {
        return this.branchScratch;
    }

    /* The pose worked out, unless it already is for that frame or rests: the animations, then the driver's moves. */
    private <T> void animate(final long frame, final double seconds, final boolean code,
                             final IAnimationListener listener, @Nullable final IKitDriver<? super T> driver,
                             @Nullable final T thing) {
        if (frame == this.animatedFrame) {
            return;
        }
        this.animatedFrame = frame;
        if (!code && !playing(seconds)) {
            if (this.moved) {
                // It has come to rest: back to the pose at rest, once, and nothing more until it moves again.
                work(seconds, listener, driver, thing);
                this.moved = false;
            }
            return;
        }
        work(seconds, listener, driver, thing);
        this.moved = true;
    }

    private <T> void work(final double seconds, final IAnimationListener listener,
                          @Nullable final IKitDriver<? super T> driver, @Nullable final T thing) {
        final ModelPose pose = this.animator.animate(seconds, listener);
        if (driver != null) {
            driver.move(thing, pose, seconds);
        }
        pose.world(this.world);
    }

    private boolean playing(final double seconds) {
        for (final AnimationController controller : this.animator.controllers()) {
            if (!controller.idle(seconds)) {
                return true;
            }
        }
        return false;
    }
}
