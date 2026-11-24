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
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.machine.ProcessingInput;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A machine a wafer goes through on its way to chips (computers/docs/design/survival.md, "How a chip is made"): the
 * Lithography Stations, the Plasma Etcher, the Ion Implanter, the Deposition Chamber and the Packaging Line. Holding a
 * wafer for its step, it works the step ({@link WaferSteps}); otherwise it works its recipes as any chain machine. It
 * keeps the layers its dopant dust or its tin still has in them, the Ion Implanter's dust doping eight layers and the
 * Advanced station's tin nugget giving the droplets of four, counted down as each layer is done. A station from the
 * Legacy on writes the mask its computer's lithography program picked on the Mask Blank it holds ({@link MaskWriting})
 * while it exposes no wafer, and sets the mask where its blank was.
 */
public class WaferMachineBlockEntity extends ChainMachineBlockEntity {

    private final IntField layersLeft;
    /* The mask the lithography program asked for, kept till it is written; none when none was asked for. */
    private final ValueField<Item> writes;
    /* What the count of layers left becomes when the work it was worked out for is done. */
    private int afterWork;
    /* The era of the wafer's step it works, or null while it works a recipe, a Vintage station's mask among them. */
    private @Nullable Era stepEra;
    /* Where the wafer of the step it works stands once the step is done: its chip, its layer and its layers. */
    private @Nullable WaferWork stepWork;
    /* The mask it is writing, while the work it works is that. */
    private MaskWriting.@Nullable Writing writing;

    public WaferMachineBlockEntity(final ChainMachine machine, final BlockPos pos, final BlockState state) {
        super(machine, pos, state);
        this.layersLeft = fields().integer("LayersLeft", 0);
        this.writes = fields().nullable("Writes", BuiltInRegistries.ITEM.byNameCodec()).save();
    }

    /** The layers its dopant dust or its tin is still good for. */
    public int layersLeft() {
        return layersLeft.get();
    }

    /** Whether it is exposing a wafer's layer, part of the way through it. */
    public boolean exposing() {
        return stepEra != null && getProgress() > 0;
    }

    /** The wafer it exposes as it will leave: its chip, the layer exposed and its layers; none when it exposes none. */
    public Optional<WaferWork> exposure() {
        return exposing() ? Optional.ofNullable(stepWork) : Optional.empty();
    }

    /** The mask it is to write on the blank it holds, or null when none was asked for. */
    public @Nullable Item writes() {
        return writes.get();
    }

    /** What its input slots hold, the first first. */
    public List<ItemStack> inputs() {
        final List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < layout().inputs(); i++) {
            out.add(getInventory().getStackInSlot(layout().laneInput(0, i)));
        }
        return out;
    }

    /* A mask is a station's tool: it exposes every layer through it and never uses it up. */
    @Override
    public boolean usesAsTool(final ItemStack stack) {
        return WaferSteps.isMask(stack) || super.usesAsTool(stack);
    }

    /* A station is listed for the computers that look for theirs while it is in the world. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && machine().linked()) {
            LithoStations.loaded(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide()) {
            LithoStations.gone(this);
        }
    }

    /** Has it write {@code mask} on the blank it holds, as soon as it may ({@link MaskWriting#write}). */
    void write(final Item mask) {
        writes.set(mask);
    }

    /* A wafer's step when one is held for it; the mask asked for when its blank is; its recipes otherwise. */
    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        final Optional<WaferSteps.Step> step = WaferSteps.step(machine(), ChainMachines.workedBy(machine()), input,
                layersLeft.get(), level.getRecipeManager());
        if (step.isPresent()) {
            afterWork = step.get().layersLeft();
            stepEra = step.get().era();
            stepWork = step.get().work().outputs().isEmpty() ? null
                    : step.get().work().outputs().getFirst().get(ComputingComponents.WAFER_WORK.get());
            writing = null;
            return run(level, input, step.get().work(), step.get().era());
        }
        afterWork = layersLeft.get();
        stepEra = null;
        stepWork = null;
        writing = MaskWriting.work(machine(), input, writes.get()).orElse(null);
        if (writing != null) {
            return run(level, input, writing.work(), writing.era());
        }
        return super.process(level, input);
    }

    /*
     * A layer done spends its share of the dust or the tin. A station's layer is told the advancements by the era it
     * was exposed for, and a mask it wrote as a mask; a mask written goes where its blank was, to expose through, and
     * nothing more is written till the program asks again.
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
        if (writing != null) {
            setInPlace(writing);
            writes.set(null);
            writing = null;
        }
        if (stepEra != null) {
            JscEvents.awardOperator(this, JscEvents.LAYER_EXPOSED, HardwareMilestones.eraDetail(stepEra));
        } else if (makes().stream().anyMatch(made -> made.getPath().startsWith("mask_"))) {
            JscEvents.awardOperator(this, JscEvents.MASK_WRITTEN);
        }
    }

    /* The mask {@code written} made, moved from its outputs to the slot its blank stood in once that is empty. */
    private void setInPlace(final MaskWriting.Writing written) {
        final int slot = layout().laneInput(0, written.slot());
        if (!getInventory().getStackInSlot(slot).isEmpty()) {
            return;
        }
        for (int i = 0; i < layout().outputs(); i++) {
            final int out = layout().laneOutput(0, i);
            if (getInventory().getStackInSlot(out).is(written.mask())) {
                getInventory().setStackInSlot(slot, getInventory().extractItem(out, 1, false));
                return;
            }
        }
    }
}
