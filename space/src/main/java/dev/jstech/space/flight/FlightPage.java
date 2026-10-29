/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.flight;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;
import java.util.List;

/**
 * The pages of the Flight Computer, one under each key along the top of its screen, as {@link #KEYS} orders them: the
 * system, the transfer, the landing, the stages and the radio. The transfer and the landing are open; the others name
 * their keys and are for the computer's later work.
 */
public enum FlightPage implements IStableName, IStableId {

    SYSTEM("sys", 0, false),
    TRANSFER("xfer", 1, true),
    LANDING("land", 2, true),
    STAGES("stg", 3, false),
    RADIO("comm", 4, false);

    /** The pages in the order of their keys along the top. */
    public static final List<FlightPage> KEYS = List.of(SYSTEM, TRANSFER, LANDING, STAGES, RADIO);

    private final String serializedName;
    private final int id;
    private final boolean open;

    FlightPage(final String serializedName, final int id, final boolean open) {
        this.serializedName = serializedName;
        this.id = id;
        this.open = open;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    @Override
    public int id() {
        return this.id;
    }

    /** Whether its key opens it. */
    public boolean open() {
        return this.open;
    }
}
