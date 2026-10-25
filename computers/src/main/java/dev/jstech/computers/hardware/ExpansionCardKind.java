/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.os.HostScope;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The kind of an expansion card, so a build can tell what an installed card <em>does</em> even though every card
 * shares the same slot; and, for a card made for one machine alone, the machines it goes in.
 *
 * <p>An id rather than a closed set, so a mod brings a card of a kind of its own (a fieldbus card, a satellite card)
 * and finds it in a build by that kind. This mod's own are the constants here. A kind made for its machine says so
 * once, where it is declared, and every computer's slots keep its cards out but that machine's:
 *
 * <pre>{@code
 * public static final ExpansionCardKind FIELDBUS = new ExpansionCardKind("myaddon:fieldbus")
 *         .onlyIn(MyTexts.FIELDBUS_ONLY, MyMachines.CONTROLLER);
 * }</pre>
 *
 * @param id       the kind's id, {@code namespace:path}: {@code jsc:gpu}
 * @param machines the machines its cards go in alone; none for a kind any computer seats
 * @param refusal  what the slot says of a card of it held over another machine's; null when it goes in any
 */
@ApiStatus.Experimental
@TextHolder
public record ExpansionCardKind(String id, List<HostScope> machines, @Nullable TextKey refusal) {

    private static final DeclaredKinds<ExpansionCardKind> KINDS = new DeclaredKinds<>("kind of expansion card",
            ExpansionCardKind::id);
    private static final TextKey CLUSTER_ONLY = TextKey.of("jsc.part_refusal.cluster_card",
            "Only a Cluster Management Computer seats a cluster card");

    public static final ExpansionCardKind GPU = own("gpu");
    public static final ExpansionCardKind CRAFTING = own("crafting");
    public static final ExpansionCardKind PHI = own("phi");
    /** A cluster interface: what lets a Cluster Management Computer address racks over the network, and it alone. */
    public static final ExpansionCardKind CLUSTER_INTERFACE = KINDS.declare(new ExpansionCardKind(
            HardwareIds.own("cluster_interface")).onlyIn(CLUSTER_ONLY, HostScope.CLUSTER_MANAGEMENT_COMPUTER));
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
        machines = List.copyOf(machines);
        if (!machines.isEmpty()) {
            Objects.requireNonNull(refusal, "a kind made for its machines says what a slot of another says");
        }
    }

    /** A kind any computer seats. */
    public ExpansionCardKind(final String id) {
        this(id, List.of(), null);
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

    /**
     * This kind, made for {@code only} alone: no other computer's slot takes its cards, and the slot says
     * {@code refusal} of one held over it.
     */
    public ExpansionCardKind onlyIn(final TextKey refusal, final HostScope... only) {
        if (only.length == 0) {
            throw new IllegalArgumentException("a kind made for its machines names at least one: " + this.id);
        }
        return new ExpansionCardKind(this.id, List.of(only), Objects.requireNonNull(refusal, "refusal"));
    }

    /** Whether a card of this kind goes in a computer that is {@code machine}. */
    public boolean fitsIn(final HostScope machine) {
        return this.machines.isEmpty() || this.machines.contains(machine);
    }

    /** Closes the list once the game has loaded; from the mod. */
    static void close() {
        KINDS.close();
    }

    private static ExpansionCardKind own(final String path) {
        return KINDS.declare(new ExpansionCardKind(HardwareIds.own(path)));
    }
}
