/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.computers.advancement.HardwareMilestones;
import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.machine.ProcessingInput;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A machine a wafer goes through on its way to chips (computers/docs/design/survival.md, "How a chip is made"): the
 * Lithography Stations, the Plasma Etcher, the Ion Implanter, the Deposition Chamber and the Packaging Line. Holding a
 * wafer for its step, it works the step ({@link WaferSteps}); otherwise it works its recipes as any chain machine. It
 * keeps the layers its dopant dust or its tin still has in them, the Ion Implanter's dust doping eight layers and the
 * Advanced station's tin nugget giving the droplets of four, counted down as each layer is done.
 */
public class WaferMachineBlockEntity extends ChainMachineBlockEntity {

    private final IntField layersLeft;
    /* What the count of layers left becomes when the work it was worked out for is done. */
    private int afterWork;
    /* The era of the wafer's step it works, or null while it works a recipe, a Vintage station's mask among them. */
    private @Nullable Era stepEra;

    public WaferMachineBlockEntity(final ChainMachine machine, final BlockPos pos, final BlockState state) {
        super(machine, pos, state);
        this.layersLeft = fields().integer("LayersLeft", 0);
    }

    /** The layers its dopant dust or its tin is still good for. */
    public int layersLeft() {
        return layersLeft.get();
    }

    /* A wafer's step when one is held for it; its recipes otherwise. */
    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        final Optional<WaferSteps.Step> step = WaferSteps.step(machine(), ChainMachines.workedBy(machine()), input,
                layersLeft.get());
        if (step.isEmpty()) {
            afterWork = layersLeft.get();
            stepEra = null;
            return super.process(level, input);
        }
        afterWork = step.get().layersLeft();
        stepEra = step.get().era();
        return run(level, input, step.get().work(), step.get().era());
    }

    /*
     * A layer done spends its share of the dust or the tin. A station's layer is told the advancements by the era it
     * was exposed for, and a mask it wrote as a mask.
     */
    @Override
    protected void madeThings(final int count, final long gameTime) {
        super.madeThings(count, gameTime);
        if (layersLeft.get() != afterWork) {
            layersLeft.set(afterWork);
        }
        if (!ChainMachines.LITHOGRAPHY.equals(ChainMachines.familyOf(machine()))) {
            return;
        }
        if (stepEra != null) {
            JscEvents.awardOperator(this, JscEvents.LAYER_EXPOSED, HardwareMilestones.eraDetail(stepEra));
        } else if (makes().stream().anyMatch(made -> made.getPath().startsWith("mask_"))) {
            JscEvents.awardOperator(this, JscEvents.MASK_WRITTEN);
        }
    }
}
