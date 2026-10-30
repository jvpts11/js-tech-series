/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.engine.CraftRequest;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.jstechseries.operation.IItemNeeds;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * What the Mainframe brings a machine of its network that waits on items for a request it was given, a crane waiting
 * for the blocks of the rocket it builds: once a second while the request waits, the first item the machine needs that
 * the network stores goes out to it, as an export the log shows, one at a time; and each item it needs that the
 * network does not store and can make is crafted into the network's storage, once for each item until that craft
 * settles, for a later look to send on. An item nothing on the network makes, or a network with no engine to plan the
 * craft, is asked about again only after a while. Both go through the network's Operations service, as every request
 * from outside the Operations core does.
 */
final class MemberSupply {

    private final MainframeBlockEntity mainframe;
    private final ServerLevel level;
    private final NetworkUuid network;
    private final String label;
    private final Map<StorageKey, INetworkOperation> crafting = new HashMap<>();
    /* When each item nothing on the network makes was last asked for. */
    private final Map<StorageKey, Long> unmade = new HashMap<>();
    private @Nullable INetworkOperation sending;
    private int wait;

    /* How often it looks at what the machine needs, in ticks. */
    private static final int LOOK_EVERY = 20;
    /* How long before an item nothing made is asked for again, in ticks. */
    private static final long ASK_AGAIN = 200L;

    MemberSupply(final MainframeBlockEntity mainframe, final ServerLevel level, final NetworkUuid network,
                 final String label) {
        this.mainframe = mainframe;
        this.level = level;
        this.network = network;
        this.label = label;
    }

    /** Looks, when it is time to, at what {@code machine} waits for, and brings it what it can. */
    void tick(final IItemNeeds machine) {
        if (--this.wait > 0) {
            return;
        }
        this.wait = LOOK_EVERY;
        if (this.sending != null && !this.sending.isDone()) {
            return;
        }
        this.sending = null;
        final Optional<IItemHandler> inbox = machine.inbox();
        if (inbox.isEmpty()) {
            return;
        }
        this.crafting.values().removeIf(INetworkOperation::isDone);
        final NetworkStorage storage = NetworkStorage.of(this.level, this.network);
        final long now = this.level.getGameTime();
        for (final IItemNeeds.ItemNeed need : machine.needs()) {
            final StorageKey key = StorageKey.of(need.item());
            final long stored = storage.count(key);
            if (stored > 0L && this.sending == null) {
                this.sending = this.mainframe.networkOperations().export(key, Math.min(stored, need.count()),
                        new ExternalDataPort(inbox.get(), null), this.label);
            }
            final long lacking = need.count() - stored;
            final Long asked = this.unmade.get(key);
            if (lacking <= 0L || this.crafting.containsKey(key) || asked != null && now - asked < ASK_AGAIN) {
                continue;
            }
            final INetworkOperation craft = this.mainframe.networkOperations().craft(CraftRequest.of(key, lacking,
                    true, this.label, null));
            if (craft == null) {
                this.unmade.put(key, now);
            } else {
                this.crafting.put(key, craft);
            }
        }
    }
}
