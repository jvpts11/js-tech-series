/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import dev.jstech.core.JsCore;
import dev.jstech.core.capability.CapabilityKind;
import dev.jstech.core.capability.CapabilityKinds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The capability of machines whose heat is a budget ({@link IThermalBudget}). The mod that makes the machine offers
 * it while the capabilities are registered, on every block of the machine; whatever reads machines' heat asks here.
 *
 * <pre>{@code
 * ThermalBudgets.BUDGET.onBlockEntity(event, MY_COMPUTER.get(), (computer, face) -> computer.thermalBudget());
 * }</pre>
 */
public final class ThermalBudgets {

    /** A machine's heat as a budget, asked by face; a machine answers the same on every face. */
    public static final CapabilityKind<IThermalBudget> BUDGET = CapabilityKinds.declare(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "thermal_budget"), IThermalBudget.class);

    private ThermalBudgets() {
    }

    /** The thermal budget of the block at {@code pos} as its {@code face} gives it; none for one that keeps none. */
    public static @Nullable IThermalBudget at(final Level level, final BlockPos pos, final @Nullable Direction face) {
        return BUDGET.at(level, pos, face);
    }
}
