/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect;

import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that something connects to on its faces: a device a cable plugs into, a machine a power line feeds. It
 * answers one question, whether a face takes what reaches it, from the ports it declares; the cable that shows a
 * connection and the device that joins through it both ask it, so they never disagree.
 *
 * <p>A block whose ports depend on more than which way it faces (a cabinet that is a server or a supercomputer node,
 * by its state) overrides {@link #accepts} and keeps {@link #ports()} as the most it could take.
 */
public interface IFaceConnector {

    /** The ports this block declares. */
    FacePorts ports();

    /** Whether the world's {@code face} of this block, in {@code state}, takes {@code offered}. */
    default boolean accepts(final BlockState state, final Direction face, final Connection offered) {
        return ports().accepts(state, face, offered);
    }

    /** Every line some face of this block may take. */
    default Set<ResourceLocation> lines() {
        return ports().lines();
    }
}
