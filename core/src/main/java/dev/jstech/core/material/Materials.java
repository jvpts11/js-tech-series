/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Every material declared, open to any mod. A mod declares its own from its constructor, as a static final, and a
 * material's things exist only for the forms an installed mod says it uses ({@link MaterialUses}):
 *
 * <pre>{@code
 * public static final Material MYTHRIL = Materials.material(rl("mythril"), MaterialKind.METAL, MYTHRIL_NAME)
 *         .colour(0x8FB8D8).with(MaterialProperties.SYMBOL, "My").declare();
 * }</pre>
 */
public final class Materials {

    private static final Map<ResourceLocation, Material> BY_ID = new ConcurrentHashMap<>();
    private static final List<Material> ALL = new CopyOnWriteArrayList<>();
    private static final String SUPERSCRIPT = "⁰¹²³⁴⁵⁶⁷⁸⁹";

    private static volatile @Nullable Map<Item, MaterialThing> providedThings;
    private static volatile int providedCount;

    private Materials() {
    }

    /** A material called {@code id}, of {@code kind}, named {@code name}. */
    public static Material.Builder material(final ResourceLocation id, final MaterialKind kind, final TextKey name) {
        return new Material.Builder(id, kind, name.text(), name);
    }

    /**
     * A material whose name is the same in every language, as a name is: an element beyond the known table under its
     * systematic name. An isotope is named after its element unless it is given a name of its own.
     */
    public static Material.Builder material(final ResourceLocation id, final MaterialKind kind, final Text name) {
        return new Material.Builder(id, kind, name, null);
    }

    /**
     * An isotope of {@code element} of mass number {@code massNumber}, called {@code id}: of its element's kind, colour
     * and atomic number, named after it ("Uranium-235") and written as chemists write it (²³⁵U), unless the builder
     * says otherwise.
     */
    public static Material.Builder isotope(final ResourceLocation id, final Material element, final int massNumber) {
        final Material.Builder builder = new Material.Builder(id, element.kind(), Text.EMPTY, null)
                .isotopeOf(element, massNumber).colour(element.colour());
        element.value(MaterialProperties.ATOMIC_NUMBER).ifPresent(z -> builder.with(MaterialProperties.ATOMIC_NUMBER,
                z));
        element.states().forEach(builder::also);
        return builder;
    }

    /** An isotope with a name of its own, as deuterium and tritium have. */
    public static Material.Builder isotope(final ResourceLocation id, final Material element, final int massNumber,
                                           final TextKey name) {
        final Material.Builder builder = new Material.Builder(id, element.kind(), name.text(), name)
                .isotopeOf(element, massNumber).colour(element.colour());
        element.value(MaterialProperties.ATOMIC_NUMBER).ifPresent(z -> builder.with(MaterialProperties.ATOMIC_NUMBER,
                z));
        element.states().forEach(builder::also);
        return builder;
    }

    /** Every material declared, in the order they were. */
    public static List<Material> all() {
        return List.copyOf(ALL);
    }

    /** The material declared as {@code id}, or none. */
    public static Optional<Material> byId(final ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /**
     * The material and the form {@code item} is a thing of: one the Core registered for a use, or one another mod
     * already had (the game's iron ingot), or none. Asked once the game's registries are filled.
     */
    public static Optional<MaterialThing> of(final Item item) {
        final Optional<MaterialThing> registered = MaterialUses.thingOf(item);
        if (registered.isPresent()) {
            return registered;
        }
        return Optional.ofNullable(provided().get(item));
    }

    /* Built the first time it is asked for, once the game's registries are filled, and again if a material came. */
    private static Map<Item, MaterialThing> provided() {
        final Map<Item, MaterialThing> known = providedThings;
        if (known != null && providedCount == ALL.size()) {
            return known;
        }
        final Map<Item, MaterialThing> built = new HashMap<>();
        for (final Material material : ALL) {
            for (final Form form : material.providedForms()) {
                material.provided(form).ifPresent(thing -> built.putIfAbsent(thing.get().asItem(),
                        new MaterialThing(material, form)));
            }
        }
        providedCount = ALL.size();
        providedThings = Map.copyOf(built);
        return providedThings;
    }

    /** {@code number} in superscript digits, as a mass number is written before a symbol: 235 as ²³⁵. */
    public static String superscript(final int number) {
        final StringBuilder out = new StringBuilder();
        for (final char digit : Integer.toString(number).toCharArray()) {
            out.append(SUPERSCRIPT.charAt(digit - '0'));
        }
        return out.toString();
    }

    static Material declare(final Material material) {
        if (BY_ID.putIfAbsent(material.id(), material) != null) {
            throw DeclarationError.of(material.id().getNamespace(), "the material " + material.id(),
                    "is declared twice", "declare it once, as a static final");
        }
        ALL.add(material);
        return material;
    }
}
