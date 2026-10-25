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
import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The physical form factor of a motherboard, the chassis standard that decides which computer a board fits into, the
 * way a real board's size and mounting decide which case accepts it.
 *
 * <p>An id rather than a closed set, as a socket is, so a mod bringing computers of its own brings the boards that
 * fit them. This mod's own are the constants here.
 *
 * @param id    the form factor's id, {@code namespace:path}: {@code jsc:atx}
 * @param label its name, the same in every language: "ATX"
 */
@ApiStatus.Experimental
public record FormFactor(String id, String label) {

    private static final DeclaredKinds<FormFactor> KINDS = new DeclaredKinds<>("form factor", FormFactor::id);

    public static final FormFactor BABY_AT = own("baby_at", "Baby-AT");
    public static final FormFactor AT = own("at", "AT");
    public static final FormFactor ATX = own("atx", "ATX");
    public static final FormFactor EATX = own("eatx", "EATX");
    public static final FormFactor EEB = own("eeb", "EEB");
    public static final FormFactor MTX = own("mtx", "MTX");
    public static final FormFactor SOCKET_Q = own("socket_q", "Socket Q");
    /**
     * Mining motherboard: a specialized board with one consumer socket and many expansion slots, built for mining
     * computers rather than general computing.
     */
    public static final FormFactor MNG = own("mng", "MNG");
    /**
     * Simulation motherboard: a specialized board for simulation-cluster nodes, with no conventional CPU or RAM slots
     * of its own.
     */
    public static final FormFactor SIM = own("sim", "SIM");

    public FormFactor {
        HardwareIds.requireNamespaced(id, "a form factor");
        Objects.requireNonNull(label, "label");
    }

    /**
     * Declares a form factor an add-on brings, while the game loads: what the register event does.
     *
     * @throws DeclarationError when one of the same id is declared already, or the game has loaded
     */
    public static FormFactor declare(final FormFactor formFactor) {
        return KINDS.declare(formFactor);
    }

    /** The form factor of {@code id}, or null when none is declared; a bare name is one of this mod's own. */
    public static @Nullable FormFactor byId(final String id) {
        return KINDS.byId(id);
    }

    /** Every form factor, this mod's own first. */
    public static List<FormFactor> all() {
        return KINDS.all();
    }

    /** Closes the list once the game has loaded; from the mod. */
    static void close() {
        KINDS.close();
    }

    private static FormFactor own(final String path, final String label) {
        return KINDS.declare(new FormFactor(HardwareIds.own(path), label));
    }
}
