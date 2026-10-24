/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.multiblock;

import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.multiblock.IStructureShape;
import java.util.Objects;
import net.minecraft.world.level.block.Block;

/**
 * What a multiblock of J's Industrial says of itself, over the series' rule that each is built of blocks of its own and
 * its terminal (industrial/docs/design/machines.md): its tier, the material of its time it is built of, and what its
 * size gives. Its blocks are declared through {@link #built}, standing, sounding and broken as its material; its
 * terminal, a machine of the mod with every tab of the standard screen, forms with {@link #structure}, keeping what it
 * holds while the structure stands apart.
 *
 * <pre>{@code
 * IndustrialMultiblock COKE_OVEN = IndustrialMultiblock.of(IndustrialTier.T1, CasingMaterial.BRICK)
 *         .growing(SizeGives.recipesPerSlice(1));
 * BlockEntry<Block> OVEN_BRICKS = COKE_OVEN.built(CONTENT.block("coke_oven_bricks", Block::new))
 *         .named("Coke Oven Bricks").register();
 * // in its terminal's constructor:
 * formsWith(COKE_OVEN.structure(PATTERN));
 * }</pre>
 *
 * @param tier     its tier
 * @param material what it is built of, which must be of its tier's time
 * @param size     what its size gives
 */
public record IndustrialMultiblock(IndustrialTier tier, CasingMaterial material, SizeGives size) {

    public IndustrialMultiblock {
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(size, "size");
        if (!material.fits(tier)) {
            throw new IllegalArgumentException("a multiblock of " + tier + " is built of "
                    + CasingMaterial.of(tier) + ", not of " + material);
        }
    }

    /** A multiblock of {@code tier}, built of {@code material}, whose size gives nothing. */
    public static IndustrialMultiblock of(final IndustrialTier tier, final CasingMaterial material) {
        return new IndustrialMultiblock(tier, material, SizeGives.NOTHING);
    }

    /** The same, its size giving what {@code gives} says. */
    public IndustrialMultiblock growing(final SizeGives gives) {
        return new IndustrialMultiblock(this.tier, this.material, gives);
    }

    /** {@code block}, one of its blocks: standing, sounding and broken as its material, mined by its tool. */
    public <B extends Block> BlockBuilder<B> built(final BlockBuilder<B> block) {
        return block.properties(this.material::apply).tag(this.material.tool());
    }

    /** Its structure, of {@code shape}, running as many recipes at once as its size gives. */
    public MachineStructure structure(final IStructureShape shape) {
        return MachineStructure.of(shape).parallel(this.size::recipesAtOnce);
    }
}
