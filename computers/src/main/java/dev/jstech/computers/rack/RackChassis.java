/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.rack;

import dev.jstech.core.tier.HardwareEra;

/**
 * The chassis a rack-mounted computer is built in: how many rack units it spends, what the rack's
 * front bays can hold for it, how much hardware fits inside, which cabinet seats it, and the era it
 * belongs to. The era is a hard gate like on every other computer: the case takes only boards of its
 * own era, and a cabinet seats servers of its own era or earlier. The hardware inside still decides the
 * speed. Numbers are estimates, tuned in play.
 */
public enum RackChassis {
    /** The Standard 1U server. */
    SERVER(1, 4, 2, Integer.MAX_VALUE, RackType.SERVER, HardwareEra.STANDARD),
    /** The Legacy 1U server: fewer drives, one expansion slot (the card that lets a player sit at it). */
    LEGACY_SERVER(1, 3, 2, 1, RackType.SERVER, HardwareEra.LEGACY),
    /** The Vintage 1U server: a single drive, one socket, one expansion slot. */
    VINTAGE_SERVER(1, 1, 1, 1, RackType.SERVER, HardwareEra.VINTAGE),
    /** 2U, ten drive bays, storage first. */
    STORAGE_SERVER(2, 10, 1, 1, RackType.SERVER, HardwareEra.STANDARD),
    /** 2U, four sockets, compute first. */
    COMPUTE_SERVER(2, 2, 4, Integer.MAX_VALUE, RackType.SERVER, HardwareEra.STANDARD),
    /** 2U node of a supercomputer: one Phi and one GPU, seated only in a Supercomputer Rack. */
    SUPERCOMPUTER_NODE(2, 2, 2, 2, RackType.SUPERCOMPUTER, HardwareEra.STANDARD),
    /** The Transition 1U server: two sockets, five drives filling the unit's front, two cards. */
    TRANSITION_SERVER(1, 5, 2, 2, RackType.SERVER, HardwareEra.TRANSITION),
    /** The Advanced 1U server: two sockets, five drives filling the unit's front, any cards. */
    ADVANCED_SERVER(1, 5, 2, Integer.MAX_VALUE, RackType.SERVER, HardwareEra.ADVANCED),
    /**
     * The Advanced 2U node: four accelerator sleds (the Phi and three GPUs) and three NVMe carriers, seated only in a
     * Supercomputer Rack of its era.
     */
    ADVANCED_SUPERCOMPUTER_NODE(2, 3, 2, 4, RackType.SUPERCOMPUTER, HardwareEra.ADVANCED),
    /*
     * The storage and compute servers of the other eras, 2U like the Standard's: each age's storage server fills
     * more of its front with drives, and each age's compute server takes more processors and cards.
     */
    /** The Vintage 2U storage server: five full-height drives, one socket, one expansion slot. */
    VINTAGE_STORAGE_SERVER(2, 5, 1, 1, RackType.SERVER, HardwareEra.VINTAGE),
    /** The Legacy 2U storage server: seven drives, one socket, one expansion slot. */
    LEGACY_STORAGE_SERVER(2, 7, 1, 1, RackType.SERVER, HardwareEra.LEGACY),
    /** The Transition 2U storage server: eight drives, one socket, one expansion slot. */
    TRANSITION_STORAGE_SERVER(2, 8, 1, 1, RackType.SERVER, HardwareEra.TRANSITION),
    /** The Advanced 2U storage server: ten NVMe carriers, one socket, one expansion slot. */
    ADVANCED_STORAGE_SERVER(2, 10, 1, 1, RackType.SERVER, HardwareEra.ADVANCED),
    /** The Vintage 2U compute server: two sockets, one drive, one expansion slot. */
    VINTAGE_COMPUTE_SERVER(2, 1, 2, 1, RackType.SERVER, HardwareEra.VINTAGE),
    /** The Legacy 2U compute server: two sockets, two drives, two expansion slots. */
    LEGACY_COMPUTE_SERVER(2, 2, 2, 2, RackType.SERVER, HardwareEra.LEGACY),
    /** The Transition 2U compute server: four sockets, two drives, two expansion slots. */
    TRANSITION_COMPUTE_SERVER(2, 2, 4, 2, RackType.SERVER, HardwareEra.TRANSITION),
    /** The Advanced 2U compute server: four sockets, three drives, as many cards as its board takes. */
    ADVANCED_COMPUTE_SERVER(2, 3, 4, Integer.MAX_VALUE, RackType.SERVER, HardwareEra.ADVANCED);

    /** The kind of cabinet a chassis seats in. */
    public enum RackType {
        SERVER,
        SUPERCOMPUTER,
        AI
    }

    private final int heightU;
    private final int driveSlots;
    private final int maxCpus;
    private final int maxPcie;
    private final RackType rackType;
    private final HardwareEra era;

    RackChassis(final int heightU, final int driveSlots, final int maxCpus, final int maxPcie,
                final RackType rackType, final HardwareEra era) {
        this.heightU = heightU;
        this.driveSlots = driveSlots;
        this.maxCpus = maxCpus;
        this.maxPcie = maxPcie;
        this.rackType = rackType;
        this.era = era;
    }

    public RackType rackType() {
        return rackType;
    }

    /** Whether this is a supercomputer's node: the chassis the fabric surveys and the co-processor goes in. */
    public boolean isSupercomputerNode() {
        return rackType == RackType.SUPERCOMPUTER;
    }

    /** The era of the case: the boards it takes and the cabinets that seat it follow from this. */
    public HardwareEra era() {
        return era;
    }

    public int heightU() {
        return heightU;
    }

    public int driveSlots() {
        return driveSlots;
    }

    public int maxCpus() {
        return maxCpus;
    }

    public int maxPcie() {
        return maxPcie;
    }
}
