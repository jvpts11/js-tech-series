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
import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.space.JsSpace;
import dev.jstech.space.SpaceContent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
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
    /** The whole crawler as one multiblock, which the Structure Projector shows and the Multiblock Builder builds. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "crawler_transporter");
    public static final MultiblockPattern PATTERN = MultiblockPatterns.declare(ID, pattern());

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

    /*
     * The crawler drawn from its layout, its length along x, its tracks' layer on the ground and its engine the
     * terminal: the engine finds the same layout it is built from, so the two cannot part. It is not mirrored, as its
     * cabs stand at opposite corners.
     */
    private static MultiblockPattern pattern() {
        final Map<BlockPos, Character> letters = new HashMap<>();
        for (final CrawlerLayout.Spot spot : CrawlerLayout.spots(BlockPos.ZERO, Direction.EAST)) {
            letters.put(spot.at(), letter(spot.kind()));
        }
        final BoundingBox box = BoundingBox.encapsulatingPositions(letters.keySet()).orElseThrow();
        final MultiblockPattern.Builder builder = MultiblockPattern.builder(ID.getPath());
        for (int y = box.minY(); y <= box.maxY(); y++) {
            final String[] rows = new String[box.getZSpan()];
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                final StringBuilder row = new StringBuilder(box.getXSpan());
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    row.append(letters.getOrDefault(new BlockPos(x, y, z), MultiblockPattern.IGNORE_CHAR));
                }
                rows[z - box.minZ()] = row.toString();
            }
            builder.layer(rows);
        }
        return builder.where(letter(CrawlerLayout.Kind.TRACK), BlockMatch.blocks(CRAWLER_TRACK.getId().toString()))
                .where(letter(CrawlerLayout.Kind.JACK), BlockMatch.blocks(LEVELING_JACK.getId().toString()))
                .where(letter(CrawlerLayout.Kind.DECK), BlockMatch.blocks(CRAWLER_DECK.getId().toString()))
                .where(letter(CrawlerLayout.Kind.CAB), BlockMatch.blocks(CRAWLER_CAB.getId().toString()))
                .terminal(CRAWLER_ENGINE.getId().toString())
                .build();
    }

    /* The letter a piece of {@code kind} is drawn with in the crawler's pattern. */
    private static char letter(final CrawlerLayout.Kind kind) {
        return switch (kind) {
            case TRACK -> 'T';
            case JACK -> 'J';
            case DECK -> 'D';
            case CAB -> 'C';
            case ENGINE -> MultiblockPattern.TERMINAL_CHAR;
        };
    }
}
