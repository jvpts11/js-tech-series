/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.core.network.DataLink;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;

/**
 * The short names the data cables go by where a screen says which cable a way runs over: a download's slowest cable, a
 * machine's link to the network.
 */
@TextHolder
public final class DataLinkNames {

    private static final TextKey THIN_COAX = TextKey.of("jsc.cable.link.thin_coax", "Thin coax");
    private static final TextKey ETHERNET = TextKey.of("jsc.cable.link.ethernet", "Ethernet");
    private static final TextKey CAT5E = TextKey.of("jsc.cable.link.cat5e", "Cat 5e");
    private static final TextKey GIGABIT = TextKey.of("jsc.cable.link.gigabit", "Gigabit");
    private static final TextKey CAT6A = TextKey.of("jsc.cable.link.cat6a", "Cat 6a");
    private static final TextKey THICK_COAX = TextKey.of("jsc.cable.link.thick_coax", "Thick coax");
    private static final TextKey HBW = TextKey.of("jsc.cable.link.hbw", "HBW");
    private static final TextKey CX4 = TextKey.of("jsc.cable.link.cx4", "CX4");
    private static final TextKey FIBRE = TextKey.of("jsc.cable.link.fibre", "Fibre");
    private static final TextKey OM5 = TextKey.of("jsc.cable.link.om5", "OM5");
    private static final TextKey TELEPHONE = TextKey.of("jsc.cable.link.telephone", "Telephone");
    private static final TextKey LEASED = TextKey.of("jsc.cable.link.leased", "Leased line");
    private static final TextKey T3 = TextKey.of("jsc.cable.link.t3", "T3");
    private static final TextKey VLDC = TextKey.of("jsc.cable.link.vldc", "VLDC");
    private static final TextKey DARK_FIBRE = TextKey.of("jsc.cable.link.dark_fibre", "Dark fibre");
    private static final TextKey INFINIBAND = TextKey.of("jsc.cable.link.infiniband", "InfiniBand");
    private static final TextKey HPC = TextKey.of("jsc.cable.link.hpc", "HPC");
    private static final TextKey OSFP = TextKey.of("jsc.cable.link.osfp", "OSFP");
    private static final TextKey CRAFTING = TextKey.of("jsc.cable.link.crafting", "Crafting");

    /* Each line's names, from the Vintage cable to the Advanced one; the HPC line begins at the Transition. */
    private static final TextKey[] ACCESS = {THIN_COAX, ETHERNET, CAT5E, GIGABIT, CAT6A};
    private static final TextKey[] BACKBONE = {THICK_COAX, HBW, CX4, FIBRE, OM5};
    private static final TextKey[] LONG_DISTANCE = {TELEPHONE, LEASED, T3, VLDC, DARK_FIBRE};
    private static final TextKey[] COMPUTE = {INFINIBAND, INFINIBAND, INFINIBAND, HPC, OSFP};

    private DataLinkNames() {
    }

    /** The short name of the access cable of {@code era}, which is the cable a board of that era takes. */
    public static TextKey access(final HardwareEra era) {
        return ACCESS[Math.min(era.id(), ACCESS.length - 1)];
    }

    /** The short name of the cable {@code link} is. */
    public static TextKey of(final DataLink link) {
        final int era = Math.min(link.era().id(), ACCESS.length - 1);
        return switch (link.line()) {
            case ACCESS -> ACCESS[era];
            case BACKBONE -> BACKBONE[era];
            case LONG_DISTANCE -> LONG_DISTANCE[era];
            case HPC -> COMPUTE[era];
            case CRAFTING -> CRAFTING;
        };
    }
}
