/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.text.Text;
import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A generation of memory, which a module is made in and a motherboard takes. Each carries the access latency the
 * virtual-thread staging gate models: older DRAM types have a noticeable staging delay before data is available to
 * the network, while DDR5 and later generations are fast enough that the delay is zero at the tick's grain.
 *
 * <p>An id rather than a closed set, as a socket is, so a mod bringing an era of its own brings its memory too. This
 * mod's own, in chronological order, are the constants here.
 *
 * @param id           the generation's id, {@code namespace:path}: {@code jsc:ddr4}
 * @param label        the memory as a part's sheet names it, the same in every language: "DDR4"
 * @param latencyTicks the ticks a virtual thread parks before the staging gate opens; zero means no delay
 */
@ApiStatus.Experimental
public record RamGeneration(String id, Text label, int latencyTicks) {

    private static final DeclaredKinds<RamGeneration> KINDS = new DeclaredKinds<>("memory generation",
            RamGeneration::id);

    public static final RamGeneration SIMM = own("simm", "SIMM", 5);
    public static final RamGeneration EDO = own("edo", "EDO", 4);
    public static final RamGeneration SDRAM = own("sdram", "SDRAM", 3);
    public static final RamGeneration DDR = own("ddr", "DDR", 2);
    public static final RamGeneration DDR2 = own("ddr2", "DDR2", 2);
    public static final RamGeneration DDR3 = own("ddr3", "DDR3", 1);
    public static final RamGeneration DDR4 = own("ddr4", "DDR4", 1);
    public static final RamGeneration DDR5 = own("ddr5", "DDR5", 0);
    public static final RamGeneration DDR6 = own("ddr6", "DDR6", 0);
    public static final RamGeneration HBM = own("hbm", "HBM", 0);

    public RamGeneration {
        HardwareIds.requireNamespaced(id, "a memory generation");
        Objects.requireNonNull(label, "label");
        if (latencyTicks < 0) {
            throw new IllegalArgumentException("latencyTicks must be >= 0; got " + latencyTicks);
        }
    }

    /**
     * Declares a generation an add-on brings, while the game loads: what the register event does.
     *
     * @throws DeclarationError when one of the same id is declared already, or the game has loaded
     */
    public static RamGeneration declare(final RamGeneration generation) {
        return KINDS.declare(generation);
    }

    /** The generation of {@code id}, or null when none is declared; a bare name is one of this mod's own. */
    public static @Nullable RamGeneration byId(final String id) {
        return KINDS.byId(id);
    }

    /** Every generation, this mod's own first, in their order. */
    public static List<RamGeneration> all() {
        return KINDS.all();
    }

    /** Closes the list once the game has loaded; from the mod. */
    static void close() {
        KINDS.close();
    }

    private static RamGeneration own(final String path, final String label, final int latencyTicks) {
        return KINDS.declare(new RamGeneration(HardwareIds.own(path), Text.literal(label), latencyTicks));
    }
}
