/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block.part;

import dev.jstech.computers.block.DataWires;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.chemical.ChemicalBridges;
import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.multipart.IPartHost;
import dev.jstech.core.util.Utf8Text;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Shared base for the two bus parts (Import, Export): both carry the same configuration surface (an optional
 * name (so a query can address the bus by it), a single ghost filter slot, a min/max stock window with
 * hysteresis, and a continuous/redstone mode) over a data cable face. Only the per-tick transfer direction
 * differs, which each subclass supplies in {@link #serverTick()}.
 */
public abstract sealed class AbstractBusPart implements IFacePart permits ImportBusPart, ExportBusPart {

    public static final int MODE_CONTINUOUS = 0;
    public static final int MODE_REDSTONE = 1;

    /** The longest name a bus may carry; keeps the name field and any query reference bounded. */
    public static final int MAX_NAME_LENGTH = 32;

    protected CableBlockEntity host;
    protected Direction face = Direction.NORTH;

    protected final ItemStackHandler filter = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(final int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(final int slot) {
            markHostChanged();
        }
    };

    protected int min;
    protected int max;
    protected int mode = MODE_CONTINUOUS;
    protected boolean linked;
    protected boolean active = true;
    protected String name = "";

    @Override
    public void attach(final IPartHost host, final Direction face) {
        if (!(host instanceof CableBlockEntity cable)) {
            throw new IllegalArgumentException("a bus mounts on a cable block, not on " + host);
        }
        this.host = cable;
        this.face = face;
    }

    /*
     * Using the bus opens its configuration menu; picking it off the cable is a left-click, handled where the cable
     * block breaks, so it never breaks the cable. The open packet carries the bus's name so the field shows it.
     */
    @Override
    public boolean use(final ServerPlayer player) {
        if (this.host == null) {
            return false;
        }
        final CableBlockEntity cable = this.host;
        final Direction mounted = this.face;
        player.openMenu(new SimpleMenuProvider((id, inventory, opener) -> createMenu(id, inventory, cable, mounted),
                        partItem().getHoverName()),
                buffer -> {
                    buffer.writeBlockPos(cable.getBlockPos());
                    buffer.writeByte(mounted.get3DDataValue());
                    buffer.writeUtf(this.name);
                });
        return true;
    }

    /** Builds this bus's configuration menu, each bus its own, so the cable stays generic. */
    public abstract AbstractContainerMenu createMenu(int containerId, Inventory inventory,
                                                     CableBlockEntity cable, Direction mountedFace);

    public ItemStackHandler getFilterHandler() {
        return filter;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public int mode() {
        return mode;
    }

    public boolean linked() {
        return linked;
    }

    public String name() {
        return name;
    }

    /** Sets the bus name, trimmed and cut to fit; an empty name means the bus is unaddressable by query. */
    public void setName(final String newName) {
        this.name = Utf8Text.field(newName, MAX_NAME_LENGTH);
        markHostChanged();
    }

    public void setFilter(final ItemStack stack) {
        filter.setStackInSlot(0, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        active = true;
        markHostChanged();
    }

    public void adjustMin(final int delta) {
        min = Math.max(0, min + delta);
        active = true;
        markHostChanged();
    }

    public void adjustMax(final int delta) {
        max = Math.max(0, max + delta);
        active = true;
        markHostChanged();
    }

    public void toggleMode() {
        mode = mode == MODE_CONTINUOUS ? MODE_REDSTONE : MODE_CONTINUOUS;
        markHostChanged();
    }

    public Item filterItem() {
        return filter.getStackInSlot(0).isEmpty() ? Items.AIR : filter.getStackInSlot(0).getItem();
    }

    /**
     * The storage key the filter selects, or {@code null} for an empty filter. A fluid container in the slot
     * (e.g. a filled bucket) selects its FLUID, and an item carrying a chemical (a filled tank item, a
     * hohlraum) selects that CHEMICAL, so a bus can target any kind of data the same way it targets an item.
     */
    @Nullable
    public StorageKey filterKey() {
        final ItemStack stack = filter.getStackInSlot(0);
        if (stack.isEmpty()) {
            return null;
        }
        return FluidUtil.getFluidContained(stack)
                .filter(f -> !f.isEmpty())
                .map(StorageKey::of)
                .or(() -> ChemicalBridges.chemicalOf(stack)
                        .map(StorageKey::chemical))
                .orElseGet(() -> StorageKey.of(stack));
    }

    /** Whether a redstone-mode bus is currently held off because its block has no neighbor signal. */
    protected boolean redstoneBlocked() {
        final ServerLevel level = serverLevel();
        return mode == MODE_REDSTONE && level != null && !level.hasNeighborSignal(host.getBlockPos());
    }

    /** The server level the bus's cable is in, or null on a player's game or before it is placed. */
    protected @Nullable ServerLevel serverLevel() {
        return host == null ? null : host.partServerLevel();
    }

    /** The network the bus's cable carries, or null when it is on none. */
    protected @Nullable NetworkUuid network() {
        return host == null || serverLevel() == null ? null : DataWires.networkOf(host);
    }

    /** The Mainframe of the network the bus's cable carries, or null when there is none. */
    protected @Nullable MainframeBlockEntity mainframe() {
        final ServerLevel level = serverLevel();
        return level == null ? null : DataWires.mainframeOf(level, network());
    }

    /** Every kind of data the block the bus faces holds: a bus moves whatever is there. */
    protected ExternalDataPort neighborPort() {
        final ServerLevel level = serverLevel();
        if (level == null) {
            return new ExternalDataPort(null, null);
        }
        return ExternalDataPort.at(level, host.getBlockPos().relative(face), face.getOpposite());
    }

    protected void markHostChanged() {
        if (host != null) {
            host.setChanged();
        }
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.put("Filter", filter.serializeNBT(registries));
        tag.putInt("Min", min);
        tag.putInt("Max", max);
        tag.putInt("Mode", mode);
        // Persist the hysteresis gate so a reload does not forget a min/max hold and over-push/pull one batch.
        tag.putBoolean("Active", active);
        if (!name.isEmpty()) {
            tag.putString("Name", name);
        }
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        filter.deserializeNBT(registries, tag.getCompound("Filter"));
        min = tag.getInt("Min");
        max = tag.getInt("Max");
        mode = tag.getInt("Mode");
        // Default to active when the key is absent (old saves), matching the field initializer.
        active = !tag.contains("Active") || tag.getBoolean("Active");
        name = tag.getString("Name");
    }
}
