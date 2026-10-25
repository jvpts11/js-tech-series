/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.rack;

import dev.jstech.computers.hardware.CaseParts;
import dev.jstech.computers.hardware.ComputerDef;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.os.HostScope;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.ApiStatus;

/**
 * The chassis a rack-mounted computer is built in: how many rack units it spends, what the rack's front bays can hold
 * for it, how much hardware fits inside, which cabinet seats it, and the era it belongs to. The era is a hard gate
 * like on every other computer: the case takes only boards of its own era, and a cabinet seats servers of its own era
 * or earlier. The hardware inside still decides the speed. Numbers are estimates, tuned in play.
 *
 * <p>The entry of a server in the computers registry: this mod's own servers are the constants here, and a mod
 * bringing a server of its own declares its chassis the same way, beside the server's item.
 *
 * @param computer what the server is as a computer: its era, the boards it takes, its machine and its parts; its
 *                 drives are the rack's front bays it cables
 * @param heightU  the rack units it spends
 * @param rackType the kind of cabinet that seats it
 * @param look     which of the server faces this mod ships the rack draws it as, in its era's version
 */
@ApiStatus.Experimental
public record RackChassis(ComputerDef computer, int heightU, RackType rackType, Look look) {

    /** The memory modules every server's board area has room for. */
    public static final int MEMORY = 8;
    /*
     * A server takes a server-class board: EEB across the eras, or the EATX workstation board where the era offers
     * one. It does not accept consumer or Mainframe-only boards.
     */
    private static final Set<FormFactor> SERVER_BOARDS = Set.of(FormFactor.EEB, FormFactor.EATX);

    /** The Standard 1U server. */
    public static final RackChassis SERVER = of(Look.SERVER, 1, 4, 2, CaseParts.AS_THE_BOARD, RackType.SERVER,
            HardwareEra.STANDARD);
    /**
     * The Legacy 1U server: fewer drives, two expansion slots, one for a graphics card, since its board has no video
     * of its own, and one for a storage card.
     */
    public static final RackChassis LEGACY_SERVER = of(Look.SERVER, 1, 3, 2, 2, RackType.SERVER, HardwareEra.LEGACY);
    /** The Vintage 1U server: a single drive, one socket, one expansion slot. */
    public static final RackChassis VINTAGE_SERVER = of(Look.SERVER, 1, 1, 1, 1, RackType.SERVER,
            HardwareEra.VINTAGE);
    /**
     * 2U, ten drive bays, storage first; two expansion slots, so its storage card has room beside a graphics card,
     * which a board with no video of its own needs for a monitor.
     */
    public static final RackChassis STORAGE_SERVER = of(Look.STORAGE, 2, 10, 1, 2, RackType.SERVER,
            HardwareEra.STANDARD);
    /** 2U, four sockets, compute first. */
    public static final RackChassis COMPUTE_SERVER = of(Look.COMPUTE, 2, 2, 4, CaseParts.AS_THE_BOARD,
            RackType.SERVER, HardwareEra.STANDARD);
    /** 2U node of a supercomputer: one Phi and one GPU, seated only in a Supercomputer Rack. */
    public static final RackChassis SUPERCOMPUTER_NODE = of(Look.NODE, 2, 2, 2, 2, RackType.SUPERCOMPUTER,
            HardwareEra.STANDARD);
    /** The Transition 1U server: two sockets, five drives filling the unit's front, two cards. */
    public static final RackChassis TRANSITION_SERVER = of(Look.SERVER, 1, 5, 2, 2, RackType.SERVER,
            HardwareEra.TRANSITION);
    /** The Advanced 1U server: two sockets, five drives filling the unit's front, any cards. */
    public static final RackChassis ADVANCED_SERVER = of(Look.SERVER, 1, 5, 2, CaseParts.AS_THE_BOARD,
            RackType.SERVER, HardwareEra.ADVANCED);
    /**
     * The Advanced 2U node: four accelerator sleds (the Phi and three GPUs) and three NVMe carriers, seated only in a
     * Supercomputer Rack of its era.
     */
    public static final RackChassis ADVANCED_SUPERCOMPUTER_NODE = of(Look.NODE, 2, 3, 2, 4, RackType.SUPERCOMPUTER,
            HardwareEra.ADVANCED);
    /*
     * The storage and compute servers of the other eras, 2U like the Standard's: each age's storage server fills
     * more of its front with drives, and each age's compute server takes more processors and cards. Every storage
     * server has two expansion slots, one for its storage card and one for a graphics card, which a board with no
     * video of its own needs for a monitor.
     */
    /** The Vintage 2U storage server: five full-height drives, one socket, two expansion slots. */
    public static final RackChassis VINTAGE_STORAGE_SERVER = of(Look.STORAGE, 2, 5, 1, 2, RackType.SERVER,
            HardwareEra.VINTAGE);
    /** The Legacy 2U storage server: seven drives, one socket, two expansion slots. */
    public static final RackChassis LEGACY_STORAGE_SERVER = of(Look.STORAGE, 2, 7, 1, 2, RackType.SERVER,
            HardwareEra.LEGACY);
    /** The Transition 2U storage server: eight drives, one socket, two expansion slots. */
    public static final RackChassis TRANSITION_STORAGE_SERVER = of(Look.STORAGE, 2, 8, 1, 2, RackType.SERVER,
            HardwareEra.TRANSITION);
    /** The Advanced 2U storage server: ten NVMe carriers, one socket, two expansion slots. */
    public static final RackChassis ADVANCED_STORAGE_SERVER = of(Look.STORAGE, 2, 10, 1, 2, RackType.SERVER,
            HardwareEra.ADVANCED);
    /** The Vintage 2U compute server: two sockets, one drive, one expansion slot. */
    public static final RackChassis VINTAGE_COMPUTE_SERVER = of(Look.COMPUTE, 2, 1, 2, 1, RackType.SERVER,
            HardwareEra.VINTAGE);
    /** The Legacy 2U compute server: two sockets, two drives, two expansion slots. */
    public static final RackChassis LEGACY_COMPUTE_SERVER = of(Look.COMPUTE, 2, 2, 2, 2, RackType.SERVER,
            HardwareEra.LEGACY);
    /** The Transition 2U compute server: four sockets, two drives, two expansion slots. */
    public static final RackChassis TRANSITION_COMPUTE_SERVER = of(Look.COMPUTE, 2, 2, 4, 2, RackType.SERVER,
            HardwareEra.TRANSITION);
    /** The Advanced 2U compute server: four sockets, three drives, as many cards as its board takes. */
    public static final RackChassis ADVANCED_COMPUTE_SERVER = of(Look.COMPUTE, 2, 3, 4, CaseParts.AS_THE_BOARD,
            RackType.SERVER, HardwareEra.ADVANCED);

