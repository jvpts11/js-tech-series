/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain;

import static dev.jstech.computers.chain.ChainParts.englishName;
import static dev.jstech.computers.chain.ChainParts.part;

import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.List;
import net.minecraft.world.item.Item;

/**
 * The plastic parts the Injection Moulder moulds besides the board components' bodies (computers/docs/design/
 * recipes.md): each era's fan frame, the frame and rotor its case fan is built on. Each is moulded in its own mould,
 * cut on a Mould Blank at the moulder by the computer linked to it and lent to every job, and takes the plastic its
 * product took before; so no product's lot makes another.
 */
public final class MouldedParts {

    /** Each era's fan frame, in the order of the eras. */
    public static final List<Part> FAN_FRAMES = List.of(
            frame(HardwareEra.VINTAGE), frame(HardwareEra.LEGACY), frame(HardwareEra.TRANSITION),
            frame(HardwareEra.STANDARD), frame(HardwareEra.ADVANCED));

    private MouldedParts() {
    }

    /** Declares the parts, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every part, family by family. */
    public static List<Part> all() {
        return FAN_FRAMES;
    }

    /** Era {@code era}'s fan frame. */
    public static Part fanFrame(final Era era) {
        return FAN_FRAMES.stream().filter(frame -> frame.era() == era).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no fan frame of " + era.id()));
    }

    /* Era {@code era}'s fan frame, "Fan Frame (Vintage)", and its mould, "Mould (Fan Frame, Vintage)". */
    private static Part frame(final Era era) {
        final String english = englishName(era);
        return new Part(Family.FANS, era, part("fan_frame_" + era.path(), era).named("Fan Frame (" + english + ")")
                .register(), part("mould_fan_frame_" + era.path(), era).named("Mould (Fan Frame, " + english + ")")
                .register());
    }

    /** What a part is, the list of the moulds the lithography program offers it in. */
    public enum Family {
        FANS
    }

    /**
     * One part.
     *
     * @param family what it is
     * @param era    the era it is first made in
     * @param item   its item
     * @param mould  the steel mould it is moulded in, cut from a Mould Blank
     */
    public record Part(Family family, Era era, ItemEntry<Item> item, ItemEntry<Item> mould) {
    }
}
