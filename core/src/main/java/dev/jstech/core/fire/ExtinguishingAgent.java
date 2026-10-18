/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fire;

import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.util.StringRepresentable;

/**
 * What puts a fire out, and what it does on each class of fire, as the real ones do: water cools wood but carries
 * current, spreads burning oil and makes a burning metal burst; foam smothers wood and liquids; carbon dioxide puts out
 * liquids and live equipment and leaves wood to smoulder; dry powder puts out nearly anything, metals included, but not
 * a pan of oil, which only foam smothers here.
 */
public enum ExtinguishingAgent implements StringRepresentable {

    /** Water, cooling what burns. */
    WATER(FireTexts.WATER),
    /** Foam, smothering a fire and the liquid under it. */
    FOAM(FireTexts.FOAM),
    /** Carbon dioxide, taking the oxygen away and leaving nothing behind. */
    CARBON_DIOXIDE(FireTexts.CARBON_DIOXIDE),
    /** Dry powder, smothering, metals included. */
    DRY_POWDER(FireTexts.DRY_POWDER);

    private final TextKey name;

    ExtinguishingAgent(final TextKey name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Its name, as shown. */
    public TextKey title() {
        return this.name;
    }

    /** What it does on a fire of the class {@code fire}. */
    public Outcome on(final FireClass fire) {
        return switch (this) {
            case WATER -> switch (fire) {
                case SOLIDS -> Outcome.PUTS_OUT;
                case LIQUIDS, COOKING_OILS -> Outcome.FLARES;
                case ELECTRICAL -> Outcome.SHOCKS;
                case METALS -> Outcome.REACTS;
            };
            case FOAM -> switch (fire) {
                case SOLIDS, LIQUIDS, COOKING_OILS -> Outcome.PUTS_OUT;
                case ELECTRICAL -> Outcome.SHOCKS;
                case METALS -> Outcome.REACTS;
            };
            case CARBON_DIOXIDE -> switch (fire) {
                case LIQUIDS, ELECTRICAL -> Outcome.PUTS_OUT;
                case SOLIDS -> Outcome.REKINDLES;
                case METALS, COOKING_OILS -> Outcome.NOTHING;
            };
            case DRY_POWDER -> switch (fire) {
                case SOLIDS, LIQUIDS, ELECTRICAL, METALS -> Outcome.PUTS_OUT;
                case COOKING_OILS -> Outcome.REKINDLES;
            };
        };
    }

    /** The classes of fire it puts out for good, in their order. */
    public List<FireClass> putsOut() {
        return Stream.of(FireClass.values()).filter(fire -> on(fire) == Outcome.PUTS_OUT).toList();
    }

    /** What an agent does on a fire. */
    public enum Outcome {
        /** It puts the fire out. */
        PUTS_OUT,
        /** It puts the fire out for a while, and it may light again from what still smoulders. */
        REKINDLES,
        /** It does nothing to the fire. */
        NOTHING,
        /** It carries the current to whoever sprays it, and the fire burns on. */
        SHOCKS,
        /** It spreads the burning liquid: the fire flares up round it. */
        FLARES,
        /** The burning metal bursts. */
        REACTS
    }
}
