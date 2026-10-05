/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.advancement;

import dev.jstech.core.JsCore;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The advancement triggers the Core gives every mod: an event by its id, and a step reached along an axis. */
public final class CoreTriggers {

    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, JsCore.MODID);

    /** {@code jscore:event}: see {@link EventTrigger}. */
    public static final DeferredHolder<CriterionTrigger<?>, EventTrigger> EVENT =
            TRIGGERS.register("event", EventTrigger::new);
    /** {@code jscore:axis_step}: see {@link AxisStepTrigger}. */
    public static final DeferredHolder<CriterionTrigger<?>, AxisStepTrigger> AXIS_STEP =
            TRIGGERS.register("axis_step", AxisStepTrigger::new);

    private CoreTriggers() {
    }

    /** Registers the triggers, from the Core's constructor. */
    public static void register(final IEventBus modEventBus) {
        TRIGGERS.register(modEventBus);
    }
}
