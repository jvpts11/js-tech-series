/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.platform;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.space.JsSpace;
import dev.jstech.space.SpaceContent;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The landing platform a rocket's stage comes back to, as Falcon 9's first stage lands on its landing zone: a square
 * slab of landing pads with its terminal in the middle, painted with the target it lands on. It is built three times as
 * wide as the stages it takes, so their legs spread on it: nine blocks for the stages of three, fifteen for those of
 * five. Built on a moving structure afloat, a raft or a barge, it is a floating platform, as SpaceX's drone ships are.
 * A flight computer sets a stage to land on one; it holds one stage at a time.
 */
public final class LandingPlatform {

    /** Its row of the mod's tab. */
    public static final ContentTab.Section PLATFORMS = SpaceContent.TAB.section();
    /** A landing pad, the concrete the platform is laid of. */
    public static final BlockEntry<Block> PAD = SpaceContent.CONTENT.block("landing_pad", Block::new)
            .named("Landing Pad")
            .properties(properties -> properties.mapColor(MapColor.STONE).strength(2.5F, 9.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE))
            .look(IBlockLook.fixed(new IBlockModel.BottomTop("landing_pad", "block/landing_pad_side",
                    "block/landing_pad_side", "block/landing_pad", null)))
            .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(PLATFORMS).register();
    /** The platform's terminal, in the middle of its pads, where the stage's engine comes down. */
    public static final BlockEntry<DeviceBlock> TERMINAL = SpaceContent.CONTENT.block("landing_platform",
                    Device.of(() -> LandingPlatform.ENTITY.get()).ticks(MachineBlockEntity::serverTick)
                            .opensMenu(MachineMenu::new)::block).named("Landing Platform")
            .properties(properties -> properties.mapColor(MapColor.STONE).strength(3.0F, 9.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL))
            .look(IBlockLook.fixed(new IBlockModel.BottomTop("landing_platform", "block/landing_platform/side",
                    "block/landing_pad_side", "block/landing_platform/top", null)))
            .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(PLATFORMS).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LandingPlatformBlockEntity>> ENTITY =
            SpaceContent.CONTENT.blockEntity("landing_platform", LandingPlatformBlockEntity::new, TERMINAL);
    /** The two sizes, the stages of three and of five. */
    public static final ResourceLocation SMALL_ID = id("landing_platform_small");
    public static final ResourceLocation LARGE_ID = id("landing_platform_large");
    public static final MultiblockPattern SMALL = MultiblockPatterns.declare(SMALL_ID, slab("landing_platform_small",
            9));
    public static final MultiblockPattern LARGE = MultiblockPatterns.declare(LARGE_ID, slab("landing_platform_large",
            15));
    /** Its shape: the larger slab that stands whole. */
    public static final LandingPlatformShape SHAPE = new LandingPlatformShape(List.of(LARGE_ID, SMALL_ID));

    private LandingPlatform() {
    }

    /** Declares the platform, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        // Loading the class declares it.
    }

    /* A square slab of pads {@code side} wide, its terminal in the middle. */
    private static MultiblockPattern slab(final String name, final int side) {
        final String[] rows = new String[side];
        final String pads = "P".repeat(side);
        for (int row = 0; row < side; row++) {
            rows[row] = row == side / 2 ? "P".repeat(side / 2) + "#" + "P".repeat(side / 2) : pads;
        }
        return MultiblockPattern.builder(name).layer(rows)
                .where('P', BlockMatch.blocks(PAD.getId().toString()))
                .terminal(TERMINAL.getId().toString())
                .build();
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
