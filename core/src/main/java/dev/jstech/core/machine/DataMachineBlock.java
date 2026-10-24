/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * The terminal block of a multiblock machine declared as data ({@link DataMachines}): a device whose block entity,
 * a {@link DataMachineBlockEntity}, takes its shape, its slots, its energy and its recipes from the machine file the
 * block names. One block for each machine, its look its own; the machine itself is the file.
 *
 * <pre>{@code
 * CONTENT.block("coke_oven", properties -> new DataMachineBlock(properties, Device.of(() -> MyContent.OVEN.get())
 *         .ticks(MachineBlockEntity::serverTick).opensMenu(MachineMenu::new), rl("coke_oven")))
 * }</pre>
 */
public class DataMachineBlock extends DeviceBlock {

    private final ResourceLocation machine;
    private final MapCodec<DataMachineBlock> codec;

    public DataMachineBlock(final Properties properties, final Device<?> device, final ResourceLocation machine) {
        super(properties, device);
        this.machine = Objects.requireNonNull(machine, "machine");
        this.codec = simpleCodec(made -> new DataMachineBlock(made, device, machine));
    }

    /** The id of the machine file it is the terminal of. */
    public ResourceLocation machine() {
        return this.machine;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return this.codec;
    }
}
