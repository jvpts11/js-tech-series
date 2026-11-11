/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.create;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import dev.jstech.core.machine.IMachineParameters;
import dev.jstech.core.machine.MachineParameter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * A Create block's kinetic values, as the industrial control cable reads them (computers/docs/design/
 * implementation.md, "Create"): its speed, the stress its kinetic network carries and the capacity it has; and on a
 * Rotation Speed Controller the speed it aims at, which a computer sets. A clutch or a gearshift is no machine here: a
 * Redstone Interface works them.
 */
final class KineticParameters implements IMachineParameters {

    private final List<MachineParameter> parameters;

    /* The fastest Create lets anything turn, either way, and more stress than a network ever carries. */
    private static final double MOST_SPEED = 256.0;
    private static final double MOST_STRESS = 1.0e12;

    private KineticParameters(final KineticBlockEntity block) {
        final List<MachineParameter> list = new ArrayList<>();
        list.add(MachineParameter.read(KineticTable.SPEED, CreateTexts.SPEED, CreateTexts.RPM, -MOST_SPEED,
                MOST_SPEED, block::getSpeed));
        list.add(MachineParameter.read(KineticTable.STRESS, CreateTexts.STRESS, CreateTexts.STRESS_UNITS, 0.0,
                MOST_STRESS, () -> block.hasNetwork() ? block.getOrCreateNetwork().calculateStress() : 0.0));
        list.add(MachineParameter.read(KineticTable.CAPACITY, CreateTexts.CAPACITY, CreateTexts.STRESS_UNITS, 0.0,
                MOST_STRESS, () -> block.hasNetwork() ? block.getOrCreateNetwork().calculateCapacity() : 0.0));
        if (block instanceof SpeedControllerBlockEntity controller) {
            list.add(MachineParameter.set(KineticTable.TARGET_SPEED, CreateTexts.TARGET_SPEED, CreateTexts.RPM,
                    -MOST_SPEED, MOST_SPEED, () -> controller.targetSpeed.getValue(),
                    to -> controller.targetSpeed.setValue((int) Math.round(to))));
        }
        this.parameters = List.copyOf(list);
    }

    /** The values of {@code block} when it turns with a kinetic network; none for any other block entity. */
    @Nullable
    static IMachineParameters of(final BlockEntity block) {
        return block instanceof KineticBlockEntity kinetic ? new KineticParameters(kinetic) : null;
    }

    @Override
    public List<MachineParameter> parameters() {
        return parameters;
    }
}
