/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.menu;

import net.minecraft.world.inventory.DataSlot;

import java.util.function.IntSupplier;

/**
 * An int an open menu shows that the server works out, from whatever the menu is open on, and the client's copy of
 * the menu receives whenever it changes. Read through {@link #get()} on both sides: the server works it out each
 * time, the client answers what it last received. Declared with {@link CoreMenu#value}.
 */
public final class MenuValue extends DataSlot {

    private final IntSupplier onServer;
    private final boolean onClient;
    private int received;

    MenuValue(final IntSupplier onServer, final boolean onClient) {
        this.onServer = onServer;
        this.onClient = onClient;
    }

    /** The value: worked out on the server, as last received on the client. */
    @Override
    public int get() {
        return onClient ? received : onServer.getAsInt();
    }

    @Override
    public void set(final int value) {
        received = value;
    }

    /** Whether the value is not zero, for a value that says yes or no. */
    public boolean isSet() {
        return get() != 0;
    }
}
