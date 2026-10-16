/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.projector;

import dev.jstech.core.JsCore;
import dev.jstech.core.call.MenuCall;
import dev.jstech.core.call.MenuCalls;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.registry.CoreItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The Structure Projector, a block of J's Core's shared content: it shows any multiblock of any installed mod in the
 * world before it is built, the blocks still missing as ghosts, what is wrong marked in red, and how much is in place
 * above it. Its screen chooses the multiblock, its turn, its mirror, up to which layer it shows, whether only what is
 * missing shows, and whether it shows at all.
 */
public final class StructureProjector {

    /** The projector. */
    public static final BlockEntry<StructureProjectorBlock> BLOCK = CoreItems.CONTENT.block("structure_projector",
                    StructureProjectorBlock::new).named("Structure Projector")
            .properties(properties -> properties.strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> 7))
            .look(IBlockLook.fixed(new IBlockModel.Handmade("structure_projector"))).item()
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StructureProjectorBlockEntity>> ENTITY =
            CoreItems.CONTENT.blockEntity("structure_projector", StructureProjectorBlockEntity::new, BLOCK);
    /** Its screen's menu. */
    public static final DeferredHolder<MenuType<?>, MenuType<StructureProjectorMenu>> MENU =
            CoreItems.CONTENT.menu("structure_projector", StructureProjectorMenu::new);
    /** What its screen sends the projector: what to show and how. */
    public static final MenuCall<StructureProjectorMenu, ProjectorSettings> SETTINGS = MenuCalls.declare(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "projector_settings"), StructureProjectorMenu.class,
            ProjectorSettings.STREAM_CODEC, (menu, player, settings) -> menu.projector().apply(settings));

    static {
        // Every mod with multiblocks shows it; the Core does too, so a world with the Core alone can try it.
        SharedUses.declare(JsCore.MODID, BLOCK::get);
    }

    private StructureProjector() {
    }

    /** Declares the content, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }
}
