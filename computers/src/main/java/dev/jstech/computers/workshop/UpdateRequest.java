/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.workshop;

import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.uuid.NodeUuid;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * One UPDATE as it was asked for: which Personal Computer's card does it, to how much of which item the network
 * holds, and what the card is to do.
 *
 * @param computer the Personal Computer whose card does the work, the one that asked
 * @param key      the item as the network holds it, its components and all
 * @param quantity how many: smelting takes any number, a stack at a time; an anvil works one stack; enchanting one
 * @param action   what the card does
 * @param offer    which of the three enchanting offers to take, from zero; ignored by every other action
 * @param name     the name an anvil gives, or empty to leave the name as it is
 * @param with     the second item a repair or a combine takes from the network; null for a repair to use whatever
 *                 mends the item
 * @param from     the server to take the item from, or null for anywhere in the network
 * @param payer    the player whose experience pays, or null when nobody asked (only smelting goes unpaid)
 * @param label    what the Operation's rows say it came from
 */
public record UpdateRequest(BlockPos computer, StorageKey key, long quantity, UpdateAction action, int offer,
                            String name, @Nullable StorageKey with, @Nullable NodeUuid from, @Nullable UUID payer,
                            String label) {

    public UpdateRequest {
        Objects.requireNonNull(computer, "computer");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(action, "action");
        name = name == null ? "" : name;
        label = label == null ? "" : label;
    }

    /** The same request from another server's stock, or from anywhere with null. */
    public UpdateRequest from(@Nullable final NodeUuid server) {
        return new UpdateRequest(computer, key, quantity, action, offer, name, with, server, payer, label);
    }
}
