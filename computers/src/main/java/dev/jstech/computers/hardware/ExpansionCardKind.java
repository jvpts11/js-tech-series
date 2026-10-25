/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.util.DeclarationError;
import java.util.List;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The kind of an expansion card, so a build can tell what an installed card <em>does</em> even though every card
 * shares the same slot.
 *
 * <p>An id rather than a closed set, so a mod brings a card of a kind of its own (a fieldbus card, a satellite card)
 * and finds it in a build by that kind. This mod's own are the constants here.
 *
 * @param id the kind's id, {@code namespace:path}: {@code jsc:gpu}
 */
@ApiStatus.Experimental
public record ExpansionCardKind(String id) {

    private static final DeclaredKinds<ExpansionCardKind> KINDS = new DeclaredKinds<>("kind of expansion card",
            ExpansionCardKind::id);

    public static final ExpansionCardKind GPU = own("gpu");
    public static final ExpansionCardKind CRAFTING = own("crafting");
    public static final ExpansionCardKind PHI = own("phi");
    /** A cluster interface: what lets a Cluster Management Computer address racks over the network. */
    public static final ExpansionCardKind CLUSTER_INTERFACE = own("cluster_interface");
    /** A sound card: what a Vintage or Legacy computer plays its sound through, beyond the beeps of its case. */
    public static final ExpansionCardKind SOUND = own("sound");
    /** A network adapter: the Optical Network Card, what lets a Mainframe or a cluster manager take the fibre. */
    public static final ExpansionCardKind NETWORK = own("network");
    /** A personal-use card: the crafting table, furnace, enchanting table or anvil the Workshop program works. */
    public static final ExpansionCardKind WORKSHOP = own("workshop");
    /** A RAID controller, the Vaultis Array: joins the drives of the server it sits in into one volume. */
    public static final ExpansionCardKind RAID_CONTROLLER = own("raid_controller");
    /** A read cache, the Vaultis Cache: answers the reads of the drives of the server it sits in faster. */
    public static final ExpansionCardKind READ_CACHE = own("read_cache");

    public ExpansionCardKind {
        HardwareIds.requireNamespaced(id, "a kind of expansion card");
    }

    /**
     * Declares a kind an add-on brings, while the game loads: what the register event does.
     *
     * @throws DeclarationError when one of the same id is declared already, or the game has loaded
     */
    public static ExpansionCardKind declare(final ExpansionCardKind kind) {
        return KINDS.declare(kind);
    }

    /** The kind of {@code id}, or null when none is declared; a bare name is one of this mod's own. */
    public static @Nullable ExpansionCardKind byId(final String id) {
        return KINDS.byId(id);
    }

    /** Every kind, this mod's own first. */
    public static List<ExpansionCardKind> all() {
        return KINDS.all();
    }

    /** Closes the list once the game has loaded; from the mod. */
    static void close() {
        KINDS.close();
    }

    private static ExpansionCardKind own(final String path) {
        return KINDS.declare(new ExpansionCardKind(HardwareIds.own(path)));
    }
}
