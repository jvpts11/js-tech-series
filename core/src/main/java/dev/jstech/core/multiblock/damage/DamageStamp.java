/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.damage;

import java.util.Objects;

/**
 * One kind of damage, made once as geometry and laid on a formed model's faces wherever it happens, turned by the
 * surface there, at the damage's size and by a seed: the hole it cuts, how rough the hole's rim is, the field that
 * pushes the faces round it and the pixels its edge is drawn with. Its lengths are shares of the damage's size. Pure.
 *
 * @param hole  how far out its rim is, a share of its size; 0 for one that cuts nothing
 * @param rough how rough its rim is, 0 for a round hole
 * @param push  the field that pushes the faces round it
 * @param depth how far the field pushes, a share of its size
 * @param curl  whether the petals round its hole also curl in over it, as metal torn from outside does
 * @param edge  the pixels its edge is drawn with
 */
public record DamageStamp(double hole, double rough, Push push, double depth, boolean curl, Edge edge) {

    public DamageStamp {
        Objects.requireNonNull(push, "push");
        Objects.requireNonNull(edge, "edge");
        if (hole < 0 || hole > 1 || rough < 0 || rough > 1 || depth < 0 || depth > 1) {
            throw new IllegalArgumentException("a stamp's hole, roughness and depth are shares from 0 to 1, not "
                    + hole + ", " + rough + " and " + depth);
        }
    }

    /** Whether it cuts a hole. */
    public boolean cuts() {
        return this.hole > 0;
    }

    /** The fields that push the faces round a stamp. */
    public enum Push {
        /** Petals bent in round the hole, as metal struck from outside. */
        PETALS_IN,
        /** Petals bent out round the hole, as metal blown from inside. */
        PETALS_OUT,
        /** A shallow crater round a small hole. */
        CRATER,
        /** The rim sagging in, as metal melted. */
        SAG,
        /** A dent with folds round it. */
        FOLDS,
        /** A plain shallow dent. */
        DENT
    }

    /** The pixels a stamp's edge is drawn with, over the faces round it. */
    public enum Edge {
        /** Bare metal torn along the rim. */
        TORN,
        /** Bare metal torn along the rim, and soot round it. */
        SOOTED,
        /** A ring of bare metal round a small crater. */
        SPALL,
        /** Char round the rim, and the last of the glow on it. */
        SCORCHED,
        /** A few scrapes where it struck, the paint rubbed off. */
        SCUFFED
    }
}
