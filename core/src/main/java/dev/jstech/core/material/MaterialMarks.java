/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

/**
 * Marks a material's things carry in their tooltips, under its properties, for what a player should know at a glance:
 * a mineral that is naturally radioactive. A mod declares a mark once, with what it says and which materials bear it,
 * asked each time a tooltip is drawn.
 *
 * <pre>{@code
 * MaterialMarks.declare(rl("radioactive"), RADIOACTIVE, material -> containsUranium(material));
 * }</pre>
 */
public final class MaterialMarks {

    private static final List<Mark> MARKS = new CopyOnWriteArrayList<>();

    private MaterialMarks() {
    }

    /**
     * Declares the mark {@code id}, saying {@code text}, borne by the materials {@code bears} lets through.
     *
     * @throws DeclarationError when a mark of that id is declared already
     */
    public static Mark declare(final ResourceLocation id, final TextKey text, final Predicate<Material> bears) {
        final Mark mark = new Mark(id, text, bears);
        for (final Mark before : MARKS) {
            if (before.id().equals(id)) {
                throw DeclarationError.of(id.getNamespace(), "the material mark " + id, "is declared twice",
                        "declare it once, from the mod's constructor");
            }
        }
        MARKS.add(mark);
        return mark;
    }

    /** The marks {@code material} bears now, in the order they were declared. */
    public static List<Mark> of(final Material material) {
        final List<Mark> out = new ArrayList<>();
        for (final Mark mark : MARKS) {
            if (mark.bears().test(material)) {
                out.add(mark);
            }
        }
        return out;
    }

    /**
     * A mark.
     *
     * @param id    its id
     * @param text  what it says: "Naturally radioactive"
     * @param bears which materials bear it
     */
    public record Mark(ResourceLocation id, TextKey text, Predicate<Material> bears) {

        public Mark {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(bears, "bears");
        }
    }
}
