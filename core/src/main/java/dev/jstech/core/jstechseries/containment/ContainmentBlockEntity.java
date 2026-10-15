/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.containment;

import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.energy.EnergyFaults;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.EnergyTooltips;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.look.IDescribed;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.text.Text;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * A containment of exotic matter, or the Antimatter Trap: it holds one material while its low voltage line of J's
 * Energy pays its upkeep each tick. Without energy it takes nothing more and lets nothing out, keeping what it holds
 * sealed until the energy comes back; broken, it keeps it in the item it drops, and placed again it holds it again.
 */
public final class ContainmentBlockEntity extends SyncedBlockEntity implements IDescribed {

    private final ExoticContainment tank;
    private final EnergyStore store;
    private final PartField matterField;
    private final PartField energyField;

    public ContainmentBlockEntity(final BlockPos pos, final BlockState state) {
        super(SharedContainment.CONTAINMENT_BE.get(), pos, state);
        final boolean trap = state.getBlock() instanceof ContainmentBlock block && block.antimatter();
        this.tank = new ExoticContainment(trap, SharedContainment.VOLUME, trap ? SharedContainment.TRAP_UPKEEP
                : SharedContainment.UPKEEP, this::matterChanged);
        this.matterField = fields().part("Matter", new MatterPart()).save();
        this.store = new EnergyStore("Energy", JsEnergy.ENERGY, JsEnergy.LOW, SharedContainment.STORE,
                SharedContainment.TAKES, 0L, this::energyChanged);
        this.energyField = fields().part("Energy", this.store).save();
        this.store.onFault(what -> {
            if (this.level instanceof ServerLevel server) {
                EnergyFaults.strike(server, this.worldPosition, what);
            }
        });
    }

    /** Pays the tick's upkeep from its store, on the server. */
    public void tick() {
        this.tank.tick((amount, simulate) -> simulate ? Math.min(amount, this.store.stored())
                : this.store.spend(amount));
    }

    /** What it holds, offered to the lines of exotic matter on every side. */
    public IMatterHandler matter() {
        return this.tank;
    }

    /** Its containment. */
    public ExoticContainment containment() {
        return this.tank;
    }

    /** Its energy store, which pays its upkeep. */
    public EnergyStore store() {
        return this.store;
    }

    /** What the game's FE capability sees of it: its store, while the series speaks FE; nothing otherwise. */
    public @Nullable IEnergyStorage fe() {
        return JsEnergy.speaksFe() ? this.store.asFe() : null;
    }

    @Override
    public void describe(final List<Text> lines, final boolean details) {
        final Optional<MatterStack> held = this.tank.held();
        lines.add(held.map(SharedContainment::holds).orElseGet(SharedContainment.EMPTY::text));
        lines.add(SharedContainment.UPKEEP_LINE.with(JsEnergy.ENERGY.rate(this.tank.upkeep())));
        if (!this.tank.powered()) {
            lines.add(SharedContainment.SEALED.text());
        }
        lines.addAll(EnergyTooltips.store(this.store, this.level == null ? 0L : this.level.getGameTime()));
    }

    /* What it holds goes into its item, which the loot table copies from the block entity. */
    @Override
    protected void collectImplicitComponents(final DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        this.tank.held().ifPresent(held -> components.set(SharedContainment.CONTAINED.get(), held));
    }

    /* A containment placed from an item that kept its matter holds it again. */
    @Override
    protected void applyImplicitComponents(final DataComponentInput input) {
        super.applyImplicitComponents(input);
        final MatterStack kept = input.get(SharedContainment.CONTAINED.get());
        if (kept != null) {
            this.tank.set(kept);
        }
    }

    private void matterChanged() {
        this.matterField.changed();
    }

    private void energyChanged() {
        this.energyField.changed();
    }

    /* What it holds, saved with the block. */
    private final class MatterPart implements IFieldPart {

        @Override
        public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
            tank.save(tag);
        }

        @Override
        public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
            tank.load(tag);
        }
    }
}
