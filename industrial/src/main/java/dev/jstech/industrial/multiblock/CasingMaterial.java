/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.multiblock;

import dev.jstech.core.jstechseries.tier.IndustrialTier;
import java.util.Arrays;
import java.util.List;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * What a multiblock of J's Industrial is built of, a material of its time (industrial/docs/design/machines.md): brick,
 * wood or iron before the T3, steel from the T3, as the plants of each age were. It sets how the multiblock's blocks
 * stand, sound and are broken, and their colour on a map, unless the multiblock paints them.
 */
public enum CasingMaterial {

    /** Fired brick, as coke ovens, kilns and glass furnaces are built. */
    BRICK(MapColor.COLOR_RED, 2.0F, SoundType.STONE, BlockTags.MINEABLE_WITH_PICKAXE),
    /** Timber, as the first mills and engine houses were framed. */
    WOOD(MapColor.WOOD, 2.0F, SoundType.WOOD, BlockTags.MINEABLE_WITH_AXE),
    /** Cast and wrought iron, the metal of the machines before steel. */
    IRON(MapColor.METAL, 3.5F, SoundType.METAL, BlockTags.MINEABLE_WITH_PICKAXE),
    /** Steel plate, every multiblock's from the T3. */
    STEEL(MapColor.COLOR_GRAY, 3.5F, SoundType.METAL, BlockTags.MINEABLE_WITH_PICKAXE);

    private final MapColor colour;
    private final float strength;
    private final SoundType sound;
    private final TagKey<Block> tool;

    /** The tier steel comes in at: every multiblock from it is of steel, and none before it. */
    public static final IndustrialTier STEEL_FROM = IndustrialTier.T3;

    CasingMaterial(final MapColor colour, final float strength, final SoundType sound, final TagKey<Block> tool) {
        this.colour = colour;
        this.strength = strength;
        this.sound = sound;
        this.tool = tool;
    }

    /** The materials a multiblock of {@code tier} may be built of: steel from the T3, the others before it. */
    public static List<CasingMaterial> of(final IndustrialTier tier) {
        return Arrays.stream(values()).filter(material -> material.fits(tier)).toList();
    }

    /** Whether a multiblock of {@code tier} may be built of it. */
    public boolean fits(final IndustrialTier tier) {
        return (this == STEEL) == tier.isAtLeast(STEEL_FROM);
    }

    /** {@code properties} as a block of it stands, sounds and is broken: only with the right tool to keep it. */
    public BlockBehaviour.Properties apply(final BlockBehaviour.Properties properties) {
        return properties.mapColor(this.colour).strength(this.strength).sound(this.sound)
                .requiresCorrectToolForDrops();
    }

    /** The tool that breaks it, as the tag of the blocks that tool mines. */
    public TagKey<Block> tool() {
        return this.tool;
    }

    /** How long it stands being broken, as the game counts a block's hardness. */
    public float strength() {
        return this.strength;
    }

    /** How it sounds. */
    public SoundType sound() {
        return this.sound;
    }
}
