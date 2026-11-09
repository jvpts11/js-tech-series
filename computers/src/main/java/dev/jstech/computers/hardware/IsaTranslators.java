/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.util.DeclarationError;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The translators this mod brings, and the place a mod adds one of its own. A system names the ones it carries; a
 * translator nobody's system carries does nothing.
 *
 * <p>Quince's two carry its programs across its two switches, as Rosetta did (computers/docs/design/quince.md), named
 * after the man who read the Rosetta Stone: Champollion runs the PowerPC programs on the Integra machines at half their
 * speed, and Champollion 2 the x86-64 programs on Quince's own chips at four fifths of it. Both speeds are estimates.
 */
public final class IsaTranslators {

    /** Runs the PowerPC programs on an x86-64 processor, at half their speed. */
    public static final IsaTranslator CHAMPOLLION = own("champollion", "Champollion", Isas.POWERPC, Isas.X86_64, 50);

    /** Runs the x86-64 programs on an ARM64 processor, at four fifths of their speed. */
    public static final IsaTranslator CHAMPOLLION_2 = own("champollion_2", "Champollion 2", Isas.X86_64, Isas.ARM64,
            80);

    private static final Map<String, IsaTranslator> KNOWN = new LinkedHashMap<>();

    static {
        add(CHAMPOLLION);
        add(CHAMPOLLION_2);
    }

    private IsaTranslators() {
    }

    /** Adds a translator. An id that is already taken is refused rather than quietly replacing the other. */
    public static void add(final IsaTranslator translator) {
        final IsaTranslator taken = KNOWN.putIfAbsent(translator.id(), translator);
        if (taken != null && !taken.equals(translator)) {
            throw DeclarationError.of(null, "the translator " + translator.id(),
                    "takes an id another translator already has", "give it an id of its own");
        }
    }

    /** The translator of that id, or nothing when no mod has brought one under it. */
    public static Optional<IsaTranslator> byId(final String id) {
        return Optional.ofNullable(KNOWN.get(id));
    }

    /** Every translator there is, this mod's first. */
    public static List<IsaTranslator> all() {
        return List.copyOf(KNOWN.values());
    }

    private static IsaTranslator own(final String path, final String name, final IsaSpec from, final IsaSpec on,
                                     final int speedPercent) {
        return new IsaTranslator(HardwareIds.own(path), name, from.id(), on.id(), speedPercent);
    }
}
