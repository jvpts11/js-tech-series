/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.block.DataLinkNames;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.wan.FederationRequestPayload;
import dev.jstech.computers.operation.payload.wan.FederationStatePayload;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.wan.WanRight;
import dev.jstech.core.jstechseries.network.DataLink;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.ShortId;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Network Interactor's Federated tab (computers/docs/design/network.md): a network this one is federated with, in a
 * view of its own that is never mixed with this network's. It keeps what the server last said of the federation, lists
 * what the network shown holds (and what it can craft and holds none of, to craft there), and takes from it, sends to
 * it and has it craft through Operations that cross. The window draws it; this says what to draw and does what is
 * asked.
 */
@TextHolder
public final class InteractorFederation {

    private final BlockPos host;
    private final BlockPos monitorPos;
    @Nullable
    private FederationStatePayload state;
    /* The network shown, by its id; empty for the nearest. */
    private String shown = "";
    /* What the grid lists: the network's items, then what it can craft and holds none of. A new list on each answer. */
    private List<NetworkItemEntry> entries = List.of();
    private final Set<StorageKey> craftable = new HashSet<>();
    private long askedAt = Long.MIN_VALUE;

    public static final TextKey CAPTION = TextKey.of("jsc.interactor.federated.caption", "%s (FEDERATED)");
    public static final TextKey CAPTION_NONE = TextKey.of("jsc.interactor.federated.caption_none",
            "FEDERATED NETWORKS");
    public static final TextKey COUNT = TextKey.of("jsc.interactor.federated.count", "%s types · %s");
    public static final TextKey NONE = TextKey.of("jsc.interactor.federated.none",
            "No network is federated with this one. Link it to another through a WAN Gateway Computer: with a"
                    + " Mainframe on each side, the two federate.");
    public static final TextKey NOT_SEEN = TextKey.of("jsc.interactor.federated.not_seen",
            "%s does not let this network see its items.");
    public static final TextKey HOLDS_NOTHING = TextKey.of("jsc.interactor.federated.holds_nothing",
            "%s holds nothing.");
    public static final TextKey NETWORK_ID = TextKey.of("jsc.interactor.federated.network_id", "network %s");
    public static final TextKey OVER_PORT = TextKey.of("jsc.interactor.federated.over_port",
            "+ federated over port %s");
    public static final TextKey THROUGH = TextKey.of("jsc.interactor.federated.through",
            "+ reached over port %s, %s lines away");
    public static final TextKey LINE = TextKey.of("jsc.interactor.federated.line", "WAN Gateway, %s, %s it/t");
    public static final TextKey MAY = TextKey.of("jsc.interactor.federated.may", "THIS NETWORK MAY");
    public static final TextKey SEE = TextKey.of("jsc.interactor.federated.see", "See items");
    public static final TextKey TAKE = TextKey.of("jsc.interactor.federated.take", "Take items");
    public static final TextKey SEND = TextKey.of("jsc.interactor.federated.send", "Send items");
    public static final TextKey CRAFT = TextKey.of("jsc.interactor.federated.craft", "Craft there");
    public static final TextKey YES = TextKey.of("jsc.interactor.federated.yes", "yes");
    public static final TextKey NO = TextKey.of("jsc.interactor.federated.no", "no");
    public static final TextKey ON_THAT = TextKey.of("jsc.interactor.federated.on_that", "ON THAT NETWORK");
    public static final TextKey CROSSING = TextKey.of("jsc.interactor.federated.crossing", "CROSSING");
    public static final TextKey HINT = TextKey.of("jsc.interactor.federated.hint", "Select an item to ask for it");
    public static final TextKey NEXT = TextKey.of("jsc.interactor.federated.next", "Next network");
    public static final TextKey FEDERATED = TextKey.of("jsc.interactor.federated.status", "%s federated");
    public static final TextKey OF_NETWORKS = TextKey.of("jsc.interactor.federated.of_networks",
            "%s of %s networks");
    /* How often the tab asks again: a federation's numbers change no faster than its lines do. */
    private static final long ASK_MS = 1_000L;

    /* The tab the server's answers are handed to: the open Interactor's. */
    @Nullable
    private static InteractorFederation active;

    InteractorFederation(final BlockPos host, final BlockPos monitorPos) {
        this.host = host;
        this.monitorPos = monitorPos;
    }

    /** Hands the server's answer to the open Interactor's Federated tab. */
    public static void accept(final FederationStatePayload payload) {
        final InteractorFederation tab = active;
        if (tab == null || !tab.host.equals(payload.hostPos())) {
            return;
        }
        tab.state = payload;
        tab.shown = payload.shown();
        tab.craftable.clear();
        tab.craftable.addAll(payload.craftable());
        tab.entries = payload.entries();
    }

