/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.JsCore;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

/**
 * The values a machine says can be read and set from outside, offered by {@link #CAPABILITY}: how J's Industrial's
 * Industrial Controller Computer and J's Computers' network command any machine of any mod, and what the network's
 * tables show, a row for each machine and a column for each value.
 */
public interface IMachineParameters {

    /** A machine's declared values, on any face. */
    BlockCapability<IMachineParameters, @Nullable Direction> CAPABILITY = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "machine_parameters"), IMachineParameters.class);

    /** Every value it declares, in the order it declares them. */
    List<MachineParameter> parameters();

    /** The value named {@code id}; none for a name it does not declare. */
    default Optional<MachineParameter> parameter(final String id) {
        return parameters().stream().filter(parameter -> parameter.id().equals(id)).findFirst();
    }

    /** What the value named {@code id} is now; none for a name it does not declare. */
    default OptionalDouble read(final String id) {
        return parameter(id).map(parameter -> OptionalDouble.of(parameter.value())).orElse(OptionalDouble.empty());
    }

    /** Sets the value named {@code id} to {@code to}, held to its span; whether it could be set. */
    default boolean write(final String id, final double to) {
        return parameter(id).map(parameter -> parameter.setTo(to)).orElse(false);
    }

    /** The state it is in now, as its lamp shows it; none for a block that is no machine, a valve or a tank. */
    default Optional<MachineStatus> statusNow() {
        return Optional.empty();
    }

    /** What it does with a redstone signal; none for a block that does nothing with one. */
    default Optional<RedstoneMode> redstoneMode() {
        return Optional.empty();
    }

    /** Sets what it does with a redstone signal; whether it could. */
    default boolean setRedstoneMode(final RedstoneMode mode) {
        return false;
    }

    /** What it draws of its energy now, and through what port; none for a block that draws none. */
    default Optional<PowerDrawn> powerDrawn() {
        return Optional.empty();
    }
}
