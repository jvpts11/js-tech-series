/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A pattern named by its id ({@link MultiblockPatterns#get}), read each time it is checked, so the pattern a data pack
 * puts in force is the one used, whichever file loaded first. A pattern that is in no file and not declared never
 * forms, and its failure names it.
 *
 * @param id the pattern's id
 */
public record PatternById(ResourceLocation id) implements IStructureShape {

    /** What a failure says is wanted when the pattern is nowhere; what it found is the pattern's id. */
    public static final String MISSING = "pattern";

    public PatternById {
        Objects.requireNonNull(id, "id");
    }

    @Override
    public ShapeMatch check(final IBlockProvider provider, final long controller) {
        return MultiblockPatterns.get(this.id).<ShapeMatch>map(pattern -> pattern.check(provider, controller))
                .orElseGet(() -> new ShapeMatch.Failure(ShapeMatch.Wrong.BLOCK, controller, 0, 0, 0, MISSING,
                        this.id.toString()));
    }

    @Override
    public int reach() {
        return MultiblockPatterns.get(this.id).map(MultiblockPattern::reach).orElse(1);
    }

    @Override
    public Map<String, Limit> limits() {
        return MultiblockPatterns.get(this.id).map(MultiblockPattern::limits).orElse(Map.of());
    }

    @Override
    public Optional<ShapePreview> preview(final int grow) {
        return MultiblockPatterns.get(this.id).flatMap(pattern -> pattern.preview(grow));
    }

    @Override
    public @Nullable IBlockMatcher wanted(final String wanted) {
        return MultiblockPatterns.get(this.id).map(pattern -> pattern.wanted(wanted)).orElse(null);
    }

    @Override
    public boolean mirrors() {
        return MultiblockPatterns.get(this.id).map(MultiblockPattern::mirrors).orElse(false);
    }

    @Override
    public boolean liesDown() {
        return MultiblockPatterns.get(this.id).map(MultiblockPattern::liesDown).orElse(false);
    }
}
