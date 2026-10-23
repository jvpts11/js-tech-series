/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * What a mineral is, the part of a material that comes out of the ground (chalcopyrite, not copper): what it contains,
 * its main element first, the one it is mined for, and then the byproducts it gives, each in a fixed share of its
 * mass; the family whose chemistry decides how it is processed; and the host rocks its veins can cross, an ore of it
 * in each. How much of each a machine gets out is the processing's to say.
 *
 * @param family   its family: a sulfide, an oxide
 * @param contents what it contains, its main element first
 * @param rocks    the rocks its veins can cross
 */
public record Mineral(MineralFamily family, List<Content> contents, Set<HostRock> rocks) {

    public Mineral {
        Objects.requireNonNull(family, "family");
        contents = List.copyOf(contents);
        rocks = Set.copyOf(rocks);
    }

    /** Its main element: what it is mined for. */
    public Content main() {
        return this.contents.getFirst();
    }

    /** The byproducts it gives, after its main element. */
    public List<Content> byproducts() {
        return this.contents.subList(1, this.contents.size());
    }

    /** Whether it contains {@code material}, as its main element or a byproduct. */
    public boolean contains(final Material material) {
        for (final Content content : this.contents) {
            if (content.material() == material) {
                return true;
            }
        }
        return false;
    }

    /**
     * One thing a mineral contains.
     *
     * @param material the element or compound
     * @param share    its share of the mineral's mass, more than nothing and at most the whole
     */
    public record Content(Material material, double share) {

        public Content {
            Objects.requireNonNull(material, "material");
        }
    }
}
