/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tier;

import dev.jstech.core.progression.IAxisStep;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Objects;

/**
 * An era of hardware, the generation of the computers a player builds: the series' seven, from Vintage to Singularity
 * ({@link HardwareEra}), and any an add-on brings, after the last or between two. An era is saved by its id; where it
 * stands in the order is the registry's ({@link Eras}), and every rule that asks for "this era or later" follows that
 * order, so an era an add-on puts between two moves no other.
 *
 * <p>This is pure domain logic with no Minecraft type in it, so it stays unit-testable; the codecs that save and send
 * an era live in {@link EraCodecs}. Every era a mod holds is the one the registry declared, so two eras are the same
 * era exactly when they are the same object.
 *
 * <pre>{@code
 * public static final Era QUANTUM = Eras.declareAfter(
 *         new Era("myaddon:quantum", MyTexts.QUANTUM.text(), 0xFF00FF, 2034), HardwareEra.ADVANCED);
 * }</pre>
 *
 * @param id     the era's id, {@code namespace:path}, saved in worlds: {@code jscore:legacy}
 * @param name   its name as a player reads it: "Legacy"
 * @param colour the colour its screens are remembered by, as RGB, which a part's tooltip writes its era in so the era
 *               reads before the word does
 * @param year   the year its machines were built, which a firmware banner and an installer's banner print
 */
@TextHolder
public record Era(String id, Text name, int colour, int year) implements IAxisStep {

    /** An era named as one: "Legacy era", with the name put where each language puts it. */
    private static final TextKey NAMED = TextKey.of("jscore.era.named", "%s era");

    public Era {
        Objects.requireNonNull(name, "name");
        Eras.requireId(id);
    }

    /** The mod the era belongs to: {@code jscore} for the series' seven. */
    public String namespace() {
        return id.substring(0, id.indexOf(':'));
    }

    /** The era's id without its mod: {@code legacy}, which the series' own file names are built from. */
    public String path() {
        return id.substring(id.indexOf(':') + 1);
    }

    /** Where the era stands in the order, from 0 for the earliest. */
    @Override
    public int level() {
        return Eras.position(this);
    }

    /** The era's id, which saves and data files name it by. */
    @Override
    public String serializedName() {
        return id;
    }

    /** The era's name: "Legacy". */
    @Override
    public Text text() {
        return name;
    }

    /** The era as an era: "Legacy era". */
    @Override
    public Text named() {
        return NAMED.with(name);
    }

    @Override
    public int colour() {
        return colour;
    }

    /** Its id, or, for one of the series' seven, its bare name too: a data file's {@code "legacy"}. */
    @Override
    public boolean answersTo(final String saved) {
        return id.equals(saved) || Eras.SERIES_NAMESPACE.equals(namespace()) && path().equals(saved);
    }

    /** Whether this era is {@code other} or later. */
    public boolean isAtLeast(final Era other) {
        return level() >= other.level();
    }

    /** Whether this era is {@code other} or earlier. */
    public boolean isAtMost(final Era other) {
        return level() <= other.level();
    }

    /** The era after this one; the last era is its own next. */
    public Era next() {
        return Eras.at(level() + 1);
    }

    /** The era before this one; the first era is its own previous. */
    public Era prev() {
        return Eras.at(level() - 1);
    }

    /** Eras are declared once each, so an era is equal only to itself. */
    @Override
    public boolean equals(final Object other) {
        return this == other;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