    public RackChassis {
        Objects.requireNonNull(computer, "computer");
        Objects.requireNonNull(rackType, "rackType");
        Objects.requireNonNull(look, "look");
        if (heightU < 1) {
            throw new IllegalArgumentException("a server spends at least one rack unit");
        }
    }

    /** The kind of cabinet a chassis seats in. */
    public enum RackType {
        SERVER,
        SUPERCOMPUTER,
        AI
    }

    /** The server faces this mod ships, each drawn in the version of the server's era. */
    public enum Look {
        /** The 1U server. */
        SERVER,
        /** The 2U storage server, its front full of drives. */
        STORAGE,
        /** The 2U compute server. */
        COMPUTE,
        /** The 2U node of a supercomputer. */
        NODE
    }

    /**
     * A server's chassis as this mod's own are: a server-class board, the server machine, its board area's memory,
     * and the counts given. A mod bringing a server of a kind of its own builds the record itself.
     */
    public static RackChassis of(final Look look, final int heightU, final int driveSlots, final int maxCpus,
                                 final int maxPcie, final RackType rackType, final Era era) {
        return new RackChassis(new ComputerDef(era, SERVER_BOARDS, HostScope.SERVER,
                new CaseParts(maxCpus, MEMORY, maxPcie, driveSlots, 0, 0)), heightU, rackType, look);
    }

    /** Whether this is a supercomputer's node: the chassis the fabric surveys and the co-processor goes in. */
    public boolean isSupercomputerNode() {
        return rackType == RackType.SUPERCOMPUTER;
    }

    /** The era of the case: the boards it takes and the cabinets that seat it follow from this. */
    public Era era() {
        return computer.era();
    }

    /** The drives it cables in the rack's front bays. */
    public int driveSlots() {
        return computer.parts().disks();
    }

    public int maxCpus() {
        return computer.parts().processors();
    }

    /** The expansion cards it seats; {@link CaseParts#AS_THE_BOARD} when its board alone decides. */
    public int maxPcie() {
        return computer.parts().cards();
    }
}
