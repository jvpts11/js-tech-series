/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.chain.machine.ChainMachines.Worked;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.core.blockentity.FieldFluidTank;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.core.multiblock.PatternById;
import dev.jstech.core.multiblock.ShapeMatch;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A chain machine at work (computers/docs/design/survival.md): the terminal of one machine, a processing machine of
 * J's Core that works its recipes once its shape stands whole round it, drawing its watts at low voltage (the Coke
 * Oven draws nothing). While its shape stands, its entries draw nothing and its formed model draws the whole machine;
 * what it is working on is kept for the players' games, which draw the machine showing it. A Lithography Station from
 * the Legacy on works only linked to a computer of the era before ({@link LithoLink}).
 */
public class ChainMachineBlockEntity extends ProcessingMachineBlockEntity {

    private final ChainMachine machine;
    /* What it works on now, by id: what goes in, what comes out, the liquids and gases it takes. */
    private final ValueField<List<ResourceLocation>> takes;
    private final ValueField<List<ResourceLocation>> makes;
    private final ValueField<List<ResourceLocation>> flows;
    /* The entries it told to draw nothing while its shape stands. */
    private final List<BlockPos> flagged = new ArrayList<>();
    private final LithoLink link;
    /* Whether its shape stood at the last check, so its forming is told the advancements once. */
    private boolean stood;

    /* Its store holds this many seconds of its draw, and takes twice a tick's draw at once. */
    private static final long STORE_SECONDS = 4;

    public ChainMachineBlockEntity(final ChainMachine machine, final BlockPos pos, final BlockState state) {
        super(ChainMachines.typeOf(machine), pos, state, machine.layout(),
                MachineEnergy.of(JsEnergy.ENERGY, JsEnergy.LOW, Math.max(1L, machine.watts() * STORE_SECONDS),
                        Math.max(1L, machine.joulesATick() * 2)), machine.joulesATick());
        this.machine = machine;
        this.takes = fields().value("Takes", ResourceLocation.CODEC.listOf(), List.<ResourceLocation>of()).toClient();
        this.makes = fields().value("Makes", ResourceLocation.CODEC.listOf(), List.<ResourceLocation>of()).toClient();
        this.flows = fields().value("Flows", ResourceLocation.CODEC.listOf(), List.<ResourceLocation>of()).toClient();
        this.link = new LithoLink(machine);
        fields().mirror(ChainFormedBlock.FORMED, this::formed);
        fields().whenBroken((level, at) -> unflag(level));
        formsWith(MachineStructure.of(new PatternById(machine.patternId())).merges());
    }

    /** The machine it runs. */
    public ChainMachine machine() {
        return machine;
    }

    /** Whether its shape stands whole round it. */
    public boolean formed() {
        final MachineStructure structure = structure();
        return structure != null && structure.formed();
    }

    /** What goes in to what it works on now, by item id; empty before it ever worked. */
    public List<ResourceLocation> takes() {
        return takes.get();
    }

    /** What comes out of what it works on now, by item id. */
    public List<ResourceLocation> makes() {
        return makes.get();
    }

    /** The liquids and gases what it works on now takes, by the liquid's or the material's id. */
    public List<ResourceLocation> flows() {
        return flows.get();
    }

    /* Every machine but the Coke Oven, unless the server lets the computers and the machines run free. */
    @Override
    public boolean takesEnergy() {
        return machine.takesEnergy() && ComputersServerConfig.drawEnergy();
    }

    /*
     * What its inputs make by its kinds of recipe, the first kind that makes anything, at that kind's draw when its
     * recipe says none; a station needs its computer.
     */
    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        for (final Worked worked : ChainMachines.workedBy(machine)) {
            final Optional<Processing> found = byKind(level, worked.kind(), input);
            if (found.isPresent()) {
                final Processing drawn = found.get().energyPerTick() == 0 && worked.joulesATick() > 0
                        ? found.get().withEnergyPerTick(worked.joulesATick()) : found.get();
                return run(level, input, drawn, worked.era());
            }
        }
        return Optional.empty();
    }

    /* Its shape was checked again: its entries draw nothing while it stands, and themselves again once apart. */
    @Override
    protected void structureChecked(final ServerLevel level) {
        final Optional<ShapeMatch.Formed> formed = formed() ? structure().structure() : Optional.empty();
        if (formed.isEmpty()) {
            stood = false;
            unflag(level);
            return;
        }
        if (!stood) {
            stood = true;
            JscEvents.awardOperator(this, JscEvents.CHAIN_FORMED, ChainMachines.familyOf(machine));
        }
        flagged.clear();
        for (final long at : formed.get().parts()) {
            final BlockPos pos = BlockPos.of(at);
            final BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ChainFormedBlock) {
                flagged.add(pos);
                if (!state.getValue(ChainFormedBlock.FORMED)) {
                    level.setBlock(pos, state.setValue(ChainFormedBlock.FORMED, true), Block.UPDATE_ALL);
                }
            }
        }
    }

    /* What it made is told the advancements, which count each machine's first. */
    @Override
    protected void madeThings(final int count, final long gameTime) {
        super.madeThings(count, gameTime);
        JscEvents.awardOperator(this, JscEvents.CHAIN_MADE, ChainMachines.familyOf(machine));
    }

    /*
     * {@code work} on {@code input} if the machine may run it, a station of {@code era} only linked to its computer,
     * what it takes and makes then kept for the players' games.
     */
    protected Optional<Processing> run(final Level level, final ProcessingInput input, final Processing work,
                                       @Nullable final Era era) {
        final Optional<Processing> linked = link.allows(level, this, work, era);
        linked.ifPresent(allowed -> shows(input, allowed));
        return linked;
    }

    /** Whether {@code a} and {@code b} hold the same stacks, place for place, counts and components alike. */
    protected static boolean sameStacks(final List<ItemStack> a, final List<ItemStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.matches(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    /* Keeps what the work takes and makes, for the players' games, only when the work changed. */
    private void shows(final ProcessingInput input, final Processing work) {
        final List<ResourceLocation> in = new ArrayList<>();
        for (final Take take : work.takes()) {
            if (take.index() < input.items().size() && !input.items().get(take.index()).isEmpty()) {
                in.add(BuiltInRegistries.ITEM.getKey(input.items().get(take.index()).getItem()));
            }
        }
        final List<ResourceLocation> out = work.outputs().stream()
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem())).toList();
        final List<ResourceLocation> fluids = new ArrayList<>();
        for (final Take take : work.fluidTakes()) {
            final FieldFluidTank tank = inputTank(take.index());
            if (!tank.getFluid().isEmpty()) {
                fluids.add(BuiltInRegistries.FLUID.getKey(tank.getFluid().getFluid()));
            }
        }
        for (final MatterTank tank : matter().inputs(MatterState.GAS)) {
            for (int i = 0; i < tank.tanks(); i++) {
                tank.stack(i).ifPresent(stack -> fluids.add(stack.material().id()));
            }
        }
        if (!in.equals(takes.get())) {
            takes.set(in);
        }
        if (!out.equals(makes.get())) {
            makes.set(out);
        }
        if (!fluids.equals(flows.get())) {
            flows.set(fluids);
        }
    }

    private void unflag(final Level level) {
        for (final BlockPos pos : flagged) {
            final BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ChainFormedBlock && state.getValue(ChainFormedBlock.FORMED)) {
                level.setBlock(pos, state.setValue(ChainFormedBlock.FORMED, false), Block.UPDATE_ALL);
            }
        }
        flagged.clear();
    }
}
