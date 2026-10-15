/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.blockentity.IFieldPart;
import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * What the protection tab of a machine holds and does: a fuse slot and a battery slot.
 *
 * <p>On overvoltage the fuse blows, cuts the energy and shuts the machine down at once, with no damage; the machine stays
 * cut until a fuse that works is put in, or the slot is emptied. A fuse facing more classes than it breaks lets the arc
 * jump over it, and the machine suffers what it would with no fuse. A machine with no fuse is unprotected.
 *
 * <p>The battery is a UPS: it charges from the machine's store while the grid keeps that full, and powers the machine
 * when the grid goes down and the store runs below half. A battery is any item holding the machine's energy, or FE.
 *
 * <pre>{@code
 * store.guardedBy(protection);         // the store asks it first when a line of another class meets it
 * protection.tick(store);              // each tick, in the block entity: the UPS
 * }</pre>
 */
public final class EnergyProtection implements IFieldPart {

    private final ItemStackHandler slots;
    private final Runnable onChange;
    private boolean cut;

    /** The fuse slot and the battery slot. */
    public static final int FUSE = 0;
    public static final int BATTERY = 1;
    private static final String SLOTS = "protection";
    private static final String CUT = "protection_cut";

    /** The protection of a machine, which tells {@code onChange} when its slots or its state change. */
    public EnergyProtection(final Runnable onChange) {
        this.onChange = Objects.requireNonNull(onChange, "onChange");
        this.slots = new ItemStackHandler(2) {
            @Override
            public boolean isItemValid(final int slot, final ItemStack stack) {
                return slot == FUSE ? stack.getItem() instanceof IFuse : holdsEnergy(stack);
            }

            @Override
            public int getSlotLimit(final int slot) {
                return 1;
            }

            @Override
            protected void onContentsChanged(final int slot) {
                if (slot == FUSE && EnergyProtection.this.cut && !blownIn()) {
                    EnergyProtection.this.cut = false;
                }
                EnergyProtection.this.onChange.run();
            }
        };
    }

    /** Its two slots: the fuse and the battery. */
    public ItemStackHandler slots() {
        return this.slots;
    }

    /** Whether a blown fuse cuts the energy, so the machine is shut down. */
    public boolean cut() {
        return this.cut;
    }

    /**
     * What befalls the machine when a line {@code steps} classes above its port meets it with {@code what}, the fuse
     * standing between: one that breaks it blows, cutting the energy, and the machine suffers nothing; one that does
     * not lets the arc jump over it and blows as well.
     */
    public WrongClass guard(final WrongClass what, final int steps) {
        if (what != WrongClass.BURNS && what != WrongClass.ARC_FLASH) {
            return what;
        }
        final ItemStack held = this.slots.getStackInSlot(FUSE);
        if (!(held.getItem() instanceof IFuse fuse)) {
            return what;
        }
        final boolean breaks = steps <= fuse.breaks(held);
        this.slots.setStackInSlot(FUSE, fuse.blown(held));
        this.cut = true;
        this.onChange.run();
        return breaks ? WrongClass.NONE : what;
    }

    /**
     * The UPS, each tick: the battery charges from {@code store} while the store is full, and gives into it when the
     * store runs below half.
     */
    public void tick(final EnergyStore store) {
        final ItemStack held = this.slots.getStackInSlot(BATTERY);
        if (held.isEmpty()) {
            return;
        }
        final IEnergyPort own = held.getCapability(EnergyPorts.ITEM);
        if (own != null && own.energy() == store.energy()) {
            work(store, own);
            return;
        }
        final IEnergyStorage fe = held.getCapability(Capabilities.EnergyStorage.ITEM);
        if (fe != null) {
            work(store, FeBattery.of(fe, store.energy()));
        }
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.put(SLOTS, this.slots.serializeNBT(registries));
        tag.putBoolean(CUT, this.cut);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        if (tag.contains(SLOTS)) {
            this.slots.deserializeNBT(registries, tag.getCompound(SLOTS));
        }
        this.cut = tag.getBoolean(CUT);
    }

    /* Whether what the fuse slot holds is a blown fuse rather than one that works or nothing. */
    private boolean blownIn() {
        final ItemStack held = this.slots.getStackInSlot(FUSE);
        return !held.isEmpty() && !(held.getItem() instanceof IFuse);
    }

    private static boolean holdsEnergy(final ItemStack stack) {
        return stack.getCapability(EnergyPorts.ITEM) != null
                || stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
    }

    private static void work(final EnergyStore store, final IEnergyPort battery) {
        if (store.stored() >= store.capacity()) {
            final long room = battery.receive(store.capacity(), true);
            battery.receive(store.spend(room), false);
        } else if (store.stored() < store.capacity() / 2) {
            final long wanted = store.capacity() - store.stored();
            store.generate(battery.extract(wanted, false));
        }
    }

    /* An FE battery seen in the store's energy, through its ratio, rounding down so no energy is made. */
    private record FeBattery(IEnergyStorage fe, Energy energy) implements IEnergyPort {

        private static FeBattery of(final IEnergyStorage fe, final Energy energy) {
            return new FeBattery(fe, energy);
        }

        @Override
        public EnergyClass portClass() {
            return this.energy.lowest();
        }

        @Override
        public long stored() {
            return this.energy.fromFe(this.fe.getEnergyStored());
        }

        @Override
        public long capacity() {
            return this.energy.fromFe(this.fe.getMaxEnergyStored());
        }

        @Override
        public boolean canReceive() {
            return this.fe.canReceive();
        }

        @Override
        public boolean canExtract() {
            return this.fe.canExtract();
        }

        @Override
        public long receive(final long amount, final boolean simulate) {
            final int fe = (int) Math.min(Integer.MAX_VALUE, this.energy.toFe(amount));
            return this.energy.fromFe(this.fe.receiveEnergy(fe, simulate));
        }

        @Override
        public long extract(final long amount, final boolean simulate) {
            final int fe = (int) Math.min(Integer.MAX_VALUE, this.energy.toFe(amount));
            return this.energy.fromFe(this.fe.extractEnergy(fe, simulate));
        }

        @Override
        public void meetWrongClass(final WrongClass what, final EnergyClass line, final long gameTime) {
        }
    }
}