    /** The line out to a federated network, as its card says it: by its name and how fast the way is. */
    public static String lineOf(final FederationStatePayload.Peer peer) {
        final DataLink link = DataLink.byName(peer.line());
        final String name = link == null ? peer.line() : GameText.resolve(DataLinkNames.of(link));
        return GameText.resolve(LINE.with(name, peer.speed()));
    }

    /** How a federated network is reached, as its card's green line says it. */
    public static String reachOf(final FederationStatePayload.Peer peer) {
        return GameText.resolve(peer.hops() > 1 ? THROUGH.with(peer.port(), peer.hops()) : OVER_PORT.with(peer.port()));
    }

    /** What the card calls doing {@code right} on a federated network. */
    public static TextKey rightLabel(final WanRight right) {
        return switch (right) {
            case SEE -> SEE;
            case TAKE -> TAKE;
            case SEND -> SEND;
            case CRAFT -> CRAFT;
        };
    }

    /** A federated network's id, as its card's header says it. */
    public static String idOf(final FederationStatePayload.Peer peer) {
        return GameText.resolve(NETWORK_ID.with(ShortId.of(peer.network())));
    }

    /** This tab takes the answers: its window is the open one. */
    void markActive() {
        active = this;
    }

    /** Asks the server how the federation stands, at most once a second unless {@code now}. */
    void ask(final boolean now) {
        final long at = Util.getMillis();
        if (now || at - askedAt >= ASK_MS) {
            askedAt = at;
            PacketDistributor.sendToServer(FederationRequestPayload.ask(host, monitorPos, shown));
        }
    }

    /** What the grid lists. */
    List<NetworkItemEntry> entries() {
        return entries;
    }

    /** The network shown, or null when this one is federated with none. */
    @Nullable
    FederationStatePayload.Peer peer() {
        final FederationStatePayload now = state;
        return now == null ? null : now.shownPeer();
    }

    /** How many networks this one is federated with. */
    int peerCount() {
        return state == null ? 0 : state.peers().size();
    }

    /** Which of them is shown, counted from one. */
    int peerIndex() {
        return state == null ? 0 : state.shownIndex();
    }

    /** Shows the next network this one is federated with. */
    void next() {
        final FederationStatePayload now = state;
        if (now != null && now.peers().size() > 1) {
            shown = now.nextNetwork();
            ask(true);
        }
    }

    /** The caption over the grid: whose items these are. */
    String caption() {
        final FederationStatePayload.Peer peer = peer();
        return GameText.resolve(peer == null ? CAPTION_NONE.text()
                : CAPTION.with(peer.name().toUpperCase(Locale.ROOT)));
    }

    /** The caption's count: how many types the network shown holds, and how much. */
    String count() {
        final FederationStatePayload.Peer peer = peer();
        return peer == null ? "" : GameText.resolve(COUNT.with(peer.types(), DiskSpec.sizeLabel(peer.usedMb())));
    }

    /** What the empty grid says, or null when it lists something. */
    @Nullable
    String emptyMessage() {
        final FederationStatePayload.Peer peer = peer();
        if (peer == null) {
            return GameText.resolve(NONE);
        }
        if (!entries.isEmpty()) {
            return null;
        }
        return GameText.resolve((peer.allows(WanRight.SEE) ? HOLDS_NOTHING : NOT_SEEN).with(peer.name()));
    }

    /** Whether the network shown lets this one do {@code right}. */
    boolean allows(final WanRight right) {
        final FederationStatePayload.Peer peer = peer();
        return peer != null && peer.allows(right);
    }

    /** Whether the network shown can craft {@code key} for this one. */
    boolean canCraft(final StorageKey key) {
        return allows(WanRight.CRAFT) && craftable.contains(key);
    }

    /** Takes {@code amount} of {@code key} from the network shown into this computer. */
    void take(final StorageKey key, final long amount) {
        act(FederationRequestPayload.TAKE, key, amount);
    }

    /** Has the network shown craft {@code amount} of {@code key}, which stays there. */
    void craft(final StorageKey key, final long amount) {
        act(FederationRequestPayload.CRAFT, key, amount);
    }

    /** Sends the stack on the cursor to the network shown. */
    void sendCursor() {
        if (peer() != null) {
            PacketDistributor.sendToServer(new FederationRequestPayload(host, monitorPos, shown,
                    FederationRequestPayload.SEND, Optional.empty(), 0L));
        }
    }

    private void act(final byte action, final StorageKey key, final long amount) {
        if (peer() != null && amount > 0L) {
            PacketDistributor.sendToServer(new FederationRequestPayload(host, monitorPos, shown, action,
                    Optional.of(key), amount));
        }
    }
}
