/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.core.content.Device;

/**
 * A chain machine's own terminal, the Plasma Etcher Terminal of the Plasma Etcher: made of the Machine Terminal and a
 * block of that machine's own, placed at the terminal's place of the machine's shape, it forms the machine once the
 * shape stands whole round it, and runs it. Broken, it gives itself back.
 */
public final class ChainMachineTerminalBlock extends ChainFormedBlock {

    private final String machine;

    public ChainMachineTerminalBlock(final Properties properties, final Device<?> device, final String machine) {
        super(properties, device);
        this.machine = machine;
    }

    /** The id of the machine it runs. */
    public String machine() {
        return machine;
    }
}
