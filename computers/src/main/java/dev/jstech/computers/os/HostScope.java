/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.hardware.DeclaredKinds;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Where a program is allowed to exist: the kind of machine a computer is, which every computer's entry names. A
 * program gated to a machine only installs on, and only shows a launcher on, a computer of that machine; {@link #ANY}
 * places no restriction. This single field replaces the scattered per-program install and launcher special cases
 * (the Crafting Manager only on a Crafting Computer, the Network Manager and the Mainframe services only on a
 * Mainframe), and every gate asks it the same question, {@link #admits}.
 *
 * <p>An id rather than a closed set, as a form factor is, so a mod bringing a computer of its own brings the machine
 * its programs are made for. This mod's own are the constants here.
 *
 * @param id      the machine's id, {@code namespace:path}: {@code jsc:mainframe}
 * @param name    the machine, as a sentence names it: "a Mainframe"; an install disc's file says it in English
 * @param refusal what the package manager says of a program of it asked onto another machine, the program's command
 *                in place of its argument; null for {@link #ANY}, which refuses nothing
 */
@TextHolder
@ApiStatus.Experimental
public record HostScope(String id, TextKey name, @Nullable TextKey refusal) {

    private static final TextKey ANY_COMPUTER = TextKey.of("jsc.host_scope.any", "any computer");
    private static final TextKey A_MAINFRAME = TextKey.of("jsc.install.setup_gate.on_a_mainframe", "a Mainframe");
    private static final TextKey A_CRAFTING_COMPUTER =
            TextKey.of("jsc.install.setup_gate.on_a_crafting_computer", "a Crafting Computer");
    private static final TextKey A_SERVER = TextKey.of("jsc.install.setup_gate.on_a_server", "a server in a rack");
    private static final TextKey A_CLUSTER_MANAGER =
            TextKey.of("jsc.install.setup_gate.on_a_cluster_manager", "a Cluster Management Computer");
    private static final TextKey A_PERSONAL_COMPUTER =
            TextKey.of("jsc.install.setup_gate.on_a_personal_computer", "a Personal Computer");
    private static final TextKey MAINFRAME_ONLY =
            TextKey.of("jsc.service.gates.mainframe_only", "%s only installs on the Mainframe");
    private static final TextKey SERVER_ONLY =
            TextKey.of("jsc.service.gates.server_only", "%s only installs on a server in a rack");
    private static final TextKey CLUSTER_ONLY =
            TextKey.of("jsc.service.gates.cluster_only", "%s only installs on a Cluster Management Computer");
    private static final TextKey PERSONAL_ONLY =
            TextKey.of("jsc.service.gates.personal_only", "%s only installs on a Personal Computer");
    private static final TextKey CRAFTING_ONLY =
            TextKey.of("jsc.service.gates.crafting_only", "%s only installs on a Crafting Computer");
    private static final DeclaredKinds<HostScope> KINDS = new DeclaredKinds<>("machine", HostScope::id);

    /** No host restriction: the program may exist on any computer that meets its platform and specs. */
    public static final HostScope ANY = own("any", ANY_COMPUTER, null);
    /** Only on a Crafting Computer (the recipe-store host). */
    public static final HostScope CRAFTING_COMPUTER = own("crafting_computer", A_CRAFTING_COMPUTER, CRAFTING_ONLY);
    /** Only on the Mainframe (the network's orchestrator, holder of the network index). */
    public static final HostScope MAINFRAME = own("mainframe", A_MAINFRAME, MAINFRAME_ONLY);
    /** Only on a server mounted in a rack, where the headless server services live. */
    public static final HostScope SERVER = own("server", A_SERVER, SERVER_ONLY);
    /** Only on a Cluster Management Computer (the master of the racks on its network). */
    public static final HostScope CLUSTER_MANAGEMENT_COMPUTER =
            own("cluster_management_computer", A_CLUSTER_MANAGER, CLUSTER_ONLY);
    /** Only on a Personal Computer, the player's own machine (where the personal-use cards are worked). */
    public static final HostScope PERSONAL_COMPUTER = own("personal_computer", A_PERSONAL_COMPUTER, PERSONAL_ONLY);

    public HostScope {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }

    /**
     * Declares a machine an add-on brings, while the game loads: what the register event does. Every machine but
     * {@link #ANY} refuses the programs made for it elsewhere, so it says how.
     *
     * @throws DeclarationError when one of the same id is declared already, or the game has loaded
     */
    public static HostScope declare(final HostScope machine) {
        Objects.requireNonNull(machine.refusal(), "refusal");
        return KINDS.declare(machine);
    }

    /** The machine of {@code id}, or null when none is declared; a bare name is one of this mod's own. */
    public static @Nullable HostScope byId(final String id) {
        return KINDS.byId(id);
    }

    /** Every machine, this mod's own first. */
    public static List<HostScope> all() {
        return KINDS.all();
    }

    /** Closes the list once the game has loaded; from the mod. */
    @ApiStatus.Internal
    public static void close() {
        KINDS.close();
    }

    /** Whether a program made for this machine may exist on a computer that is {@code machine}. */
    public boolean admits(final HostScope machine) {
        return this == ANY || this == machine;
    }

    private static HostScope own(final String path, final TextKey name, final @Nullable TextKey refusal) {
        return KINDS.declare(new HostScope(JsComputers.MODID + ":" + path, name, refusal));
    }
}
