/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.Set;

/**
 * The boards each of the three towers takes, era by era, as the tower cases of the time did: Baby-AT and AT in the
 * Vintage, ATX after it, and a workstation's EATX for the Personal Computer from the Transition on and the Cluster
 * Management Computer from the Legacy on; never for the Crafting Computer.
 *
 * <p>An EATX board may carry two sockets, and a tower that takes one seats a second row for them: a second processor
 * and four more modules, used only while a two-socket board is in.
 */
public final class TowerBoards {

    private static final Set<FormFactor> VINTAGE = Set.of(FormFactor.BABY_AT, FormFactor.AT);
    private static final Set<FormFactor> CONSUMER = Set.of(FormFactor.ATX);
    private static final Set<FormFactor> WORKSTATION = Set.of(FormFactor.ATX, FormFactor.EATX);

    private TowerBoards() {
    }

    /** The boards a Personal Computer of {@code era} takes. */
    public static Set<FormFactor> personalComputer(final Era era) {
        return era == HardwareEra.VINTAGE ? VINTAGE : era == HardwareEra.LEGACY ? CONSUMER : WORKSTATION;
    }

    /** The boards a Cluster Management Computer of {@code era} takes. */
    public static Set<FormFactor> clusterManagementComputer(final Era era) {
        return era == HardwareEra.VINTAGE ? VINTAGE : WORKSTATION;
    }

    /** The boards a Crafting Computer of {@code era} takes. */
    public static Set<FormFactor> craftingComputer(final Era era) {
        return era == HardwareEra.VINTAGE ? VINTAGE : CONSUMER;
    }

    /** Whether a tower that takes {@code boards} seats a second row, for a two-socket workstation board. */
    public static boolean secondRow(final Set<FormFactor> boards) {
        return boards.contains(FormFactor.EATX);
    }
}
