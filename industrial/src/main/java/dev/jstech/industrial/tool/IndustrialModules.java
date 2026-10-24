/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.tool;

import dev.jstech.core.jstechseries.SeriesModules;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.module.IModuleEffect;
import dev.jstech.core.module.ModuleDef;
import dev.jstech.core.module.ModuleHostDef;
import dev.jstech.core.module.Modules;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * How J's Industrial's tools and armour take modules (industrial/docs/design/tools-and-armour.md), on J's Core's module
 * kit: physical modules that go in and come out, never the enchanting table ({@link NoEnchanting}). A tool or a piece
 * of armour has its tier's sockets, two up to the T3, three at the T4, four at the T5 and T6 and five from the T7, all
 * of the series' armour or tool kind ({@link SeriesModules}); a piece of armour says which piece it is, and a closed
 * exo-suit that it is sealed, so a module that needs a seal (J's Space's space module) fits only the closed suits.
 *
 * <pre>{@code
 * IndustrialModules.armour(EXO_CHEST.getId(), IndustrialTier.T5, SeriesModules.CHEST, true);
 * IndustrialModules.tool(DRILL.getId(), IndustrialTier.T2);
 * Modules.declare(OVERCLOCKED_MOTOR.getId(), IndustrialModules.module(SeriesModules.TOOL, 20, effects).upTo(4));
 * }</pre>
 */
public final class IndustrialModules {

    /** The sockets of a tool or armour up to the T3, at the T4, at the T5 and T6, and from the T7. */
    public static final int EARLY_SOCKETS = 2;
    public static final int T4_SOCKETS = 3;
    public static final int T5_SOCKETS = 4;
    public static final int LATE_SOCKETS = 5;

    private IndustrialModules() {
    }

    /** How many module sockets a tool or a piece of armour of {@code tier} has. */
    public static int sockets(final IndustrialTier tier) {
        if (tier.isAtMost(IndustrialTier.T3)) {
            return EARLY_SOCKETS;
        }
        if (tier.isAtMost(IndustrialTier.T4)) {
            return T4_SOCKETS;
        }
        return tier.isAtMost(IndustrialTier.T6) ? T5_SOCKETS : LATE_SOCKETS;
    }

    /**
     * Declares the piece of armour {@code item} of {@code tier} a host of its tier's sockets: {@code piece}, one of the
     * series' pieces, and sealed when it closes its wearer in.
     */
    public static ModuleHostDef armour(final ResourceLocation item, final IndustrialTier tier, final String piece,
                                       final boolean sealed) {
        if (!SeriesModules.pieces().contains(Objects.requireNonNull(piece, "piece"))) {
            throw new IllegalArgumentException("a piece of armour is one of " + SeriesModules.pieces() + ", not "
                    + piece);
        }
        final List<String> traits = new ArrayList<>(List.of(piece));
        if (sealed) {
            traits.add(SeriesModules.SEALED);
        }
        return Modules.declareHost(item, ModuleHostDef.worn(List.of(new ModuleHostDef.Socket(SeriesModules.ARMOUR,
                sockets(tier)))).with(traits.toArray(String[]::new)));
    }

    /** Declares the tool {@code item} of {@code tier} a host of its tier's sockets, working while it is held. */
    public static ModuleHostDef tool(final ResourceLocation item, final IndustrialTier tier) {
        return Modules.declareHost(item, ModuleHostDef.tool(List.of(new ModuleHostDef.Socket(SeriesModules.TOOL,
                sockets(tier)))));
    }

    /**
     * A module of {@code kind}, the series' armour or tool, spending {@code energy} while it works and doing
     * {@code effects}, to declare with {@link Modules#declare} once said how many one host takes and what it needs.
     */
    public static ModuleDef module(final String kind, final int energy, final List<IModuleEffect> effects) {
        if (!SeriesModules.ARMOUR.equals(kind) && !SeriesModules.TOOL.equals(kind)) {
            throw new IllegalArgumentException("a module of J's Industrial goes in armour or a tool, not " + kind);
        }
        return new ModuleDef(kind, energy, effects);
    }
}
