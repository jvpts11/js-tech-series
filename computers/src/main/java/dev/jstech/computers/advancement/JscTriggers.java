/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.advancement;

import dev.jstech.computers.JsComputers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The advancement criterion only this mod pays out: a computer booting into a system for the first time. Every other
 * advancement of the mod stands on the Core's event trigger, by the ids in {@link JscEvents}.
 */
public final class JscTriggers {

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, JsComputers.MODID);

    /** Booting a computer into a system (the Arch and Gentoo challenges use it). */
    public static final DeferredHolder<CriterionTrigger<?>, OsFirstBootTrigger> OS_FIRST_BOOT =
            TRIGGERS.register("os_first_boot", OsFirstBootTrigger::new);

    private JscTriggers() {
    }

    public static void register(final IEventBus modEventBus) {
        TRIGGERS.register(modEventBus);
    }
}
