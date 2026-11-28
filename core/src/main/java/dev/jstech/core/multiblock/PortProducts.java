/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * The products a multiblock's repeated ports are bound to: a bound port takes in, or gives out, its own products
 * alone, so the pipe on it carries one thing (an air separation unit's four gas outlets, its nitrogen, its oxygen, its
 * argon and its krypton); a port may be bound to a few, as a chiller's second inlet takes the cold water or the plain
 * water it is given. The ports of a letter of the pattern are bound in the order the pattern is read:
 * its layers from the bottom, each layer's rows from the first, each row from its first character; a port past the
 * last binding is bound to none.
 *
 * @param byLetter each bound letter's ports' products, in that order
 */
@ApiStatus.Experimental
public record PortProducts(Map<Character, List<Set<ResourceLocation>>> byLetter) {

    /** No port bound: every port takes and gives anything of its kind. */
    public static final PortProducts NONE = new PortProducts(Map.of());

    public PortProducts {
        final Map<Character, List<Set<ResourceLocation>>> copy = new LinkedHashMap<>();
        byLetter.forEach((letter, ports) -> copy.put(letter, ports.stream().map(Set::copyOf).toList()));
        byLetter = Map.copyOf(copy);
    }

    /** These, and the ports of {@code letter} bound one product each to {@code products}, in reading order. */
    public PortProducts with(final char letter, final List<ResourceLocation> products) {
        final List<Set<ResourceLocation>> ports = new ArrayList<>();
        products.forEach(product -> ports.add(Set.of(product)));
        return withPorts(letter, ports);
    }

    /** These, and the ports of {@code letter} bound each to its set of {@code ports}, in reading order. */
    public PortProducts withPorts(final char letter, final List<Set<ResourceLocation>> ports) {
        final Map<Character, List<Set<ResourceLocation>>> more = new LinkedHashMap<>(this.byLetter);
        more.put(letter, ports);
        return new PortProducts(more);
    }

    /** Whether no port is bound. */
    public boolean isEmpty() {
        return this.byLetter.isEmpty();
    }

    /**
     * Where each bound port of {@code formed}, a structure standing as {@code pattern}, is, by its place in the world
     * as {@link PatternMatcher#encodePosition} encodes it, and its products.
     */
    public Map<Long, Set<ResourceLocation>> places(final MultiblockPattern pattern, final ShapeMatch.Formed formed) {
        final Map<Long, Set<ResourceLocation>> out = new HashMap<>();
        this.byLetter.forEach((letter, ports) -> {
            final List<Long> found = inReadingOrder(pattern, formed, letter);
            for (int k = 0; k < Math.min(found.size(), ports.size()); k++) {
                out.put(found.get(k), ports.get(k));
            }
        });
        return Map.copyOf(out);
    }

    /**
     * Where the places of {@code letter} of {@code pattern} stand in the world for {@code formed}, a structure of it,
     * as {@link PatternMatcher#encodePosition} encodes them, in the order the pattern is read.
     */
    public static List<Long> inReadingOrder(final MultiblockPattern pattern, final ShapeMatch.Formed formed,
                                            final char letter) {
        final int[] terminal = PatternMatcher.decodePosition(formed.terminal());
        final List<Long> out = new ArrayList<>();
        for (int y = 0; y < pattern.sizeY(); y++) {
            for (int z = 0; z < pattern.sizeZ(); z++) {
                for (int x = 0; x < pattern.sizeX(); x++) {
                    if (pattern.charAt(x, y, z) != letter) {
                        continue;
                    }
                    final int[] w = formed.orientation().apply(x - pattern.terminalX(), y - pattern.terminalY(),
                            z - pattern.terminalZ());
                    out.add(PatternMatcher.encodePosition(terminal[0] + w[0], terminal[1] + w[1],
                            terminal[2] + w[2]));
                }
            }
        }
        return List.copyOf(out);
    }
}
