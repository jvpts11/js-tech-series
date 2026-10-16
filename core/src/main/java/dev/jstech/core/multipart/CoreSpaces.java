/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.registry.CoreItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The Core's block spaces of parts alone: the block a space is when no cable runs through it, its entity, and the
 * microblock, one item for every shape, thickness and material, which the shape and the block it is cut from ride on.
 */
public final class CoreSpaces {

    /**
     * The block a space of parts is. It has no item: a part placed where nothing stands makes it. What it hides of its
     * neighbours is in its state ({@link SpaceBlock#COVERED}), so it occludes only there.
     */
    public static final BlockEntry<SpaceBlock> BLOCK = CoreItems.CONTENT.block("multipart", SpaceBlock::new)
            .properties(properties -> properties.mapColor(MapColor.STONE).strength(0.5F).sound(SoundType.STONE)
                    .dynamicShape())
            .named("Parts")
            .look(IBlockLook.fixed(new IBlockModel.ParticleOnly("multipart", "block/cable/housing")))
            .drops(Drops.NONE)
            .register();
    /** Its entity, which keeps its parts. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpaceBlockEntity>> BLOCK_ENTITY =
            CoreItems.CONTENT.blockEntity("multipart", SpaceBlockEntity::new, BLOCK);
    /** The shape, thickness and material a microblock's item carries. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Microblock.MicroblockPiece>> PIECE =
            CoreItems.CONTENT.component("microblock", Microblock.MicroblockPiece.CODEC,
                    ByteBufCodecs.fromCodec(Microblock.MicroblockPiece.CODEC));
    /** A microblock: what the saw cuts, placed in a block space. Bare, it is a stone cover. */
    public static final ItemEntry<MicroblockItem> MICROBLOCK = CoreItems.CONTENT.item("microblock",
                    MicroblockItem::new).named("Microblock").look(IItemLook.HANDMADE)
            .component(PIECE, new Microblock.MicroblockPiece(MicroShape.FACE, MicroShape.COVER, Blocks.STONE))
            .register();
    /** The tools that cut microblocks in the crafting grid: any mod's saw joins it. */
    public static final TagKey<Item> SAWS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "saws"));

    private CoreSpaces() {
    }

    /** Declares the space's block, entity and item, and the microblocks' recipes, from the Core's constructor. */
    public static void declare() {
        MicroblockRecipes.declare();
    }

    /** Hands the mod's bus what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        // A space answers on each face with what the device mounted there offers, as a cable does.
        modBus.addListener(RegisterCapabilitiesEvent.class,
                event -> PartCapabilities.register(event, BLOCK_ENTITY.get(), SpaceBlockEntity::parts));
        SpaceBreaking.listen();
    }

    /** A microblock's item of {@code piece}. */
    public static ItemStack stack(final Microblock.MicroblockPiece piece) {
        final ItemStack stack = new ItemStack(MICROBLOCK.get());
        stack.set(PIECE.get(), piece);
        return stack;
    }
}
