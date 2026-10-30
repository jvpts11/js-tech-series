/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.crawler;

import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.space.SpaceContent;
import java.util.List;
import java.util.function.Function;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The crawler-transporter's kit of J's Space: the tracks of its trucks, the leveling jacks over them, the plates of its
 * deck, its cabs and its engine, each the kit's face until the crawler round the engine is whole, which the engine
 * then draws whole as the approved crawler.
 */
public final class CrawlerBlocks {

    /** Its row of the mod's tab. */
    public static final ContentTab.Section CRAWLER = SpaceContent.TAB.section();
    public static final BlockEntry<CrawlerPieceBlock> CRAWLER_TRACK = piece("crawler_track",
            CrawlerLayout.Kind.TRACK).named("Crawler Track").register();
    public static final BlockEntry<CrawlerPieceBlock> CRAWLER_DECK = piece("crawler_deck",
            CrawlerLayout.Kind.DECK).named("Crawler Deck").register();
    public static final BlockEntry<CrawlerPieceBlock> CRAWLER_CAB = piece("crawler_cab",
            CrawlerLayout.Kind.CAB).named("Crawler Cab").register();
    public static final BlockEntry<CrawlerPieceBlock> LEVELING_JACK = piece("leveling_jack",
            CrawlerLayout.Kind.JACK).named("Leveling Jack").register();
    public static final BlockEntry<CrawlerEngineBlock> CRAWLER_ENGINE = piece("crawler_engine",
            CrawlerEngineBlock::new).named("Crawler Engine").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrawlerEngineBlockEntity>> ENGINE_ENTITY =
            SpaceContent.CONTENT.blockEntity("crawler_engine", CrawlerEngineBlockEntity::new, CRAWLER_ENGINE);

    private CrawlerBlocks() {
    }

    /** Declares the blocks, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every block of the kit, in its order. */
    public static List<BlockEntry<? extends Block>> all() {
        return List.of(CRAWLER_TRACK, CRAWLER_DECK, CRAWLER_CAB, LEVELING_JACK, CRAWLER_ENGINE);
    }

    private static BlockBuilder<CrawlerPieceBlock> piece(final String id, final CrawlerLayout.Kind kind) {
        return piece(id, properties -> new CrawlerPieceBlock(properties, kind));
    }

    /* A piece made by {@code made}: the kit's face on all six sides, nothing of its own once formed. */
    private static <B extends Block> BlockBuilder<B> piece(final String id,
                                                           final Function<BlockBehaviour.Properties, B> made) {
        final IBlockModel face = new IBlockModel.CubeAll(id, "block/crawler/" + id);
        final IBlockModel drawn = new IBlockModel.ParticleOnly(id + "_drawn", "block/crawler/" + id);
        return SpaceContent.CONTENT.block(id, made).properties(properties -> properties.mapColor(MapColor.METAL)
                        .strength(3.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL))
                .look(IBlockLook.byState(state -> state.getValue(CrawlerPieceBlock.FORMED) ? drawn : face))
                .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(CRAWLER);
    }
}
