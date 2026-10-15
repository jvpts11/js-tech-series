/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.JsCore;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The elements beyond the known table, 119 to 250: the series' bet that the island of stability goes on, stable
 * superheavy elements like any other of the catalogue. Each is known by its systematic name ({@code jscore:ununennium})
 * and shows under it until it is discovered; then the name its discoverer gives it, or, in a world where discoverers
 * do not name them, the series' own name for the seventeen that have one, after scientists, places and ideas.
 */
@TextHolder
public final class Superheavies {

    /** The first and the last element beyond the known table. */
    public static final int FIRST = 119;
    public static final int LAST = 250;

    public static final TextKey GENEVIUM = TextKey.of("jscore.material.genevium", "Genevium");
    public static final TextKey TESLIUM = TextKey.of("jscore.material.teslium", "Teslium");
    public static final TextKey KELVINIUM = TextKey.of("jscore.material.kelvinium", "Kelvinium");
    public static final TextKey DIRACIUM = TextKey.of("jscore.material.diracium", "Diracium");
    public static final TextKey LEHMANNIUM = TextKey.of("jscore.material.lehmannium", "Lehmannium");
    public static final TextKey BOSIUM = TextKey.of("jscore.material.bosium", "Bosium");
    public static final TextKey PLANCKIUM = TextKey.of("jscore.material.planckium", "Planckium");
    public static final TextKey BARDEENIUM = TextKey.of("jscore.material.bardeenium", "Bardeenium");
    public static final TextKey HAWKINGIUM = TextKey.of("jscore.material.hawkingium", "Hawkingium");
    public static final TextKey BOLTZMANNIUM = TextKey.of("jscore.material.boltzmannium", "Boltzmannium");
    public static final TextKey ZWICKIUM = TextKey.of("jscore.material.zwickium", "Zwickium");
    public static final TextKey MAIMANIUM = TextKey.of("jscore.material.maimanium", "Maimanium");
    public static final TextKey ILLAENIUM = TextKey.of("jscore.material.illaenium", "Illaenium");
    public static final TextKey CANTORIUM = TextKey.of("jscore.material.cantorium", "Cantorium");
    public static final TextKey NEWTONIUM = TextKey.of("jscore.material.newtonium", "Newtonium");
    public static final TextKey TURINGIUM = TextKey.of("jscore.material.turingium", "Turingium");
    public static final TextKey AETERNIUM = TextKey.of("jscore.material.aeternium", "Aeternium");

    /* The series' names and symbols, the symbols repeating none of the known table's. */
    private static final Map<Integer, SeriesName> SERIES_NAMES = Map.ofEntries(
            Map.entry(119, new SeriesName(GENEVIUM, "Gv")), Map.entry(120, new SeriesName(TESLIUM, "Tu")),
            Map.entry(121, new SeriesName(KELVINIUM, "Ke")), Map.entry(122, new SeriesName(DIRACIUM, "Di")),
            Map.entry(123, new SeriesName(LEHMANNIUM, "Le")), Map.entry(135, new SeriesName(BOSIUM, "Bo")),
            Map.entry(145, new SeriesName(PLANCKIUM, "Pl")), Map.entry(148, new SeriesName(BARDEENIUM, "Bd")),
            Map.entry(152, new SeriesName(HAWKINGIUM, "Hw")), Map.entry(156, new SeriesName(BOLTZMANNIUM, "Bz")),
            Map.entry(160, new SeriesName(ZWICKIUM, "Zw")), Map.entry(165, new SeriesName(MAIMANIUM, "Ma")),
            Map.entry(170, new SeriesName(ILLAENIUM, "Il")), Map.entry(180, new SeriesName(CANTORIUM, "Ct")),
            Map.entry(200, new SeriesName(NEWTONIUM, "Nw")), Map.entry(210, new SeriesName(TURINGIUM, "Tr")),
            Map.entry(250, new SeriesName(AETERNIUM, "Ae")));
    /* Grey, as the heavy metals of the known table are drawn, until a mod gives one a look of its own. */
    private static final int COLOUR = 0xA8AAAC;

    private static final Map<Integer, Material> BY_NUMBER = new LinkedHashMap<>();

    static {
        for (int number = FIRST; number <= LAST; number++) {
            BY_NUMBER.put(number, declare(number));
        }
    }

    private Superheavies() {
    }

    /** Declares the elements, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /**
     * The element of atomic number {@code number}, from {@link #FIRST} to {@link #LAST}.
     *
     * @throws IllegalArgumentException for a number outside them
     */
    public static Material of(final int number) {
        final Material material = BY_NUMBER.get(number);
        if (material == null) {
            throw new IllegalArgumentException("there is no element " + number + " beyond the known table");
        }
        return material;
    }

    /** Every element beyond the known table, by atomic number. */
    public static Map<Integer, Material> all() {
        return Collections.unmodifiableMap(BY_NUMBER);
    }

    /** The atomic number of {@code material}, when it is an element beyond the known table. */
    public static Optional<Integer> number(final Material material) {
        for (final Map.Entry<Integer, Material> entry : BY_NUMBER.entrySet()) {
            if (entry.getValue() == material) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /** The series' own name for the element {@code number}, used where discoverers do not name them, or none. */
    public static Optional<TextKey> seriesName(final int number) {
        return Optional.ofNullable(SERIES_NAMES.get(number)).map(SeriesName::name);
    }

    /** The series' own symbol for the element {@code number}, or none. */
    public static Optional<String> seriesSymbol(final int number) {
        return Optional.ofNullable(SERIES_NAMES.get(number)).map(SeriesName::symbol);
    }

    private static Material declare(final int number) {
        return Materials.material(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, SystematicNames.name(number)),
                        MaterialKind.METAL, Text.literal(SystematicNames.title(number)))
                .colour(COLOUR).also(MatterState.LIQUID)
                .with(MaterialProperties.SYMBOL, SystematicNames.symbol(number))
                .with(MaterialProperties.ATOMIC_NUMBER, number)
                .nameable().namedBy(ElementNaming.NAMER).declare();
    }

    /* The series' name and symbol for an element. */
    private record SeriesName(TextKey name, String symbol) {
    }
}
