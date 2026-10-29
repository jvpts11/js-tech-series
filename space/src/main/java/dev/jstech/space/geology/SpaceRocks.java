/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.geology;

import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.space.SpaceContent;
import java.util.function.Function;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * The ground of the Moon and of Mars: their regoliths and sands, loose and falling as sand does; their rocks and the
 * water ice of Mars' caps, broken with a pickaxe; and the frost and the icy regolith. Martian Basalt and Mudstone
 * come in three blocks each, put where geology puts them: the basalt faceted below ground and in the highlands, in
 * columns on the scarps of lava plateaus and the walls of canyons, pocked by gas bubbles on the tops of lava flows; the
 * mudstone in the grey of Gale's lakebeds, in the buff of Jezero's delta, and dusted red in the upper layers of every
 * exposure.
 */
public final class SpaceRocks {

    /** The tab's row of the worlds' ground. */
    public static final ContentTab.Section GROUND = SpaceContent.TAB.section();

    /** The pale dust of the Moon's highlands. */
    public static final BlockEntry<ColoredFallingBlock> HIGHLAND_REGOLITH = loose("highland_regolith", 0x959188,
            MapColor.COLOR_LIGHT_GRAY).named("Highland Regolith").register();
    /** The dark dust of the Moon's maria. */
    public static final BlockEntry<ColoredFallingBlock> MARE_REGOLITH = loose("mare_regolith", 0x605F61,
            MapColor.COLOR_GRAY).named("Mare Regolith").register();
    /** The pale rock of the Moon's highlands, almost all plagioclase. */
    public static final BlockEntry<Block> ANORTHOSITE = rock("anorthosite", MapColor.QUARTZ, false)
            .named("Anorthosite").register();
    /** The dark lava that filled the Moon's maria. */
    public static final BlockEntry<Block> MARE_BASALT = rock("mare_basalt", MapColor.COLOR_BLACK, false)
            .named("Mare Basalt").register();
    /** The regolith of the craters of eternal shadow, held hard by its ice. */
    public static final BlockEntry<Block> ICY_REGOLITH = frozen("icy_regolith", MapColor.COLOR_LIGHT_GRAY,
            SoundType.GRAVEL).named("Icy Regolith").register();
    /** The iron-oxide dust that covers Mars. */
    public static final BlockEntry<ColoredFallingBlock> MARTIAN_REGOLITH = loose("martian_regolith", 0xA45230,
            MapColor.COLOR_ORANGE).named("Martian Regolith").register();
    /** The ground of Mars' oxide plains, with hematite's grey spherules. */
    public static final BlockEntry<ColoredFallingBlock> HEMATITE_REGOLITH = loose("hematite_regolith", 0xA25232,
            MapColor.TERRACOTTA_RED).named("Hematite Regolith").register();
    /** The dark sand of Mars' dunes. */
    public static final BlockEntry<ColoredFallingBlock> BASALT_SAND = loose("basalt_sand", 0x3D3634,
            MapColor.COLOR_BLACK).named("Basalt Sand").register();
    /** The water ice of Mars' polar caps, with dust between its layers, frozen as hard as rock in their cold. */
    public static final BlockEntry<WaterIceBlock> WATER_ICE = rock("water_ice", WaterIceBlock::new, MapColor.ICE,
            false).properties(properties -> properties.sound(SoundType.GLASS).randomTicks()).named("Water Ice")
            .register();
    /** The frost of carbon dioxide over Mars' polar caps. */
    public static final BlockEntry<DryIceBlock> DRY_ICE = frozen("dry_ice", DryIceBlock::new, MapColor.SNOW,
            SoundType.SNOW).properties(BlockBehaviour.Properties::randomTicks).named("Dry Ice").register();
    /** Martian Basalt broken in fine facets: the bedrock of the highlands and the rock below ground. */
    public static final BlockEntry<Block> MARTIAN_BASALT = rock("martian_basalt", MapColor.TERRACOTTA_BLACK, true)
            .named("Martian Basalt").register();
    /** Martian Basalt in columns: the scarps of lava plateaus, Tharsis' flows, the walls of Valles Marineris. */
    public static final BlockEntry<Block> COLUMNAR_MARTIAN_BASALT = rock("columnar_martian_basalt",
            MapColor.TERRACOTTA_BLACK, true).named("Columnar Martian Basalt").register();
    /** Martian Basalt pocked by the holes gas bubbles left: the tops of lava flows, the plains' loose stones. */
    public static final BlockEntry<Block> VESICULAR_MARTIAN_BASALT = rock("vesicular_martian_basalt",
            MapColor.TERRACOTTA_BLACK, true).named("Vesicular Martian Basalt").register();
    /** Mudstone in the grey of Gale's lakebeds, laid in thin layers. */
    public static final BlockEntry<Block> MUDSTONE = rock("mudstone", MapColor.COLOR_GRAY, true)
            .named("Mudstone").register();
    /** Mudstone in the buff of Jezero's delta. */
    public static final BlockEntry<Block> BUFF_MUDSTONE = rock("buff_mudstone", MapColor.TERRACOTTA_YELLOW, true)
            .named("Buff Mudstone").register();
    /** Mudstone with the red dust of Mars caught in its partings: the upper layers of every exposure. */
    public static final BlockEntry<Block> DUSTED_MUDSTONE = rock("dusted_mudstone", MapColor.TERRACOTTA_BROWN, true)
            .named("Dusted Mudstone").register();

