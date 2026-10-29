/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.wreck;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.space.SpaceContent;
import dev.jstech.space.platform.LandingPlatform;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Rocket wreckage: the torn and burnt metal a stage leaves where it strikes the ground, as the first stages of real
 * rockets lie in their fall zones. Broken, it gives back the part of the stage's blocks it holds, about a third of
 * what the stage was made of; the rest is lost in the fall.
 */
public final class RocketWreckage {

    /** The wreckage. */
    public static final BlockEntry<DeviceBlock> BLOCK = SpaceContent.CONTENT.block("rocket_wreckage",
                    Device.of(() -> RocketWreckage.ENTITY.get())::block).named("Rocket Wreckage")
            .properties(properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL))
            .look(IBlockLook.fixed(new IBlockModel.CubeAll("rocket_wreckage", "block/rocket_wreckage")))
            .item().drops(Drops.NONE).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(LandingPlatform.PLATFORMS)
            .register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RocketWreckageBlockEntity>> ENTITY =
            SpaceContent.CONTENT.blockEntity("rocket_wreckage", RocketWreckageBlockEntity::new, BLOCK);

    private RocketWreckage() {
    }

    /** Declares the wreckage, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        // Loading the class declares it.
    }
}
