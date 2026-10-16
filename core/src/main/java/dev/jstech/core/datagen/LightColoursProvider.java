/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.JsCore;
import dev.jstech.core.light.LightColours;
import dev.jstech.core.light.LightMath;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.DataMapProvider;

/**
 * The colours of the light the game's own blocks give, where it is not the warm white of fire: the soul flames' blue,
 * redstone's red, the sea lantern's and the beacon's pale cyan, amethyst's and crying obsidian's violet, the
 * froglights' yellow, green and purple, the glow lichen's green; the light of the game's mobs that glow; and the light
 * of what glows held, a bucket of lava, a blaze rod. A block it says nothing of gives white light, which tints
 * nothing.
 */
public final class LightColoursProvider extends DataMapProvider {

    public LightColoursProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    public String getName() {
        return JsCore.MODID + ":light_colours";
    }

    @Override
    protected void gather() {
        final Builder<Integer, Block> blocks = builder(LightColours.BLOCKS);
        block(blocks, Blocks.SOUL_TORCH, 0x50C8FF);
        block(blocks, Blocks.SOUL_WALL_TORCH, 0x50C8FF);
        block(blocks, Blocks.SOUL_LANTERN, 0x50C8FF);
        block(blocks, Blocks.SOUL_CAMPFIRE, 0x50C8FF);
        block(blocks, Blocks.SOUL_FIRE, 0x50C8FF);
        block(blocks, Blocks.REDSTONE_TORCH, 0xFF3018);
        block(blocks, Blocks.REDSTONE_WALL_TORCH, 0xFF3018);
        block(blocks, Blocks.REDSTONE_ORE, 0xFF3018);
        block(blocks, Blocks.DEEPSLATE_REDSTONE_ORE, 0xFF3018);
        block(blocks, Blocks.SEA_LANTERN, 0xB8F0FF);
        block(blocks, Blocks.BEACON, 0xB0F8FF);
        block(blocks, Blocks.CONDUIT, 0xB0F8FF);
        block(blocks, Blocks.AMETHYST_CLUSTER, 0xC080FF);
        block(blocks, Blocks.LARGE_AMETHYST_BUD, 0xC080FF);
        block(blocks, Blocks.MEDIUM_AMETHYST_BUD, 0xC080FF);
        block(blocks, Blocks.SMALL_AMETHYST_BUD, 0xC080FF);
        block(blocks, Blocks.CRYING_OBSIDIAN, 0xA040FF);
        block(blocks, Blocks.RESPAWN_ANCHOR, 0xA040FF);
        block(blocks, Blocks.NETHER_PORTAL, 0xB040FF);
        block(blocks, Blocks.END_ROD, 0xF0E8FF);
        block(blocks, Blocks.END_GATEWAY, 0x60FFE0);
        block(blocks, Blocks.OCHRE_FROGLIGHT, 0xFFE080);
        block(blocks, Blocks.VERDANT_FROGLIGHT, 0xB0FF90);
        block(blocks, Blocks.PEARLESCENT_FROGLIGHT, 0xF0B0FF);
        block(blocks, Blocks.GLOW_LICHEN, 0xA0FFC8);
        block(blocks, Blocks.SCULK_CATALYST, 0x40FFE0);
        block(blocks, Blocks.ENCHANTING_TABLE, 0xC0A0FF);
        final Builder<LightColours.Glow, EntityType<?>> entities = builder(LightColours.ENTITIES);
        entity(entities, EntityType.BLAZE, new LightColours.Glow(10, 0xFFA040));
        entity(entities, EntityType.MAGMA_CUBE, new LightColours.Glow(8, 0xFF6020));
        entity(entities, EntityType.GLOW_SQUID, new LightColours.Glow(6, 0x60FFE0));
        entity(entities, EntityType.ALLAY, new LightColours.Glow(5, 0x80D0FF));
        entity(entities, EntityType.GLOW_ITEM_FRAME, new LightColours.Glow(4, 0xA0FFC8));
        // What gives light held besides the blocks that give it, which give their own.
        final Builder<LightColours.Glow, Item> items = builder(LightColours.HELD);
        item(items, Items.LAVA_BUCKET, new LightColours.Glow(15, 0xFF8030));
        item(items, Items.BLAZE_ROD, new LightColours.Glow(10, 0xFFA040));
        item(items, Items.BLAZE_POWDER, new LightColours.Glow(8, 0xFFA040));
        item(items, Items.FIRE_CHARGE, new LightColours.Glow(8, 0xFF8030));
        item(items, Items.MAGMA_CREAM, new LightColours.Glow(6, 0xFF6020));
        item(items, Items.GLOWSTONE_DUST, new LightColours.Glow(8, LightMath.WHITE));
        item(items, Items.GLOW_INK_SAC, new LightColours.Glow(6, 0xA0FFC8));
        item(items, Items.GLOW_BERRIES, new LightColours.Glow(8, 0xFFC060));
        item(items, Items.PRISMARINE_CRYSTALS, new LightColours.Glow(6, 0xB8F0FF));
        item(items, Items.NETHER_STAR, new LightColours.Glow(12, LightMath.WHITE));
    }

    private static void item(final Builder<LightColours.Glow, Item> map, final Item item,
                             final LightColours.Glow glow) {
        map.add(BuiltInRegistries.ITEM.wrapAsHolder(item), glow, false);
    }

    private static void block(final Builder<Integer, Block> map, final Block block, final int colour) {
        map.add(BuiltInRegistries.BLOCK.wrapAsHolder(block), colour, false);
    }

    private static void entity(final Builder<LightColours.Glow, EntityType<?>> map, final EntityType<?> type,
                               final LightColours.Glow glow) {
        map.add(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type), glow, false);
    }
}
