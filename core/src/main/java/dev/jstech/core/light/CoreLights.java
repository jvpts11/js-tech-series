/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.registry.CoreItems;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;

/**
 * The Core's own part of the lighting kit: the light that stands in the air ({@link LightBlock}), the data maps of
 * light's colours ({@link LightColours}), the kinds of light ({@link LightKinds}), and the spotlights' light on the
 * world ({@link BeamLights}).
 */
public final class CoreLights {

    /** Light standing in the air. */
    public static final BlockEntry<LightBlock> LIGHT = CoreItems.CONTENT.block("light", LightBlock::new)
            .properties(properties -> properties.mapColor(MapColor.NONE).replaceable().noCollission().noOcclusion()
                    .strength(-1.0F, 3_600_000.8F).pushReaction(PushReaction.DESTROY))
            .named("Light")
            .look(IBlockLook.fixed(new IBlockModel.ParticleOnly("light", "minecraft:block/glass")))
            .drops(Drops.NONE)
            .register();

    private CoreLights() {
    }

    /** Declares the block, and hands the buses what the kit answers, once, from the Core's constructor. */
    public static void declare(final IEventBus modBus) {
        LightColours.listen(modBus);
        BeamLights.listen();
        // Loading the kinds' class declares their data registry before the server reads its data.
        LightKinds.declared();
    }
}
