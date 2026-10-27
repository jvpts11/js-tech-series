/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.block.IEraChassisBlock;
import dev.jstech.computers.hardware.BayDriveSpec;
import dev.jstech.computers.hardware.CasePorts;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.cable.IPlugged;
import dev.jstech.core.blockentity.DerivedInt;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.model.network.ModelAnimations;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A small computer, one block: the Personal Computer, the Crafting Computer and the Cluster Management Computer. Its
 * case is drawn by its block entity, from the model its block names: the tower of its age, or in the later ages the
 * one of three cases it comes in; with the parts put in it, its lamps lit as it runs and works its disk, and its
 * fans turning while it runs.
 */
public abstract class AbstractSmallComputerBlockEntity extends AbstractComputerBlockEntity implements IPlugged {

    /** The left side taken off the case, showing what the player put inside; the world and the screen both show it. */
    private final BoolField sidePanelOff = fields().flag("SidePanelOff", false).save().toClient().toMenu();
    /**
     * The item in each hardware slot, air where there is none, told to the players who see the machine so they see
     * each part inside it. It is the hardware's own record, worked out again whenever the hardware changes or is read.
     */
    private final ValueField<List<ResourceLocation>> installed =
            fields().value("Installed", ResourceLocation.CODEC.listOf(), List.of()).toClient();
    /** The lamps on the front, worked out on the server: the power lamp's and the disk lamp's. */
    private final DerivedInt lamps =
            fields().derived("Lamps", () -> (isRunning() ? POWER_LAMP : 0) | (diskBusy() ? DISK_LAMP : 0)).toClient();
    /** What the case's bays and front ports hold, and the trays of the drives in its bays. */
    private final CaseFront front = new CaseFront(this, fields());
    /** The plug its control cables were last drawn ending in, on the client, so a card changed redraws them. */
    private String drawnPlug = "";

    /** The controller the power button's press plays on, and the press. */
    public static final String POWER = "power";
    public static final String PRESS = "animation.computer.press_power";
    private static final int POWER_LAMP = 1;
    private static final int DISK_LAMP = 2;

    protected AbstractSmallComputerBlockEntity(final BlockEntityType<?> type, final BlockPos pos,
                                               final BlockState state) {
        super(type, pos, state);
    }

    /** The front of the case: its bays, its USB ports and what they hold. */
    public CaseFront front() {
        return front;
    }

    /** How many USB ports the case has on its front: its era's. */
    public int casePorts() {
        return getBlockState().getBlock() instanceof IEraChassisBlock chassis ? CasePorts.of(chassis.chassisEra()) : 0;
    }

    /** Spills what the case's bays and ports hold, as the case breaks. */
    public void spillFront(final ServerLevel level, final BlockPos pos) {
        front.spill(level, pos);
    }

    /** The power button pressed on the front: the button goes in for whoever sees the case. */
    public void showPowerPressed() {
        ModelAnimations.replay(this, POWER, PRESS);
    }

    /** Whether the left side is off the case. */
    public boolean sidePanelOff() {
        return sidePanelOff.get();
    }

    /** Takes the left side off the case, or puts it back. */
    public void toggleSidePanel() {
        sidePanelOff.set(!sidePanelOff.get());
    }

    /** Which hardware slot takes which part: the board's, the processor's, the memory's, and on. */
    public ComputerHardwareLayout hardwareLayout() {
        return layout();
    }

    /** The id of the item in hardware slot {@code slot}, as the players who see the machine know it, or null. */
    public @Nullable ResourceLocation installedPart(final int slot) {
        final List<ResourceLocation> parts = installed.get();
        if (slot < 0 || slot >= parts.size()) {
            return null;
        }
        final ResourceLocation part = parts.get(slot);
        return part.equals(BuiltInRegistries.ITEM.getKey(Items.AIR)) ? null : part;
    }

    /**
     * The port a control cable plugs into on it: the connector of its first card that reads machines, as a PROFIBUS
     * card's violet D-sub; read off the parts the players who see it are told of, so it is the same on both sides.
     * Every other cable ends at it in its own plug.
     */
    @Override
    public String plugKind() {
        for (final ResourceLocation part : installed.get()) {
            final IExpansionCardSpec card = HardwareRegistry.card(new ItemStack(BuiltInRegistries.ITEM.get(part)));
            if (card != null && !card.controlPlug().isEmpty()) {
                return card.controlPlug();
            }
        }
        return "";
    }

    /** Whether the machine runs, as its power lamp shows it on every side. */
    public boolean visualRunning() {
        return (lamps.getAsInt() & POWER_LAMP) != 0;
    }

    /** Whether the machine worked its disk a moment ago, as its disk lamp shows it, blinking, on every side. */
    public boolean visualDiskBusy() {
        return (lamps.getAsInt() & DISK_LAMP) != 0;
    }

    /*
     * Every bay up to the last hot-swap one counts, a hot-swap bay or not, so a bay's disk slot keeps its number
     * whatever sits in the bays before it; a case with none adds no slot at all, and a bay past the last hot-swap
     * one (or one the case does not have) adds none either, so no slot shows that could never hold a disk.
     */
    @Override
    protected int frontDiskSlots() {
        for (int bay = CaseFront.BAYS - 1; bay >= 0; bay--) {
            if (front.kind(bay) == BayDriveSpec.Kind.HOT_SWAP) {
                return bay + 1;
            }
        }
        return 0;
    }

    @Override
    protected ItemStack frontDisk(final int bay) {
        return front.hotSwapDisk(bay);
    }

    /**
     * The disks the machine keeps its items on: those in its own disk slots, then those in its case's hot-swap bays,
     * empty where a slot holds none, so a disk's place in the list is its place in the machine.
     */
    protected List<ItemStack> storeDisks() {
        final ComputerHardwareLayout layout = hardwareLayout();
        final int front = frontDiskSlots();
        final List<ItemStack> disks = new ArrayList<>(layout.diskCount() + front);
        for (int i = 0; i < layout.diskCount(); i++) {
            disks.add(getHardware().getStackInSlot(layout.diskStart() + i));
        }
        for (int bay = 0; bay < front; bay++) {
            disks.add(frontDisk(bay));
        }
        return disks;
    }

    @Override
    protected void putFrontDisk(final int bay, final ItemStack disk) {
        front.putHotSwapDisk(bay, disk);
    }

    @Override
    protected void beforeClientUpdate() {
        super.beforeClientUpdate();
        front.beforeClientUpdate();
    }

    /*
     * A card that reads machines put in or taken out changes the plug its control cables end in, which they choose as
     * they are drawn: its block is drawn again, and the cable blocks around it with it.
     */
    @Override
    protected void afterClientUpdate() {
        super.afterClientUpdate();
        front.afterClientUpdate();
        final String plug = plugKind();
        if (!plug.equals(drawnPlug) && level != null) {
            drawnPlug = plug;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void partsChanged() {
        final ItemStackHandler slots = getHardware();
        final List<ResourceLocation> parts = new ArrayList<>(slots.getSlots());
        for (int slot = 0; slot < slots.getSlots(); slot++) {
            parts.add(BuiltInRegistries.ITEM.getKey(slots.getStackInSlot(slot).getItem()));
        }
        final List<ResourceLocation> before = installed.get();
        installed.set(List.copyOf(parts));
        // A part taken out of a bay, or swapped for another, lets fall what it held.
        front.partsChanged(before, installed.get());
    }
}
