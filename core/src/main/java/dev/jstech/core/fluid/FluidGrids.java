/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fluid;

import dev.jstech.core.JsCore;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.GridPlace;
import dev.jstech.core.registry.CoreAttachments;
import java.util.OptionalLong;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Where a dimension's fluid grid is found, and the tick that moves its fluids, after the machines have ticked.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class FluidGrids {

    private FluidGrids() {
    }

    /** How the fluid grid of {@code level} moves fluids. */
    public static LevelFluids of(final ServerLevel level) {
        return level.getData(CoreAttachments.FLUIDS);
    }

    /** How many millibuckets the part of the fluid grid the pipe at {@code place} is in moved in the last tick. */
    public static OptionalLong movedLastTickAt(final ServerLevel level, final GridPlace place) {
        final OptionalLong number = CoreGrids.places(level).find(place);
        return number.isEmpty() ? OptionalLong.empty() : of(level).movedLastTickOf(number.getAsLong());
    }

    @SubscribeEvent
    public static void afterTick(final LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            of(level).tick(level);
        }
    }
}