    /* A rock's strength against breaking and against blasts, as stone's; loose ground's, as sand's. */
    private static final float ROCK = 1.5F;
    private static final float ROCK_BLAST = 6.0F;
    private static final float LOOSE = 0.5F;
    private static final float ICE = 0.5F;

    private SpaceRocks() {
    }

    /** Declares the worlds' ground, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        // Loading the class declares every block above.
    }

    /* Loose ground that falls as sand does, its falling dust {@code dust}, as RGB; dug with a shovel. */
    private static BlockBuilder<ColoredFallingBlock> loose(final String id, final int dust, final MapColor colour) {
        return SpaceContent.CONTENT.block(id, properties -> new ColoredFallingBlock(new ColorRGBA(dust), properties))
                .properties(properties -> properties.mapColor(colour).strength(LOOSE).sound(SoundType.SAND))
                .look(IBlockLook.fixed(new IBlockModel.CubeAll(id, "block/" + id)))
                .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_SHOVEL).tab(GROUND);
    }

    /* Rock, broken with a pickaxe to drop itself; with a top of its own when {@code ownTop}. */
    private static BlockBuilder<Block> rock(final String id, final MapColor colour, final boolean ownTop) {
        return rock(id, Block::new, colour, ownTop);
    }

    private static <B extends Block> BlockBuilder<B> rock(final String id,
                                                          final Function<BlockBehaviour.Properties, B> made,
                                                          final MapColor colour, final boolean ownTop) {
        final IBlockModel model = ownTop ? new IBlockModel.Column(id, "block/" + id, "block/" + id + "_top")
                : new IBlockModel.CubeAll(id, "block/" + id);
        return SpaceContent.CONTENT.block(id, made)
                .properties(properties -> properties.mapColor(colour).strength(ROCK, ROCK_BLAST)
                        .requiresCorrectToolForDrops().sound(SoundType.STONE))
                .look(IBlockLook.fixed(model))
                .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(GROUND);
    }

    /* Frozen ground, broken with anything; how it melts or sublimates is its block's own. */
    private static BlockBuilder<Block> frozen(final String id, final MapColor colour, final SoundType sound) {
        return frozen(id, Block::new, colour, sound);
    }

    private static <B extends Block> BlockBuilder<B> frozen(final String id,
                                                            final Function<BlockBehaviour.Properties, B> made,
                                                            final MapColor colour, final SoundType sound) {
        return SpaceContent.CONTENT.block(id, made)
                .properties(properties -> properties.mapColor(colour).strength(ICE).sound(sound))
                .look(IBlockLook.fixed(new IBlockModel.CubeAll(id, "block/" + id)))
                .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(GROUND);
    }
}
